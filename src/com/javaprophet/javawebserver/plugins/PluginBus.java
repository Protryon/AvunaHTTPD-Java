package com.javaprophet.javawebserver.plugins;

import com.javaprophet.javawebserver.JavaWebServer;
import com.javaprophet.javawebserver.networking.Packet;
import com.javaprophet.javawebserver.networking.packets.RequestPacket;
import com.javaprophet.javawebserver.networking.packets.ResponsePacket;

public class PluginBus {
	public PluginBus() {
		
	}
	
	public void setupFolders() {
		for (Patch patch : Patch.patchs) {
			if (patch.enabled) {
				JavaWebServer.fileManager.getPlugin(patch).mkdirs();
			}
		}
	}
	
	public void processPacket(Packet p) {
		for (Patch patch : Patch.patchs) {
			if (patch.enabled && patch.shouldProcessPacket(p)) {
				patch.processPacket(p);
			}
		}
	}
	
	public byte[] processResponse(ResponsePacket response, RequestPacket request, byte[] data) {
		byte[] rres = data;
		for (Patch patch : Patch.patchs) {
			if (patch.enabled && patch.shouldProcessResponse(response, request, data)) {
				rres = patch.processResponse(response, request, data);
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
