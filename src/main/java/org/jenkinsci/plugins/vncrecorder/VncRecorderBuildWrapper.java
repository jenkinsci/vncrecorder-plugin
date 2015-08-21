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
package org.jenkinsci.plugins.vncrecorder;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.Future;

import org.apache.commons.lang.SystemUtils;
import org.jenkinsci.plugins.vncrecorder.vncutil.VncRecorder;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import hudson.Extension;
import hudson.Launcher;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Hudson;
import hudson.model.Item;
import hudson.model.Result;
import hudson.tasks.BuildWrapper;
import hudson.tasks.BuildWrapperDescriptor;
import hudson.util.FormValidation;
import net.sf.json.JSONObject;


public class VncRecorderBuildWrapper extends BuildWrapper {

	public static final String CANT_FIND_VNC2SWF = "/Set/your/path/for/vnc2swf";; 
	private String vncServ;
	private String vncPasswFilePath;
	private String  outFileName = "${JOB_NAME}_${BUILD_NUMBER}";
	private Boolean setDisplay = false;
	private Boolean removeIfSuccessful = false;

	@DataBoundConstructor
	public VncRecorderBuildWrapper(String vncServ, String vncPasswFilePath, String outFileName, Boolean setDisplay, Boolean removeIfSuccessful) 
	{
		this.vncServ = vncServ;
		this.vncPasswFilePath = vncPasswFilePath;
		this.setDisplay  = setDisplay;
		this.setRemoveIfSuccessful(removeIfSuccessful);
		this.setOutFileName(outFileName);
	}


	public String getOutFileName() {
		return outFileName;
	}


	public void setOutFileName(String outFileName)
	{
		if (outFileName == null || outFileName.isEmpty() || outFileName.equalsIgnoreCase("null"))
			this.outFileName = "${JOB_NAME}_${BUILD_NUMBER}";
		else
			this.outFileName = outFileName;
	}


	public String getVncServ() {
		return vncServ;
	}

	public void setVncServ(String vncServ) {
		this.vncServ = vncServ;
	}

	public String getVncPasswFilePath() {
		return vncPasswFilePath;
	}

	public void setVncPasswFilePath(String vncPasswFilePath) {
		this.vncPasswFilePath = vncPasswFilePath;
	}

	public Boolean getSetDisplay() {
		return setDisplay;
	}

	public void setSetDisplay(Boolean setDisplay) {
		this.setDisplay = setDisplay;
	}

	public Boolean getRemoveIfSuccessful() {
		return removeIfSuccessful;
	}


	public void setRemoveIfSuccessful(Boolean removeIfSuccessful) 
	{
		if (removeIfSuccessful == null)
			this.removeIfSuccessful = false;
		else
			this.removeIfSuccessful = removeIfSuccessful;
	}


	@Override
	public Environment setUp(@SuppressWarnings("rawtypes")AbstractBuild build, Launcher launcher,
			final BuildListener listener) throws IOException, InterruptedException
	{
		DescriptorImpl DESCRIPTOR = Hudson.getInstance().getDescriptorByType(DescriptorImpl.class);
		String vnc2swf = Util.nullify(DESCRIPTOR.getVnc2swf());
		if(vnc2swf.equals(CANT_FIND_VNC2SWF))
		{
			listener.fatalError("VNC Recorder: can't find 'vnc2swf' please check your jenkins global settings!");
			return null;
		}
		else 
		{
			File vnc2swfFile = new File(vnc2swf);
			if (!vnc2swfFile.exists())
			{
				listener.fatalError("VNC Recorder: can't find '" + vnc2swf + "' please check your jenkins global settings!");
				return null;
			}
		}

		final VncRecorder vr = new VncRecorder();
		vr.setLoggingStream(listener.getLogger());
//		final Logger vncLogger = vr.getLoggerForPrintStream(listener.getLogger());
		if (!SystemUtils.IS_OS_UNIX)
		{
			listener.fatalError("Feature \"Record VNC session\" works only under Unix/Linux!");
			return null;
		}
		String vncServReplaced = Util.replaceMacro(vncServ,build.getEnvironment(listener));
		if (vncServReplaced.indexOf(":") > 0 && vncServReplaced.split(":")[1].length() == 4 && vncServReplaced.split(":")[1].startsWith("59") )
		{
			vncServReplaced = vncServReplaced.replace(":59", ":");
		}
		
		String vncPasswFilePathReplaced = Util.replaceMacro(vncPasswFilePath,build.getEnvironment(listener));
		//String outFileBase = build.getEnvironment(listener).get("JOB_NAME") + "_" +  build.getEnvironment(listener).get("BUILD_NUMBER") + ".swf";
		if (outFileName == null || outFileName.equalsIgnoreCase("null"))
		{
			outFileName = "${JOB_NAME}_${BUILD_NUMBER}";
		}
		String outFileBase =  Util.replaceMacro(outFileName,build.getEnvironment(listener)) + ".swf";
		listener.getLogger().println("Recording from vnc server: " + vncServReplaced);
		listener.getLogger().println("Using vnc passwd file: " + vncPasswFilePathReplaced);
		//		listener.getLogger().printf("Using vnc passwd file: %s\n",vncPasswFilePath);	


		File vncPasswFile = new File(vncPasswFilePathReplaced);
		if (vncPasswFilePathReplaced.isEmpty())
		{
			listener.getLogger().println("VNC password file is an empty string, trying vnc connection without password");
			vncPasswFile = null;
		}
		else if (!vncPasswFile.exists())
		{
			listener.getLogger().println("Can't find " +vncPasswFile  +", trying vnc connection without password ");
			vncPasswFile = null;
		}

		File artifactsDir = build.getArtifactsDir();
		listener.getLogger().print(build.getUrl());
		if(!artifactsDir.exists())
		{
			artifactsDir.mkdir();
		}

		if (outFileBase == null || outFileBase.equalsIgnoreCase("null.swf"))
		{
			outFileBase = build.getNumber() + ".swf";

		}
		final File outFileSwf = new File(artifactsDir,outFileBase); 
		final File outFileHtml = new File(outFileSwf.getAbsolutePath().replace(".swf", ".html"));

		final Date from = new Date();
		final Future<Integer> recordState = vr.record(vncServReplaced, outFileSwf.getAbsolutePath(), vncPasswFile,vnc2swf);

		return new Environment() {
			@Override
			public void buildEnvVars(Map<String, String> env) {
				//				env.put("PATH",env.get("PATH"));
				//				env.put("DISPLAY", vncServ);
				if (setDisplay && env != null && vncServ != null)
					env.put("DISPLAY",Util.replaceMacro(vncServ,env));
			}
			@Override
			public boolean tearDown(AbstractBuild build, BuildListener listener)
					throws IOException, InterruptedException {
				final Date to = new Date();
				if (recordState != null)
				{	
					recordState.cancel(true);
					Thread.sleep(1000);
				}
				if (removeIfSuccessful == null)
					removeIfSuccessful = false;


				if ((removeIfSuccessful && outFileSwf.exists()) && (build == null || build.getResult() == Result.SUCCESS || build.getResult() == null)  )
				{
					listener.getLogger().println("Build successful: Removing video file " + outFileSwf.getAbsolutePath() + " \n");
					outFileSwf.delete();
					outFileHtml.delete();
					return true;
				}

				if (!outFileSwf.exists())
				{
					listener.error("File " + outFileSwf.getAbsolutePath() +" doesn't exist. \nFeature \"Record VNC session\" failed!");
					return false;
				}  


				SimpleDateFormat sf = new SimpleDateFormat("yyyy-MM-dd 'T' HH:mm:ss");
				listener.hyperlink("artifact/" + outFileHtml.getName(),"Video from " + sf.format(from) + " to " + sf.format(to));
				listener.getLogger().print("\n");
				//					String con = com.google.common.io.Files.toString(outFileHtml, Charset.forName("utf-8"));
				//					con = con.replaceAll("<embed src=\""+ outFileSwf.getName() +"\"", "<embed src=\""+ "artifact/" + outFileSwf.getName()  +"\"");
				//					ExpandableDetailsNote dn = new ExpandableDetailsNote(new Date().toString(),con);
				//					listener.annotate(dn);
				return true;
			}
		};

	}


	@Extension(ordinal = -1)
	public static final class DescriptorImpl extends BuildWrapperDescriptor {

		private String vnc2swf = "";;

		public DescriptorImpl() {
			super(VncRecorderBuildWrapper.class);
			load();
		}

		public String getVnc2swf()
		{
			if (vnc2swf.isEmpty())
				vnc2swf = getDefaultVnc2swf();
			return vnc2swf;
		}


		public void setVnc2swf(String vnc2swf) {
			this.vnc2swf = vnc2swf;
		}

		public String getDefaultVnc2swf()
		{
			String ret = CANT_FIND_VNC2SWF;
			String[] coms = new String []{"vnc2swf","vnc2swf.py"};
			Process process;
			for (String s : coms) 
			{
				try {
					String[] com = new String []{"which",s};
					process = new ProcessBuilder(com ).start();
					int whichRet = process.waitFor();
					if (whichRet == 0)				
					{
						BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()));
						String line;
						while (( line = in.readLine()) != null)
						{
							if (line.contains(s))
							{
								return line;
							}
						}
					}
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			return ret;
		}
		@Override
		public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
			// XXX is this now the right style?
			req.bindJSON(this,json);
			save();
			return true;
		}


		public FormValidation doCheckVncServ(@AncestorInPath AbstractProject<?,?> project, @QueryParameter String value ) {
			// Require CONFIGURE permission on this project
			if(!project.hasPermission(Item.CONFIGURE)){
				return FormValidation.ok();
			}
			//	    		List<String> com;
			DescriptorImpl DESCRIPTOR = Hudson.getInstance().getDescriptorByType(DescriptorImpl.class);
			String vnc2swf = Util.nullify(DESCRIPTOR.getVnc2swf());
			try {
				vnc2swf = vnc2swf.equals(CANT_FIND_VNC2SWF) ? "vnc2swf" : vnc2swf;
				if(!new File(vnc2swf).exists())
				{
					return FormValidation.errorWithMarkup("Can't find '" + vnc2swf + "' on your system! Please install <a href=\"http://www.unixuser.org/~euske/vnc2swf/pyvnc2swf.html\">vnc2swf</a> or check your configured vnc2swf path in your jenkins global settings.");
				}

			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			//				String vnsServCom = "Example for start of vncserver: " + value.split(":").length == 2 : ;
			return FormValidation.okWithMarkup("<strong><font color=\"blue\">Please, make sure that your vncserer is running on '" + value  + "'</font></strong>");
		}


		public FormValidation doCheckOutFileName(@AncestorInPath AbstractProject<?,?> project, @QueryParameter String value ) {
			// Require CONFIGURE permission on this project
			if(!project.hasPermission(Item.CONFIGURE)){
				return FormValidation.ok();
			}
			if (value.isEmpty())
			{
				return FormValidation.errorWithMarkup("Out file name can't be empty!" );
			}
			return FormValidation.ok();
		}


		@Override
		public String getDisplayName() {
			return "Record VNC session";
		}

		public String getDefaultPasswdFile()
		{
			return new File(System.getProperty("user.home"),".vnc" + File.separator + "passwd").getAbsolutePath();
		}

		public String getDefaultVncServ()
		{
			return "localhost:88";
		}


		public String getDefaultOutFileName() {
			return "${JOB_NAME}_${BUILD_NUMBER}";
		}



		@Override
		public boolean isApplicable(AbstractProject<?, ?> item) {
			return !SystemUtils.IS_OS_WINDOWS;
			//			return true;
		}
	}
}


