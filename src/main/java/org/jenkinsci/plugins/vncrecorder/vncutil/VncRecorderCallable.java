package org.jenkinsci.plugins.vncrecorder.vncutil;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.concurrent.Callable;

public class VncRecorderCallable extends VncProcess implements Callable<Integer>  {
	private String vncServ;
	private String targetPath;
	private VncTerminatorCallable term = new VncTerminatorCallable();
	private File vncPasswFile;

	public VncRecorderCallable(String vncServ,String targetPath, File vncPasswFile) {
		this.vncServ = vncServ;
		this.targetPath = targetPath;
		this.vncPasswFile = vncPasswFile;
	}

	public Integer call() 
	{
		//				String[] com = new String []{"/usr/bin/flvrec.py", "-r1","-o", targetPath, vncServ};
		//String[] com = new String []{"vnc2swf","-o", targetPath,"-P", "/home/dimitri/.vnc/passwd_status", "-t", "swf5", "-n", "-e", "0", vncServ};
		String[] com = new String []{"vnc2swf","-o", targetPath, "-t", "swf5", "-n", "-e", "0", vncServ};
		if (vncPasswFile != null)
		{
			com = new String []{"vnc2swf","-o", targetPath,"-P", vncPasswFile.getAbsolutePath(), "-t", "swf5", "-n", "-e", "0", vncServ};
		}

		try {
			int i = 0;
			while(i++ < 50)
			{
				int rc=0;
				Process proc = null;
				try {
					proc = executeProcess(com);
					term.setPid(getUnixPID(proc));
					rc = proc.waitFor();
				} catch (InterruptedException e)
				{
					logger.info("Command: " + Arrays.toString(com) + " canceled");
					stop();
					return -999;
				}
				catch (Exception e)
				{
					logger.error("Command: " + Arrays.toString(com) + " failed",e);
					terminatorServ.submit(term);
					return -999;
				}
				if (rc != 0)
				{
					logger.error("Command: " + Arrays.toString(com) + " returned: " + rc + ", trying " + i + " from " + 50);
					String line;
					BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()));
					BufferedReader errIn = new BufferedReader(new InputStreamReader(process.getErrorStream()));
					while (( line = in.readLine()) != null)
					{
						logger.info(line);
					}
					while (( line = errIn.readLine()) != null)
					{
						logger.error(line);
					}
					Thread.sleep(5000);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("Error in VncRecorderCallable", e);
		}
		return -1;
	}

	public String getVncServ() {
		return vncServ;
	}

	public void setVncServ(String vncServ) {
		this.vncServ = vncServ;
	}

	public String getTargetPath() {
		return targetPath;
	}

	public void setTargetPath(String targetPath) {
		this.targetPath = targetPath;
	}
	public void stop() throws Exception {
		term.call();
	}
}