package com.javaprophet.javawebserver;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.NoSuchElementException;
import java.util.Scanner;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import com.javaprophet.javawebserver.http.ResponseGenerator;
import com.javaprophet.javawebserver.networking.Connection;
import com.javaprophet.javawebserver.plugins.PatchBus;
import com.javaprophet.javawebserver.plugins.base.BaseLoader;
import com.javaprophet.javawebserver.plugins.javaloader.PatchJavaLoader;
import com.javaprophet.javawebserver.util.Config;
import com.javaprophet.javawebserver.util.ConfigFormat;
import com.javaprophet.javawebserver.util.FileManager;
import com.javaprophet.javawebserver.util.Logger;

public class JavaWebServer {
	public static final String VERSION = "1.0";
	public static Config mainConfig;
	public static ArrayList<Connection> runningThreads = new ArrayList<Connection>();
	public static final FileManager fileManager = new FileManager();
	public static final PatchBus patchBus = new PatchBus();
	public static final HashMap<String, String> extensionToMime = new HashMap<String, String>();
	public static final ResponseGenerator rg = new ResponseGenerator();
	
	public static void setupFolders() {
		fileManager.getMainDir().mkdirs();
		fileManager.getHTDocs().mkdirs();
		fileManager.getLogs().mkdirs();
		fileManager.getHTSrc().mkdirs();
		fileManager.getPlugins().mkdirs();
		fileManager.getTemp().mkdirs();
		fileManager.getSSL().mkdirs();
		patchBus.setupFolders();
	}
	
	public static void unpack() {
		try {
			String[] unpacks = new String[]{"mime.txt", "run.sh", "kill.sh", "restart.sh"};
			for (String up : unpacks) {
				File mime = fileManager.getBaseFile(up);
				if (!mime.exists()) {
					Logger.log("Unpacking " + up + "...");
					InputStream in = JavaWebServer.class.getResourceAsStream("/unpack/" + up);
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
			Logger.logError(e);
		}
	}
	
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
			Logger.logError(e);
		}
	}
	
	private static boolean nsslr = false;
	private static boolean sslr = false;
	private static boolean ap = false;
	
	public static final ArrayList<String> bannedIPs = new ArrayList<String>();
	
	public static void main(String[] args) {
		try {
			final File cfg = new File(args.length > 0 ? args[0] : (System.getProperty("os.name").toLowerCase().contains("windows") ? "C:\\jws\\main.cfg" : "/etc/jws/main.cfg"));
			mainConfig = new Config(cfg, new ConfigFormat() {
				public void format(HashMap<String, Object> map) {
					if (!map.containsKey("version")) map.put("version", JavaWebServer.VERSION);
					File dir = null;
					if (!map.containsKey("dir")) {
						map.put("dir", (dir = cfg.getParentFile()).getAbsolutePath());
					}else {
						dir = new File((String)map.get("dir"));
					}
					if (!map.containsKey("htdocs")) map.put("htdocs", new File(dir, "htdocs").toString());
					if (!map.containsKey("htsrc")) map.put("htsrc", new File(dir, "htsrc").toString());
					if (!map.containsKey("logs")) map.put("logs", new File(dir, "logs").toString());
					if (!map.containsKey("plugins")) map.put("plugins", new File(dir, "plugins").toString());
					if (!map.containsKey("javac")) map.put("javac", "javac");
					if (!map.containsKey("temp")) map.put("temp", new File(dir, "temp").toString());
					if (!map.containsKey("bindport")) map.put("bindport", 80);
					if (!map.containsKey("bindip")) map.put("bindip", "0.0.0.0");
					if (!map.containsKey("nginxThreadCount")) map.put("nginxThreadCount", Runtime.getRuntime().availableProcessors());
					if (!map.containsKey("errorpages")) map.put("errorpages", new HashMap<String, Object>());
					if (!map.containsKey("index")) map.put("index", "Index.class,index.jwsl,index.php,index.html");
					if (!map.containsKey("cacheClock")) map.put("cacheClock", -1);
					if (!map.containsKey("ssl")) map.put("ssl", new HashMap<String, Object>());
					HashMap<String, Object> ssl = (HashMap<String, Object>)map.get("ssl");
					if (!ssl.containsKey("enabled")) ssl.put("enabled", false);
					if (!ssl.containsKey("forceSSL")) ssl.put("forceSSL", false); // TODO: implement
					if (!ssl.containsKey("bindport")) ssl.put("bindport", 443);
					if (!ssl.containsKey("folder")) ssl.put("folder", new File(dir, "ssl").toString());
					if (!ssl.containsKey("keyFile")) ssl.put("keyFile", "keystore");
					if (!ssl.containsKey("keystorePassword")) ssl.put("keystorePassword", "password");
					if (!ssl.containsKey("keyPassword")) ssl.put("keyPassword", "password");
				}
			});
			mainConfig.load();
			mainConfig.save();
			File lf = new File(fileManager.getLogs(), "" + (System.currentTimeMillis() / 1000L));
			lf.createNewFile();
			Logger.INSTANCE = new Logger(new PrintStream(new FileOutputStream(lf)));
			setupFolders();
			unpack();
			loadUnpacked();
			Logger.log("Loaded Configs");
			Logger.log("Loading Connection Handling");
			Connection.init();
			Logger.log("Loading Base Plugins");
			BaseLoader.loadBases();
			final int bindport = Integer.parseInt((String)mainConfig.get("bindport"));
			final String bindip = (String)mainConfig.get("bindip");
			Logger.log("Starting Server on " + bindip + ":" + bindport);
			Thread tnssl = new Thread() {
				public void run() {
					try {
						ServerSocket server = new ServerSocket(bindport, 1000, InetAddress.getByName(bindip));
						nsslr = true;
						ap = true;
						while (!server.isClosed()) {
							Socket s = server.accept();
							if (bannedIPs.contains(s.getInetAddress().getHostAddress())) {
								s.close();
								continue;
							}
							DataOutputStream out = new DataOutputStream(s.getOutputStream());
							out.flush();
							DataInputStream in = new DataInputStream(s.getInputStream());
							Connection c = new Connection(s, in, out, false);
							c.handleConnection();
							runningThreads.add(c);
						}
						Logger.log("Server Closed on " + bindip + ":" + bindport);
					}catch (Exception e) {
						Logger.logError(e);
						ap = true;
					}
					nsslr = false;
				}
			};
			tnssl.start();
			final HashMap<String, Object> ssl = (HashMap<String, Object>)mainConfig.get("ssl");
			if (((String)ssl.get("enabled")).equals("true")) {
				Thread tssl = new Thread() {
					public void run() {
						try {
							KeyStore ks = KeyStore.getInstance("JKS");
							InputStream ksIs = new FileInputStream(fileManager.getSSLKeystore());
							try {
								ks.load(ksIs, ssl.get("keystorePassword").toString().toCharArray());
							}finally {
								if (ksIs != null) {
									ksIs.close();
								}
							}
							KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
							kmf.init(ks, ssl.get("keyPassword").toString().toCharArray());
							TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
								public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {
								}
								
								public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {
								}
								
								public java.security.cert.X509Certificate[] getAcceptedIssuers() {
									return null;
								}
							}};
							SSLContext sc = null;
							String[] possibleProtocols = new String[]{"TLSv1.2", "TLSv1.1", "TLSv1", "TLSv1.0"};
							String fp = "";
							for (String protocol : possibleProtocols) {
								try {
									sc = SSLContext.getInstance(protocol);
									fp = protocol;
								}catch (NoSuchAlgorithmException e) {
									continue;
								}
							}
							if (sc == null) {
								Logger.log("No suitable TLS protocols found, please upgrade Java! SSL disabled.");
								return;
							}
							sc.init(kmf.getKeyManagers(), trustAllCerts, new SecureRandom());
							int sslport = Integer.parseInt((String)ssl.get("bindport"));
							Logger.log("Starting SSLServer on " + sslport);
							SSLServerSocket sslserver = (SSLServerSocket)sc.getServerSocketFactory().createServerSocket(sslport, 1000, InetAddress.getByName(bindip));
							sslserver.setEnabledProtocols(new String[]{fp});
							sslr = true;
							ap = true;
							while (!sslserver.isClosed()) {
								Socket s = sslserver.accept();
								DataOutputStream out = new DataOutputStream(s.getOutputStream());
								out.flush();
								DataInputStream in = new DataInputStream(s.getInputStream());
								Connection c = new Connection(s, in, out, false);
								c.handleConnection();
								runningThreads.add(c);
							}
							Logger.log("Server Closed on " + bindip + ":" + sslport);
						}catch (Exception e) {
							Logger.logError(e);
							ap = true;
						}
						sslr = false;
					}
				};
				tssl.start();
			}
		}catch (Exception e) {
			Logger.logError(e);
			ap = true;
		}
		while (!ap) {
			try {
				Thread.sleep(100L);
			}catch (InterruptedException e) {
				Logger.logError(e);
			}
		}
		boolean read = true;
		Scanner scan = new Scanner(System.in);
		while (sslr || nsslr) {
			if (read) {
				try {
					String command = scan.nextLine();
					String[] cargs = command.contains(" ") ? command.substring(command.indexOf(" ") + 1).split(" ") : new String[0];
					command = command.contains(" ") ? command.substring(0, command.indexOf(" ")) : command;
					if (command.equals("exit") || command.equals("stop")) {
						System.exit(0);
					}else if (command.equals("reload")) {
						try {
							mainConfig.load();
							patchBus.preExit();
						}catch (Exception e) {
							Logger.logError(e);
						}
						Logger.log("Loaded Config! Some entries will require a restart.");
					}else if (command.equals("flushcache")) {
						try {
							fileManager.clearCache();
						}catch (Exception e) {
							Logger.logError(e);
						}
						Logger.log("Cache Flushed! This is not necessary for php files, and does not work for .class files(restart jws for those).");
					}else if (command.equals("jhtml")) {
						if (cargs.length != 2 && cargs.length != 1) {
							Logger.log("Invalid arguments. (input, output[optional])");
							continue;
						}
						try {
							File sc2 = null;
							Scanner scan2 = new Scanner(new FileInputStream(sc2 = new File(fileManager.getHTDocs(), cargs[0])));
							PrintStream ps;
							File temp = null;
							if (cargs.length == 2) {
								ps = new PrintStream(new FileOutputStream(temp = new File(fileManager.getHTSrc(), cargs[1])));
							}else {
								ps = new PrintStream(new FileOutputStream(temp = new File(fileManager.getHTSrc(), cargs[0].substring(0, cargs[0].indexOf(".")) + ".java")));
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
						Logger.log("JHTML completed.");
					}else if (command.equals("jcomp")) {
						boolean all = cargs.length < 1;
						String cp = fileManager.getBaseFile("jws.jar").toString() + ";" + fileManager.getHTDocs().toString() + ";" + fileManager.getHTSrc().toString() + ";" + PatchJavaLoader.lib.toString() + ";";
						for (File f : PatchJavaLoader.lib.listFiles()) {
							if (!f.isDirectory() && f.getName().endsWith(".jar")) {
								cp += f.toString() + ";";
							}
						}
						cp = cp.substring(0, cp.length() - 1);
						ArrayList<String> cfs = new ArrayList<String>();
						cfs.add((String)mainConfig.get("javac"));
						cfs.add("-cp");
						cfs.add(cp);
						cfs.add("-d");
						cfs.add(fileManager.getHTDocs().toString());
						if (all) {
							recurForComp(cfs, fileManager.getHTSrc());
						}else {
							cfs.add("htsrc/" + cargs[0]);
						}
						ProcessBuilder pb = new ProcessBuilder(cfs.toArray(new String[]{}));
						pb.directory(fileManager.getMainDir());
						pb.redirectErrorStream(true);
						Process proc = pb.start();
						Scanner s = new Scanner(proc.getInputStream());
						while (s.hasNextLine()) {
							Logger.log("javac: " + s.nextLine());
						}
						s.close();
					}else if (command.equals("help")) {
						Logger.log("Commands:");
						Logger.log("exit/stop");
						Logger.log("reload");
						Logger.log("flushcache");
						Logger.log("jhtml");
						Logger.log("jcomp");
						Logger.log("help");
						Logger.log("");
						Logger.log("Java Web Server(JWS) version " + VERSION);
					}else {
						Logger.log("Unknown Command: " + command);
					}
				}catch (NoSuchElementException fe) {
					read = false;
					continue;
				}catch (Exception e) {
					Logger.logError(e);;
				}
			}else {
				try {
					Thread.sleep(100L);
				}catch (InterruptedException e) {
					Logger.logError(e);
				}
			}
		}
		patchBus.preExit();
		if (mainConfig != null) {
			mainConfig.save();
		}
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
}
