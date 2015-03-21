package com.javaprophet.javawebserver.plugins;

import java.io.IOException;
import com.javaprophet.javawebserver.JavaWebServer;
import com.javaprophet.javawebserver.http.networking.Packet;
import com.javaprophet.javawebserver.http.networking.RequestPacket;
import com.javaprophet.javawebserver.http.networking.ResponsePacket;

public class PatchBus {
	public PatchBus() {
		
	}
	
	public boolean processMethod(RequestPacket request, ResponsePacket response) {
		Patch handler = PatchRegistry.registeredMethods.get(request.method);
		if (handler != null && handler.pcfg.get("enabled").equals("true")) {
			handler.processMethod(request, response);
			return true;
		}
		return false;
	}
	
	public void setupFolders() {
		for (Patch patch : PatchRegistry.patchs) {
			if (patch.pcfg.get("enabled").equals("true")) {
				JavaWebServer.fileManager.getPlugin(patch).mkdirs();
			}
		}
	}
	
	public void processPacket(Packet p) {
		for (Patch patch : PatchRegistry.patchs) {
			if (patch.pcfg.get("enabled").equals("true") && patch.shouldProcessPacket(p)) {
				patch.processPacket(p);
				if (p.drop) return;
			}
		}
	}
	
	public byte[] processResponse(ResponsePacket response, RequestPacket request, byte[] data) {
		byte[] rres = data;
		for (Patch patch : PatchRegistry.patchs) {
			// long start = System.nanoTime();
			if (patch.pcfg.get("enabled").equals("true") && patch.shouldProcessResponse(response, request, rres)) {
				rres = patch.processResponse(response, request, rres);
			}
			// Logger.log(request.target + patch.name + ": " + (System.nanoTime() - start) / 1000000D + " ms");
			if (response.drop) {
				break;
			}
		}
		return rres;
	}
	
	public void preExit() {
		for (Patch patch : PatchRegistry.patchs) {
			patch.preExit();
		}
	}
	
	public void reload() throws IOException {
		for (Patch patch : PatchRegistry.patchs) {
			patch.reload();
		}
	}
}
