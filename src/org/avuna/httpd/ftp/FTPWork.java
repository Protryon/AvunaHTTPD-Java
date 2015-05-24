package org.avuna.httpd.ftp;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import org.avuna.httpd.AvunaHTTPD;
import org.avuna.httpd.util.Logger;

public class FTPWork {
	public Socket s;
	public DataInputStream in;
	public DataOutputStream out;
	public int state = 0;
	public long sns = 0L;
	public int tos = 0;
	public String user = "";
	public boolean auth = false;
	public String cwd = "/";
	public String root = "/";
	public FTPType type = FTPType.ASCII;
	public boolean isPASV = false, isPORT = false;
	public ThreadPassive psv = null;
	public int port = 0;
	public String rnfr = "";
	public int skip = 0;
	
	public FTPWork(Socket s, DataInputStream in, DataOutputStream out) {
		this.s = s;
		this.in = in;
		this.out = out;
	}
	
	public void writeBMLine(int response, String data) throws IOException {
		Logger.log(hashCode() + ": " + response + "-" + data);
		out.write((response + "-" + data + AvunaHTTPD.crlf).getBytes());
	}
	
	public void writeMMLine(String data) throws IOException {
		Logger.log(hashCode() + ":  " + data);
		out.write((" " + data + AvunaHTTPD.crlf).getBytes());
	}
	
	public void writeLine(int response, String data) throws IOException {
		Logger.log(hashCode() + ": " + response + " " + data);
		out.write((response + " " + data + AvunaHTTPD.crlf).getBytes());
	}
}
