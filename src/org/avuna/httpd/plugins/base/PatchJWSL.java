package org.avuna.httpd.plugins.base;

import java.util.HashMap;
import org.avuna.httpd.AvunaHTTPD;
import org.avuna.httpd.http.networking.Packet;
import org.avuna.httpd.http.networking.RequestPacket;
import org.avuna.httpd.http.networking.ResponsePacket;
import org.avuna.httpd.plugins.Patch;

public class PatchJWSL extends Patch {
	
	public PatchJWSL(String name) {
		super(name);
	}
	
	@Override
	public void formatConfig(HashMap<String, Object> json) {
		super.formatConfig(json);
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
		return response.headers.hasHeader("Content-Type") && response.headers.getHeader("Content-Type").equals("application/x-jwsl") && response.body != null && data != null && data.length > 0;
	}
	
	@Override
	public byte[] processResponse(ResponsePacket response, RequestPacket request, byte[] data) {
		String sdata = new String(data);
		String[] ls = sdata.split(AvunaHTTPD.crlf);
		HashMap<String, Integer> codeToIndex = new HashMap<String, Integer>();
		boolean inCode = false;
		int cIndex = 0;
		int lIndex = 0;
		String cCode = "";
		for (int i = 0; i < ls.length; i++) {
			String line = ls[i];
			if (line.contains("<?jwsl")) {
				line = line.substring(line.indexOf("<?jwsl") + 6);
				inCode = true;
				cIndex = lIndex + line.indexOf("<?jwsl");
			}
			if (inCode && line.contains("?>")) { // TODO: not in quotes, etc
				line = line.substring(0, line.indexOf("?>"));
				cCode += line;
				codeToIndex.put(cCode, cIndex);
				cIndex = 0;
				cCode = "";
				inCode = false;
			}
			if (inCode) {
				cCode += line + AvunaHTTPD.crlf;
			}
			lIndex += ls[i].length() + AvunaHTTPD.crlf.length();
		}
		String[] codeArray = new String[codeToIndex.size()];
		int i = 0;
		for (String code : codeToIndex.keySet()) {
			codeArray[i++] = code;
		}
		for (int i2 = 0; i2 < codeArray.length; i2++) {
			String code = codeArray[i2];
			int loc = codeToIndex.get(code);
			String before = sdata.substring(0, loc);
			String after = sdata.substring(loc + code.length() + 9);
			sdata = before + after;
			for (int i3 = i2 + 1; i3 < codeArray.length; i3++) {
				codeToIndex.put(codeArray[i3], codeToIndex.get(codeArray[i3]) - code.length() - 8);
			}
		}
		// TODO: continue processing, eval, and then reoutput.
		// TODO: proper header mod
		response.headers.updateHeader("Content-Type", "text/html");
		return sdata.getBytes();
	}
	
	@Override
	public void processMethod(RequestPacket request, ResponsePacket response) {
		
	}
}
