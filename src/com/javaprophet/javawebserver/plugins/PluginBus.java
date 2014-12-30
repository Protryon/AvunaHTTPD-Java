package com.javaprophet.javawebserver.plugins;

import com.javaprophet.javawebserver.http.ContentEncoding;
import com.javaprophet.javawebserver.http.Headers;
import com.javaprophet.javawebserver.networking.Packet;

public class PluginBus {
	public PluginBus() {
		
	}
	
	public void processPacket(Packet p) {
		for (Patch patch : Patch.patchs) {
			if (patch.enabled && patch.shouldProcessPacket(p)) {
				patch.processPacket(p);
			}
		}
	}
	
	public byte[] processResponse(Headers headers, ContentEncoding ce, boolean data, byte[] response) {
		byte[] rres = response;
		for (Patch patch : Patch.patchs) {
			if (patch.enabled && patch.shouldProcessResponse(headers, ce, data, rres)) {
				rres = patch.processResponse(headers, ce, data, rres);
			}
		}
		return rres;
	}
	
	public void preExit() {
		for (Patch patch : Patch.patchs) {
			patch.preExit();
		}
	}
}
