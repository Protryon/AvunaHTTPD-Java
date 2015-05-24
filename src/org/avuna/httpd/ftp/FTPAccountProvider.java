package org.avuna.httpd.ftp;

public abstract class FTPAccountProvider {
	public abstract boolean isValid(String user, String pass);
	
	public abstract String getRoot(String user);
}
