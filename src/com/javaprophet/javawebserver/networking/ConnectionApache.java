package com.javaprophet.javawebserver.networking;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Date;
import com.javaprophet.javawebserver.JavaWebServer;
import com.javaprophet.javawebserver.networking.packets.RequestPacket;
import com.javaprophet.javawebserver.networking.packets.ResponsePacket;

/**
 * Handles a single connection.
 */
public class ConnectionApache extends Connection {
	
	public ConnectionApache(Socket s, DataInputStream in, DataOutputStream out, boolean ssl) {
		super(s, in, out, ssl);
	}
	
	public void handleConnection() {
		Thread t = new Thread() {
			public void run() {
				process();
			}
		};
		t.start();
	}
	
	public void process() {
		while (!s.isClosed() && !closeWanted) {
			try {
				RequestPacket incomingRequest = RequestPacket.read(in);
				if (incomingRequest == null) {
					closeWanted = true;
					continue;
				}
				incomingRequest.userIP = s.getInetAddress().getHostAddress();
				incomingRequest.userPort = s.getPort();
				ResponsePacket outgoingResponse = new ResponsePacket();
				outgoingResponse.request = incomingRequest;
				JavaWebServer.patchBus.processPacket(incomingRequest);
				JavaWebServer.rg.process(incomingRequest, outgoingResponse);
				JavaWebServer.patchBus.processPacket(outgoingResponse);
				outgoingResponse.write(out);
				System.out.println("[" + Connection.timestamp.format(new Date()) + "]" + incomingRequest.userIP + " requested " + incomingRequest.target + " returned " + outgoingResponse.statusCode + " " + outgoingResponse.reasonPhrase);
			}catch (Exception ex) {
				ex.printStackTrace();
				try {
					s.close();
				}catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		if (!s.isClosed()) {
			try {
				s.close();
			}catch (IOException e) {
				e.printStackTrace();
			}
		}
		JavaWebServer.runningThreads.remove(this);
	}
}
