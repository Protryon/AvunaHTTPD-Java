package com.javaprophet.javawebserver.hosts;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.SocketException;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.LinkedHashMap;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import com.javaprophet.javawebserver.JavaWebServer;
import com.javaprophet.javawebserver.util.Logger;

public abstract class Host extends Thread {
	protected final String ip, keyPassword, keystorePassword, name;
	protected final File keyFile;
	protected final int port;
	protected final boolean isSSL;
	protected final Protocol protocol;
	
	public Host(String threadName, String ip, int port, boolean isSSL, File keyFile, String keyPassword, String keystorePassword, Protocol protocol) {
		super(threadName + " Host");
		setDaemon(true);
		this.name = threadName;
		this.ip = ip;
		this.port = port;
		this.isSSL = isSSL;
		this.keyFile = keyFile;
		this.keyPassword = keyPassword;
		this.keystorePassword = keystorePassword;
		this.protocol = protocol;
	}
	
	public void setupFolders() {
		
	}
	
	public final LinkedHashMap<String, Object> getConfig() {
		return (LinkedHashMap<String, Object>)JavaWebServer.hostsConfig.get(name);
	}
	
	public final void run() {
		Logger.log("Starting " + name + "/" + protocol.name + " Server on " + ip + ":" + port);
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
			setup(server);
		}catch (Exception e) {
			if (!(e instanceof SocketException)) Logger.logError(e);
			Logger.log("Closing " + name + "/" + protocol.name + " Server on " + ip + ":" + port);
		}finally {
			
		}
	}
	
	public void formatConfig(HashMap<String, Object> map) {
		
	}
	
	public String getHostname() {
		return name;
	}
	
	public int getPort() {
		return port;
	}
	
	public abstract void setup(ServerSocket s);
}
