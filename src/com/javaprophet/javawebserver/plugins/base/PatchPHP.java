package com.javaprophet.javawebserver.plugins.base;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
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
		return headers.hasHeader("Content-Type") && headers.getHeader("Content-Type").value.equals("application/x-php");
	}
	
	@Override
	public byte[] processResponse(Headers headers, ContentEncoding ce, boolean data, byte[] response) {
		ScriptEngineManager manager = new ScriptEngineManager();
		ScriptEngine engine = manager.getEngineByExtension("php");
		try {
			headers.getHeader("Content-Type").value = "text/html";
			Object s = engine.eval(new String(response));
			System.out.println(s);
		}catch (ScriptException ex) {
			ex.printStackTrace();
		}
		return response;
	}
	
}
