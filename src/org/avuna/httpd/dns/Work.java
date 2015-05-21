package org.avuna.httpd.dns;

public abstract class Work {
	protected final boolean UDP;
	
	protected Work(boolean UDP) {
		this.UDP = UDP;
	}
}