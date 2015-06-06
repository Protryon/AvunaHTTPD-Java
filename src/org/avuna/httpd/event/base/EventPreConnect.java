package org.avuna.httpd.event.base;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import org.avuna.httpd.event.Event;

public class EventPreConnect extends Event {
	private final Socket s;
	private final DataOutputStream out;
	private final DataInputStream in;
	
	public Socket getSocket() {
		return s;
	}
	
	public DataInputStream getInputStream() {
		return in;
	}
	
	public DataOutputStream getOutputStream() {
		return out;
	}
	
	public EventPreConnect(Socket s, DataOutputStream out, DataInputStream in) {
		super(EventID.PRECONNECT);
		this.s = s;
		this.out = out;
		this.in = in;
	}
	
}
