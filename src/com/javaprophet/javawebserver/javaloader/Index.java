package com.javaprophet.javawebserver.javaloader;

import java.io.PrintStream;
import com.javaprophet.javawebserver.networking.packets.RequestPacket;
import com.javaprophet.javawebserver.networking.packets.ResponsePacket;
import com.javaprophet.javawebserver.plugins.javaloader.JavaLoaderStream;

public class Index extends JavaLoaderStream {
	
	@Override
	public void generate(PrintStream out, ResponsePacket response, RequestPacket request) {
		if (request.get.containsKey("data")) {
			out.println(request.get.get("data"));
		}else {
			out.println("<form action=\"\" method=\"GET\">");
			out.println("<input type=\"text\" name=\"data\" />");
			out.println("<input type=\"submit\" value=\"Submit\" />");
			out.println("</form>");
		}
	}
	
}
