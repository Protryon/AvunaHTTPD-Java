package com.javaprophet.javawebserver.dns;

public abstract class Work {
	protected final boolean UDP;
	
	protected Work(boolean UDP) {
		this.UDP = UDP;
	}
}