package org.jenkinsci.plugins.vncrecorder;
import hudson.Extension;
import hudson.Launcher;
import hudson.Launcher.ProcStarter;
import hudson.Util;
import hudson.model.BuildListener;
import hudson.model.Item;
import hudson.model.TaskListener;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.tasks.BuildWrapper;
import hudson.tasks.BuildWrapperDescriptor;
import hudson.tasks.Messages;
import hudson.util.FormValidation;
import hudson.util.FormValidation.CheckMethod;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.concurrent.Future;

import jenkins.model.Jenkins;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.SystemUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.jenkinsci.plugins.vnrcecorder.vncutil.VncRecorder;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;


public class VncRecorderBuildWrapper extends BuildWrapper {

	private Future<?> recordState;
	private File outFileSwf;
	private File outFileHtml;

	private Date from;
	private Date to;
	private String vncServ;
	private String vncPasswFilePath;
	public Logger vncLogger;
	private Boolean setDisplay = false;

	@DataBoundConstructor
	public VncRecorderBuildWrapper(String vncServ, String vncPasswFilePath, Boolean setDisplay) 
	{
		this.vncServ = vncServ;
		this.vncPasswFilePath = vncPasswFilePath;
		this.setDisplay  = setDisplay;
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


	@Override
	public Environment setUp(AbstractBuild build, Launcher launcher,
			final BuildListener listener) throws IOException, InterruptedException
	{
		final VncRecorder vr = new VncRecorder();
		vncLogger = vr.getLoggerForPrintStream(listener.getLogger());
		if (!launcher.isUnix())
		{
			listener.fatalError("Feature \"Record VNC session\" works only under Unix/Linux!");
		}
		vncServ = Util.replaceMacro(vncServ,System.getenv());
		vncPasswFilePath = Util.replaceMacro(vncPasswFilePath,System.getenv());
		vncLogger.info("Recording from vnc server: " + vncServ);
		vncLogger.info("Using vnc passwd file: " + vncPasswFilePath);
		vncLogger.setLevel(Level.WARN);
		//		listener.getLogger().printf("Using vnc passwd file: %s\n",vncPasswFilePath);	
	

		File vncPasswFile = new File(vncPasswFilePath);
		if (vncPasswFilePath.isEmpty())
		{
			vncLogger.warn("VNC password file is an empty string, trying vnc connection without password");
			vncPasswFile = null;
		}
		else if (!vncPasswFile.exists())
		{
			vncLogger.warn("Can't find " +vncPasswFile  +", trying vnc connection without password ");
			vncPasswFile = null;
		}

		File artifactsDir = build.getArtifactsDir();
		listener.getLogger().print(build.getUrl());
		if(!artifactsDir.exists())
		{
			artifactsDir.mkdir();
		}

		String outFileBase = build.getEnvironment(listener).get("JOB_NAME") + "_" +  build.getEnvironment(listener).get("BUILD_NUMBER") + ".swf";
		outFileSwf = new File(artifactsDir,outFileBase); 
		outFileHtml = new File(outFileSwf.getAbsolutePath().replace(".swf", ".html"));
		
		from = new Date();
		recordState = vr.record(vncServ, outFileSwf.getAbsolutePath(), vncPasswFile);

		return new Environment() {
			@Override
			public void buildEnvVars(Map<String, String> env) {
//				env.put("PATH",env.get("PATH"));
//				env.put("DISPLAY", vncServ);
				if (setDisplay)
					env.put("DISPLAY",vncServ);
			}
			@Override
			public boolean tearDown(AbstractBuild build, BuildListener listener)
					throws IOException, InterruptedException {
				to = new Date();
				recordState.cancel(true);
				Thread.sleep(1000);

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
	//	@Extension
	//	public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();
	//
	//	@Override
	//	public Descriptor<BuildWrapper> getDescriptor() {
	//		return DESCRIPTOR;
	//	}

	@Extension
	public static final class DescriptorImpl extends BuildWrapperDescriptor {

		public DescriptorImpl() {
			super(VncRecorderBuildWrapper.class);
			load();
		}


		 public FormValidation doCheckVncServ(@AncestorInPath AbstractProject<?,?> project, @QueryParameter String value ) {
	            // Require CONFIGURE permission on this project
	            if(!project.hasPermission(Item.CONFIGURE)){
	            	return FormValidation.ok();
	            }
//	    		List<String> com;
	    		String[] com = new String []{"which","vnc2swf"};
				try {
					Process process = new ProcessBuilder(com ).start();
					int whichRet = process.waitFor();
					if (whichRet != 0)
					{
						return FormValidation.errorWithMarkup("Can't find vnc2swf on your system! Please install <a href=\"http://www.unixuser.org/~euske/vnc2swf/pyvnc2swf.html\">vnc2swf</a> first. ");
					}
				
					
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
//				String vnsServCom = "Example for start of vncserver: " + value.split(":").length == 2 : ;
				return FormValidation.okWithMarkup("<strong><font color=\"blue\">Please, make sure that your vncserer is running on '" + value  + "'</font></strong>");
//	          p2.stdout(vncLogger);
	            
	            
//	            StringTokenizer tokens = new StringTokenizer(Util.fixNull(value),",");
//	            boolean hasProjects = false;
//	            while(tokens.hasMoreTokens()) {
//	                String projectName = tokens.nextToken().trim();
//	                if (StringUtils.isNotBlank(projectName)) {
//	                	Item item = Jenkins.getInstance().getItem(projectName,project,Item.class); // only works after version 1.410
//	                    if(item==null){
//	                        return FormValidation.error(Messages.BuildTrigger_NoSuchProject(projectName,AbstractProject.findNearest(projectName).getName()));
//	                    }
//	                    if(!(item instanceof AbstractProject)){
//	                        return FormValidation.error(Messages.BuildTrigger_NotBuildable(projectName));
//	                    }
//	                    hasProjects = true;
//	                }
//	            }
//	            if (!hasProjects) {
////	            	return FormValidation.error(Messages.BuildTrigger_NoProjectSpecified()); // only works with Jenkins version built after 2011-01-30
//	            	return FormValidation.error("No project specified");
//	            }

//	            return FormValidation.ok();
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


		@Override
		public boolean isApplicable(AbstractProject<?, ?> item) {
			return !SystemUtils.IS_OS_WINDOWS;
//			return true;
		}
	}
}


