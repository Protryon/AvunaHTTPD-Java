package org.avuna.httpd.com;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Scanner;
import org.avuna.httpd.AvunaHTTPD;
import org.avuna.httpd.hosts.Host;
import org.avuna.httpd.hosts.HostHTTP;
import org.avuna.httpd.hosts.HostMail;
import org.avuna.httpd.hosts.VHost;
import org.avuna.httpd.http.j2p.JavaToPHP;
import org.avuna.httpd.mail.mailbox.EmailAccount;
import org.avuna.httpd.plugins.PatchRegistry;
import org.avuna.httpd.plugins.base.PatchOverride;
import org.avuna.httpd.plugins.javaloader.PatchJavaLoader;
import org.avuna.httpd.util.Logger;

/**
 * Handles all incoming commands from ComClient & ComServer & the console.
 */
public class CommandProcessor {
	
	/**
	 * Our selected Host.
	 */
	public static String selectedHost = "main";
	public static String selectedVHost = "main";
	
	private static void recurseHTML(PrintWriter sw, File f, File ng) {
		for (File sf : f.listFiles()) {
			if (sf.getAbsolutePath().equals(ng.getAbsolutePath())) {
				continue;
			}
			if (sf.isDirectory()) {
				recurseHTML(sw, sf, ng);
			}else {
				String ext = sf.getName().substring(sf.getName().lastIndexOf(".") + 1).toLowerCase();
				if (!(ext.equals("html") || ext.equals("json") || ext.equals("xml") || ext.equals("xhtml") || ext.equals("htm") || ext.equals("css") || ext.equals("js") || ext.equals("txt") || ext.equals("php"))) continue;
				try {
					Scanner s = new Scanner(sf);
					int l = 0;
					ArrayList<String> lines = new ArrayList<String>(64);
					while (s.hasNextLine()) {
						lines.add(s.nextLine());
						l++;
					}
					sw.println(sf.getName() + ":" + l);
					for (String line : lines) {
						sw.println(line);
					}
				}catch (IOException e) {
					Logger.logError(e);
				}
			}
		}
	}
	
	/**
	 * Processes our command
	 * 
	 * @param command the command to process
	 * @param out the output to write to
	 * @param scan the scanner to read from
	 * @throws Exception an exception gets thrown when something doesn't work out well.
	 */
	public static void process(String command, final PrintStream out, final Scanner scan) throws Exception {
		se = false;
		// Fancy command parsing right here
		String[] cargs = command.contains(" ") ? command.substring(command.indexOf(" ") + 1).split(" ") : new String[0];
		if (cargs.length > 0) {
			String[] tcargs = new String[cargs.length];
			int nl = 0;
			boolean iq = false;
			String tmp = "";
			for (int i = 0; i < cargs.length; i++) {
				boolean niq = false;
				String ct = cargs[i].trim();
				if (!iq && ct.startsWith("\"")) {
					iq = true;
					niq = true;
				}
				if (iq) {
					tmp += (niq ? ct.substring(1) : ct) + " ";
				}else {
					tcargs[nl++] = ct;
				}
				if ((!niq || ct.length() > 3) && iq && ct.endsWith("\"")) {
					iq = false;
					String n = tmp.trim();
					if (n.endsWith("\"")) n = n.substring(0, n.length() - 1);
					tcargs[nl++] = n;
					tmp = "";
				}
			}
			cargs = new String[nl];
			System.arraycopy(tcargs, 0, cargs, 0, nl);
		}
		String targs = command.contains(" ") ? command.substring(command.indexOf(" ") + 1) : "";
		command = command.contains(" ") ? command.substring(0, command.indexOf(" ")) : command;
		
		// If else statements here, no usage of switch as the server should support JDK/JRE 6.45 (or laziness)
		if (command.equals("exit") || command.equals("stop")) {
			
			// TODO: No pls make better exit method.
			System.exit(0);
		}else if (command.equals("reload")) {
			// Reloads our patches and config
			try {
				AvunaHTTPD.fileManager.clearCache();
				((PatchOverride)PatchRegistry.getPatchForClass(PatchOverride.class)).flush();
				AvunaHTTPD.mainConfig.load();
				AvunaHTTPD.patchBus.reload();
			}catch (Exception e) {
				Logger.logError(e);
				e.printStackTrace(out);
			}
			out.println("Avuna Reloaded! This does NOT update all configs, but it reloads most of them and flushes the cache. Doesn't flush the JavaLoader cache, for that use jlflush.");
		}else if (command.equals("flushjl")) {
			// Reloads our patches and config
			try {
				((PatchJavaLoader)PatchRegistry.getPatchForClass(PatchJavaLoader.class)).flushjl();
				AvunaHTTPD.fileManager.flushjl();
			}catch (Exception e) {
				Logger.logError(e);
				e.printStackTrace(out);
			}
			out.println("JavaLoader flushed!");
		}else if (command.equals("restart")) {
			// Restarts our server completely
			// TODO: Doesnt exit the program so maybe JVMBindException will occur?
			try {
				AvunaHTTPD.patchBus.preExit();
				if (System.getProperty("os.name").contains("nux") || System.getProperty("os.name").contains("nix")) {
					Runtime.getRuntime().exec("sh " + AvunaHTTPD.fileManager.getBaseFile("restart.sh"));
				}else if (System.getProperty("os.name").toLowerCase().contains("windows")) {
					Runtime.getRuntime().exec(AvunaHTTPD.fileManager.getBaseFile("restart.bat").toString());
				}
			}catch (Exception e) {
				Logger.logError(e);
				e.printStackTrace(out);
			}
			out.println("Restarting...");
		}else if (command.equals("select")) {
			if (cargs.length != 2 && cargs.length != 1) {
				out.println("Invalid arguments. (host, vhost)");
				return;
			}
			selectedHost = cargs[0];
			if (cargs.length == 2) selectedVHost = cargs[1];
			out.println("Selected " + selectedHost + (cargs.length == 2 ? ("/" + selectedVHost) : "") + "!");
		}else if (command.equals("register")) {
			Host host = AvunaHTTPD.hosts.get(selectedHost);
			if (!(host instanceof HostMail)) {
				out.println("Not a mail host!");
				return;
			}
			if (cargs.length != 2) {
				out.println("Invalid arguments. (email, password)");
				return;
			}
			((HostMail)host).accounts.add(new EmailAccount(cargs[0], cargs[1]));
			out.println("Registered " + cargs[0] + "!");
		}else if (command.equals("jhtmldir")) {
			if (cargs.length != 2) {
				out.println("Invalid arguments. (input[dir], output[html])");
				return;
			}
			try {
				Host ghost = (Host)AvunaHTTPD.hosts.get(selectedHost);
				if (ghost == null) {
					out.println("Invalid Selected Host (select)");
					return;
				}
				if (!(ghost instanceof HostHTTP)) {
					out.println("Not a http host!");
					return;
				}
				HostHTTP phost = (HostHTTP)ghost;
				VHost host = phost.getVHostByName(selectedVHost);
				File temp = null;
				PrintWriter ps = new PrintWriter(new FileOutputStream(cargs[1]));
				recurseHTML(ps, temp = new File(cargs[0]), temp);
				ps.flush();
				ps.close();
			}catch (IOException e) {
				Logger.log(e.getMessage());
				e.printStackTrace(out);
			}
			out.println("JHTMLDIR completed.");
		}else if (command.equals("jhtml")) {
			if (cargs.length != 2 && cargs.length != 1) {
				out.println("Invalid arguments. (input, output[optional])");
				return;
			}
			try {
				Host ghost = (Host)AvunaHTTPD.hosts.get(selectedHost);
				if (ghost == null) {
					out.println("Invalid Selected Host (select)");
					return;
				}
				if (!(ghost instanceof HostHTTP)) {
					out.println("Not a http host!");
					return;
				}
				HostHTTP phost = (HostHTTP)ghost;
				VHost host = phost.getVHostByName(selectedVHost);
				File sc2 = null;
				Scanner scan2 = new Scanner(new FileInputStream(sc2 = new File(host.getHTDocs(), cargs[0])));
				PrintStream ps;
				File temp = null;
				if (cargs.length == 2) {
					temp = new File(host.getHTSrc(), cargs[1]);
				}else {
					temp = new File(host.getHTSrc(), cargs[0].substring(0, cargs[0].indexOf(".")) + ".java");
				}
				temp.getParentFile().mkdirs();
				temp.createNewFile();
				ps = new PrintStream(new FileOutputStream(temp));
				ps.println("import com.javaprophet.javawebserver.plugins.javaloader.HTMLBuilder;");
				ps.println("import com.javaprophet.javawebserver.networking.packets.RequestPacket;");
				ps.println("import com.javaprophet.javawebserver.networking.packets.ResponsePacket;");
				ps.println("import com.javaprophet.javawebserver.plugins.javaloader.JavaLoaderPrint;");
				ps.println();
				ps.println("public class " + (cargs.length == 2 ? temp.getName().substring(0, temp.getName().indexOf(".")) : sc2.getName().substring(0, sc2.getName().indexOf("."))) + " extends JavaLoaderPrint {");
				ps.println("    public void generate(HTMLBuilder out, ResponsePacket response, RequestPacket request) {");
				while (scan2.hasNextLine()) {
					String line = scan2.nextLine().trim();
					ps.println("        " + "out.println(\"" + line.replace("\\", "\\\\").replace("\"", "\\\"") + "\");");
				}
				ps.println("    }");
				ps.println("}");
				ps.flush();
				ps.close();
				scan2.close();
			}catch (IOException e) {
				Logger.log(e.getMessage());
				e.printStackTrace(out);
			}
			out.println("JHTML completed.");
		}else if (command.equals("jphp")) {
			if (cargs.length != 2 && cargs.length != 1) {
				out.println("Invalid arguments. (input, output[optional])");
				return;
			}
			Host ghost = (Host)AvunaHTTPD.hosts.get(selectedHost);
			if (ghost == null) {
				out.println("Invalid Selected Host (select)");
				return;
			}
			if (!(ghost instanceof HostHTTP)) {
				out.println("Not a http host!");
				return;
			}
			HostHTTP phost = (HostHTTP)ghost;
			VHost host = phost.getVHostByName(selectedVHost);
			try {
				File sc2 = null;
				Scanner scan2 = new Scanner(new FileInputStream(sc2 = new File(host.getHTDocs(), cargs[0])));
				PrintStream ps;
				File temp = null;
				if (cargs.length == 2) {
					ps = new PrintStream(new FileOutputStream(temp = new File(host.getHTSrc(), cargs[1])));
				}else {
					ps = new PrintStream(new FileOutputStream(temp = new File(host.getHTSrc(), cargs[0].substring(0, cargs[0].indexOf(".")) + ".java")));
				}
				StringBuilder php = new StringBuilder();
				while (scan2.hasNextLine()) {
					php.append(scan2.nextLine().trim() + AvunaHTTPD.crlf);
				}
				JavaToPHP.convert((cargs.length == 3 ? temp.getName().substring(0, temp.getName().indexOf(".")) : sc2.getName().substring(0, sc2.getName().indexOf("."))), ps, php.toString());
				ps.flush();
				ps.close();
				scan2.close();
			}catch (IOException e) {
				Logger.log(e.getMessage());
				e.printStackTrace(out);
			}
			out.println("JPHP completed.");
		}else if (command.equals("jcomp")) {
			boolean all = cargs.length < 1;
			Host ghost = (Host)AvunaHTTPD.hosts.get(selectedHost);
			if (ghost == null) {
				out.println("Invalid Selected Host (select)");
				return;
			}
			if (!(ghost instanceof HostHTTP)) {
				out.println("Not a http host!");
				return;
			}
			HostHTTP phost = (HostHTTP)ghost;
			VHost host = phost.getVHostByName(selectedVHost);
			String sep = System.getProperty("os.name").toLowerCase().contains("windows") ? ";" : ":";
			File us = null;
			try {
				us = new File(AvunaHTTPD.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath());
			}catch (Exception e) {
			}
			String cp = us.toString() + sep + host.getHTDocs().toString() + sep + host.getHTSrc().toString() + sep + PatchJavaLoader.lib.toString() + sep;
			for (File f : PatchJavaLoader.lib.listFiles()) {
				if (!f.isDirectory() && f.getName().endsWith(".jar")) {
					cp += f.toString() + sep;
				}
			}
			cp = cp.substring(0, cp.length() - 1);
			ArrayList<String> cfs = new ArrayList<String>();
			cfs.add((String)AvunaHTTPD.mainConfig.get("javac"));
			cfs.add("-cp");
			cfs.add(cp);
			cfs.add("-d");
			cfs.add(host.getHTDocs().toString());
			if (all) {
				recurForComp(cfs, host.getHTSrc());
			}else {
				cfs.add("htsrc/" + cargs[0]);
			}
			ProcessBuilder pb = new ProcessBuilder(cfs.toArray(new String[]{}));
			pb.directory(AvunaHTTPD.fileManager.getMainDir());
			pb.redirectErrorStream(true);
			Process proc = pb.start();
			Scanner s = new Scanner(proc.getInputStream());
			while (s.hasNextLine()) {
				out.println("javac: " + s.nextLine());
			}
			s.close();
			out.println("JCOMP completed.");
		}else if (command.equals("mem")) {
			out.println(((Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1048576) + "/" + (Runtime.getRuntime().totalMemory() / 1048576) + " (MB) memory used.");
		}else if (command.equals("shell")) {
			if (targs.length() > 0) {
				Thread temp = null;
				Thread temp2 = null;
				try {
					Process proc = Runtime.getRuntime().exec(targs);
					final InputStream pin = proc.getInputStream();
					final OutputStream pout = proc.getOutputStream();
					final Scanner s = new Scanner(pin);
					se = true;
					temp = new Thread() {
						public void run() {
							while (se && scan.hasNextLine()) {
								try {
									String line = scan.nextLine();
									out.println(line);
									if (line.equals("exit")) {
										se = false;
										continue;
									}else {
										pout.write((line + AvunaHTTPD.crlf).getBytes());
										pout.flush();
									}
								}catch (IOException e) {
									Logger.logError(e);
								}
							}
						}
					};
					temp.start();
					temp2 = new Thread() {
						public void run() {
							while (se && s.hasNextLine()) {
								out.println(s.nextLine());
							}
							se = false;
						}
					};
					temp2.start();
					while (se) {
						Thread.sleep(10L);
					}
				}catch (Exception e) {
					Logger.logError(e);
					e.printStackTrace(out);
				}finally {
					if (temp != null) temp.interrupt();
					if (temp2 != null) temp2.interrupt();
				}
				out.println("Finished Execution.");
			}else {
				out.println("Invalid arguments. (exec)");
			}
		}else if (command.equals("help")) {
			out.println("Commands:");
			out.println("exit/stop - exits Avuna");
			out.println("reload    - flushes all caches and reloads");
			out.println("select    - select a host/vhost");
			out.println("restart   - attempts to restart the server by running the provided .bat/.sh files");
			out.println("jhtml     - converts HTML to JavaLoaderPrint");
			out.println("jhtmldir  - converts a directory of HTML or other text to HTMLCache format.");
			out.println("jcomp     - compiles all(or specified) files in the htsrc folder to the htdocs folder");
			out.println("jphp      - attempts to roughly convert PHP->Java, will require fine tuning");
			out.println("flushjl   - attempts to clear JavaLoaders, and reload them.");
			out.println("shell     - runs a shell on the host computer.");
			out.println("mem       - shows memory stats");
			out.println("help      - lists these commands + version");
			out.println("");
			out.println("Avuna HTTPD Version " + AvunaHTTPD.VERSION);
		}else {
			out.println("Unknown Command: " + command + " - Use help command.");
		}
	}
	
	/**
	 * WTf
	 */
	private static boolean se = false;
	
	/**
	 * Needs comp
	 * 
	 * @param cfs
	 * @param base
	 */
	private static void recurForComp(ArrayList<String> cfs, File base) {
		for (File f : base.listFiles()) {
			if (f.isDirectory()) {
				recurForComp(cfs, f);
			}else {
				cfs.add(f.getAbsolutePath());
			}
		}
	}
}
