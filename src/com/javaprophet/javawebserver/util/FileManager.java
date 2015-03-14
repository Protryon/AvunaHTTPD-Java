package com.javaprophet.javawebserver.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URLDecoder;
import java.util.HashMap;
import com.javaprophet.javawebserver.JavaWebServer;
import com.javaprophet.javawebserver.http.Resource;
import com.javaprophet.javawebserver.http.StatusCode;
import com.javaprophet.javawebserver.networking.packets.RequestPacket;
import com.javaprophet.javawebserver.plugins.Patch;
import com.javaprophet.javawebserver.plugins.PatchRegistry;
import com.javaprophet.javawebserver.plugins.base.PatchChunked;
import com.javaprophet.javawebserver.plugins.base.PatchGZip;
import com.javaprophet.javawebserver.plugins.base.PatchInline;
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
	
	private File dir = null, plugins = null, logs = null;
	private HashMap<String, File> plugin = new HashMap<String, File>();
	private HashMap<String, File> base = new HashMap<String, File>();
	
	public File getMainDir() {
		return dir == null ? (dir = new File((String)JavaWebServer.mainConfig.get("dir"))) : dir;
	}
	
	public File getPlugins() {
		return plugins == null ? (plugins = new File((String)JavaWebServer.mainConfig.get("plugins"))) : plugins;
	}
	
	public File getLogs() {
		return logs == null ? (logs = new File((String)JavaWebServer.mainConfig.get("logs"))) : logs;
	}
	
	public File getPlugin(Patch p) {
		if (!plugin.containsKey(p.name)) {
			plugin.put(p.name, new File(getPlugins(), p.name));
		}
		return plugin.get(p.name);
	}
	
	public File getBaseFile(String name) {
		if (!base.containsKey(name)) {
			base.put(name, new File(getMainDir(), name));
			
		}
		return base.get(name);
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
		cConfigCache.clear();
		((PatchInline)PatchRegistry.getPatchForClass(PatchInline.class)).clearCache();
		((PatchGZip)PatchRegistry.getPatchForClass(PatchGZip.class)).clearCache();
	}
	
	public void flushjl() throws IOException {
		String[] delKeys = new String[cache.size()];
		int delSize = 0;
		for (String file : cache.keySet()) {
			if (extCache.get(file).equals("application/x-java")) {
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
	
	public Resource getErrorPage(RequestPacket request, String reqTarget, StatusCode status, String info) {
		HashMap<String, Object> errorPages = (HashMap<String, Object>)JavaWebServer.mainConfig.get("errorpages");
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
					return resource;
				}
			}catch (Exception e) {
				Logger.logError(e);
			}
		}
		StringBuilder pb = new StringBuilder();
		pb.append("<!DOCTYPE HTML PUBLIC \"-//IETF//DTD HTML 2.0//EN\">");
		pb.append("<html><head>");
		pb.append("<title>");
		pb.append(status.getStatus());
		pb.append(" ");
		pb.append(status.getPhrase());
		pb.append("</title>");
		pb.append("</head><body>");
		pb.append("<h1>");
		pb.append(status.getPhrase());
		pb.append("</h1>");
		pb.append("<p>");
		pb.append(info);
		pb.append("</p>");
		pb.append("</body></html>");
		Resource error = new Resource(pb.toString().getBytes(), "text/html");
		return error;
	}
	
	private boolean lwi = false;
	
	public File getAbsolutePath(String reqTarget, RequestPacket request) {
		lwi = false;
		File abs = new File(request.host.getHTDocs(), URLDecoder.decode(reqTarget));
		if (abs.isDirectory()) {
			String[] index = null;
			if (request.overrideIndex != null) {
				index = request.overrideIndex;
			}else {
				index = ((String)JavaWebServer.mainConfig.get("index")).split(",");
			}
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
	public static final HashMap<String, OverrideConfig> cConfigCache = new HashMap<String, OverrideConfig>();
	private static long cacheClock = 0L;
	
	public OverrideConfig loadDirective(File file, String path) throws IOException { // TODO: load superdirectory directives.
		if (!file.exists()) return null;
		OverrideConfig cfg = new OverrideConfig(file);
		cfg.load();
		cConfigCache.put(path, cfg);
		return cfg;
	}
	
	public String getSuperDirectory(String path) {
		return path.contains("/") ? path.substring(0, path.lastIndexOf("/") + 1) : path;
	}
	
	public Resource preloadOverride(RequestPacket request, Resource resource) throws IOException {
		if (resource == null) return null;
		String rt = request.target;
		if (rt.contains("#")) {
			rt = rt.substring(0, rt.indexOf("#"));
		}
		if (rt.contains("?")) {
			rt = rt.substring(0, rt.indexOf("?"));
		}
		String nrt = getSuperDirectory(request.host.getHostPath() + rt);
		if (cConfigCache.containsKey(nrt)) {
			resource.effectiveOverride = cConfigCache.get(nrt);
		}else {
			File abs = getAbsolutePath(rt, request).getParentFile();
			if (abs.exists()) resource.effectiveOverride = loadDirective(new File(abs, ".override"), nrt);
		}
		return resource;
	}
	
	public Resource getResource(String reqTarget, RequestPacket request) {
		try {
			String rt = reqTarget;
			if (rt.contains("#")) {
				rt = rt.substring(0, rt.indexOf("#"));
			}
			if (rt.contains("?")) {
				rt = rt.substring(0, rt.indexOf("?"));
			}
			String nrt = request.host.getHostPath() + rt; // TODO: overlapping htdocs caching w/o file io
			String superdir = getSuperDirectory(nrt);
			byte[] resource = null;
			String ext = "";
			boolean lwi = false;
			boolean tooBig = false;
			OverrideConfig directive = null;
			if (cache.containsKey(nrt)) {
				long t = System.currentTimeMillis();
				long cc = Integer.parseInt(((String)JavaWebServer.mainConfig.get("cacheClock")));
				if (request.overrideCache >= -1) {
					cc = request.overrideCache;
				}
				boolean tc = cc > 0 && t - cc < cacheClock;
				if (tc || cc == -1 || extCache.get(nrt).equals("application/x-java")) {
					resource = cache.get(nrt);
					if (resource == null) {
						return null;
					}
					ext = extCache.get(nrt);
					lwi = lwiCache.get(nrt);
					tooBig = tbCache.get(nrt);
					directive = cConfigCache.get(superdir);
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
					cConfigCache.clear();
					((PatchInline)PatchRegistry.getPatchForClass(PatchInline.class)).clearCache();
				}
			}
			if (resource == null) {
				File abs = getAbsolutePath(rt, request);
				if (!cConfigCache.containsKey(superdir)) {
					directive = loadDirective(new File(abs.getParentFile(), ".override"), superdir);
				}
				if (abs.exists()) {
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
						if (chunked.pcfg.get("enabled").equals("true") && bout.size() > Integer.parseInt((String)chunked.pcfg.get("minsize")) && !ext.startsWith("application")) {
							bout.reset();
							tooBig = true;
							break;
						}
					}
					fin.close();
					resource = bout.toByteArray();
				}else {
					cache.put(nrt, null);
					extCache.put(nrt, "text/html");
					lwi = this.lwi;
					lwiCache.put(nrt, lwi);
					tbCache.put(nrt, false);
					return null;
				}
				cache.put(nrt, resource);
				extCache.put(nrt, ext);
				lwi = this.lwi;
				lwiCache.put(nrt, lwi);
				tbCache.put(nrt, tooBig);
			}
			Resource r = new Resource(resource, ext, rt, directive);
			r.wasDir = lwi;
			r.tooBig = tooBig;
			return r;
		}catch (IOException e) {
			if (!(e instanceof FileNotFoundException)) Logger.logError(e);
			return null;
		}
	}
}
