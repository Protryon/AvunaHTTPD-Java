package com.javaprophet.javawebserver.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URLDecoder;
import java.util.HashMap;
import com.javaprophet.javawebserver.JavaWebServer;
import com.javaprophet.javawebserver.http.MessageBody;
import com.javaprophet.javawebserver.http.Resource;
import com.javaprophet.javawebserver.http.StatusCode;
import com.javaprophet.javawebserver.networking.packets.RequestPacket;
import com.javaprophet.javawebserver.plugins.Patch;
import com.javaprophet.javawebserver.plugins.PatchRegistry;
import com.javaprophet.javawebserver.plugins.base.PatchChunked;
import com.javaprophet.javawebserver.plugins.javaloader.lib.HTMLCache;

public class FileManager {
	public FileManager() {
		
	}
	
	private final static char[] hexArray = "0123456789ABCDEF".toCharArray();
	
	public String bytesToHex(byte[] bytes) {
		char[] hexChars = new char[bytes.length * 2];
		for (int j = 0; j < bytes.length; j++) {
			int v = bytes[j] & 0xFF;
			hexChars[j * 2] = hexArray[v >>> 4];
			hexChars[j * 2 + 1] = hexArray[v & 0x0F];
		}
		return new String(hexChars);
	}
	
	public File getMainDir() {
		return new File((String)JavaWebServer.mainConfig.get("dir", null));
	}
	
	public File getPlugins() {
		return new File((String)JavaWebServer.mainConfig.get("plugins", null));
	}
	
	public File getLogs() {
		return new File((String)JavaWebServer.mainConfig.get("logs", null));
	}
	
	public File getPlugin(Patch p) {
		return new File(getPlugins(), p.name);
	}
	
	public File getBaseFile(String name) {
		return new File(getMainDir(), name);
	}
	
	public void clearCache() throws IOException {
		HTMLCache.reloadAll();
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
			lwiCache.remove(delKeys[i]);
			tbCache.remove(delKeys[i]);
		}
	}
	
	public void getErrorPage(RequestPacket request, MessageBody body, String reqTarget, StatusCode status, String info) {
		HashMap<String, Object> errorPages = (HashMap<String, Object>)JavaWebServer.mainConfig.get("errorpages", request);
		if (errorPages.containsKey(status.getStatus())) {
			try {
				String path = (String)errorPages.get(status.getStatus());
				Resource resource = getResource(path, request);
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
				Logger.logError(e);
			}
		}
		Resource error = new Resource(("<!DOCTYPE HTML PUBLIC \"-//IETF//DTD HTML 2.0//EN\">" + JavaWebServer.crlf + "<html><head>" + JavaWebServer.crlf + "<title>" + status.getStatus() + " " + status.getPhrase() + "</title>" + JavaWebServer.crlf + "</head><body>" + JavaWebServer.crlf + "<h1>" + status.getPhrase() + "</h1>" + JavaWebServer.crlf + "<p>" + info + "</p>" + JavaWebServer.crlf + "</body></html>").getBytes(), "text/html");
		body.setBody(error);
		return;
	}
	
	private boolean lwi = false;
	
	public File getAbsolutePath(String reqTarget, RequestPacket request) {
		lwi = false;
		File abs = new File(request.host.getHTDocs(), URLDecoder.decode(reqTarget));
		if (abs.isDirectory()) {
			String[] index = ((String)JavaWebServer.mainConfig.get("index", request)).split(",");
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
	
	public String correctForIndex(String reqTarget, RequestPacket request) {
		String p = getAbsolutePath(reqTarget, request).getAbsolutePath().replace("\\", "/");
		return p.substring(request.host.getHTDocs().getAbsolutePath().replace("\\", "/").length());
	}
	
	public static final HashMap<String, byte[]> cache = new HashMap<String, byte[]>();
	public static final HashMap<String, String> extCache = new HashMap<String, String>();
	public static final HashMap<String, Boolean> lwiCache = new HashMap<String, Boolean>();
	public static final HashMap<String, Boolean> tbCache = new HashMap<String, Boolean>();
	private static long cacheClock = 0L;
	
	public Resource getResource(String reqTarget, RequestPacket request) {
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
			boolean tooBig = false;
			if (cache.containsKey(rt)) {
				long t = System.currentTimeMillis();
				long cc = Integer.parseInt(((String)JavaWebServer.mainConfig.get("cacheClock", request)));
				boolean tc = cc > 0 && t - cc < cacheClock;
				if (tc || cc == -1 || extCache.get(rt).equals("application/x-java")) {
					resource = cache.get(rt);
					ext = extCache.get(rt);
					lwi = lwiCache.get(rt);
					tooBig = tbCache.get(rt);
				}else if (!tc && cc > 0) {
					cacheClock = t;
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
						lwiCache.remove(delKeys[i]);
						tbCache.remove(delKeys[i]);
					}
				}
			}
			if (resource == null) {
				File abs = getAbsolutePath(rt, request);
				ext = abs.getName().substring(abs.getName().lastIndexOf(".") + 1);
				ext = JavaWebServer.extensionToMime.containsKey(ext) ? JavaWebServer.extensionToMime.get(ext) : "application/octet-stream";
				FileInputStream fin = new FileInputStream(abs);
				ByteArrayOutputStream bout = new ByteArrayOutputStream();
				int i = 1;
				byte[] buf = new byte[4096];
				while (i > 0) {
					i = fin.read(buf);
					if (i > 0) {
						bout.write(buf, 0, i);
					}
					PatchChunked chunked = (PatchChunked)PatchRegistry.getPatchForClass(PatchChunked.class);
					if (chunked.pcfg.get("enabled", null).equals("true") && bout.size() > Integer.parseInt((String)chunked.pcfg.get("minsize", request)) && !ext.startsWith("application")) {
						bout.reset();
						tooBig = true;
						break;
					}
				}
				fin.close();
				resource = bout.toByteArray();
				cache.put(rt, resource);
				extCache.put(rt, ext);
				lwi = this.lwi;
				lwiCache.put(rt, lwi);
				tbCache.put(rt, tooBig);
			}
			Resource r = new Resource(resource, ext, rt);
			r.wasDir = lwi;
			r.tooBig = tooBig;
			return r;
		}catch (IOException e) {
			if (!(e instanceof FileNotFoundException)) Logger.logError(e);
			return null;
		}
	}
}
