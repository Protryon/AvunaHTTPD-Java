package org.avuna.httpd.http.plugins.javaloader;

import java.io.IOException;
import org.avuna.httpd.http.networking.ChunkedOutputStream;
import org.avuna.httpd.http.networking.RequestPacket;
import org.avuna.httpd.http.networking.ResponsePacket;

public abstract class JavaLoaderStream extends JavaLoader {
	public abstract void generate(ChunkedOutputStream out, RequestPacket request, ResponsePacket response) throws IOException;
	
	public final int getType() {
		return 2;
	}
}
