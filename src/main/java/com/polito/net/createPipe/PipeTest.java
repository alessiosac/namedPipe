package com.polito.net.createPipe;

import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.WinBase;
import com.sun.jna.platform.win32.WinError;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.platform.win32.WinNT.HANDLE;
import com.sun.jna.ptr.IntByReference;


public class PipeTest {
	
	public static void testMultiThreadedNamedPipe(final Logger logger, final String pipeName) {
		
        final int MAX_BUFFER_SIZE=1024;
        ExecutorService executors = Executors.newFixedThreadPool(2);
        try {
            Future<?> server = executors.submit(new Runnable() {
                    public void run() {
                        // based on https://msdn.microsoft.com/en-us/library/windows/desktop/aa365588(v=vs.85).aspx
                        HANDLE hNamedPipe = assertValidHandle("CreateNamedPipe", Kernel32.INSTANCE.CreateNamedPipe(pipeName,
                                WinBase.PIPE_ACCESS_DUPLEX,        // dwOpenMode
                                WinBase.PIPE_TYPE_MESSAGE | WinBase.PIPE_READMODE_MESSAGE | WinBase.PIPE_WAIT,    // dwPipeMode
                                1,    // nMaxInstances,
                                MAX_BUFFER_SIZE,    // nOutBufferSize,
                                MAX_BUFFER_SIZE,    // nInBufferSize,
                                (int) TimeUnit.SECONDS.toMillis(30L),    // nDefaultTimeOut,
                                null    // lpSecurityAttributes
                              ));

                        try {
                            logger.info("Await client connection");
                            assertCallSucceeded("ConnectNamedPipe", Kernel32.INSTANCE.ConnectNamedPipe(hNamedPipe, null));
                            logger.info("Client connected");

                            byte[] readBuffer = new byte[MAX_BUFFER_SIZE];
                            IntByReference lpNumberOfBytesRead = new IntByReference(0);
                            assertCallSucceeded("ReadFile", Kernel32.INSTANCE.ReadFile(hNamedPipe, readBuffer, readBuffer.length, lpNumberOfBytesRead, null));

                            int readSize = lpNumberOfBytesRead.getValue();
                            logger.info("Received client data - length=" + readSize);
                            //assertTrue("No data receieved from client", readSize > 0);

                            IntByReference lpNumberOfBytesWritten = new IntByReference(0);
                            assertCallSucceeded("WriteFile", Kernel32.INSTANCE.WriteFile(hNamedPipe, readBuffer, readSize, lpNumberOfBytesWritten, null));
                            logger.info("Echoed client data - length=" + lpNumberOfBytesWritten.getValue());
                            //assertEquals("Mismatched write buffer size", readSize, lpNumberOfBytesWritten.getValue());

                            // Flush the pipe to allow the client to read the pipe's contents before disconnecting
                            assertCallSucceeded("FlushFileBuffers", Kernel32.INSTANCE.FlushFileBuffers(hNamedPipe));
                            logger.info("Disconnecting");
                            assertCallSucceeded("DisconnectNamedPipe", Kernel32.INSTANCE.DisconnectNamedPipe(hNamedPipe));
                            logger.info("Disconnected");
                        } finally {    // clean up
                            assertCallSucceeded("Named pipe handle close", Kernel32.INSTANCE.CloseHandle(hNamedPipe));
                        }
                    }
                });
            logger.info("Started server - handle=" + server);

            Future<?> client = executors.submit(new Runnable() {
                    public void run() {
                        // based on https://msdn.microsoft.com/en-us/library/windows/desktop/aa365592(v=vs.85).aspx
                        assertCallSucceeded("WaitNamedPipe", Kernel32.INSTANCE.WaitNamedPipe(pipeName, (int) TimeUnit.SECONDS.toMillis(15L)));
                        logger.info("Connected to server");

                        HANDLE hPipe = assertValidHandle("CreateNamedPipe", Kernel32.INSTANCE.CreateFile(pipeName,
                                 WinNT.GENERIC_READ | WinNT.GENERIC_WRITE,
                                 0,                      // no sharing
                                 null,                   // default security attributes
                                 WinNT.OPEN_EXISTING,      // opens existing pipe
                                 0,                      // default attributes
                                 null                  // no template file
                               ));

                        try {
                            IntByReference lpMode = new IntByReference(WinBase.PIPE_READMODE_MESSAGE);
                            assertCallSucceeded("SetNamedPipeHandleState", Kernel32.INSTANCE.SetNamedPipeHandleState(hPipe, lpMode, null, null));

                            String expMessage = Thread.currentThread().getName() + " says hello";
                            byte[] expData = expMessage.getBytes();
                            IntByReference lpNumberOfBytesWritten = new IntByReference(0);
                            assertCallSucceeded("WriteFile", Kernel32.INSTANCE.WriteFile(hPipe, expData, expData.length, lpNumberOfBytesWritten, null));
                            logger.info("Sent hello message");
                            //assertEquals("Mismatched write buffer size", expData.length, lpNumberOfBytesWritten.getValue());

                            byte[] readBuffer = new byte[MAX_BUFFER_SIZE];
                            IntByReference lpNumberOfBytesRead = new IntByReference(0);
                            assertCallSucceeded("ReadFile", Kernel32.INSTANCE.ReadFile(hPipe, readBuffer, readBuffer.length, lpNumberOfBytesRead, null));

                            int readSize = lpNumberOfBytesRead.getValue();
                            logger.info("Received server data - length=" + readSize);
                            //assertTrue("No data receieved from server", readSize > 0);

                            String actMessage = new String(readBuffer, 0, readSize);
                            //assertEquals("Mismatched server data", expMessage, actMessage);
                        } finally {    // clean up
                            assertCallSucceeded("Named pipe handle close", Kernel32.INSTANCE.CloseHandle(hPipe));
                        }
                    }
                });
            logger.info("Started client - handle=" + client);

            for (Future<?> f : Arrays.asList(client, server)) {
                try {
                    f.get(30L, TimeUnit.SECONDS);
                    logger.info("Finished " + f);
                } catch(Exception e) {
                    logger.warning(e.getClass().getSimpleName() + " while await completion of " + f + ": " + e.getMessage());
                }
            }
        } finally {
            executors.shutdownNow();
        }
    }
	
	public static final void assertCallSucceeded(String message, boolean result) {
        if (result) {
            return;
        }

        int hr = Kernel32.INSTANCE.GetLastError();
        if (hr == WinError.ERROR_SUCCESS) {
            System.out.println(message + " failed with unknown reason code");
        } else {
        	System.out.println(message + " failed: hr=" + hr + " - 0x" + Integer.toHexString(hr));
        }
    }
	
	public static final HANDLE assertValidHandle(String message, HANDLE handle) {
        if ((handle == null) || WinBase.INVALID_HANDLE_VALUE.equals(handle)) {
            int hr = Kernel32.INSTANCE.GetLastError();
            if (hr == WinError.ERROR_SUCCESS) {
            	System.out.println(message + " failed with unknown reason code");
            } else {
            	System.out.println(message + " failed: hr=" + hr + " - 0x" + Integer.toHexString(hr));
            }
        }

        return handle;
    }

}
