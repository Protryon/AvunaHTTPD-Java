package org.avuna.httpd.http.plugins.servlet;

import java.util.Enumeration;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;

public class AvunaServletConfig implements ServletConfig {
	private final ServletClassLoader parent;
	private final String sname;
	private final AvunaServletContext context;
	
	protected AvunaServletConfig(String sname, ServletClassLoader parent, AvunaServletContext context) {
		this.parent = parent;
		this.sname = sname;
		this.context = context;
	}
	
	@Override
	public String getInitParameter(String arg0) {
		return parent.getInitParameter(sname, arg0);
	}
	
	@Override
	public Enumeration<String> getInitParameterNames() {
		return parent.getInitParameters(sname);
	}
	
	@Override
	public ServletContext getServletContext() {
		return context;
	}
	
	@Override
	public String getServletName() {
		return sname;
	}
}
