package org.avuna.httpd.http.plugins.base.fcgi;

import java.io.IOException;

public interface IFCGIManager {
	public void close() throws IOException;
	
	public void start();
}
