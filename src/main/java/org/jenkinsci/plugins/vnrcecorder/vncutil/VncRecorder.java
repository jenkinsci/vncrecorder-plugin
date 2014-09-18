package org.jenkinsci.plugins.vnrcecorder.vncutil;

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
		VncRecorderCallable vn = new VncRecorderCallable(vncServ, targetFile, new File(System.getProperty("user.home"),".vnc" + File.separator + "passwd"));
		recordingState = execServ.submit(vn);
		Thread.sleep(15000);
		recordingState.cancel(true);		
	}

	public Future<Integer> record(String vncServ, String targetFile, File vncPassw) 
	{
		logger.info("Recording from server: " + vncServ + " into " + targetFile);
		VncRecorderCallable vn = new VncRecorderCallable(vncServ, targetFile,vncPassw);
		recordingState = execServ.submit(vn);
		return recordingState;
	}
	
	public void stop() 
	{
		recordingState.cancel(true);
		shutdown();
	}
}
