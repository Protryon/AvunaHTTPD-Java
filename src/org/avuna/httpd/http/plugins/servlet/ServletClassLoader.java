/* Avuna HTTPD - General Server Applications Copyright (C) 2015 Maxwell Bruce This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>. */

package org.avuna.httpd.http.plugins.servlet;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import org.avuna.httpd.hosts.VHost;

public class ServletClassLoader extends URLClassLoader {
	protected final VHost vhost;
	private HashMap<String, String> name2class = new HashMap<String, String>();
	private HashMap<String, InitParam[]> name2init = new HashMap<String, InitParam[]>();
	private ArrayList<InitParam> contextParams = new ArrayList<InitParam>();
	private HashMap<String, String> path2name = new HashMap<String, String>();
	private HashMap<Class<? extends Servlet>, Servlet> lservs = new HashMap<Class<? extends Servlet>, Servlet>();
	protected final AvunaServletContext context = new AvunaServletContext(this);
	
	protected String getMountDir(String cls) {
		for (String key : name2class.keySet()) {
			if (name2class.get(key).equals(cls)) {
				for (String key2 : path2name.keySet()) {
					if (path2name.get(key2).equals(key)) {
						return key2;
					}
				}
				return null;
			}
		}
		return null;
	}
	
	@SuppressWarnings("unchecked")
	public Servlet getServlet(String path) throws ClassNotFoundException, ServletException, InstantiationException, IllegalAccessException {
		Class<? extends Servlet> sc = null;
		String n = null;
		for (String p : path2name.keySet()) {
			if (path.startsWith(p)) {
				String name = path2name.get(p);
				if (name == null) continue;
				n = name;
				String clss = name2class.get(name);
				if (clss == null) continue;
				Class<?> cls;
				cls = loadClass(clss);
				if (cls != null && Servlet.class.isAssignableFrom(cls)) {
					sc = (Class<? extends Servlet>) cls;
					break;
				}
			}
		}
		if (sc == null) return null;
		Servlet s = lservs.get(sc);
		if (s == null) {
			s = sc.newInstance();
			s.init(new AvunaServletConfig(n, this, context));
			lservs.put(sc, s);
		}
		return s;
	}
	
	protected String getInitParameter(String sname, String param) {
		InitParam[] ip = name2init.get(sname);
		if (ip == null) return null;
		for (InitParam ipp : ip) {
			if (ipp == null) continue;
			if (ipp.name.equalsIgnoreCase(param)) {
				return ipp.value;
			}
		}
		return null;
	}
	
	protected String getContextInitParameter(String param) {
		for (InitParam ip : contextParams) {
			if (ip.name.equalsIgnoreCase(param)) {
				return ip.value;
			}
		}
		return null;
	}
	
	protected Enumeration<String> getContextInitParameters() {
		String[] names = new String[contextParams.size()];
		for (int i = 0; i < contextParams.size(); i++) {
			names[i] = contextParams.get(i).name;
		}
		return Collections.enumeration(new HashSet<String>(Arrays.asList(names)));
	}
	
	protected Enumeration<String> getInitParameters(String sname) {
		InitParam[] ip = name2init.get(sname);
		if (ip == null) return Collections.emptyEnumeration();
		String[] name = new String[ip.length];
		for (int i = 0; i < ip.length; i++) {
			if (ip[i] == null) continue;
			name[i] = ip[i].name;
		}
		return Collections.enumeration(new HashSet<String>(Arrays.asList(name)));
	}
	
	// TODO: resource-ref
	public ServletClassLoader(VHost vhost, URL[] url, ClassLoader parent, File war) throws IOException {
		super(url, parent);
		this.vhost = vhost;
		loadZIP(new FileInputStream(war), true);
	}
	
	private final void loadXML(String xml) {
		String webxml = xml.substring(xml.indexOf("<web-app>") + "<web-app>".length(), xml.lastIndexOf("</web-app>")).trim();
		int i = -1;
		while ((i = webxml.indexOf("<context-param>", i)) >= 0) {
			int end2 = webxml.indexOf("</context-param>", i);
			i += "<context-param>".length();
			String param = webxml.substring(i, end2).trim();
			int pns = param.indexOf("<param-name>");
			int pne = param.indexOf("</param-name>");
			int pvs = param.indexOf("<param-value>");
			int pve = param.indexOf("</param-value>");
			if (pns < 0 || pne < 0 || pvs < 0 || pve < 0) continue;
			int descs = param.indexOf("<description>");
			int desce = param.indexOf("</description>");
			if ((descs == -1 && desce != -1) || (descs != 1 && desce == -1)) continue;
			InitParam ipp = new InitParam(param.substring(pns + "<param-name>".length(), pne).trim(), param.substring(pvs + "<param-value>".length(), pve).trim(), descs > 0 ? param.substring(descs + "<description>".length(), desce).trim() : "");
			contextParams.add(ipp);
		}
		i = -1;
		do {
			i = webxml.indexOf("<servlet>", i);
			if (i >= 0) {
				int end = webxml.indexOf("</servlet>", i);
				i += "<servlet>".length();
				String serv = webxml.substring(i, end).trim();
				String servname = serv.substring(serv.indexOf("<servlet-name>") + "<servlet-name>".length(), serv.indexOf("</servlet-name>"));
				String servclass = serv.substring(serv.indexOf("<servlet-class>") + "<servlet-class>".length(), serv.indexOf("</servlet-class>"));
				int ip = 0;
				ArrayList<InitParam> ips = new ArrayList<InitParam>();
				while ((ip = serv.indexOf("<init-param>", ip)) >= 0) {
					int end2 = serv.indexOf("</init-param>", ip);
					ip += "<init-param>".length();
					String param = serv.substring(ip, end2).trim();
					int pns = param.indexOf("<param-name>");
					int pne = param.indexOf("</param-name>");
					int pvs = param.indexOf("<param-value>");
					int pve = param.indexOf("</param-value>");
					if (pns < 0 || pne < 0 || pvs < 0 || pve < 0) continue;
					int descs = param.indexOf("<description>");
					int desce = param.indexOf("</description>");
					if ((descs == -1 && desce != -1) || (descs != 1 && desce == -1)) continue;
					InitParam ipp = new InitParam(param.substring(pns + "<param-name>".length(), pne).trim(), param.substring(pvs + "<param-value>".length(), pve).trim(), descs > 0 ? param.substring(descs + "<description>".length(), desce).trim() : "");
					ips.add(ipp);
				}
				name2init.put(servname, ips.toArray(new InitParam[0]));
				name2class.put(servname, servclass);
			}
		}while (i >= 0);
		i = -1;
		do {
			i = webxml.indexOf("<servlet-mapping>", i);
			if (i >= 0) {
				int end = webxml.indexOf("</servlet-mapping>", i);
				i += "<servlet-mapping>".length();
				String serv = webxml.substring(i, end).trim();
				String servname = serv.substring(serv.indexOf("<servlet-name>") + "<servlet-name>".length(), serv.indexOf("</servlet-name>"));
				String servpath = serv.substring(serv.indexOf("<url-pattern>") + "<url-pattern>".length(), serv.indexOf("</url-pattern>"));
				path2name.put(servpath, servname);
			}
		}while (i >= 0);
	}
	
	private final void loadZIP(InputStream f, boolean war) throws IOException {
		ZipInputStream zin = new ZipInputStream(f);
		ZipEntry ze = null;
		byte[] buf = new byte[1024];
		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		int i = 0;
		while ((ze = zin.getNextEntry()) != null) {
			String name = ze.getName();
			if (name.startsWith("/")) name = name.substring(1);
			String ap = null, re = null;
			if (war && name.startsWith("WEB-INF/classes/") && !ze.isDirectory()) {
				String p = name.substring("WEB-INF/classes/".length()).replace('/', '.');
				if (name.endsWith(".class")) ap = p.substring(0, p.length() - 6);
				else re = p;
			}else if (war && name.startsWith("WEB-INF/lib/") && !ze.isDirectory()) {
				do {
					i = zin.read(buf);
					if (i > 0) bout.write(buf, 0, i);
				}while (i > 0);
				loadZIP(new ByteArrayInputStream(bout.toByteArray()), false);
				bout.reset();
			}else if (war && name.equals("WEB-INF/web.xml")) {
				do {
					i = zin.read(buf);
					if (i > 0) bout.write(buf, 0, i);
				}while (i > 0);
				loadXML(bout.toString());
				bout.reset();
			}else if (name.endsWith(".class")) {
				ap = name.replace('/', '.').substring(0, name.length() - 6);
			}else if (!ze.isDirectory()) {
				re = name.replace('/', '.');
			}
			if (ap != null) {
				do {
					i = zin.read(buf);
					if (i > 0) bout.write(buf, 0, i);
				}while (i > 0);
				addClass(ap, bout.toByteArray());
				bout.reset();
			}else if (re != null) {
				do {
					i = zin.read(buf);
					if (i > 0) bout.write(buf, 0, i);
				}while (i > 0);
				servletResources.put(ap, bout.toByteArray());
				bout.reset();
			}
		}
	}
	
	private HashMap<String, Class<?>> servletClasses = new HashMap<String, Class<?>>();
	private HashMap<String, byte[]> servletResources = new HashMap<String, byte[]>();
	
	public InputStream getResourceAsStream(String name) {
		byte[] b = servletResources.get(name);
		if (b != null) {
			return new ByteArrayInputStream(b);
		}
		return super.getResourceAsStream(name);
	}
	
	public String addClass(byte[] data) throws LinkageError {
		return addClass(null, data);
	}
	
	public String addClass(String name, byte[] data) throws LinkageError {
		Class<?> cls = defineClass(name, data, 0, data.length);
		servletClasses.put(cls.getName(), cls);
		return cls.getName();
	}
	
	public Class<?> findClass(String name) {
		if (servletClasses.containsKey(name)) return servletClasses.get(name);
		try {
			Class<?> see = super.findClass(name);
			if (see != null) return see;
		}catch (ClassNotFoundException e) {
			vhost.logger.logError(e);
		}
		return null;
	}
	
	public Class<?> loadClass(String name, boolean resolve) {
		if (servletClasses.containsKey(name)) return servletClasses.get(name);
		try {
			Class<?> see = super.loadClass(name, resolve);
			if (see != null) return see;
		}catch (ClassNotFoundException e) {
			vhost.logger.logError(e);
		}
		return null;
	}
	
	public void finalize() throws Throwable {
		super.finalize();
		servletClasses = null;
		name2class = null;
		path2name = null;
		lservs = null;
		name2init = null;
		contextParams = null;
	}
}
