package org.avuna.httpd.http.plugins.servlet;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import javax.servlet.AsyncContext;
import javax.servlet.DispatcherType;
import javax.servlet.ReadListener;
import javax.servlet.RequestDispatcher;
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

public class AvunaServletRequest implements HttpServletRequest {
	
	private final RequestPacket request;
	private final AvunaServletContext context;
	
	public AvunaServletRequest(RequestPacket request, AvunaServletContext context) {
		this.request = request;
		this.context = context;
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
		return new ServletInputStream() {
			private int i = 0;
			private ByteArrayInputStream bin = new ByteArrayInputStream(request.body.data);
			
			@Override
			public boolean isFinished() {
				return request.body.data.length >= i;
			}
			
			@Override
			public boolean isReady() {
				return !isFinished();
			}
			
			private ReadListener listener = null;
			
			@Override
			public void setReadListener(ReadListener arg0) {
				try {
					arg0.onDataAvailable();
				}catch (IOException e) {
					request.host.logger.logError(e);
				}
				listener = arg0;
			}
			
			@Override
			public int read() throws IOException {
				int b = bin.read();
				if (listener != null && isFinished()) {
					listener.onAllDataRead();
				}
				return b;
			}
			
			public int read(byte[] buf) throws IOException {
				int b = bin.read(buf);
				if (listener != null && isFinished()) {
					listener.onAllDataRead();
				}
				return b;
			}
			
			public int read(byte[] buf, int off, int len) throws IOException {
				int b = bin.read(buf, off, len);
				if (listener != null && isFinished()) {
					listener.onAllDataRead();
				}
				return b;
			}
			
		};
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
		
		return null;
	}
	
	@Override
	public String getAuthType() {
		
		return null;
	}
	
	@Override
	public String getContextPath() {
		
		return null;
	}
	
	@Override
	public Cookie[] getCookies() {
		
		return null;
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
		
		return null;
	}
	
	@Override
	public Enumeration<String> getHeaders(String arg0) {
		
		return null;
	}
	
	@Override
	public int getIntHeader(String arg0) {
		
		return 0;
	}
	
	@Override
	public String getMethod() {
		return request.method.name;
	}
	
	@Override
	public Part getPart(String arg0) throws IOException, ServletException {
		
		return null;
	}
	
	@Override
	public Collection<Part> getParts() throws IOException, ServletException {
		
		return null;
	}
	
	@Override
	public String getPathInfo() {
		
		return null;
	}
	
	@Override
	public String getPathTranslated() {
		
		return null;
	}
	
	@Override
	public String getQueryString() {
		
		return null;
	}
	
	@Override
	public String getRemoteUser() {
		
		return null;
	}
	
	@Override
	public String getRequestURI() {
		
		return null;
	}
	
	@Override
	public StringBuffer getRequestURL() {
		
		return null;
	}
	
	@Override
	public String getRequestedSessionId() {
		
		return null;
	}
	
	@Override
	public String getServletPath() {
		
		return null;
	}
	
	@Override
	public HttpSession getSession() {
		
		return null;
	}
	
	@Override
	public HttpSession getSession(boolean arg0) {
		
		return null;
	}
	
	@Override
	public Principal getUserPrincipal() {
		
		return null;
	}
	
	@Override
	public boolean isRequestedSessionIdFromCookie() {
		
		return false;
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
		
		return false;
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
