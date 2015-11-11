/* Avuna HTTPD - General Server Applications Copyright (C) 2015 Maxwell Bruce This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>. */

package org.avuna.httpd;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Scanner;
import org.avuna.httpd.com.ComClient;
import org.avuna.httpd.com.CommandContext;
import org.avuna.httpd.com.CommandRegistry;
import org.avuna.httpd.com.base.CommandComp;
import org.avuna.httpd.com.base.CommandExit;
import org.avuna.httpd.com.base.CommandHTML;
import org.avuna.httpd.com.base.CommandHTMLDir;
import org.avuna.httpd.com.base.CommandHelp;
import org.avuna.httpd.com.base.CommandMem;
import org.avuna.httpd.com.base.CommandRegister;
import org.avuna.httpd.com.base.CommandReload;
import org.avuna.httpd.com.base.CommandRestart;
import org.avuna.httpd.com.base.CommandSelect;
import org.avuna.httpd.hosts.Host;
import org.avuna.httpd.hosts.HostCom;
import org.avuna.httpd.hosts.HostDNS;
import org.avuna.httpd.hosts.HostFTP;
import org.avuna.httpd.hosts.HostHTTP;
import org.avuna.httpd.hosts.HostMail;
import org.avuna.httpd.hosts.HostRegistry;
import org.avuna.httpd.hosts.Protocol;
import org.avuna.httpd.util.CLib;
import org.avuna.httpd.util.Config;
import org.avuna.httpd.util.ConfigFormat;
import org.avuna.httpd.util.ConfigNode;
import org.avuna.httpd.util.FileManager;
import org.avuna.httpd.util.SafeMode;
import org.avuna.httpd.util.logging.Logger;
import org.avuna.httpd.util.unio.GNUTLS;

public class AvunaHTTPD {
	public static final String VERSION = "1.3.5";
	public static Config mainConfig, hostsConfig;
	public static final FileManager fileManager = new FileManager();
	public static final HashMap<String, String> extensionToMime = new HashMap<String, String>();
	public static final String crlf = new String(new byte[] { 13, 10 });
	public static final byte[] crlfb = new byte[] { 13, 10 };
	public static final CommandRegistry commandRegistry = new CommandRegistry();
	
	public static Logger logger = null;
	
	static {
		commandRegistry.registerCommand(new CommandHelp(), "help");
		commandRegistry.registerCommand(new CommandExit(), "exit", "stop");
		commandRegistry.registerCommand(new CommandComp(), "comp");
		commandRegistry.registerCommand(new CommandHTML(), "html");
		commandRegistry.registerCommand(new CommandHTMLDir(), "htmldir");
		commandRegistry.registerCommand(new CommandMem(), "mem");
		commandRegistry.registerCommand(new CommandRegister(), "register");
		commandRegistry.registerCommand(new CommandReload(), "reload");
		commandRegistry.registerCommand(new CommandRestart(), "restart");
		commandRegistry.registerCommand(new CommandSelect(), "select");
	}
	
	public static final CommandContext mainCommandContext = commandRegistry.newContext(System.out, new Scanner(System.in));
	
	/** Setup folders if they don't exist.
	 * 
	 * @see FileManager
	 * @see Host#setupFolders() */
	public static void setupFolders() {
		fileManager.getMainDir().mkdirs();
		fileManager.getLogs().mkdirs();
		for (Host host : hosts.values()) {
			host.setupFolders();
		}
	}
	
	/** Creates default scripts if they don't exist.
	 * 
	 * @throws IOException
	 * @see FileManager */
	public static void setupScripts() throws IOException {
		String os = System.getProperty("os.name").toLowerCase();
		if (os.contains("windows")) {
			File f = fileManager.getBaseFile("run.bat");
			if (!f.exists()) {
				FileOutputStream fout = new FileOutputStream(f);
				fout.write(("javaw -jar \"" + fileManager.getBaseFile("avuna.jar").getAbsolutePath() + "\" \"" + fileManager.getBaseFile("main.cfg").getAbsolutePath() + "\"").getBytes());
				fout.flush();
				fout.close();
			}
			f = fileManager.getBaseFile("kill.bat");
			if (!f.exists()) {
				FileOutputStream fout = new FileOutputStream(f);
				fout.write(("taskkill /f /im javaw").getBytes());
				fout.flush();
				fout.close();
			}
			f = fileManager.getBaseFile("restart.bat");
			if (!f.exists()) {
				FileOutputStream fout = new FileOutputStream(f);
				fout.write(("kill.bat & run.bat").getBytes());
				fout.flush();
				fout.close();
			}
			f = fileManager.getBaseFile("cmd.bat");
			if (!f.exists()) {
				FileOutputStream fout = new FileOutputStream(f);
				fout.write(("java -jar \"" + fileManager.getBaseFile("avuna.jar").getAbsolutePath() + "\" cmd").getBytes());
				fout.flush();
				fout.close();
			}
		}else {
			String ll = new String(new byte[] { 0x0A });
			File f = fileManager.getBaseFile("run.sh");
			if (!f.exists()) {
				FileOutputStream fout = new FileOutputStream(f);
				fout.write(("#!/bin/bash" + ll + ll + "nohup java -jar \"" + fileManager.getBaseFile("avuna.jar").getAbsolutePath() + "\" \"" + fileManager.getBaseFile("main.cfg").getAbsolutePath() + "\" >& /dev/null &").getBytes());
				fout.flush();
				fout.close();
			}
			f = fileManager.getBaseFile("kill.sh");
			if (!f.exists()) {
				FileOutputStream fout = new FileOutputStream(f);
				fout.write(("#!/bin/bash" + ll + ll + "pkill -f " + fileManager.getBaseFile("avuna.jar").getAbsolutePath() + "").getBytes());
				fout.flush();
				fout.close();
			}
			f = fileManager.getBaseFile("restart.sh");
			if (!f.exists()) {
				FileOutputStream fout = new FileOutputStream(f);
				fout.write(("#!/bin/bash" + ll + ll + "" + fileManager.getBaseFile("kill.sh").getAbsolutePath() + ll + fileManager.getBaseFile("run.sh").getAbsolutePath()).getBytes());
				fout.flush();
				fout.close();
			}
			f = fileManager.getBaseFile("cmd.sh");
			if (!f.exists()) {
				FileOutputStream fout = new FileOutputStream(f);
				fout.write(("#!/bin/bash" + ll + "java -jar \"" + fileManager.getBaseFile("avuna.jar").getAbsolutePath() + "\" cmd").getBytes());
				fout.flush();
				fout.close();
			}
		}
	}
	
	/** Checks file names in String array, creates mime.txt from unpack dir if it exists.
	 * 
	 * @throws IOException if no files end in .so or .dll */
	public static void unpack() {
		try {
			setupScripts();
			String[] unpacks = new String[] {};
			for (String up : unpacks) {
				if (windows && up.endsWith(".so")) continue;
				if (!windows && up.endsWith(".dll")) continue;
				File mime = fileManager.getBaseFile(up);
				mime.getParentFile().mkdirs();
				if (!mime.exists()) {
					logger.log("Unpacking " + up + "...");
					InputStream in = AvunaHTTPD.class.getResourceAsStream("/unpack/" + up);
					int i = 1;
					byte[] buf = new byte[4096];
					ByteArrayOutputStream bout = new ByteArrayOutputStream();
					while (i > 0) {
						i = in.read(buf);
						if (i > 0) {
							bout.write(buf, 0, i);
						}
					}
					in.close();
					FileOutputStream fout = new FileOutputStream(mime);
					fout.write(bout.toByteArray());
					fout.flush();
					fout.close();
				}
			}
		}catch (IOException e) {
			logger.logError(e);
		}
	}
	
	public static final String[] dlMirrors = new String[] { "http://mirror.avuna.org/" };
	private static final Random rand = new Random();
	private static MessageDigest sha1;
	
	static {
		try {
			sha1 = MessageDigest.getInstance("sha-1");
		}catch (NoSuchAlgorithmException e) {
			logger.logError(e);
		}
	}
	
	public static void download() {
		try {
			String[] dls = new String[] { "mime.txt", "jni/i386/libAvunaHTTPD_JNI.so", "jni/amd64/libAvunaHTTPD_JNI.so", "jni/amd64/libffi.so.6", "jni/amd64/libgmp.so.10", "jni/amd64/libgnutls.so.28", "jni/amd64/libhogweed.so.4", "jni/amd64/libnettle.so.6", "jni/amd64/libp11-kit.so.0", "jni/amd64/libtasn1.so.6", "jni/amd64/libc.so.6", "jni/amd64/libpthread.so.0" };
			String[] hashes = new String[] { "cf6a1d74d9c106e038d481bbc0903f9b80b5bf0c", "72774362efc9b3ef0fc9b717863562160e795465", "8334ffdde4dbf97d1dc90170d3f486c75163f5e9", "9afb3ce7abdc9d47a276c0bd853a21b5b6d87a18", "9fac7d2951d486ced7de08631bbb76970f3f6552", "473b28781bdf15e19e446c8ab69b0105952eb0db", "afaee3cacd95931008f8e50442e2c1f7e44ce691", "8410b3f7a8287f3578bbd4b390af6482aced7adc", "f46c6679517df3380cf3a16b922ee1edcf98a097", "8f4986aa1fa8fe96c383b1d8adb2d6b243577471", "1f18479f19ee690de8aab335fafd244c8aa6f17c", "2ddda8d8ea269e2744757f1ea19f7c7009255591" };
			if (dls.length != hashes.length) {
				logger.logError("dls to hashes length mismatch!");
				return;
			}
			for (int ui = 0; ui < dls.length; ui++) {
				String up = dls[ui];
				if (windows && up.endsWith(".so")) continue;
				if (!windows && up.endsWith(".dll")) continue;
				File mime = fileManager.getBaseFile(up);
				mime.getParentFile().mkdirs();
				if (!mime.exists()) {
					mime.createNewFile();
					logger.log("Downloading " + up + "...");
					int si = rand.nextInt(dlMirrors.length);
					int ei = si == 0 ? (dlMirrors.length) : -1;
					for (int i = si;; i++) {
						if (i == ei) break;
						if (i == dlMirrors.length) i = 0;
						byte[] md;
						try {
							URL url = new URL(dlMirrors[i] + up);
							HttpURLConnection huc = (HttpURLConnection) url.openConnection();
							huc.setRequestProperty("User-Agent", "Avuna " + VERSION);
							InputStream in = huc.getInputStream();
							int bi = 1;
							byte[] buf = new byte[4096];
							ByteArrayOutputStream bout = new ByteArrayOutputStream();
							while (bi > 0) {
								bi = in.read(buf);
								if (bi > 0) {
									bout.write(buf, 0, bi);
								}
							}
							in.close();
							md = bout.toByteArray();
						}catch (Exception e) {
							logger.logError("Mirror " + dlMirrors[i] + " failed to send file. Continuing to next mirror.");
							continue;
						}
						sha1.update(md);
						String cs = fileManager.bytesToHex(sha1.digest()).toLowerCase();
						if (!cs.equals(hashes[ui])) {
							logger.logError("Mirror " + dlMirrors[i] + " sent corrupted/out of date/too new file. Continuing to next mirror.");
							continue;
						}
						FileOutputStream fout = new FileOutputStream(mime);
						fout.write(md);
						fout.flush();
						fout.close();
						break;
					}
				}
				if (!mime.exists()) {
					logger.logError("Failed to download file: " + up + " from all mirrors, or there were no mirrors.");
				}
			}
		}catch (IOException e) {
			logger.logError(e);
		}
	}
	
	/** Reads in mime.txt file to {@link #extensionToMime}
	 * 
	 * @throws IOException */
	public static void loadUnpacked() {
		try {
			File mime = fileManager.getBaseFile("mime.txt");
			Scanner s = new Scanner(new FileInputStream(mime));
			while (s.hasNextLine()) {
				String line = s.nextLine().trim();
				if (line.length() > 0) {
					String[] ls = line.split(" ");
					if (ls.length > 1) {
						for (int i = 1; i < ls.length; i++) {
							extensionToMime.put(ls[i], ls[0]);
						}
					}
				}
			}
			s.close();
		}catch (IOException e) {
			logger.logError(e);
		}
	}
	
	public static final Map<String, Host> hosts = Collections.synchronizedMap(new LinkedHashMap<String, Host>());
	
	public static final List<String> bannedIPs = Collections.synchronizedList(new ArrayList<String>());
	
	public static long lastbipc = 0L;
	public static final boolean windows = System.getProperty("os.name").toLowerCase().contains("windows");
	public static boolean legacy = false;
	
	/** This is the main method for running Avuna
	 * <p>
	 * If first arg is "cmd" attach remote access service {@link ComClient#run(String, int)} <br>
	 * Optional args (cmd {@code ip} {@code port}):
	 * <ul>
	 * <li>ip - formated "x.x.x.x" (default: 127.0.0.1)
	 * <li>port - integer (default: 6049)
	 * </ul>
	 * <p>
	 * If first arg is "ucmd" attach remote access service (cmd via unix socket) {@link ComClient#runUnix(String)} <br>
	 * Optional args (ucmd {@code path/to/socket}):
	 * <ul>
	 * <li>unix socket file path
	 * </ul>
	 * <p>
	 * If first arg is "setid" process executable command with de-escalated privileges via {@link ProcessBuilder} <br>
	 * Required args (setid {@code uid} {@code gid} {@code exec...}):
	 * <ul>
	 * <li>uid - user id to use
	 * <li>gid - group id to use
	 * <li>exec... - executable command + arguments
	 * </ul>
	 * If first arg is "unpack" or single argument (hopefully a file name) import and store in main.cfg otherwise get "main.cfg" from either executable parent directory or C:\Avuna\ or /etc/Avuna/
	 * <p>
	 * main.cfg file is verified via {@link Config#Config(String, File, ConfigFormat)} and creates if necessary, main.cfg, adding missing elements with default values. main.cfg as {@link #mainConfig} is loaded, optionally saved if "unpack" arg used.
	 * <p>
	 * {@link #unpack()} runs {@link #setupScripts()} to create default scripts and reads mime.txt and Jni object files.
	 * <p>
	 * {@link #loadUnpacked()} loads mime.txt else, creates mime.txt from unpack dir to {@link #extensionToMime}
	 * <p>
	 * Extend Host Protocol types with {@link HostRegistry#addHost(Protocol, Class)} and create any required directories or files via {@link HostDNS#unpack()} {@link HostHTTP#unpack()} {@link HostHTTPM#unpack()} or {@link HostMail#unpack()}
	 * <p>
	 * hosts.cfg file is verified via {@link Config#Config(String, File, ConfigFormat)} and creates if necessary, hosts.cfg, adding missing elements with default values. hosts.cfg as {@link #hostsConfig} is loaded and saved.
	 * <p>
	 * Create default directories via {@link #setupFolders()}
	 * <p>
	 * Create new log file for each host in {@link #hostsConfig} and start {@link Logger#INSTANCE} add host configs to log. Load base plug-ins {@link HostHTTP#loadBases()} if host types contain "http". If first arg "unpack", then exit.
	 * <p>
	 * Start each host in {@link #hostsConfig} from hosts.values via {@link Host#start()}
	 * <p>
	 * Set SafeMode if {@link #mainConfig} safemode is true via {@link SafeMode#setPerms(File, int, int)}
	 * <p>
	 * Check if started as root user (uid=0). Wait for hosts to load. Check if {@link CLib#INSTANCE} uid & gid have been changed to values in {@link #mainConfig}. Log de-escalation status of process.
	 * <p>
	 * If host types include "http" load custom plug-ins {@link HostHTTP#loadCustoms()}
	 * <p>
	 * Set {@link Runtime#addShutdownHook(Thread)}
	 * <p>
	 * Start command line processor {@link Scanner#hasNextLine()}, {@link Scanner#nextLine()}, {@link CommandRegistry#processCommand(String, CommandContext)}
	 * 
	 * @param args string array arguments to run Avuna */
	public static void main(String[] args) {
		try {
			boolean dosetid = false;
			boolean doucmd = false;
			if (args.length >= 1) {
				if (args[0].equals("cmd")) {
					String ip = args.length >= 2 ? args[1] : "127.0.0.1";
					int port = args.length >= 3 ? Integer.parseInt(args[2]) : 6049;
					ComClient.run(ip, port);
					return;
				}else if (args[0].equals("ucmd")) {
					if (windows) {
						System.out.println("MUST be on a unix system!");
						return;
					}
					if (args.length != 2) {
						System.out.println("MUST specify unix socket file!");
						return;
					}
					doucmd = true;
				}else if (args[0].equals("setid")) {
					if (windows) {
						System.out.println("Unix only!");
						return;
					}
					if (args.length < 4) {
						System.out.println("Usage: setid <mainConfigFile> <uid> <gid> <exec...>");
						return;
					}
					dosetid = true;
				}
				
			}
			
			System.setProperty("line.separator", crlf);
			final boolean unpack = args.length >= 1 && args[0].equals("unpack");
			File us = null;
			try {
				us = new File(AvunaHTTPD.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath());
			}catch (Exception e) {}
			File fcfg = null;
			if (dosetid) {
				fcfg = new File(args[1]);
			}else if (!doucmd && ((unpack && args.length == 2) || (!unpack && args.length == 1))) {// TODO: ucmd set main.cfg?
				fcfg = new File(args[unpack ? 1 : 0]);
			}else if (us != null) {
				fcfg = new File(us.getParentFile(), "main.cfg");
			}else {
				fcfg = new File((windows ? "C:\\avuna\\main.cfg" : "/etc/avuna/main.cfg"));
			}
			final File cfg = fcfg;
			mainConfig = new Config("main", cfg, new ConfigFormat() {
				public void format(ConfigNode map) {
					File dir = null;
					if (!map.containsNode("dir")) {
						map.insertNode("dir", (dir = cfg.getParentFile()).getAbsolutePath());
					}else {
						dir = new File(map.getNode("dir").getValue());
					}
					if (!map.containsNode("hosts")) map.insertNode("hosts", new File(dir, "hosts.cfg").toString(), "main hosts file");
					if (!map.containsNode("logs")) map.insertNode("logs", new File(dir, "logs").toString(), "logs folder");
					if (!map.containsNode("javac")) map.insertNode("javac", "javac", "command for javac for comp command.");
					if (!map.containsNode("forceLegacy")) map.insertNode("forceLegacy", "false", "If true, will force use of no-jni mode. Useful for libc < 2.14");
					if (!windows && !map.containsNode("uid")) map.insertNode("uid", unpack ? "6833" : "0", "uid to de-escalate to, must be ran as root");
					if (!windows && !map.containsNode("gid")) map.insertNode("gid", unpack ? "6833" : "0", "gid to de-escalate to, must be ran as root");
					if (!windows && !map.containsNode("safeMode")) map.insertNode("safeMode", "true", "if true, automatically enforces file permissions. generally reccomended to prevent critical misconfiguration.");
				}
			});
			mainConfig.load();
			legacy = mainConfig.getValue("forceLegacy").equals("true");
			Logger.init(fileManager.getLogs());
			logger = new Logger("");
			unpack();
			download();
			if (!CLib.failed) CLib.umask(0007);
			if (unpack || CLib.failed || (!CLib.failed && CLib.getuid() == 0)) {
				mainConfig.save();
			}
			loadUnpacked();
			if (doucmd) {
				ComClient.runUnix(args[1]);
				return;
			}else if (dosetid) {
				int wuid = Integer.parseInt(args[2]);
				int wgid = Integer.parseInt(args[3]);
				CLib.setgid(wgid);
				CLib.setuid(wuid);
				int uid = CLib.getuid();
				int gid = CLib.getgid();
				System.out.println("setuid = " + uid + " (wanted " + wuid + ")");
				System.out.println("setgid = " + gid + " (wanted " + wgid + ")");
				if (uid != wuid || gid != wgid) {
					System.out.println("Failed to de-escalate, terminating!");
					return;
				}
				String[] rargs = new String[args.length - 4];
				System.arraycopy(args, 4, rargs, 0, rargs.length);
				ProcessBuilder pb = new ProcessBuilder(rargs);
				pb.redirectErrorStream(true);
				Process p = pb.start();
				p.waitFor();
				InputStream in = p.getInputStream();
				while (in.available() > 0) {
					System.out.append((char) in.read());
				}
				return;
			}
			if (!CLib.failed && CLib.getuid() == 0) {
				System.out.println("[NOTIFY] Running as root, will load servers and attempt de-escalate, if configured.");
			}
			/*{
				try {
					UNIOServerSocket server = new UNIOServerSocket("0.0.0.0", 80, new PacketReceiverFactory() {
						
						@Override
						public PacketReceiver newCallback() {
							return new PacketReceiver() {
								
								@Override
								public void readPacket(UNIOSocket sock, byte[] buf) {
									System.out.println("recvd: " + buf.length);
									System.out.println(new String(buf));
								}
								
								@Override
								public int nextDelimType(UNIOSocket sock) {
									return 0;
								}
								
								public byte[] nextDelim(UNIOSocket sock) {
									return "\r\n\r\n".getBytes();
								}
								
								@Override
								public void closed(UNIOSocket sock) {
									System.out.println("closed.");
								}
								
							};
						}
						
					});
					server.bind();
					Poller poller = new Poller();
					while (!server.isClosed()) {
						UNIOSocket s = server.accept();
						System.out.println("connected!");
						poller.addSocket(s);
						while (!s.isClosed()) {
							poller.poll();
						}
					}
				}catch (Exception e) {
					e.printStackTrace();
				}
				if (true) return;
			}*/
			GNUTLS.nothing(); // calls the static initializer
			HostRegistry.addHost(Protocol.HTTP, HostHTTP.class);
			HostRegistry.addHost(Protocol.COM, HostCom.class);
			HostRegistry.addHost(Protocol.DNS, HostDNS.class);
			HostRegistry.addHost(Protocol.MAIL, HostMail.class);
			HostRegistry.addHost(Protocol.FTP, HostFTP.class);
			HostHTTP.unpack();
			HostCom.unpack();
			HostDNS.unpack();
			HostMail.unpack();
			HostFTP.unpack();
			hostsConfig = new Config("hosts", new File(mainConfig.getNode("hosts").getValue()), new ConfigFormat() {
				boolean rf = false;
				
				@Override
				public void format(ConfigNode map) {
					boolean nc = false, nd = false;
					if (!map.containsNode("main")) {
						map.insertNode("main", null, "main node, must exist");
					}
					if (!map.containsNode("com")) {
						map.insertNode("com", null, "set enabled=true to use the local/remote command system.");
						nc = true;
					}
					// if (!map.containsKey("dns")) {
					// map.put("dns", new LinkedHashMap<String, Object>());
					// nd = true;
					// }
					if (!rf) for (String key : map.getSubnodes()) {
						ConfigNode host = map.getNode(key);
						if (!host.containsNode("enabled")) host.insertNode("enabled", (nc && key.equals("com")) ? "false" : "true");
						if (!host.containsNode("protocol")) host.insertNode("protocol", ((nd && key.equals("dns")) ? "dns" : ((nc && key.equals("com")) ? "com" : "http")), "set to http/com/dns/mail/ftp for respective servers, load Avuna with these to have other config options autofill.");
						Protocol p = Protocol.fromString(host.getNode("protocol").getValue());
						if (p == null) {
							Logger.stdout.println("Skipping Host: " + key + " due to invalid protocol!");
							continue;
						}
						if (!host.getNode("enabled").getValue().equals("true")) {
							continue;
						}
						try {
							Host h = (Host) HostRegistry.getHost(p).getConstructors()[0].newInstance(key);
							h.formatConfig(host);
							hosts.put(key, h);
						}catch (Exception e) {
							e.printStackTrace(Logger.stderr);
							continue;
						}
					}
					rf = true;
				}
				
			});
			hostsConfig.load();
			hostsConfig.save();
			setupFolders();
			logger.log("Loaded Configs");
			if (unpack) {
				logger.log("Unpack complete, terminating.");
				logger.flush();
				System.exit(0);
			}
			logger.log("Loading Connection Handling");
			for (Host h : hosts.values()) {
				if (!h.hasStarted()) h.start();
			}
			if (!CLib.failed && mainConfig.getNode("safeMode").getValue().equals("true")) {
				SafeMode.recurPerms(cfg.getParentFile(), Integer.parseInt(mainConfig.getNode("uid").getValue()), Integer.parseInt(mainConfig.getNode("gid").getValue()));
			}
			if (!CLib.failed && CLib.getuid() == 0 && !mainConfig.getNode("uid").getValue().equals("0")) {
				major: while (true) {
					for (Host h : hosts.values()) {
						if (!h.loaded) {
							Thread.sleep(1L);
							continue major;
						}
					}
					break;
				}
				CLib.setgid(Integer.parseInt(mainConfig.getNode("gid").getValue()));
				CLib.setuid(Integer.parseInt(mainConfig.getNode("uid").getValue()));
				logger.log("[NOTIFY] De-escalated to uid " + CLib.getuid());
			}else if (!CLib.failed) {
				logger.log("[NOTIFY] We did NOT de-escalate, currently running as uid " + CLib.getuid());
			}
			Runtime.getRuntime().addShutdownHook(new Thread() {
				public void run() {
					logger.log("Softly Terminating!");
					for (Host h : hosts.values()) {
						h.preExit();
					}
					Logger.flushAll();
				}
			});
			for (Host host : hosts.values()) {
				if (host instanceof HostHTTP) {
					((HostHTTP) host).postload();
				}
			}
		}catch (Exception e) {
			if (logger == null) {
				e.printStackTrace();
			}else {
				logger.logError(e);
			}
		}
		@SuppressWarnings("resource")
		Scanner scan = new Scanner(System.in);
		while (scan.hasNextLine()) {
			try {
				String command = scan.nextLine();
				commandRegistry.processCommand(command, mainCommandContext);
			}catch (NoSuchElementException fe) {
				break;
			}catch (Exception e) {
				logger.logError(e);
			}
		}
	}
}
