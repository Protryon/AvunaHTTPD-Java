package com.javaprophet.javawebserver.networking.command;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Scanner;
import com.javaprophet.javawebserver.JavaWebServer;
import com.javaprophet.javawebserver.plugins.javaloader.PatchJavaLoader;
import com.javaprophet.javawebserver.util.Logger;

public class CommandProcessor {
	public static void process(String command, final PrintStream out, final Scanner scan, boolean telnet) throws IOException {
		se = false;
		String[] cargs = command.contains(" ") ? command.substring(command.indexOf(" ") + 1).split(" ") : new String[0];
		String targs = command.contains(" ") ? command.substring(command.indexOf(" ") + 1) : "";
		command = command.contains(" ") ? command.substring(0, command.indexOf(" ")) : command;
		if (command.equals("exit") || command.equals("stop")) {
			JavaWebServer.patchBus.preExit();
			if (JavaWebServer.mainConfig != null) {
				JavaWebServer.mainConfig.save();
			}
			System.exit(0);
		}else if (command.equals("reload")) {
			try {
				JavaWebServer.mainConfig.load();
				JavaWebServer.patchBus.preExit();
			}catch (Exception e) {
				e.printStackTrace(out);
			}
			out.println("Loaded Config! Some entries will require a restart.");
		}else if (command.equals("restart")) {
			try {
				JavaWebServer.patchBus.preExit();
				if (JavaWebServer.mainConfig != null) {
					JavaWebServer.mainConfig.save();
				}
				Runtime.getRuntime().exec("sh " + JavaWebServer.fileManager.getBaseFile("restart.sh"));
			}catch (Exception e) {
				e.printStackTrace(out);
			}
			out.println("Loaded Config! Some entries will require a restart.");
		}else if (command.equals("flushcache")) {
			try {
				JavaWebServer.fileManager.clearCache();
			}catch (Exception e) {
				e.printStackTrace(out);
			}
			out.println("Cache Flushed! This is not necessary for php files, and does not work for .class files(restart jws for those).");
		}else if (command.equals("jhtml")) {
			if (cargs.length != 2 && cargs.length != 1) {
				out.println("Invalid arguments. (input, output[optional])");
				return;
			}
			try {
				File sc2 = null;
				Scanner scan2 = new Scanner(new FileInputStream(sc2 = new File(JavaWebServer.fileManager.getHTDocs(), cargs[0])));
				PrintStream ps;
				File temp = null;
				if (cargs.length == 2) {
					ps = new PrintStream(new FileOutputStream(temp = new File(JavaWebServer.fileManager.getHTSrc(), cargs[1])));
				}else {
					ps = new PrintStream(new FileOutputStream(temp = new File(JavaWebServer.fileManager.getHTSrc(), cargs[0].substring(0, cargs[0].indexOf(".")) + ".java")));
				}
				ps.println("import java.io.PrintStream;");
				ps.println("import com.javaprophet.javawebserver.networking.packets.RequestPacket;");
				ps.println("import com.javaprophet.javawebserver.networking.packets.ResponsePacket;");
				ps.println("import com.javaprophet.javawebserver.plugins.javaloader.JavaLoaderStream;");
				ps.println();
				ps.println("public class " + (cargs.length == 3 ? temp.getName().substring(0, temp.getName().indexOf(".")) : sc2.getName().substring(0, sc2.getName().indexOf("."))) + " extends JavaLoaderStream {");
				ps.println("    public void generate(PrintStream out, ResponsePacket response, RequestPacket request) {");
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
			}
			out.println("JHTML completed.");
		}else if (command.equals("jcomp")) {
			boolean all = cargs.length < 1;
			String sep = System.getProperty("os.name").toLowerCase().contains("windows") ? ";" : ":";
			String cp = JavaWebServer.fileManager.getBaseFile("jws.jar").toString() + sep + JavaWebServer.fileManager.getHTDocs().toString() + sep + JavaWebServer.fileManager.getHTSrc().toString() + sep + PatchJavaLoader.lib.toString() + sep;
			for (File f : PatchJavaLoader.lib.listFiles()) {
				if (!f.isDirectory() && f.getName().endsWith(".jar")) {
					cp += f.toString() + sep;
				}
			}
			cp = cp.substring(0, cp.length() - 1);
			ArrayList<String> cfs = new ArrayList<String>();
			cfs.add((String)JavaWebServer.mainConfig.get("javac"));
			cfs.add("-cp");
			cfs.add(cp);
			cfs.add("-d");
			cfs.add(JavaWebServer.fileManager.getHTDocs().toString());
			if (all) {
				recurForComp(cfs, JavaWebServer.fileManager.getHTSrc());
			}else {
				cfs.add("htsrc/" + cargs[0]);
			}
			ProcessBuilder pb = new ProcessBuilder(cfs.toArray(new String[]{}));
			pb.directory(JavaWebServer.fileManager.getMainDir());
			pb.redirectErrorStream(true);
			Process proc = pb.start();
			Scanner s = new Scanner(proc.getInputStream());
			while (s.hasNextLine()) {
				out.println("javac: " + s.nextLine());
			}
			s.close();
			out.println("JCOMP completed.");
		}else if (command.equals("shell")) {
			if (targs.length() > 0) {
				try {
					Process proc = Runtime.getRuntime().exec(targs);
					final InputStream pin = proc.getInputStream();
					final OutputStream pout = proc.getOutputStream();
					final Scanner s = new Scanner(pin);
					se = true;
					Thread temp = new Thread() {
						public void run() {
							while (se && scan.hasNextLine()) {
								try {
									String line = scan.nextLine();
									out.println(line);
									if (line.equals("exit")) {
										se = false;
										continue;
									}else {
										pout.write((line + JavaWebServer.crlf).getBytes());
										pout.flush();
									}
								}catch (IOException e) {
									e.printStackTrace(out);
								}
							}
						}
					};
					temp.start();
					Thread temp2 = new Thread() {
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
					temp.interrupt();
					temp2.interrupt();
				}catch (Exception e) {
					e.printStackTrace(out);
				}
				out.println("Finished Execution.");
			}else {
				out.println("Invalid arguments. (exec)");
			}
		}else if (command.equals("help")) {
			out.println("Commands:");
			out.println("exit/stop");
			out.println("reload");
			out.println("restart");
			out.println("flushcache");
			out.println("jhtml");
			out.println("jcomp");
			out.println("shell");
			out.println("help");
			out.println("");
			out.println("Java Web Server(JWS) Version " + JavaWebServer.VERSION);
		}else {
			out.println("Unknown Command: " + command);
		}
	}
	
	private static boolean se = false;
	
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
