package com.javaprophet.javawebserver.http;

import java.util.ArrayList;

public class Headers {
	private ArrayList<Header> headers = new ArrayList<Header>();
	
	public Headers() {
		
	}
	
	public Headers clone() {
		Headers h = new Headers();
		for (Header hh : headers) {
			h.addHeader(new Header(hh.name, hh.value));
		}
		return h;
	}
	
	public void addHeader(String name, String value) {
		addHeader(new Header(name, value));
	}
	
	public void addHeader(Header header) {
		this.headers.add(header);
	}
	
	public void addHeader(String header) {
		this.headers.add(Header.fromLine(header));
	}
	
	public ArrayList<Header> getHeaders(String name) {
		ArrayList<Header> mheaders = new ArrayList<Header>();
		for (Header header : headers) {
			if (header.name.equalsIgnoreCase(name)) {
				mheaders.add(header);
			}
		}
		return mheaders;
	}
	
	public Header getHeader(String name) {
		for (Header header : headers) {
			if (header.name.equalsIgnoreCase(name)) {
				return header;
			}
		}
		return null;
	}
	
	public void removeHeaders(String name) {
		for (int i = 0; i < headers.size(); i++) {
			if (headers.get(i).name.equalsIgnoreCase(name)) {
				headers.remove(headers.get(i));
				i--;
			}
		}
	}
	
	public boolean hasHeader(String name) {
		for (Header header : headers) {
			if (header.name.equalsIgnoreCase(name)) {
				return true;
			}
		}
		return false;
	}
	
	public ArrayList<Header> getHeaders() {
		return headers;
	}
}
