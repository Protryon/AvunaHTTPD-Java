package org.avuna.httpd.http2.networking;

public enum StreamState {
	CLOSED(0), RESERVED_LOCAL(1), RESERVED_REMOTE(2), HALFCLOSED_LOCAL(3), HALFCLOSED_REMOTE(4), IDLE(5), OPEN(6);
	public final int id;
	
	private StreamState(int id) {
		this.id = id;
	}
}
