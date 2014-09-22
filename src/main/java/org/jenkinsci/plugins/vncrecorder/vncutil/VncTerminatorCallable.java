package org.jenkinsci.plugins.vncrecorder.vncutil;

import java.util.concurrent.Callable;


public class VncTerminatorCallable extends VncProcess implements Callable<Integer>  {

	private int pid;

	public VncTerminatorCallable(int pid) {
		this.setPid(pid);
	}

	public VncTerminatorCallable() {
	}

	public Integer call() throws Exception 
	{
		executeProcess(new String []{"kill", "-SIGINT", String.valueOf(pid)});
//		executeProcess(new String []{"kill", "-SIGTERM", String.valueOf(pid)});
		return 0;

	}

	public int getPid() {
		return pid;
	}

	public void setPid(int pid) {
		this.pid = pid;
	}


}


