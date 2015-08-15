/* Avuna HTTPD - General Server Applications Copyright (C) 2015 Maxwell Bruce This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>. */

package org.avuna.httpd.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import org.avuna.httpd.AvunaHTTPD;
import org.avuna.httpd.event.EventBus;
import org.avuna.httpd.hosts.Host;
import org.avuna.httpd.hosts.HostHTTP;
import org.avuna.httpd.hosts.VHost;
import org.avuna.httpd.http.Resource;
import org.avuna.httpd.http.StatusCode;
import org.avuna.httpd.http.event.EventClearCache;
import org.avuna.httpd.http.networking.RequestPacket;
import org.avuna.httpd.http.plugins.avunaagent.PluginAvunaAgent;
import org.avuna.httpd.http.plugins.avunaagent.lib.AvunaAgentUtil;
import org.avuna.httpd.http.plugins.avunaagent.lib.HTMLCache;
import org.avuna.httpd.http.plugins.base.PluginChunked;
import org.avuna.httpd.http.plugins.base.PluginFCGI;
import org.avuna.httpd.http.plugins.base.PluginOverride;
import org.avuna.httpd.http.util.OverrideConfig;

/** General utility for File type objects.
 * 
 * @author Max */
public class FileManager {
	public FileManager() {
	
	}
	
	/** Character array of valid hex values. */
	private final static char[] hexArray = "0123456789ABCDEF".toCharArray();
	
	/** Converts a byte array to a string in hex format.
	 * 
	 * @param bytes a byte array
	 * @return String in hex */
	public String bytesToHex(byte[] bytes) {
		char[] hexChars = new char[bytes.length * 2];
		for (int j = 0; j < bytes.length; j++) {
			int v = bytes[j] & 0xFF;
			hexChars[j * 2] = hexArray[v >>> 4];
			hexChars[j * 2 + 1] = hexArray[v & 0x0F];
		}
		return new String(hexChars);
	}
	
	private File dir = null, logs = null;
	private HashMap<String, File> base = new HashMap<String, File>();
	
	/** Get object dir value if exists or get value from {@link AvunaHTTPD#mainConfig mainConfig}.
	 * 
	 * @return dir */
	public File getMainDir() {
		return dir == null ? (dir = new File(AvunaHTTPD.mainConfig.getNode("dir").getValue())) : dir;
	}
	
	/** Get logs value if exists else get value from {@link AvunaHTTPD#mainConfig mainConfig}.
	 * 
	 * @return logs value */
	public File getLogs() {
		return logs == null ? (logs = new File(AvunaHTTPD.mainConfig.getNode("logs").getValue())) : logs;
	}
	
	/** Get base file value for object else get main directory and set it.
	 * 
	 * @param name
	 * @return name base file name */
	public File getBaseFile(String name) {
		if (!base.containsKey(name)) {
			base.put(name, new File(getMainDir(), name));
		}
		return base.get(name);
	}
	
	/** Clear the HTML cache, including extCache, lwiCache, and tbCache, except where extCache has key "application/x-java". Also clears Patch maps.
	 * 
	 * @see EventBus#callEvent
	 * @throws IOException */
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
			absCache.remove(delKeys[i]);
		}
		cConfigCache.clear();
		for (Host host : AvunaHTTPD.hosts.values()) {
			if (host instanceof HostHTTP) {
				((HostHTTP) host).eventBus.callEvent(new EventClearCache());
			}
		}
		
	}
	
	public void flushjl() throws IOException {
		String[] delKeys = new String[cache.size()];
		int delSize = 0;
		for (String file : cache.keySet()) {
			if (extCache.get(file) == null) continue;
			if (extCache.get(file).equals("application/x-java")) {
				if (delSize > delKeys.length) {
					String[] ndk = new String[delKeys.length + 1];
					System.arraycopy(delKeys, 0, ndk, 0, delKeys.length);
					delKeys = ndk;
				}
				delKeys[delSize++] = file;
			}
		}
		for (int i = 0; i < delSize; i++) {
			cache.remove(delKeys[i]);
			extCache.remove(delKeys[i]);
			lwiCache.remove(delKeys[i]);
			tbCache.remove(delKeys[i]);
			absCache.remove(delKeys[i]);
		}
	}
	
	/** Takes error page request, if error page is in configuration deliver it back, else deliver standard html notice.
	 * 
	 * @param request
	 * @param reqTarget
	 * @param status
	 * @param info
	 * @see Resource
	 * @return error page if configured, else standard error message. */
	public Resource getErrorPage(RequestPacket request, String reqTarget, StatusCode status, String info) {
		ConfigNode errorPages = request.host.getErrorPages();
		if (errorPages != null && errorPages.containsNode(status.getStatus() + "")) {
			try {
				String path = errorPages.getNode(status.getStatus() + "").getValue();
				Resource resource = getResource(path, request);
				if (resource != null) {
					if (resource.type.startsWith("text")) {
						String res = new String(resource.data);
						res = res.replace("$_statusCode_$", status.getStatus() + "").replace("$_reason_$", status.getPhrase()).replace("$_info_$", AvunaAgentUtil.htmlescape(info)).replace("$_reqTarget_$", AvunaAgentUtil.htmlescape(reqTarget));
						resource.data = res.getBytes();
					}
					return resource;
				}
			}catch (Exception e) {
				request.host.logger.logError(e);
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
		pb.append(AvunaAgentUtil.htmlescape(info));
		pb.append("</p>");
		pb.append("</body></html>");
		Resource error = new Resource(pb.toString().getBytes(), "text/html");
		return error;
	}
	
	private boolean lwi = false;// TODO: thread safety?
	
	/** Retrieve file system path to requested URL. If request is to directory, append index file path from host configuration. Set extra parameters to child of path.
	 * 
	 * @param reqTarget2 request URL
	 * @param request Incoming packet for host data
	 * @see PluginOverride#processPacket(org.avuna.httpd.http.networking.Packet)
	 * @return absolute file system path and parameter string (as child) */
	public File getAbsolutePath(String reqTarget2, RequestPacket request) {
		String reqTarget = reqTarget2;
		lwi = false;
		File htd = request.host.getHTDocs();
		File abs = htd;
		String htds = htd.getAbsolutePath();
		String rdtf = null;
		if (request.rags1 != null && request.rags2 != null) {
			String subabs = reqTarget;
			String nsa = subabs.replaceAll(request.rags1, request.rags2); // TODO: edward snowden ;)
			if (!subabs.equals(nsa)) {
				File saf = new File(htd, subabs);
				File nsaf = new File(htd, nsa);
				String safs = saf.getAbsolutePath();
				String nsafs = nsaf.getAbsolutePath();
				if (!safs.startsWith(htds) || !nsafs.startsWith(htds)) {
					return null;
				}
				rdtf = safs;
				if (!saf.exists()) {
					reqTarget = nsa;
				}
			}
		}
		String[] t = new String[0];
		try {
			t = URLDecoder.decode(reqTarget, "UTF-8").split("/");
		}catch (UnsupportedEncodingException e) {
			request.host.logger.logError(e);
		}
		boolean ext = false;
		String ep = "";
		for (String st : t) {
			if (ext) {
				ep += "/" + st;
			}else {
				abs = new File(abs, st);
				if (!CLib.failed) {
					try {
						String sm = SafeMode.readIfSymlink(abs);
						if (sm != null) {
							if (sm.startsWith("/")) {
								abs = new File(sm);
							}else {
								abs = new File(abs.getParentFile(), sm);
							}
						}
						if (SafeMode.isHardlink(abs)) {
							return null;
						}
					}catch (CException e) {
						request.host.logger.logError(e);
						return null;
					}
					
				}
				if (abs.isFile() || (rdtf != null && abs.getAbsolutePath().startsWith(rdtf))) {
					ext = true;
				}
			}
		}
		request.extraPath = ep;
		String abspr = abs.getAbsolutePath();
		if (!abspr.startsWith(htds)) {
			return null;
		}
		
		if (abs.isDirectory()) {
			String[] index = null;
			if (request.overrideIndex != null) {
				index = request.overrideIndex;
			}else {
				index = request.host.getIndex();
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
					if (ep.length() == 0) lwi = true;
					break;
				}
			}
		}
		return abs;
	}
	
	/** Correct from Windows directory index characters to Unix style index for htdocs for incoming request.
	 * 
	 * @param reqTarget request URL
	 * @param request Incoming packet
	 * @return htdocs absolute path in Unix format */
	public String correctForIndex(String reqTarget, RequestPacket request) {
		String p = getAbsolutePath(reqTarget, request).getAbsolutePath().replace("\\", "/");
		return p.substring(request.host.getHTDocs().getAbsolutePath().replace("\\", "/").length());
	}
	
	public static final HashMap<String, byte[]> cache = new HashMap<String, byte[]>();
	public static final HashMap<String, String> extCache = new HashMap<String, String>();
	public static final HashMap<String, String> absCache = new HashMap<String, String>();
	public static final HashMap<String, Boolean> lwiCache = new HashMap<String, Boolean>();
	public static final HashMap<String, Boolean> tbCache = new HashMap<String, Boolean>();
	public static final HashMap<String, OverrideConfig> cConfigCache = new HashMap<String, OverrideConfig>();
	private static long cacheClock = 0L;
	
	/** Instantiates an {@link OverrideConfig} from .override files in htdocs, adds it to to the {@link #cConfigCache} with path as key.
	 * 
	 * @param file name of override file
	 * @param path path to override file
	 * @return
	 * @throws IOException */
	public OverrideConfig loadDirective(VHost vhost, File file, String path) throws IOException { // TODO: load superdirectory directives.
		if (!file.exists()) return null;
		OverrideConfig cfg = new OverrideConfig(file);
		cfg.load(vhost);
		cConfigCache.put(path, cfg);
		return cfg;
	}
	
	/** @param path
	 * @return path to last index "/" or path if no index included */
	public String getSuperDirectory(String path) {
		return path.contains("/") ? path.substring(0, path.lastIndexOf("/") + 1) : path;
	}
	
	/** Sets {@link Resource#effectiveOverride} from {@link #cConfigCache} or loads values from lowest child directory in host tree .override file if it exists.
	 * 
	 * @param request URL request
	 * @param resource page content
	 * @param htds the host document source path
	 * @see Resource
	 * @return resource with {@link Resource#effectiveOverride}
	 * @throws IOException */
	public Resource preloadOverride(RequestPacket request, Resource resource, String htds) throws IOException {
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
			File abs = getAbsolutePath(rt, request);
			if (abs == null) {
				return null;
			}
			abs = abs.getParentFile();
			if (abs == null) {
				return null;
			}
			File override = null;
			do {
				if (!abs.exists()) abs = abs.getParentFile();
				File no = new File(abs, ".override");
				if (no.isFile()) {
					override = no;
					continue;
				}else {
					abs = abs.getParentFile();
					if (!abs.getAbsolutePath().startsWith(htds)) break;
				}
			}while (override == null);
			if (override != null) {
				resource.effectiveOverride = loadDirective(request.host, new File(abs, ".override"), nrt);
			}else {
				cConfigCache.put(nrt, null);
			}
		}
		return resource;
	}
	
	/** Check incoming request against cache. If request is cached and not expired return cached resource. If cache has expired clear caches. Otherwise read htdocs file and build resource.
	 * 
	 * @param reqTarget URl request
	 * @param request page content
	 * @see Resource
	 * @see EventBus#callEvent
	 * @see PluginChunked
	 * @return resource */
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
			String oabs = null;
			if (cache.containsKey(nrt)) {
				long t = System.currentTimeMillis();
				long cc = request.host.getCacheClock();
				if (request.overrideCache >= -1) {
					cc = request.overrideCache;
				}
				boolean tc = cc > 0 && t - cc < cacheClock;
				if (tc || cc == -1 || extCache.get(nrt).equals("application/x-java")) {
					synchronized (cache) {
						resource = cache.get(nrt);
						if (resource == null) {
							return null;
						}
						ext = extCache.get(nrt);
						lwi = lwiCache.get(nrt);
						tooBig = tbCache.get(nrt);
						oabs = absCache.get(nrt);
						directive = cConfigCache.get(superdir);
					}
				}else if (!tc && cc >= 0) {
					synchronized (cache) {
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
							absCache.remove(delKeys[i]);
							tbCache.remove(delKeys[i]);
						}
						cConfigCache.clear();
					}
					request.host.getHost().eventBus.callEvent(new EventClearCache());
				}
			}
			if (resource == null) {
				File abs = getAbsolutePath(rt, request);
				if (abs == null) {
					synchronized (cache) {
						cache.put(nrt, null);
						extCache.put(nrt, "text/html");
						lwi = this.lwi;
						lwiCache.put(nrt, lwi);
						absCache.put(nrt, oabs);
						tbCache.put(nrt, false);
					}
					return null;
				}
				if (!cConfigCache.containsKey(superdir) && abs != null) {
					directive = loadDirective(request.host, new File(abs.getParentFile(), ".override"), superdir);
				}
				oabs = abs.getAbsolutePath();
				if (abs != null && abs.exists()) {
					ext = abs.getName().substring(abs.getName().lastIndexOf(".") + 1);
					ext = AvunaHTTPD.extensionToMime.containsKey(ext) ? AvunaHTTPD.extensionToMime.get(ext) : "application/octet-stream";
					if (request.overrideType != null) {
						ext = request.overrideType;
					}
					FileInputStream fin = new FileInputStream(abs);
					ByteArrayOutputStream bout = new ByteArrayOutputStream();
					int i = 1;
					byte[] buf = new byte[4096];
					PluginChunked chunked = (PluginChunked) request.host.registry.getPatchForClass(PluginChunked.class);
					PluginFCGI fcgi = (PluginFCGI) request.host.registry.getPatchForClass(PluginFCGI.class);
					PluginAvunaAgent jl = (PluginAvunaAgent) request.host.registry.getPatchForClass(PluginAvunaAgent.class);
					boolean dc = chunked != null && chunked.pcfg.getNode("enabled").getValue().equals("true");
					if (dc && fcgi != null && fcgi.pcfg.getNode("enabled").getValue().equals("true")) {
						major: for (String key : fcgi.fcgis.keySet()) {
							String[] pcts = key.split(",");
							for (String pct : pcts) {
								if (pct.trim().equals(ext)) {
									dc = false;
									break major;
								}
							}
						}
					}
					if (dc && jl != null && jl.pcfg.getNode("enabled").getValue().equals("true")) {
						if (ext.equals("application/x-java")) {
							dc = false;
						}
					}
					int s = dc ? Integer.parseInt(chunked.pcfg.getNode("minsize").getValue()) : -1;
					while (i > 0) {
						i = fin.read(buf);
						if (i > 0) {
							bout.write(buf, 0, i);
						}
						if (dc && bout.size() > s) {
							bout.reset();
							tooBig = true;
							break;
						}
					}
					fin.close();
					resource = bout.toByteArray();
				}else {
					synchronized (cache) {
						cache.put(nrt, null);
						extCache.put(nrt, "text/html");
						lwi = this.lwi;
						lwiCache.put(nrt, lwi);
						absCache.put(nrt, oabs);
						tbCache.put(nrt, false);
					}
					return null;
				}
				synchronized (cache) {
					cache.put(nrt, resource);
					extCache.put(nrt, ext);
					lwi = this.lwi;
					lwiCache.put(nrt, lwi);
					absCache.put(nrt, oabs);
					tbCache.put(nrt, tooBig);
				}
			}
			Resource r = new Resource(resource, ext, rt, directive, oabs);
			r.wasDir = lwi;
			r.tooBig = tooBig;
			return r;
		}catch (IOException e) {
			if (!(e instanceof FileNotFoundException)) request.host.logger.logError(e);
			return null;
		}
	}
}
