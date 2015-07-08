package org.avuna.httpd.http.plugins.security;

public class SecurityNibble {
	public int[] ips = new int[0];
	public RequestPacketShell[] requests = new RequestPacketShell[0];
	public int connections = 0;
	public long banned = 0L;
	public String[] session = new String[0];
	private double trouble = 0D;
	
	public void addRequest(RequestPacketShell req) {
		RequestPacketShell[] nreq = new RequestPacketShell[requests.length + 1];
		System.arraycopy(requests, 0, nreq, 0, requests.length);
		nreq[requests.length] = req;
		requests = nreq;
	}
	
	public void addSession(String sess) {
		String[] nreq = new String[session.length + 1];
		System.arraycopy(session, 0, nreq, 0, session.length);
		nreq[session.length] = sess;
		session = nreq;
	}
	
	public void addIP(int ip) {
		for (int s : ips) {
			if (s == ip) {
				return;
			}
		}
		int[] nips = new int[ips.length + 1];
		System.arraycopy(ips, 0, nips, 0, ips.length);
		nips[ips.length] = ip;
		ips = nips;
	}
	
	public void fireInfraction(double severity) {
		this.trouble += severity;
	}
}
