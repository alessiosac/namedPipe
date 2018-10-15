package com.polito.net.createPipe;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.logging.Logger;

/**
 * Hello world!
 *
 */
public class Client 
{
	//Pipe name should be identical to the name used by server.
		public static final String pipeName = "\\\\.\\pipe\\MyWindowsNamedPipe";
		
		public static void main(String[] args) {
			final Logger logger = Logger.getLogger("NamedPipe");
			//testMultiThreadedNamedPipe(logger);
			try {          
				PipeFactory.createNamedPipe(pipeName);
				
	            //Data to be transmitted over pipe.
	            final String pipeData = "Hello Pipe";
	   
	            final byte[] pipeDataBytes = pipeData.getBytes();
	   
	            //Open pipe with read / write access.
	            RandomAccessFile pipe = new RandomAccessFile(pipeName, "rw");
	   
	            try {
	                for (int i = 0; i < pipeData.length(); i++) {
	                    System.out.println("Writing data byte 0x" + 
	                                        Integer.toHexString(pipeDataBytes[i]) + 
	                                        " (" + ((char)pipeDataBytes[i]) + ") " +
	                                        "to pipe...");
	                    pipe.write(pipeDataBytes[i]);
	                    do {
	                        int rxData = pipe.read();
	                        System.out.println("Byte read from pipe: 0x" + 
	                                           Integer.toHexString(rxData) +
	                                           " (" + ((char)rxData) + ")");
	                    } while (pipe.length() > 0);
	                    System.out.println();
	                }
	    
	                //Send exit command.
	                pipe.write(0xFF);
	            } catch (IOException e) {
	                e.printStackTrace();
	            }
	      
	            //Close pipe connection.
	            pipe.close();
	        } catch (FileNotFoundException e) {
	            e.printStackTrace();
	        } catch (IOException e) {
	            e.printStackTrace();
	        } 
	        
	    }
}
