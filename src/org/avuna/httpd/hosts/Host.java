/* Avuna HTTPD - General Server Applications Copyright (C) 2015 Maxwell Bruce This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>. */

package org.avuna.httpd.hosts;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import org.avuna.httpd.AvunaHTTPD;
import org.avuna.httpd.event.Event;
import org.avuna.httpd.event.EventBus;
import org.avuna.httpd.event.IEventReceiver;
import org.avuna.httpd.event.base.EventID;
import org.avuna.httpd.event.base.EventReload;
import org.avuna.httpd.util.CLib;
import org.avuna.httpd.util.ConfigNode;
import org.avuna.httpd.util.logging.Logger;
import org.avuna.httpd.util.unio.PacketReceiver;
import org.avuna.httpd.util.unio.PacketReceiverFactory;
import org.avuna.httpd.util.unio.UNIOServerSocket;
import org.avuna.httpd.util.unixsocket.UnixServerSocket;

public abstract class Host extends Thread implements ITerminatable, IEventReceiver {
	protected final String name;
	protected final Protocol protocol;
	private boolean isStarted = false;
	public final EventBus eventBus;
	public final Logger logger;
	
	public Host(String threadName, Protocol protocol) {
		super(threadName + " Host");
		this.name = threadName;
		this.logger = new Logger(this);
		this.protocol = protocol;
		eventBus = new EventBus();
		eventBus.registerEvent(EventID.RELOAD, this, 0);
	}
	
	public void receive(EventBus bus, Event event) {
		if (event instanceof EventReload) {
			formatConfig(getConfig());
		}
	}
	
	public void setupFolders() {
		
	}
	
	private ConfigNode virtualConfig = null;
	
	public boolean hasVirtualConfig() {
		return virtualConfig != null;
	}
	
	public void setVirtualConfig(ConfigNode node) {
		this.virtualConfig = node;
	}
	
	public final ConfigNode getConfig() {
		return virtualConfig != null ? virtualConfig : AvunaHTTPD.hostsConfig.getNode(name);
	}
	
	public boolean loaded = false;
	private List<ITerminatable> terms = Collections.synchronizedList(new ArrayList<ITerminatable>());
	
	public void addTerm(ITerminatable it) {
		terms.add(it);
	}
	
	private ArrayList<ServerSocket> servers = new ArrayList<ServerSocket>();
	
	public void postload() throws IOException {
		
	}
	
	@Override
	public void terminate() {
		for (ITerminatable worker : terms) {
			worker.terminate();
		}
		for (ServerSocket server : servers) {
			try {
				server.close();
			}catch (IOException e) {
				logger.logError(e);
			}
		}
	}
	
	public final ServerSocket makeServer(String ip, int port, boolean ssl, SSLServerSocketFactory sc) throws IOException {
		logger.log("Starting " + name + "/" + protocol.name + " " + (ssl ? "TLS-" : "") + "Server on " + ip + ":" + port);
		if (ssl) {
			try {
				ServerSocket server = sc.createServerSocket(port, 50, InetAddress.getByName(ip));
				servers.add(server);
				return server;
			}catch (Exception e) {
				logger.logError(e);
				return null;
			}
		}else {
			ServerSocket server = unio() ? new UNIOServerSocket(ip, port, new PacketReceiverFactory() {
				
				@Override
				public PacketReceiver newCallback() {
					return makeReceiver();
				}
				
			}) : new ServerSocket(port, 1000, InetAddress.getByName(ip));
			if (server instanceof UNIOServerSocket) ((UNIOServerSocket) server).bind();
			servers.add(server);
			return server;
		}
	}
	
	public final UnixServerSocket makeUnixServer(String file) throws IOException {
		logger.log("Starting " + name + "/" + protocol.name + " " + "Server on " + file);
		UnixServerSocket uss = new UnixServerSocket(file);
		servers.add(uss);
		return uss;
	}
	
	public final SSLContext makeSSLContext(File keyFile, String keyPassword, String keystorePassword) throws IOException {
		try {
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
			TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
				public void checkClientTrusted(X509Certificate[] certs, String authType) {}
				
				public void checkServerTrusted(X509Certificate[] certs, String authType) {}
				
				public X509Certificate[] getAcceptedIssuers() {
					return null;
				}
			} };
			SSLContext sc = null;
			String[] possibleProtocols = new String[] { "TLSv1.3", "TLSv1.2", "TLSv1.1", "TLSv1", "TLSv1.0" };
			for (int i = 0; i < possibleProtocols.length; i++) {
				try {
					sc = SSLContext.getInstance(possibleProtocols[i]);
					String[] tp = new String[possibleProtocols.length - i];
					System.arraycopy(possibleProtocols, i, tp, 0, tp.length);
					possibleProtocols = tp;
					break;
				}catch (NoSuchAlgorithmException e) {
					continue;
				}
			}
			if (sc == null) {
				logger.log(name + ": No suitable TLS protocols found, please upgrade Java! Host not loaded.");
				return null;
			}
			sc.init(kmf.getKeyManagers(), trustAllCerts, new SecureRandom());
			return sc;
		}catch (Exception e) {
			logger.logError(e);
			return null;
		}
	}
	
	public SSLContext sslContext = null;
	
	public boolean hasStarted() {
		return isStarted;
	}
	
	private boolean iu = false;
	
	public boolean isUnix() {
		return iu;
	}
	
	public void run() {
		isStarted = true;
		try {
			ConfigNode cfg = getConfig();
			ConfigNode ssl = cfg.getNode("ssl");
			boolean isSSL = !(ssl == null || !ssl.getNode("enabled").getValue().equals("true"));
			if (isSSL) {
				sslContext = makeSSLContext(new File(ssl.getNode("keyFile").getValue()), ssl.getNode("keyPassword").getValue(), ssl.getNode("keystorePassword").getValue());
			}
			if (cfg.containsNode("unix") && cfg.getNode("unix").getValue().equals("true")) {
				iu = true;
				setup(makeUnixServer(cfg.getNode("ip").getValue()));
			}else {
				iu = false;
				setup(makeServer(cfg.getNode("ip").getValue(), Integer.parseInt(cfg.getNode("port").getValue()), isSSL, !isSSL ? null : sslContext.getServerSocketFactory()));
			}
		}catch (Exception e) {
			logger.logError(e);
			logger.log("Closing " + name + "/" + protocol.name + " Server on " + getConfig().getNode("ip").getValue() + ":" + getConfig().getNode("port").getValue());
			// TODO: destroy
		}finally {
			loaded = true;
		}
	}
	
	public void formatConfig(ConfigNode map) {
		if (!map.containsNode("port")) map.insertNode("port", "80");
		if (!map.containsNode("unix")) map.insertNode("unix", "false", "set to true, and set ip to the socket file to use a unix socket. port is ignored. mostly used for shared hosting.");
		if (!map.containsNode("ip")) map.insertNode("ip", "0.0.0.0");
		if (!map.containsNode("ssl")) map.insertNode("ssl");
		ConfigNode ssl = map.getNode("ssl");
		if (!ssl.containsNode("enabled")) ssl.insertNode("enabled", "false");
		if (!ssl.containsNode("keyFile")) ssl.insertNode("keyFile", AvunaHTTPD.fileManager.getBaseFile("ssl/keyFile").toString());
		if (!ssl.containsNode("keystorePassword")) ssl.insertNode("keystorePassword", "password");
		if (!ssl.containsNode("keyPassword")) ssl.insertNode("keyPassword", "password");
	}
	
	public String getHostname() {
		return name;
	}
	
	public int getPort() {
		return Integer.parseInt((String) getConfig().getNode("port").getValue());
	}
	
	public abstract void setup(ServerSocket s);
	
	public void preExit() {
		
	}
	
	public boolean unio() {
		return enableUNIO() && !CLib.failed && !getConfig().getValue("unix").equals("true");
	}
	
	public PacketReceiver makeReceiver() {
		return null;
	}
	
	public boolean enableUNIO() {
		return false;
	}
}
