package com.javaprophet.javawebserver.plugins.javaloader;

import java.io.IOException;
import java.io.OutputStream;
import com.javaprophet.javawebserver.JavaWebServer;

public class HTMLBuilder extends OutputStream {
	private final OutputStream out;
	
	public HTMLBuilder(OutputStream out) {
		this.out = out;
	}
	
	@Override
	public void write(int b) throws IOException {
		out.write(b);
	}
	
	public void flush() throws IOException {
		out.flush();
	}
	
	public void print(String s) throws IOException {
		write(s.getBytes());
	}
	
	public void println(String s) throws IOException {
		print(s);
		print(JavaWebServer.crlf);
	}
}
