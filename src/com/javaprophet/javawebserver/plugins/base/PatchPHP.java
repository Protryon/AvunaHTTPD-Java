package com.javaprophet.javawebserver.plugins.base;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URLDecoder;
import java.util.Date;
import java.util.HashMap;
import java.util.Scanner;
import org.json.simple.JSONObject;
import com.javaprophet.javawebserver.JavaWebServer;
import com.javaprophet.javawebserver.http.ContentEncoding;
import com.javaprophet.javawebserver.http.Headers;
import com.javaprophet.javawebserver.http.Method;
import com.javaprophet.javawebserver.http.ResponseGenerator;
import com.javaprophet.javawebserver.networking.Connection;
import com.javaprophet.javawebserver.networking.Packet;
import com.javaprophet.javawebserver.networking.packets.RequestPacket;
import com.javaprophet.javawebserver.networking.packets.ResponsePacket;
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
	public boolean shouldProcessResponse(ResponsePacket response, RequestPacket request, Headers headers, ContentEncoding ce, byte[] data) {
		return headers.hasHeader("Content-Type") && headers.getHeader("Content-Type").value.equals("application/x-php") && data.length > 0;
	}
	
	private static final String crlf = System.getProperty("line.separator");
	
	public HashMap<String, Object> postProcess(HashMap<String, Object> prev) {
		String[] keySet = new String[prev.size()];
		int i = 0;
		for (String key : prev.keySet()) {
			keySet[i++] = key;
		}
		HashMap<String, Object> keyArray = new HashMap<String, Object>();
		for (int o = 0; o < keySet.length; o++) {
			String key = keySet[o];
			int ac = 1;
			for (int o2 = 0; o2 < key.length(); o2++) {
				if (key.charAt(o2) == '[') {
					ac++;
				}
			}
			String[] keyArray2 = new String[ac];
			if (!key.contains("[")) {
				keyArray2[0] = key;
			}else {
				String[] bspl = key.split("\\[");
				for (int o2 = 0; o2 < bspl.length; o2++) {
					keyArray2[o2] = bspl[o2].trim(); // TODO: o2 != ac?
					if (keyArray2[o2].endsWith("]")) {
						keyArray2[o2] = keyArray2[o2].substring(0, keyArray2[o2].length() - 1);
					}
				}
			}
			if (keyArray2.length == 1) {
				keyArray.put(keyArray2[0], prev.get(keyArray2[0]));
			}else {
				HashMap<String, Object> last = keyArray;
				for (int o2 = 0; o2 < keyArray2.length - 1; o2++) {
					if (last.containsKey(keyArray2[o2])) {
						Object obj = last.get(keyArray2[o2]);
						if (obj instanceof HashMap) {
							HashMap<String, Object> objh = (HashMap<String, Object>)obj;
							last = objh;
						}
					}else {
						HashMap<String, Object> sub = new HashMap<String, Object>();
						last.put(keyArray2[o2], sub);
						last = sub;
					}
				}
				last.put(keyArray2[keyArray2.length - 1], prev.get(key));
			}
		}
		return keyArray;
	}
	
	public String postProcess2(HashMap<String, Object> pos) {
		String prepend = "";
		for (String key : pos.keySet()) {
			Object value = pos.get(key);
			String fd = "";
			if (value instanceof String) {
				fd += "\"" + (String)value + "\"";
			}else if (value instanceof HashMap) {
				HashMap<String, Object> tpos = (HashMap<String, Object>)value;
				fd += "array(" + crlf;
				fd += postProcess2(tpos);
				fd += ")" + crlf;
			}
			prepend += "\"" + key + "\" => " + fd + "," + crlf;
		}
		return prepend;
	}
	
	@Override
	public byte[] processResponse(ResponsePacket response, RequestPacket request, Headers headers, ContentEncoding ce, byte[] data) {
		try {
			String prepend = "<?php" + crlf;
			HashMap<String, String> _SERVER = new HashMap<String, String>();
			String __FILE__ = response.body.getBody().loc.replace("\\", "/");
			_SERVER.put("PHP_SELF", __FILE__);
			__FILE__ = Connection.rg.getAbsolutePath(__FILE__).getAbsolutePath().replace("\\", "/");
			String get = request.target;
			if (get.contains("#")) {
				get = get.substring(0, get.indexOf("#"));
			}
			String rq = get;
			if (get.contains("?")) {
				rq = get.substring(0, get.indexOf("?"));
				get = get.substring(get.indexOf("?") + 1);
			}else {
				get = "";
			}
			_SERVER.put("argv", get);
			_SERVER.put("argc", "");
			_SERVER.put("GATEWAY_INTERFACE", "N/I");
			_SERVER.put("SERVER_ADDR", "127.0.0.1");
			_SERVER.put("SERVER_NAME", "JWS/1.0");
			_SERVER.put("SERVER_SOFTWARE", "JWS/1.0");
			_SERVER.put("SERVER_PROTOCOL", request.httpVersion);
			_SERVER.put("REQUEST_METHOD", request.method.name);
			_SERVER.put("REQUEST_TIME", request.headers.hasHeader("Date") ? request.headers.getHeader("Date").value : ResponseGenerator.sdf.format(new Date()));
			_SERVER.put("REQUEST_TIME_FLOAT", System.currentTimeMillis() + "");
			_SERVER.put("QUERY_STRING", get); // TODO: post?
			_SERVER.put("DOCUMENT_ROOT", JavaWebServer.fileManager.getHTDocs().getAbsolutePath().replace("\\", "/"));
			if (request.headers.hasHeader("Accept")) _SERVER.put("HTTP_ACCEPT", request.headers.getHeader("Accept").value);
			if (request.headers.hasHeader("Accept-Charset")) _SERVER.put("HTTP_ACCEPT_CHARSET", request.headers.getHeader("Accept-Charset").value);
			if (request.headers.hasHeader("Accept-Encoding")) _SERVER.put("HTTP_ACCEPT_ENCODING", request.headers.getHeader("Accept-Encoding").value);
			if (request.headers.hasHeader("Accept-Language")) _SERVER.put("HTTP_ACCEPT_LANGUAGE", request.headers.getHeader("Accept-Language").value);
			if (request.headers.hasHeader("Connection")) _SERVER.put("HTTP_CONNECTION", request.headers.getHeader("Connection").value);
			if (request.headers.hasHeader("Host")) _SERVER.put("HTTP_HOST", request.headers.getHeader("Host").value);
			if (request.headers.hasHeader("Referer")) _SERVER.put("HTTP_REFERER", request.headers.getHeader("Referer").value);
			if (request.headers.hasHeader("User-Agent")) _SERVER.put("HTTP_USER_AGENT", request.headers.getHeader("User-Agent").value);
			// if https then _SERVER.put("HTTPS", "true");
			_SERVER.put("REMOTE_ADDR", request.userIP);
			_SERVER.put("REMOTE_PORT", request.userPort + "");
			// _SERVER.put("REMOTE_USER", "");
			// _SERVER.put("REDIRECT_REMOTE_USER", ""); TODO: auths + htaccess
			_SERVER.put("SCRIPT_FILENAME", Connection.rg.getAbsolutePath(rq).getAbsolutePath().replace("\\", "/"));
			_SERVER.put("SERVER_PORT", JavaWebServer.mainConfig.get("bindport").toString());
			_SERVER.put("SCRIPT_NAME", rq.substring(rq.lastIndexOf("/") + 1));
			_SERVER.put("REQUEST_URI", rq);
			_SERVER.put("PATH_INFO", "");// no CGI
			_SERVER.put("ORIG_PATH_INFO", "");
			prepend += "$_SERVER = array(" + crlf;
			for (String key : _SERVER.keySet()) {
				prepend += "\"" + key + "\" => \"" + _SERVER.get(key) + "\"," + crlf;
			}
			prepend += crlf + ");" + crlf + "$_GET = array(" + crlf;
			if (get.length() > 0) {
				String[] posts = get.split("&");
				HashMap<String, Object> pos = new HashMap<String, Object>();
				for (String key : posts) {
					String[] spl = key.split("=");
					String pn = URLDecoder.decode(spl[0]);
					String pd = URLDecoder.decode(spl[1]);
					pos.put(pn, pd);
				}
				pos = postProcess(pos);
				get = postProcess2(pos);
				prepend += get;
			}
			prepend += crlf + ");" + crlf + "$_POST = array(" + crlf;
			String post = "";
			if (request.method == Method.POST && request.body.getBody().type.equals("application/x-www-form-urlencoded")) {
				post = new String(request.body.getBody().data);
			}
			if (post.length() > 0) {
				String[] posts = post.split("&");
				HashMap<String, Object> pos = new HashMap<String, Object>();
				for (String key : posts) {
					String[] spl = key.split("=");
					String pn = URLDecoder.decode(spl[0]);
					String pd = URLDecoder.decode(spl[1]);
					pos.put(pn, pd);
				}
				pos = postProcess(pos);
				post = postProcess2(pos);
				prepend += post;
			}
			prepend += crlf + ");" + crlf + "$_COOKIE = array(" + crlf;
			// TODO: $_FILES
			String cookie = "";
			if (request.headers.hasHeader("Cookie")) {
				cookie = request.headers.getHeader("Cookie").value;
			}
			if (cookie.length() > 0) for (String key : cookie.split(";")) {
				prepend += "\"" + key.substring(0, key.indexOf("=")).trim() + "\" => \"" + key.substring(key.indexOf("=") + 1).trim() + "\",";
			}
			// TODO: Session?
			prepend += crlf + ");" + crlf + "$_REQUEST = array(" + crlf;
			prepend += get + post;
			if (cookie.length() > 0) for (String key : cookie.split(";")) {
				prepend += "\"" + key.substring(0, key.indexOf("=")).trim() + "\" => \"" + key.substring(key.indexOf("=") + 1).trim() + "\"," + crlf;
			}
			prepend += crlf + ");" + crlf;
			prepend += "chdir('" + __FILE__.substring(0, __FILE__.lastIndexOf("/")) + "');" + crlf;
			prepend += "require_once '" + __FILE__ + "';" + crlf;
			prepend += "?>" + crlf;
			System.out.println(prepend);
			// ^^^^^prepend
			File temp = new File(JavaWebServer.fileManager.getTemp(), System.nanoTime() + data.length + ".php");
			temp.createNewFile();
			FileOutputStream fout = new FileOutputStream(temp);
			fout.write(prepend.getBytes());
			fout.flush();
			fout.close();
			Process proc = Runtime.getRuntime().exec(pcfg.get("cmd") + " \"" + temp.getAbsolutePath() + "\"");
			Scanner s = new Scanner(proc.getInputStream());
			boolean tt = true;
			ByteArrayOutputStream bout = new ByteArrayOutputStream();
			while (s.hasNextLine()) {
				String line = s.nextLine().trim();
				if (line.length() > 0) {
					if (tt && line.contains(":")) {
						String[] lt = line.split(":");
						String hn = lt[0].trim();
						String hd = lt[1].trim();
						if (hn.equals("Status")) {
							response.statusCode = Integer.parseInt(hd.substring(0, hd.indexOf(" ")));
							response.reasonPhrase = hd.substring(hd.indexOf(" ") + 1);
						}else {
							headers.updateHeader(hn, hd);
						}
					}else if (tt) {
						tt = false;
					}else {
						bout.write((line + crlf).getBytes());
					}
				}else {
					tt = false;
				}
			}
			s.close();
			temp.delete();
			return bout.toByteArray();
		}catch (IOException e) {
			e.printStackTrace(); // TODO: throws HTMLException?
		}
		return null;// TODO: to prevent PHP leaks
	}
	
	@Override
	public void formatConfig(JSONObject json) {
		if (!json.containsKey("cmd")) json.put("cmd", "php-cgi");
	}
	
}
