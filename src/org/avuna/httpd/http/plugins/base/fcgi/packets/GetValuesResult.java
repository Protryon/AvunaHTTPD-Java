package org.avuna.httpd.http.plugins.base.fcgi.packets;

import java.io.DataInputStream;
import java.io.IOException;
import org.avuna.httpd.http.plugins.base.fcgi.Type;

public class GetValuesResult extends Stream {
	
	public GetValuesResult(DataInputStream in, int l) throws IOException {
		super(Type.FCGI_GET_VALUES_RESULT);
		readContent(in, l);
	}
	
	public GetValuesResult(int id) {
		super(Type.FCGI_GET_VALUES_RESULT, id);
	}
	
}
