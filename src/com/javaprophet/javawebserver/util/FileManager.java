package com.javaprophet.javawebserver.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import org.json.simple.JSONObject;
import com.javaprophet.javawebserver.JavaWebServer;
import com.javaprophet.javawebserver.http.MessageBody;
import com.javaprophet.javawebserver.http.Resource;
import com.javaprophet.javawebserver.http.StatusCode;
import com.javaprophet.javawebserver.plugins.Patch;

public class FileManager {
	public FileManager() {
		
	}
	
	public File getMainDir() {
		return new File((String)JavaWebServer.mainConfig.get("dir"));
	}
	
	public File getHTDocs() {
		return new File(getMainDir(), (String)JavaWebServer.mainConfig.get("htdocs"));
	}
	
	public File getSSL() {
		return new File(getMainDir(), (String)(((JSONObject)JavaWebServer.mainConfig.get("ssl")).get("folder")));
	}
	
	public File getSSLKeystore() {
		return new File(getSSL(), (String)(((JSONObject)JavaWebServer.mainConfig.get("ssl")).get("keyFile")));
	}
	
	public File getPlugins() {
		return new File(getMainDir(), (String)JavaWebServer.mainConfig.get("plugins"));
	}
	
	public File getTemp() {
		return new File(getMainDir(), (String)JavaWebServer.mainConfig.get("temp"));
	}
	
	public File getPlugin(Patch p) {
		return new File(getPlugins(), p.name);
	}
	
	public File getBaseFile(String name) {
		return new File(getMainDir(), name);
	}
	
	public void clearCache() {
		String[] delKeys = new String[cache.size()];
		int delSize = 0;
		for (String file : cache.keySet()) {
			if (!extCache.get(file).equals("application/x-java")) {
				delKeys[delSize++] = file;
			}
		}
		for (int i = 0; i < delSize; i++) {
			cache.remove(delKeys[i]);
			extCache.remove(delKeys[i]);
		}
	}
	
	public static final String crlf = System.getProperty("line.separator");
	
	public void getErrorPage(MessageBody body, String reqTarget, StatusCode status, String info) {
		JSONObject errorPages = (JSONObject)JavaWebServer.mainConfig.get("errorpages");
		if (errorPages.containsKey(status.getStatus())) {
			try {
				String path = (String)errorPages.get(status.getStatus());
				Resource resource = getResource(path);
				if (resource != null) {
					if (resource.type.startsWith("text")) {
						String res = new String(resource.data);
						res = res.replace("$_statusCode_$", status.getStatus() + "").replace("$_reason_$", status.getPhrase()).replace("$_info_$", info).replace("$_reqTarget_$", reqTarget);
						resource.data = res.getBytes();
					}
					body.setBody(resource);
					return;
				}
			}catch (Exception e) {
				e.printStackTrace();
			}
		}
		Resource error = new Resource(("<!DOCTYPE HTML PUBLIC \"-//IETF//DTD HTML 2.0//EN\">" + crlf + "<html><head>" + crlf + "<title>" + status.getStatus() + " " + status.getPhrase() + "</title>" + crlf + "</head><body>" + crlf + "<h1>" + status.getPhrase() + "</h1>" + crlf + "<p>" + info + "</p>" + crlf + "</body></html>").getBytes(), "text/html");
		body.setBody(error);
		return;
	}
	
	private boolean lwi = false;
	
	public File getAbsolutePath(String reqTarget) {
		lwi = false;
		File abs = new File(JavaWebServer.fileManager.getHTDocs(), reqTarget);
		if (abs.isDirectory()) {
			String[] index = ((String)JavaWebServer.mainConfig.get("index")).split(",");
			for (String i : index) {
				i = i.trim();
				if (i.startsWith("/")) {
					i = i.substring(1);
				}
				String abst = abs.toString().replace("\\", "/");
				if (!abst.endsWith("/")) {
					abst += "/";
				}
				File f = new File(abst + i);
				if (f.exists()) {
					abs = f;
					lwi = true;
					break;
				}
			}
		}
		return abs;
	}
	
	public String correctForIndex(String reqTarget) {
		String p = getAbsolutePath(reqTarget).getAbsolutePath().replace("\\", "/");
		return p.substring(JavaWebServer.fileManager.getHTDocs().getAbsolutePath().replace("\\", "/").length());
	}
	
	public static final HashMap<String, byte[]> cache = new HashMap<String, byte[]>();
	public static final HashMap<String, String> extCache = new HashMap<String, String>();
	public static final HashMap<String, Boolean> lwiCache = new HashMap<String, Boolean>();
	private static long cacheClock = 0L;
	
	public Resource getResource(String reqTarget) {
		try {
			String rt = reqTarget;
			if (rt.contains("#")) {
				rt = rt.substring(0, rt.indexOf("#"));
			}
			if (rt.contains("?")) {
				rt = rt.substring(0, rt.indexOf("?"));
			}
			byte[] resource = null;
			String ext = "";
			boolean lwi = false;
			if (cache.containsKey(rt)) {
				long t = System.currentTimeMillis();
				long cc = ((Number)JavaWebServer.mainConfig.get("cacheClock")).longValue();
				if ((cc > 0 && t - cc < cacheClock) || (cc == -1)) {
					resource = cache.get(rt);
					ext = extCache.get(rt);
					lwi = lwiCache.get(rt);
				}else {
					cacheClock = t;
					clearCache();
				}
			}
			if (resource == null) {
				File abs = getAbsolutePath(rt);
				FileInputStream fin = new FileInputStream(abs);
				ByteArrayOutputStream bout = new ByteArrayOutputStream();
				int i = 1;
				byte[] buf = new byte[4096];
				while (i > 0) {
					i = fin.read(buf);
					if (i > 0) {
						bout.write(buf, 0, i);
					}
				}
				fin.close();
				resource = bout.toByteArray();
				cache.put(rt, resource);
				ext = abs.getName().substring(abs.getName().lastIndexOf(".") + 1);
				ext = JavaWebServer.extensionToMime.containsKey(ext) ? JavaWebServer.extensionToMime.get(ext) : "application/octet-stream";
				extCache.put(rt, ext);
				lwi = this.lwi;
				lwiCache.put(rt, lwi);
			}
			Resource r = new Resource(resource, ext, rt);
			r.wasDir = lwi;
			return r;
		}catch (IOException e) {
			if (!(e instanceof FileNotFoundException)) e.printStackTrace();
			return null;
		}
	}
}
