package org.avuna.httpd.http.plugins.servlet;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import javax.servlet.AsyncContext;
import javax.servlet.DispatcherType;
import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpUpgradeHandler;
import javax.servlet.http.Part;
import org.avuna.httpd.http.networking.RequestPacket;
import org.avuna.httpd.http.plugins.avunaagent.lib.Multipart;
import org.avuna.httpd.http.plugins.avunaagent.lib.Multipart.MultiPartData;
import org.avuna.httpd.http.plugins.avunaagent.lib.Session;

public class AvunaServletRequest implements HttpServletRequest {
	
	private final RequestPacket request;
	private final AvunaServletContext context;
	private final Servlet servlet;
	
	protected AvunaServletRequest(RequestPacket request, AvunaServletContext context, Servlet servlet) {
		this.request = request;
		this.context = context;
		this.servlet = servlet;
	}
	
	@Override
	public AsyncContext getAsyncContext() {
		return null;
	}
	
	@Override
	public Object getAttribute(String arg0) {
		return attributes.get(arg0);
	}
	
	@Override
	public Enumeration<String> getAttributeNames() {
		return Collections.emptyEnumeration();
	}
	
	private Session session = null;
	
	@Override
	public String getCharacterEncoding() {
		if (encoding != null) return encoding;
		String ct = request.headers.getHeader("Content-Type");
		String[] spl = ct.split("\\:");
		for (String sp : spl) {
			sp = sp.trim();
			if (sp.startsWith("charset=")) {
				sp = sp.substring(8);
				return sp;
			}
		}
		return "utf-8";
	}
	
	@Override
	public int getContentLength() {
		return request.body == null ? 0 : (request.body.data == null ? 0 : request.body.data.length);
	}
	
	@Override
	public long getContentLengthLong() {
		return request.body == null ? 0 : (request.body.data == null ? 0 : request.body.data.length);
	}
	
	@Override
	public String getContentType() {
		return request.headers.getHeader("Content-Type");
	}
	
	@Override
	public DispatcherType getDispatcherType() {
		return DispatcherType.ASYNC;
	}
	
	@Override
	public ServletInputStream getInputStream() throws IOException {
		if (request.body == null || request.body.data == null) return null;
		return new ServletInputStreamWrapper(request);
	}
	
	@Override
	public String getLocalAddr() {
		return request.work.s.getLocalAddress().getHostAddress();
	}
	
	@Override
	public String getLocalName() {
		return getLocalAddr();
	}
	
	@Override
	public int getLocalPort() {
		return request.work.s.getLocalPort();
	}
	
	@Override
	public Locale getLocale() {
		return Locale.ENGLISH; // TODO: ?
	}
	
	@Override
	public Enumeration<Locale> getLocales() {
		return Collections.emptyEnumeration();
	}
	
	@Override
	public String getParameter(String arg0) {
		String param = request.get.get(arg0);
		return param == null ? request.post.get(arg0) : param;
	}
	
	@Override
	public Map<String, String[]> getParameterMap() {
		HashMap<String, String[]> paramMap = new HashMap<String, String[]>();
		for (String value : request.get.keySet())
			paramMap.put(value, new String[] { request.get.get(value) });
		for (String value : request.post.keySet())
			paramMap.put(value, new String[] { request.post.get(value) });
		return paramMap;
	}
	
	@Override
	public Enumeration<String> getParameterNames() {
		ArrayList<String> paramMap = new ArrayList<String>();
		for (String value : request.get.keySet())
			paramMap.add(value);
		for (String value : request.post.keySet())
			paramMap.add(value);
		return Collections.enumeration(paramMap);
	}
	
	@Override
	public String[] getParameterValues(String arg0) {
		String param = request.get.get(arg0);
		return new String[] { param == null ? request.post.get(arg0) : param };
	}
	
	@Override
	public String getProtocol() {
		return request.work.ssl ? "HTTPS" : "HTTP";
	}
	
	@Override
	public BufferedReader getReader() throws IOException {
		if (request.body == null || request.body.data == null) return null;
		return new BufferedReader(new InputStreamReader(new ByteArrayInputStream(request.body.data), getCharacterEncoding()));
	}
	
	@Override
	public String getRealPath(String arg0) {
		return context.getRealPath(arg0);
	}
	
	@Override
	public String getRemoteAddr() {
		return request.work.s.getInetAddress().getHostAddress();
	}
	
	@Override
	public String getRemoteHost() {
		return request.work.s.getInetAddress().getHostName();
	}
	
	@Override
	public int getRemotePort() {
		return request.work.s.getPort();
	}
	
	@Override
	public RequestDispatcher getRequestDispatcher(String arg0) {
		return context.getRequestDispatcher(arg0);
	}
	
	@Override
	public String getScheme() {
		return request.work.ssl ? "HTTPS" : "HTTP";
	}
	
	@Override
	public String getServerName() {
		return request.headers.getHeader("Host");
	}
	
	@Override
	public int getServerPort() {
		return request.work.host.getPort();
	}
	
	@Override
	public ServletContext getServletContext() {
		return context;
	}
	
	@Override
	public boolean isAsyncStarted() {
		return false;
	}
	
	@Override
	public boolean isAsyncSupported() {
		return false;
	}
	
	@Override
	public boolean isSecure() {
		return request.work.ssl;
	}
	
	private HashMap<String, Object> attributes = new HashMap<String, Object>();
	
	@Override
	public void removeAttribute(String arg0) {
		attributes.remove(arg0);
	}
	
	@Override
	public void setAttribute(String arg0, Object arg1) {
		attributes.put(arg0, arg1);
	}
	
	private String encoding = null;
	
	@Override
	public void setCharacterEncoding(String arg0) throws UnsupportedEncodingException {
		this.encoding = arg0;
	}
	
	@Override
	public AsyncContext startAsync() throws IllegalStateException {
		return null;
	}
	
	@Override
	public AsyncContext startAsync(ServletRequest arg0, ServletResponse arg1) throws IllegalStateException {
		return null;
	}
	
	@Override
	public boolean authenticate(HttpServletResponse arg0) throws IOException, ServletException {
		return false; // TODO: impl
	}
	
	@Override
	public String changeSessionId() {
		this.session = Session.restartSession(request.child);
		return this.session.getSessionID();
	}
	
	@Override
	public String getAuthType() {
		return null;
	}
	
	@Override
	public String getContextPath() {
		return context.getContextPath();
	}
	
	@Override
	public Cookie[] getCookies() {
		Cookie[] cs = new Cookie[request.cookie.size()];
		int i = 0;
		for (String key : request.cookie.keySet()) {
			cs[i++] = new Cookie(key, request.cookie.get(key));
		}
		return cs;
	}
	
	@Override
	public long getDateHeader(String arg0) {
		return -1L;
	}
	
	@Override
	public String getHeader(String arg0) {
		return request.headers.getHeader(arg0);
	}
	
	@Override
	public Enumeration<String> getHeaderNames() {
		return Collections.enumeration(request.headers.getHeaders().keySet());
	}
	
	@Override
	public Enumeration<String> getHeaders(String arg0) {
		return Collections.enumeration(Arrays.asList(request.headers.getHeaders(arg0)));
	}
	
	@Override
	public int getIntHeader(String arg0) {
		return Integer.parseInt(request.headers.getHeader(arg0));
	}
	
	@Override
	public String getMethod() {
		return request.method.name;
	}
	
	private Multipart mp = null;
	
	@Override
	public Part getPart(String arg0) throws IOException, ServletException {
		String ct = request.headers.getHeader("Content-Type");
		if (ct == null || !ct.startsWith("multipart/") || request.body == null || request.body.data == null) throw new ServletException("Not a multipart request!");
		if (mp == null) {
			mp = new Multipart(request.host.logger, null, request.body.data);
		}
		for (MultiPartData mpd : mp.mpds) {
			String mpdn = mpd.vars.get("name");
			if (mpdn != null && mpdn.equalsIgnoreCase(arg0)) {
				return new PartWrapper(mpd);
			}
		}
		return null;
	}
	
	@Override
	public Collection<Part> getParts() throws IOException, ServletException {
		String ct = request.headers.getHeader("Content-Type");
		if (ct == null || !ct.startsWith("multipart/") || request.body == null || request.body.data == null) throw new ServletException("Not a multipart request!");
		if (mp == null) {
			mp = new Multipart(request.host.logger, null, request.body.data);
		}
		ArrayList<Part> parts = new ArrayList<Part>();
		for (MultiPartData mpd : mp.mpds) {
			parts.add(new PartWrapper(mpd));
		}
		return parts;
	}
	
	@Override
	public String getPathInfo() {
		return request.extraPath; // TODO: probably broken due to VFI-like system for servlets
	}
	
	@Override
	public String getPathTranslated() {
		try {
			return new File(request.host.getHTDocs(), URLDecoder.decode(request.extraPath, "UTF-8")).getAbsolutePath();
		}catch (UnsupportedEncodingException e) {
			request.host.logger.logError(e);
			return null;
		}
	}
	
	@Override
	public String getQueryString() {
		String get = request.target;
		if (get.contains("#")) {
			get = get.substring(0, get.indexOf("#"));
		}
		if (get.contains("?")) {
			get = get.substring(get.indexOf("?") + 1);
		}else {
			get = "";
		}
		return get;
	}
	
	@Override
	public String getRemoteUser() {
		return null;
	}
	
	@Override
	public String getRequestURI() {
		String get = request.target;
		if (get.contains("#")) {
			get = get.substring(0, get.indexOf("#"));
		}
		String rq = get;
		if (get.contains("?")) {
			rq = get.substring(0, get.indexOf("?"));
		}
		return rq;
	}
	
	@Override
	public StringBuffer getRequestURL() {
		return new StringBuffer((request.work.ssl ? "https" : "http") + "://" + request.headers.getHeader("Host") + request.target);
	}
	
	@Override
	public String getRequestedSessionId() {
		if (this.session == null) this.session = Session.getSession(request.child);
		return this.session.getSessionID();
	}
	
	@Override
	public String getServletPath() {
		return context.getClassLoader().getMountDir(servlet.getClass().getName());
	}
	
	@Override
	public HttpSession getSession() {
		if (this.session == null) this.session = Session.getSession(request.child);
		return new HttpSessionWrapper(this.session, this.context);// TODO
	}
	
	@Override
	public HttpSession getSession(boolean arg0) {
		if (arg0 || this.session != null) {
			return getSession();
		}else {
			this.session = Session.existingSession(request.child);
			if (this.session == null) {
				return null;
			}else {
				return new HttpSessionWrapper(this.session, this.context);// TODO
			}
		}
	}
	
	@Override
	public Principal getUserPrincipal() {
		return null;
	}
	
	@Override
	public boolean isRequestedSessionIdFromCookie() {
		return true;
	}
	
	@Override
	public boolean isRequestedSessionIdFromURL() {
		return false;
	}
	
	@Override
	public boolean isRequestedSessionIdFromUrl() {
		return false;
	}
	
	@Override
	public boolean isRequestedSessionIdValid() {
		return true; // we dont return a session if its not valid
	}
	
	@Override
	public boolean isUserInRole(String arg0) {
		return false;
	}
	
	@Override
	public void login(String arg0, String arg1) throws ServletException {
	
	}
	
	@Override
	public void logout() throws ServletException {
	
	}
	
	@Override
	public <T extends HttpUpgradeHandler> T upgrade(Class<T> arg0) throws IOException, ServletException {
		
		return null;
	}
	
}
