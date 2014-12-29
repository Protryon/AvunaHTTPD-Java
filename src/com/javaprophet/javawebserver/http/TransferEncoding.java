package com.javaprophet.javawebserver.http;

public enum TransferEncoding {
	chunked("chunked"), compress("compress"), deflate("deflate"), gzip("gzip"), identity("identity"), xcompress("x-compress"), xgzip("x-gzip");
	public String name = "";
	
	private TransferEncoding(String name) {
		this.name = name;
	}
	
	public String toString() {
		if (this == identity) {
			return "";
		}else {
			return name;
		}
	}
}
