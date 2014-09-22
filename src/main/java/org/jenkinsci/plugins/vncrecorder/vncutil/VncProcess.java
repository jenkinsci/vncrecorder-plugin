package org.jenkinsci.plugins.vncrecorder.vncutil;

import java.io.PrintStream;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.WriterAppender;

public class VncProcess {

	protected Logger logger = Logger.getLogger(VncProcess.class);
	static protected ExecutorService execServ = Executors.newFixedThreadPool(10);
	static protected ExecutorService terminatorServ = Executors.newFixedThreadPool(10);
	static protected ScheduledExecutorService splitCheckerExec = Executors.newSingleThreadScheduledExecutor();	
	static final protected String VNC2SWF_BINARY =  "vnc2swf";
	protected Process process;

	public Logger getLoggerForPrintStream(PrintStream ps)
	{
		PatternLayout p = new PatternLayout("%d{ISO8601} %-5p [%t] %m%n");
		WriterAppender wa = new WriterAppender(p,ps);
		logger.addAppender(wa);
		return logger;
	}
	
	public VncProcess() {
		super();
		logger.addAppender(new ConsoleAppender( new PatternLayout("%d{ISO8601} %-5p [%t] %m%n")));
	}


	protected Process executeProcess(String[] com) throws Exception,
	InterruptedException {
		logger.debug("Starting command " + Arrays.toString(com));
		process = new ProcessBuilder( com ).start();
		if (!com[0].equalsIgnoreCase("kill"))
			logger.debug("PID of command " + Arrays.toString(com) + " is: " + getUnixPID(process));
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


	public static void shutdown() 
	{
//		execServ.shutdownNow();
//		terminatorServ.shutdownNow();
//		splitCheckerExec.shutdownNow();
	}  

}