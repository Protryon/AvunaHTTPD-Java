package org.avuna.httpd.http.plugins.ssi;

public abstract class SSIFunction {
	public abstract String call(Page page, String arg);
}
