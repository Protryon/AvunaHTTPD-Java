package org.avuna.httpd.http2.networking;

public enum StreamState {
	IDLE(0), OPEN(1), RESERVED_LOCAL(2), RESERVED_REMOTE(2), HALFCLOSED_LOCAL(3), HALFCLOSED_REMOTE(4), CLOSED(5);
	public final int id;
	
	private StreamState(int id) {
		this.id = id;
	}
}
