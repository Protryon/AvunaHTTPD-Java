package org.avuna.httpd.http.plugins.servlet;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import javax.servlet.http.Part;
import org.avuna.httpd.http.plugins.avunaagent.lib.Multipart.MultiPartData;

public class PartWrapper implements Part {
	private final MultiPartData mpd;
	
	protected PartWrapper(MultiPartData mpd) {
		this.mpd = mpd;
	}
	
	@Override
	public void delete() throws IOException {
	
	}
	
	@Override
	public String getContentType() {
		return mpd.contentType;
	}
	
	@Override
	public String getHeader(String arg0) {
		for (String s : mpd.extraHeaders) {
			if (s.toLowerCase().startsWith(arg0.toLowerCase() + ": ")) {
				return s.substring(s.indexOf(": ") + 2);
			}
		}
		return null;
	}
	
	@Override
	public Collection<String> getHeaderNames() {
		ArrayList<String> names = new ArrayList<String>();
		for (String s : mpd.extraHeaders) {
			names.add(s.substring(0, s.indexOf(": ")));
		}
		return names;
	}
	
	@Override
	public Collection<String> getHeaders(String arg0) {
		return Arrays.asList(new String[] { getHeader(arg0) });
	}
	
	@Override
	public InputStream getInputStream() throws IOException {
		return new ByteArrayInputStream(mpd.data);
	}
	
	@Override
	public String getName() {
		return mpd.vars.get("name");
	}
	
	@Override
	public long getSize() {
		return mpd.data.length;
	}
	
	@Override
	public String getSubmittedFileName() {
		return mpd.vars.get("filename");
	}
	
	@Override
	public void write(String arg0) throws IOException {
		throw new IOException("Write not implemented!");
	}
	
}
