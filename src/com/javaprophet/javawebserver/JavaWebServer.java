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
import com.javaprophet.javawebserver.networking.command.ComClient;
import com.javaprophet.javawebserver.networking.command.ComServer;
import com.javaprophet.javawebserver.networking.command.CommandProcessor;
import com.javaprophet.javawebserver.plugins.PatchBus;
import com.javaprophet.javawebserver.plugins.base.BaseLoader;
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
	public static final String crlf = new String(new byte[]{13, 10});
	
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
			if (args.length >= 1 && args[0].equals("cmd")) {
				String ip = args.length >= 2 ? args[1] : "127.0.0.1";
				int port = args.length >= 3 ? Integer.parseInt(args[2]) : 6049;
				ComClient.run(ip, port);
				return;
			}
			System.setProperty("line.separator", crlf);
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
					if (!map.containsKey("bindport")) map.put("bindport", "80");
					if (!map.containsKey("bindip")) map.put("bindip", "0.0.0.0");
					if (!map.containsKey("nginxThreadCount")) map.put("nginxThreadCount", "" + Runtime.getRuntime().availableProcessors());
					if (!map.containsKey("errorpages")) map.put("errorpages", new HashMap<String, Object>());
					if (!map.containsKey("index")) map.put("index", "Index.class,index.jwsl,index.php,index.html");
					if (!map.containsKey("cacheClock")) map.put("cacheClock", "-1");
					if (!map.containsKey("com")) map.put("com", new HashMap<String, Object>());
					HashMap<String, Object> telnet = (HashMap<String, Object>)map.get("com");
					if (!telnet.containsKey("enabled")) telnet.put("enabled", "true");
					if (!telnet.containsKey("bindport")) telnet.put("bindport", "6049");
					if (!telnet.containsKey("bindip")) telnet.put("bindip", "0.0.0.0");
					if (!telnet.containsKey("auth")) telnet.put("auth", "jwsisawesome");
					if (!telnet.containsKey("doAuth")) telnet.put("doAuth", "true");
					if (!map.containsKey("ssl")) map.put("ssl", new HashMap<String, Object>());
					HashMap<String, Object> ssl = (HashMap<String, Object>)map.get("ssl");
					if (!ssl.containsKey("enabled")) ssl.put("enabled", "false");
					if (!ssl.containsKey("forceSSL")) ssl.put("forceSSL", "false"); // TODO: implement
					if (!ssl.containsKey("bindport")) ssl.put("bindport", "443");
					if (!ssl.containsKey("folder")) ssl.put("folder", new File(dir, "ssl").toString());
					if (!ssl.containsKey("keyFile")) ssl.put("keyFile", "keystore");
					if (!ssl.containsKey("keystorePassword")) ssl.put("keystorePassword", "password");
					if (!ssl.containsKey("keyPassword")) ssl.put("keyPassword", "password");
				}
			});
			mainConfig.load();
			mainConfig.save();
			setupFolders();
			File lf = new File(fileManager.getLogs(), "" + (System.currentTimeMillis() / 1000L));
			lf.createNewFile();
			Logger.INSTANCE = new Logger(new PrintStream(new FileOutputStream(lf)));
			unpack();
			loadUnpacked();
			Logger.log("Loaded Configs");
			Logger.log("Loading Connection Handling");
			Connection.init();
			Logger.log("Loading Base Plugins");
			BaseLoader.loadBases();
			HashMap<String, Object> com = ((HashMap<String, Object>)mainConfig.get("com"));
			if (((String)com.get("enabled")).equals("true")) {
				Logger.log("Starting Com server on " + ((String)com.get("bindip")) + ":" + ((String)com.get("bindport")));
				ComServer server = new ComServer();
				server.start();
			}
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
							s.setSoTimeout(1000);
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
					CommandProcessor.process(command, Logger.INSTANCE, scan, false);
				}catch (NoSuchElementException fe) {
					read = false;
					continue;
				}catch (Exception e) {
					Logger.logError(e);
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
		System.exit(0);
	}
}
