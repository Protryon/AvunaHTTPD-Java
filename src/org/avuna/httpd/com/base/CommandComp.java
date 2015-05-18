package org.avuna.httpd.com.base;

import java.io.File;
import java.util.ArrayList;
import java.util.Scanner;
import org.avuna.httpd.AvunaHTTPD;
import org.avuna.httpd.com.Command;
import org.avuna.httpd.com.CommandContext;
import org.avuna.httpd.hosts.Host;
import org.avuna.httpd.hosts.HostHTTP;
import org.avuna.httpd.hosts.VHost;
import org.avuna.httpd.http.plugins.javaloader.PatchJavaLoader;
import org.avuna.httpd.util.Logger;

public class CommandComp extends Command {
	
	@Override
	public int processCommand(String[] args, CommandContext context) throws Exception {
		boolean all = args.length < 1;
		Host ghost = (Host)AvunaHTTPD.hosts.get(context.getSelectedHost());
		if (ghost == null) {
			context.println("Invalid Selected Host! (select)");
			return 2;
		}
		if (!(ghost instanceof HostHTTP)) {
			context.println("Not a http host! (select)");
			return 4;
		}
		HostHTTP phost = (HostHTTP)ghost;
		VHost host = phost.getVHostByName(context.getSelectedVHost());
		if (host == null) {
			context.println("Invalid Selected VHost! (select)");
			return 3;
		}
		String sep = AvunaHTTPD.windows ? ";" : ":";
		String us = null;
		try {
			String fp = AvunaHTTPD.class.getProtectionDomain().getCodeSource().getLocation().getPath();
			us = fp;
		}catch (Exception e) {
			Logger.logError(e);
			context.println("Critical error <Couldn't find us! Are we bound in memory?>!");
			return -2;
		}
		if (us.equals("./")) {
			us = new File("avuna.jar").getAbsolutePath();
		}
		String cp = new File(us).getAbsolutePath() + sep + host.getHTDocs().toString() + sep + host.getHTSrc().toString() + sep + PatchJavaLoader.lib.toString() + sep;
		for (File f : PatchJavaLoader.lib.listFiles()) {
			if (!f.isDirectory() && f.getName().endsWith(".jar")) {
				cp += f.toString() + sep;
			}
		}
		cp = cp.substring(0, cp.length() - 1);
		ArrayList<String> cfs = new ArrayList<String>();
		cfs.add(AvunaHTTPD.mainConfig.getNode("javac").getValue());
		cfs.add("-cp");
		cfs.add(cp);
		cfs.add("-d");
		cfs.add(host.getHTDocs().toString());
		if (all) {
			recurForComp(cfs, host.getHTSrc());
		}else {
			cfs.add("htsrc/" + args[0]);
		}
		ProcessBuilder pb = new ProcessBuilder(cfs.toArray(new String[]{}));
		pb.directory(AvunaHTTPD.fileManager.getMainDir());
		pb.redirectErrorStream(true);
		Process proc = pb.start();
		Scanner s = new Scanner(proc.getInputStream());
		while (s.hasNextLine()) {
			context.println("javac: " + s.nextLine());
		}
		s.close();
		context.println("Compile completed.");
		return 0;
	}
	
	private static void recurForComp(ArrayList<String> cfs, File base) {
		for (File f : base.listFiles()) {
			if (f.isDirectory()) {
				recurForComp(cfs, f);
			}else {
				cfs.add(f.getAbsolutePath());
			}
		}
	}
	
	@Override
	public String getHelp() {
		return "Compiles all/a specific file from htsrc to htdocs.";
	}
}