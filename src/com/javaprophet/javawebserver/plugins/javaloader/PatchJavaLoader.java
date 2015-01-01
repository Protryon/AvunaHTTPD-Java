package com.javaprophet.javawebserver.plugins.javaloader;

import java.util.HashMap;
import org.json.simple.JSONObject;
import com.javaprophet.javawebserver.networking.Packet;
import com.javaprophet.javawebserver.networking.packets.RequestPacket;
import com.javaprophet.javawebserver.networking.packets.ResponsePacket;
import com.javaprophet.javawebserver.plugins.Patch;

public class PatchJavaLoader extends Patch {
	
	public PatchJavaLoader(String name) {
		super(name);
	}
	
	@Override
	public void formatConfig(JSONObject json) {
		
	}
	
	@Override
	public boolean shouldProcessPacket(Packet packet) {
		return false;
	}
	
	@Override
	public void processPacket(Packet packet) {
		
	}
	
	@Override
	public boolean shouldProcessResponse(ResponsePacket response, RequestPacket request, byte[] data) {
		return response.headers.hasHeader("Content-Type") && response.headers.getHeader("Content-Type").value.equals("application/x-java") && response.body != null && data != null && data.length > 0;
	}
	
	private static final HashMap<byte[], String> loadedClasses = new HashMap<byte[], String>();
	private static final JavaLoaderClassLoader jlcl = new JavaLoaderClassLoader();
	
	@Override
	public byte[] processResponse(ResponsePacket response, RequestPacket request, byte[] data) {
		try {
			response.headers.updateHeader("Content-Type", "text/html");
			String name = "";
			if (!loadedClasses.containsKey(data)) {
				name = jlcl.addClass(data);
				loadedClasses.put(data, name);
			}else {
				name = loadedClasses.get(data);
			}
			Class<? extends JavaLoader> loaderClass = (Class<? extends JavaLoader>)jlcl.loadClass(name);
			if (loaderClass == null) {
				return null;
			}
			JavaLoader loader = loaderClass.newInstance();
			byte[] ndata = loader.generate(response, request);
			return ndata;
		}catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	@Override
	public void processMethod(RequestPacket request, ResponsePacket response) {
		
	}
}
