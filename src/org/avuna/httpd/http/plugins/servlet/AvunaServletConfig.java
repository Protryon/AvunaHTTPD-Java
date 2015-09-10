package org.avuna.httpd.http.plugins.servlet;

import java.util.Enumeration;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;

public class AvunaServletConfig implements ServletConfig {
	
	@Override
	public String getInitParameter(String arg0) {
		return null;
	}
	
	@Override
	public Enumeration<String> getInitParameterNames() {
		return null;
	}
	
	@Override
	public ServletContext getServletContext() {
		return null;
	}
	
	@Override
	public String getServletName() {
		return null;
	}
}
