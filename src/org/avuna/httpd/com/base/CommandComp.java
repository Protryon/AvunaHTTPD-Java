/*	Avuna HTTPD - General Server Applications
    Copyright (C) 2015 Maxwell Bruce

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.*/

package org.avuna.httpd.com.base;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.avuna.httpd.AvunaHTTPD;
import org.avuna.httpd.com.Command;
import org.avuna.httpd.com.CommandContext;
import org.avuna.httpd.hosts.Host;
import org.avuna.httpd.hosts.HostHTTP;
import org.avuna.httpd.hosts.VHost;
import org.avuna.httpd.http.plugins.javaloader.PluginJavaLoader;
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
		String cp = new File(us).getAbsolutePath() + sep + host.getHTDocs().toString() + sep + host.getHTSrc().toString() + sep + PluginJavaLoader.lib.toString() + sep;
		for (File f : PluginJavaLoader.lib.listFiles()) {
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
	
	private static void recurForComp(ArrayList<String> cfs, File base) throws Exception {
		for (File f : base.listFiles()) {
			if (f.isDirectory()) {
				recurForComp(cfs, f);
			}else {
				String fileName = f.getName();
				if (fileName.substring(fileName.lastIndexOf(".")).equals(".xjsp")) {
					f = convertXJSP(f);
				}
				cfs.add(f.getAbsolutePath());
			}
		}
	}
	
	private static File convertXJSP(File file) throws Exception {
		String outPath = file.getParent() + File.separator + file.getName().substring(0, file.getName().lastIndexOf(".")) + ".java";
		String xjspString = "";
		String outWrite = "";

		try {
			byte[] encoded = Files.readAllBytes(Paths.get(file.getAbsolutePath()));
			String fileAsString = new String(encoded, "UTF8");
			xjspString = fileAsString;
			}
		catch(IOException e) {
			Logger.log("Error reading file '" + file + "'");
		}
		Pattern p1 = Pattern.compile("(?s)<%=\\s+(.*?)\\s+%>");
		Matcher m1 = p1.matcher(xjspString);
		StringBuffer sb1 = new StringBuffer();
		while (m1.find()) {
			String htmlBit = m1.group(1);
			String htmlLine = buildLine(htmlBit);
			m1.appendReplacement(sb1, htmlLine);
		}
		m1.appendTail(sb1);
		outWrite = sb1.toString();
		Files.write(Paths.get(outPath), outWrite.getBytes());
		return new File(outPath);
	}
	
	private static String buildLine (String bit) throws IOException {
		BufferedReader bitReader = new BufferedReader(new StringReader(bit));
		StringBuilder outBit = new StringBuilder();
		String bitLine = "";
		while ((bitLine  = bitReader.readLine()) != null) {
			outBit.append("out.println(\"" + bitLine.trim() + "\");\n");
		}
		return outBit.toString();
	}
	
	@Override
	public String getHelp() {
		return "Compiles all/a specific file from htsrc to htdocs.";
	}
}
