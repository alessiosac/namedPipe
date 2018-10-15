package com.polito.net.createPipe;

import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.WinBase;
import com.sun.jna.platform.win32.WinError;
import com.sun.jna.platform.win32.WinDef.ULONGByReference;
import com.sun.jna.platform.win32.WinNT.HANDLE;
import com.sun.jna.ptr.IntByReference;

public class PipeFactory {
	
	public static void createNamedPipe(String pipeName) {
        HANDLE    hNamedPipe=Kernel32.INSTANCE.CreateNamedPipe(pipeName,
                WinBase.PIPE_ACCESS_DUPLEX,        // dwOpenMode
                WinBase.PIPE_TYPE_BYTE | WinBase.PIPE_READMODE_BYTE | WinBase.PIPE_WAIT,    // dwPipeMode
                1,    // nMaxInstances,
                Byte.MAX_VALUE,    // nOutBufferSize,
                Byte.MAX_VALUE,    // nInBufferSize,
                1000,    // nDefaultTimeOut,
                null);    // lpSecurityAttributes

        assertCallSucceeded("CreateNamedPipe", !WinBase.INVALID_HANDLE_VALUE.equals(hNamedPipe));
       
        try {
            IntByReference    lpFlags=new IntByReference(0);
            IntByReference    lpOutBuffferSize=new IntByReference(0);
            IntByReference    lpInBufferSize=new IntByReference(0);
                IntByReference    lpMaxInstances=new IntByReference(0);
                assertCallSucceeded("GetNamedPipeInfo",
                                    Kernel32.INSTANCE.GetNamedPipeInfo(hNamedPipe, lpFlags,
                                            lpOutBuffferSize, lpInBufferSize, lpMaxInstances));

                ULONGByReference    ServerProcessId=new ULONGByReference();
                assertCallSucceeded("GetNamedPipeServerProcessId", Kernel32.INSTANCE.GetNamedPipeServerProcessId(hNamedPipe, ServerProcessId));

                ULONGByReference ServerSessionId=new ULONGByReference();
                assertCallSucceeded("GetNamedPipeServerSessionId", Kernel32.INSTANCE.GetNamedPipeServerSessionId(hNamedPipe, ServerSessionId));

                assertCallSucceeded("DisconnectNamedPipe", Kernel32.INSTANCE.DisconnectNamedPipe(hNamedPipe));
        } finally {    // clean up
            assertCallSucceeded("Named pipe handle close", Kernel32.INSTANCE.CloseHandle(hNamedPipe));
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
}
