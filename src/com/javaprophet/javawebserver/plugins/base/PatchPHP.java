package com.javaprophet.javawebserver.plugins.base;

import com.javaprophet.javawebserver.http.ContentEncoding;
import com.javaprophet.javawebserver.http.Headers;
import com.javaprophet.javawebserver.networking.Packet;
import com.javaprophet.javawebserver.plugins.Patch;

public class PatchPHP extends Patch {
	
	public PatchPHP(String name) {
		super(name);
	}
	
	@Override
	public boolean shouldProcessPacket(Packet packet) {
		return false;
	}
	
	@Override
	public void processPacket(Packet packet) {
		
	}
	
	@Override
	public boolean shouldProcessResponse(Headers headers, ContentEncoding ce, boolean data, byte[] response) {
		return false;
	}
	
	@Override
	public byte[] processResponse(Headers headers, ContentEncoding ce, boolean data, byte[] response) {
		return response;
	}
	
}
