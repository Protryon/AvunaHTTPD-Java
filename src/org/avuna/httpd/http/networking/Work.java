package org.avuna.httpd.http.networking;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.ArrayBlockingQueue;
import org.avuna.httpd.hosts.HostHTTP;
import org.avuna.httpd.http.event.EventDisconnected;
import org.avuna.httpd.http.networking.httpm.MasterConn;

public class Work {
	public final Socket s;
	public final DataInputStream in;
	public final DataOutputStream out;
	public final boolean ssl;
	public final HostHTTP host;
	public int tos = 0;
	public long sns = 0L;
	public int nreqid = 1;
	public ByteArrayOutputStream sslprep = null;
	public ArrayBlockingQueue<ResponsePacket> outQueue = new ArrayBlockingQueue<ResponsePacket>(64);
	public boolean blockTimeout = false;
	public MasterConn cn = null;
	public int rqs = 0;
	public long rqst = 0L;
	
	// public ResponsePacket[] pipeline = new ResponsePacket[32];
	
	public Work(HostHTTP host, Socket s, DataInputStream in, DataOutputStream out, boolean ssl) {
		this.host = host;
		this.s = s;
		this.in = in;
		this.out = out;
		this.ssl = ssl;
		if (ssl) {
			sslprep = new ByteArrayOutputStream();
		}
	}
	
	public void close() throws IOException {
		s.close();
		host.eventBus.callEvent(new EventDisconnected(this));
	}
}