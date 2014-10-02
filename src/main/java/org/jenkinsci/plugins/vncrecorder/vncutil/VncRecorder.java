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

import java.io.File;
import java.util.concurrent.Future;

public class VncRecorder extends VncProcess{
	private volatile static Future<Integer> recordingState;
/*
 * vnc2swf
 * xorg-x11-server-Xvfb
 * vncviewer
 * */

	public static void main(String[] args) throws Exception 
	{
		String targetFile = "/tmp/test2166.swf";
		String vncServ = "ci-loadgen1:14";
		VncRecorderCallable vn = new VncRecorderCallable(vncServ, targetFile, new File(System.getProperty("user.home"),".vnc" + File.separator + "passwd"),"vnc2swf");
		recordingState = execServ.submit(vn);
		Thread.sleep(15000);
		recordingState.cancel(true);		
	}

	public Future<Integer> record(String vncServ, String targetFile, File vncPassw, String vnc2swfPath) 
	{
		logger.info("Recording from server: " + vncServ + " into " + targetFile);
		VncRecorderCallable vn = new VncRecorderCallable(vncServ, targetFile,vncPassw,vnc2swfPath);
		recordingState = execServ.submit(vn);
		return recordingState;
	}
	
	public void stop() 
	{
		recordingState.cancel(true);
		shutdown();
	}
}
