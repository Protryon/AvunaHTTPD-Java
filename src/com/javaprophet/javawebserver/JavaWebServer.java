package com.javaprophet.javawebserver;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import org.json.simple.JSONObject;
import com.javaprophet.javawebserver.networking.Connection;
import com.javaprophet.javawebserver.plugins.PatchBus;
import com.javaprophet.javawebserver.plugins.base.BaseLoader;
import com.javaprophet.javawebserver.util.Config;
import com.javaprophet.javawebserver.util.ConfigFormat;
import com.javaprophet.javawebserver.util.FileManager;

public class JavaWebServer {
	public static final String VERSION = "1.0";
	public static Config mainConfig;
	public static ArrayList<Connection> runningThreads = new ArrayList<Connection>();
	public static final FileManager fileManager = new FileManager();
	public static final PatchBus patchBus = new PatchBus();
	public static final HashMap<String, String> extensionToMime = new HashMap<String, String>();
	
	public static void setupFolders() {
		fileManager.getMainDir().mkdirs();
		fileManager.getHTDocs().mkdirs();
		fileManager.getPlugins().mkdirs();
		fileManager.getTemp().mkdirs();
		fileManager.getSSL().mkdirs();
		patchBus.setupFolders();
	}
	
	public static void unpack() {
		try {
			File mime = fileManager.getBaseFile("mime.txt");
			if (!mime.exists()) {
				System.out.println("Unpacking mime.txt...");
				InputStream in = JavaWebServer.class.getResourceAsStream("/unpack/mime.txt");
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
		}catch (IOException e) {
			e.printStackTrace();
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
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		try {
			System.out.println("Loading Configs");
			mainConfig = new Config(new File("C:\\jws\\main.cfg"), new ConfigFormat() {
				public void format(JSONObject json) {
					if (!json.containsKey("version")) json.put("version", JavaWebServer.VERSION);
					if (!json.containsKey("dir")) json.put("dir", "C:\\jws");
					if (!json.containsKey("htdocs")) json.put("htdocs", "htdocs");
					if (!json.containsKey("plugins")) json.put("plugins", "plugins");
					if (!json.containsKey("temp")) json.put("temp", "temp");
					if (!json.containsKey("bindport")) json.put("bindport", 80);
					if (!json.containsKey("errorpages")) json.put("errorpages", new JSONObject());
					if (!json.containsKey("index")) json.put("index", "index.php,index.php");
					if (!json.containsKey("ssl")) json.put("ssl", new JSONObject());
					JSONObject ssl = (JSONObject)json.get("ssl");
					if (!ssl.containsKey("enabled")) ssl.put("enabled", false);
					if (!ssl.containsKey("forceSSL")) ssl.put("forceSSL", false);
					if (!ssl.containsKey("bindport")) ssl.put("bindport", 443);
					if (!ssl.containsKey("folder")) ssl.put("folder", "ssl");
					if (!ssl.containsKey("keyFile")) ssl.put("keyFile", "keystore");
					if (!ssl.containsKey("keystorePassword")) ssl.put("keystorePassword", "password");
					if (!ssl.containsKey("keyPassword")) ssl.put("keyPassword", "password");
				}
			});
			mainConfig.load();
			setupFolders();
			unpack();
			loadUnpacked();
			System.out.println("Loading Base Plugins");
			BaseLoader.loadBases();
			int bindport = Integer.parseInt(mainConfig.get("bindport").toString());
			System.out.println("Starting Server on " + bindport);
			Thread tnssl = new Thread() {
				public void run() {
					try {
						ServerSocket server = new ServerSocket(bindport);
						while (!server.isClosed()) {
							Socket s = server.accept();
							DataOutputStream out = new DataOutputStream(s.getOutputStream());
							out.flush();
							DataInputStream in = new DataInputStream(s.getInputStream());
							Connection c = new Connection(s, in, out, false);
							c.start();
							runningThreads.add(c);
						}
					}catch (Exception e) {
						e.printStackTrace();
					}
				}
			};
			tnssl.start();
			if ((boolean)((JSONObject)mainConfig.get("ssl")).get("enabled")) {
				Thread tssl = new Thread() {
					public void run() {
						try {
							KeyStore ks = KeyStore.getInstance("JKS");
							InputStream ksIs = new FileInputStream(fileManager.getSSLKeystore());
							try {
								ks.load(ksIs, ((JSONObject)mainConfig.get("ssl")).get("keystorePassword").toString().toCharArray());
							}finally {
								if (ksIs != null) {
									ksIs.close();
								}
							}
							KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
							kmf.init(ks, ((JSONObject)mainConfig.get("ssl")).get("keyPassword").toString().toCharArray());
							TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
								public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {
								}
								
								public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {
								}
								
								public java.security.cert.X509Certificate[] getAcceptedIssuers() {
									return null;
								}
							}};
							SSLContext sc = SSLContext.getInstance("TLSv1.2");
							sc.init(kmf.getKeyManagers(), trustAllCerts, new SecureRandom());
							int sslport = Integer.parseInt(((JSONObject)mainConfig.get("ssl")).get("bindport").toString());
							System.out.println("Starting SSLServer on " + sslport);
							SSLServerSocket sslserver = (SSLServerSocket)sc.getServerSocketFactory().createServerSocket(sslport);
							sslserver.setEnabledProtocols(new String[]{"TLSv1", "TLSv1.1", "TLSv1.2"});
							while (!sslserver.isClosed()) {
								Socket s = sslserver.accept();
								DataOutputStream out = new DataOutputStream(s.getOutputStream());
								out.flush();
								DataInputStream in = new DataInputStream(s.getInputStream());
								Connection c = new Connection(s, in, out, true);
								c.start();
								runningThreads.add(c);
							}
						}catch (Exception e) {
							e.printStackTrace();
						}
					}
				};
				tssl.start();
			}
		}catch (Exception e) {
			e.printStackTrace();
		}
		patchBus.preExit();
		if (mainConfig != null) {
			mainConfig.save();
		}
	}
}
