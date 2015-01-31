package com.javaprophet.javawebserver.javaloader;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import com.javaprophet.javawebserver.networking.packets.RequestPacket;
import com.javaprophet.javawebserver.networking.packets.ResponsePacket;
import com.javaprophet.javawebserver.plugins.javaloader.JavaLoader;

public class Index extends JavaLoader {
	
	@Override
	public byte[] generate(ResponsePacket response, RequestPacket request) {
		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		PrintStream write = new PrintStream(bout);
		if (request.get.containsKey("data")) {
			write.println(request.get.get("data"));
		}else {
			write.println("<form action=\"\" method=\"GET\">");
			write.println("<input type=\"text\" name=\"data\" />");
			write.println("<input type=\"submit\" value=\"Submit\" />");
			write.println("</form>");
		}
		write.close();
		return bout.toByteArray();
	}
	
}
