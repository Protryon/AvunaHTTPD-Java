package com.javaprophet.javawebserver.hosts;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import com.javaprophet.javawebserver.JavaWebServer;
import com.javaprophet.javawebserver.networking.ThreadWorker;
import com.javaprophet.javawebserver.util.Logger;

public class Host extends Thread {
	private final String ip, keyPassword, keystorePassword, name;
	private final File htdocs, htsrc, keyFile;
	private final int port, cl;
	private final boolean isSSL;
	
	public File getHTDocs() {
		return htdocs;
	}
	
	public File getHTSrc() {
		return htsrc;
	}
	
	public String getHostname() {
		return name;
	}
	
	public Host(String name, String ip, int port, File htdocs, File htsrc, int cl, boolean isSSL, File keyFile, String keyPassword, String keystorePassword) {
		this.name = name;
		this.ip = ip;
		this.port = port;
		this.htdocs = htdocs;
		this.htsrc = htsrc;
		this.cl = cl;
		this.isSSL = isSSL;
		this.keyFile = keyFile;
		this.keyPassword = keyPassword;
		this.keystorePassword = keystorePassword;
	}
	
	public void setupFolders() {
		htdocs.mkdirs();
		htsrc.mkdirs();
	}
	
	public void run() {
		Logger.log("Starting " + name + " Server on " + ip + ":" + port);
		try {
			ServerSocket server = null;
			if (!isSSL) {
				server = new ServerSocket(port, 1000, InetAddress.getByName(ip));
			}else {
				KeyStore ks = KeyStore.getInstance("JKS");
				InputStream ksIs = new FileInputStream(keyFile);
				try {
					ks.load(ksIs, keystorePassword.toCharArray());
				}finally {
					if (ksIs != null) {
						ksIs.close();
					}
				}
				KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
				kmf.init(ks, keyPassword.toCharArray());
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
					Logger.log(name + ": No suitable TLS protocols found, please upgrade Java! Host not loaded.");
					return;
				}
				sc.init(kmf.getKeyManagers(), trustAllCerts, new SecureRandom());
				server = (SSLServerSocket)sc.getServerSocketFactory().createServerSocket(port, 1000, InetAddress.getByName(ip));
				((SSLServerSocket)server).setEnabledProtocols(new String[]{fp});
			}
			while (!server.isClosed()) {
				Socket s = server.accept();
				if (cl >= 0 && ThreadWorker.getQueueSize() >= cl) {
					s.close();
					continue;
				}
				if (JavaWebServer.bannedIPs.contains(s.getInetAddress().getHostAddress())) {
					s.close();
					continue;
				}
				s.setSoTimeout(1000);
				DataOutputStream out = new DataOutputStream(s.getOutputStream());
				out.flush();
				DataInputStream in = new DataInputStream(s.getInputStream());
				ThreadWorker.addWork(this, s, in, out, server instanceof SSLServerSocket);
			}
		}catch (Exception e) {
			Logger.logError(e);
		}finally {
			Logger.log("Server " + name + " Closed on " + ip + ":" + port);
		}
	}
}
