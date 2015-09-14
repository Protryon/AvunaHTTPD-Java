package org.avuna.httpd.http.plugins.servlet;

import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionContext;
import org.avuna.httpd.http.plugins.avunaagent.lib.Session;

public class HttpSessionWrapper implements HttpSession {
	private final Session session;
	private final ServletContext context;
	
	protected HttpSessionWrapper(Session session, ServletContext context) {
		this.session = session;
		this.context = context;
	}
	
	@Override
	public Object getAttribute(String arg0) {
		return session.get(arg0);
	}
	
	@Override
	public Enumeration<String> getAttributeNames() {
		return Collections.enumeration(Arrays.asList(session.getNames()));
	}
	
	@Override
	public long getCreationTime() {
		return session.getCreationTime();
	}
	
	@Override
	public String getId() {
		return session.getSessionID();
	}
	
	@Override
	public long getLastAccessedTime() {
		return System.currentTimeMillis();
	}
	
	@Override
	public int getMaxInactiveInterval() {
		return 0;
	}
	
	@Override
	public ServletContext getServletContext() {
		return context;
	}
	
	@Override
	public HttpSessionContext getSessionContext() {
		return null;
	}
	
	@Override
	public Object getValue(String arg0) {
		return session.get(arg0);
	}
	
	@Override
	public String[] getValueNames() {
		return session.getNames();
	}
	
	@Override
	public void invalidate() {
		session.invalidate();
	}
	
	@Override
	public boolean isNew() {
		return session.isNew();
	}
	
	@Override
	public void putValue(String arg0, Object arg1) {
		session.set(arg0, arg1 + "");
	}
	
	@Override
	public void removeAttribute(String arg0) {
		session.remove(arg0);
	}
	
	@Override
	public void removeValue(String arg0) {
		removeAttribute(arg0);
	}
	
	@Override
	public void setAttribute(String arg0, Object arg1) {
		putValue(arg0, arg1);
	}
	
	@Override
	public void setMaxInactiveInterval(int arg0) {
		// nope
	}
	
}
