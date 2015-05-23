package org.avuna.httpd.dns;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import org.avuna.httpd.hosts.HostDNS;
import org.avuna.httpd.util.Logger;

/**
 * Created by JavaProphet on 8/13/14 at 10:56 PM.
 */
public class TCPServer extends Thread implements IServer {
	private final ServerSocket server;
	private final HostDNS host;
	
	public TCPServer(HostDNS host, ServerSocket server) {
		super("DNS TCPServer");
		this.server = server;
		this.host = host;
	}
	
	public void run() {
		try {
			while (!server.isClosed()) {
				final Socket s = server.accept(); // TODO: thread
				s.setSoTimeout(1000);
				final DataOutputStream out = new DataOutputStream(s.getOutputStream());
				out.flush();
				final DataInputStream in = new DataInputStream(s.getInputStream());
				host.addWork(new WorkTCP(s, in, out));
			}
		}catch (Exception e) {
			Logger.logError(e);
		}finally {
			try {
				server.close();
			}catch (IOException e) {
				Logger.logError(e);
			}
		}
	}
}
