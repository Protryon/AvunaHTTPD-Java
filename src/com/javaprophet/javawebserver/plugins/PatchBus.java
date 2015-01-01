package com.javaprophet.javawebserver.plugins;

import com.javaprophet.javawebserver.JavaWebServer;
import com.javaprophet.javawebserver.networking.Packet;
import com.javaprophet.javawebserver.networking.packets.RequestPacket;
import com.javaprophet.javawebserver.networking.packets.ResponsePacket;

public class PatchBus {
	public PatchBus() {
		
	}
	
	public boolean processMethod(RequestPacket request, ResponsePacket response) {
		Patch handler = PatchRegistry.registeredMethods.get(request.method);
		if (handler != null && handler.enabled) {
			handler.processMethod(request, response);
			return true;
		}
		return false;
	}
	
	public void setupFolders() {
		for (Patch patch : PatchRegistry.patchs) {
			if (patch.enabled) {
				JavaWebServer.fileManager.getPlugin(patch).mkdirs();
			}
		}
	}
	
	public void processPacket(Packet p) {
		for (Patch patch : PatchRegistry.patchs) {
			if (patch.enabled && patch.shouldProcessPacket(p)) {
				patch.processPacket(p);
			}
		}
	}
	
	public byte[] processResponse(ResponsePacket response, RequestPacket request, byte[] data) {
		byte[] rres = data;
		for (Patch patch : PatchRegistry.patchs) {
			if (patch.enabled && patch.shouldProcessResponse(response, request, rres)) {
				rres = patch.processResponse(response, request, rres);
			}
		}
		return rres;
	}
	
	public void preExit() {
		for (Patch patch : PatchRegistry.patchs) {
			patch.preExit();
		}
	}
}
