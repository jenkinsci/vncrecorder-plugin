/*
 * Copyright (c) 2014 Dimitri Tenenbaum All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR AND CONTRIBUTORS ``AS IS'' AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE AUTHOR OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
 * OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 */
package org.jenkinsci.plugins.vncrecorder.vncutil;

import java.io.PrintStream;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class VncProcess {

	static protected ExecutorService execServ = Executors.newFixedThreadPool(10);
	static protected ExecutorService terminatorServ = Executors.newFixedThreadPool(10);
	static protected ScheduledExecutorService splitCheckerExec = Executors.newSingleThreadScheduledExecutor();	
	static final protected String VNC2SWF_BINARY =  "vnc2swf";
	protected Process process;
	protected PrintStream loggerStream = new PrintStream(System.out);

	public VncProcess() {
		super();
	}

	protected Process executeProcess(String[] com) throws Exception,
	InterruptedException {
		loggerStream.println("Starting command " + Arrays.toString(com));
		process = new ProcessBuilder( com ).start();
//		if (!com[0].equalsIgnoreCase("kill"))
//			loggerStream.println("PID of command " + Arrays.toString(com) + " is: " + getUnixPID(process));
		return process;
	}

	protected int getUnixPID(Process process) throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException {  
		if (process.getClass().getName().equals("java.lang.UNIXProcess")) {  
			Class<? extends Process> proc = process.getClass();  
			Field field = proc.getDeclaredField("pid");  
			field.setAccessible(true);  
			Object pid = field.get(process);  
			return (Integer) pid;  
		} else {  
			throw new IllegalArgumentException("Not a UNIXProcess");  
		}  
	}

	public void setLoggingStream(PrintStream loggerStream)
	{
		this.loggerStream = loggerStream;
	}
	
	
	public static void shutdown() 
	{
//		execServ.shutdownNow();
//		terminatorServ.shutdownNow();
//		splitCheckerExec.shutdownNow();
	}  

}