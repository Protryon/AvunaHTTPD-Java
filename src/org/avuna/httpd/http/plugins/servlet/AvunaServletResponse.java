package org.avuna.httpd.http.plugins.servlet;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Collection;
import java.util.Locale;
import javax.servlet.Servlet;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import org.avuna.httpd.AvunaHTTPD;
import org.avuna.httpd.http.Resource;
import org.avuna.httpd.http.ResponseGenerator;
import org.avuna.httpd.http.StatusCode;
import org.avuna.httpd.http.networking.ResponsePacket;
import org.avuna.httpd.http.plugins.avunaagent.HTMLBuilder;
import org.avuna.httpd.http.plugins.avunaagent.lib.SetCookie;

public class AvunaServletResponse implements HttpServletResponse {
	private final ResponsePacket response;
	private final AvunaServletContext context;
	private final Servlet servlet;
	
	public AvunaServletResponse(ResponsePacket response, AvunaServletContext context, Servlet servlet) {
		this.response = response;
		this.context = context;
		this.servlet = servlet;
	}
	
	private ByteArrayOutputStream content = new ByteArrayOutputStream();
	private ByteArrayOutputStream buf = new ByteArrayOutputStream();
	
	@Override
	public void flushBuffer() throws IOException {
		content.write(buf.toByteArray());
		buf.reset();
	}
	
	@Override
	public int getBufferSize() {
		return bs;
	}
	
	@Override
	public String getCharacterEncoding() {
		String ct = response.headers.getHeader("Content-Type");
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
	public String getContentType() {
		return response.headers.getHeader("Content-Type");
	}
	
	@Override
	public Locale getLocale() {
		return Locale.ENGLISH; // TODO: ?
	}
	
	@Override
	public ServletOutputStream getOutputStream() throws IOException {
		return new ServletOutputStreamWrapper(response);
	}
	
	protected String getOutput() {
		return writer.toString();
	}
	
	private final StringWriter writer = new StringWriter();
	private final HTMLBuilder builder = new HTMLBuilder(writer);
	
	@Override
	public PrintWriter getWriter() throws IOException {
		return builder;
	}
	
	@Override
	public boolean isCommitted() {
		return false;
	}
	
	@Override
	public void reset() {
		// TODO: reset headers ?!?
		content.reset();
		buf.reset();
	}
	
	@Override
	public void resetBuffer() {
		buf.reset();
	}
	
	private int bs = 0;
	
	@Override
	public void setBufferSize(int arg0) {
		bs = arg0;
	}
	
	@Override
	public void setCharacterEncoding(String arg0) {
		String ct = response.headers.getHeader("Content-Type");
		if (ct.contains("charset=")) {
			ct = ct.substring(0, ct.indexOf("charset=") + 8) + arg0; // TODO: other parts
		}
	}
	
	@Override
	public void setContentLength(int arg0) {
		response.headers.updateHeader("Content-Length", arg0 + "");
	}
	
	@Override
	public void setContentLengthLong(long arg0) {
		response.headers.updateHeader("Content-Length", arg0 + "");
	}
	
	@Override
	public void setContentType(String arg0) {
		response.headers.updateHeader("Content-Type", arg0);
	}
	
	@Override
	public void setLocale(Locale arg0) {
		// TODO ?
	}
	
	@Override
	public void addCookie(Cookie arg0) {
		new SetCookie(response).setCookie(arg0.getName(), arg0.getValue(), arg0.getMaxAge(), arg0.getPath(), arg0.getDomain());
	}
	
	@Override
	public void addDateHeader(String arg0, long arg1) {
		// we are dateless
	}
	
	@Override
	public void addHeader(String arg0, String arg1) {
		response.headers.addHeader(arg0, arg1);
	}
	
	@Override
	public void addIntHeader(String arg0, int arg1) {
		response.headers.addHeader(arg0, arg1 + "");
	}
	
	@Override
	public boolean containsHeader(String arg0) {
		return response.headers.hasHeader(arg0);
	}
	
	@Override
	public String encodeRedirectURL(String arg0) {
		try {
			return URLEncoder.encode(arg0, "UTF-8");
		}catch (UnsupportedEncodingException e) {
			response.request.host.logger.logError(e);
			return null;
		}
	}
	
	@Override
	public String encodeRedirectUrl(String arg0) {
		return encodeRedirectURL(arg0);
	}
	
	@Override
	public String encodeURL(String arg0) {
		return encodeRedirectURL(arg0);
	}
	
	@Override
	public String encodeUrl(String arg0) {
		return encodeRedirectURL(arg0);
	}
	
	@Override
	public String getHeader(String arg0) {
		return response.headers.getHeader(arg0);
	}
	
	@Override
	public Collection<String> getHeaderNames() {
		return response.headers.getHeaders().keySet();
	}
	
	@Override
	public Collection<String> getHeaders(String arg0) {
		return Arrays.asList(response.headers.getHeaders(arg0));
	}
	
	@Override
	public int getStatus() {
		return response.statusCode;
	}
	
	@Override
	public void sendError(int arg0) throws IOException {
		ResponseGenerator.generateDefaultResponse(response, StatusCode.INTERNAL_SERVER_ERROR);
		Resource rsc = AvunaHTTPD.fileManager.getErrorPage(response.request, response.request.target, StatusCode.INTERNAL_SERVER_ERROR, "Avuna had a critical error attempting to serve your page. Please contact your server administrator and try again. This error has been recorded in the Avuna log file. Reported Error Number: " + arg0);
		response.headers.updateHeader("Content-Type", rsc.type);
		response.body = rsc;
	}
	
	@Override
	public void sendError(int arg0, String arg1) throws IOException {
		ResponseGenerator.generateDefaultResponse(response, StatusCode.INTERNAL_SERVER_ERROR);
		Resource rsc = AvunaHTTPD.fileManager.getErrorPage(response.request, response.request.target, StatusCode.INTERNAL_SERVER_ERROR, arg1);
		response.headers.updateHeader("Content-Type", rsc.type);
		response.body = rsc;
	}
	
	@Override
	public void sendRedirect(String arg0) throws IOException {
		ResponseGenerator.generateDefaultResponse(response, StatusCode.FOUND);
		response.body.data = new byte[0];
		response.headers.updateHeader("Location", arg0);
	}
	
	@Override
	public void setDateHeader(String arg0, long arg1) {
		// we are dateless
	}
	
	@Override
	public void setHeader(String arg0, String arg1) {
		response.headers.updateHeader(arg0, arg1);
	}
	
	@Override
	public void setIntHeader(String arg0, int arg1) {
		response.headers.updateHeader(arg0, arg1 + "");
	}
	
	@Override
	public void setStatus(int arg0) {
		response.statusCode = arg0;
	}
	
	@Override
	public void setStatus(int arg0, String arg1) {
		response.statusCode = arg0;
		response.reasonPhrase = arg1;
	}
	
}
