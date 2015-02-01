package com.javaprophet.javawebserver.javaloader;

import java.io.PrintStream;
import com.javaprophet.javawebserver.networking.packets.RequestPacket;
import com.javaprophet.javawebserver.networking.packets.ResponsePacket;
import com.javaprophet.javawebserver.plugins.javaloader.JavaLoaderStream;

public class Index extends JavaLoaderStream {
	
	public void generate(PrintStream out, ResponsePacket response, RequestPacket request) {
		if (request.get.containsKey("data")) {
			out.println(request.get.get("data"));
		}else {
			out.println("<html>");
			out.println("<head>");
			out.println("<title>SMD</title>");
			out.println("</title>");
			out.println("<body>");
			out.println("<center> LAWL </center>");
			out.println("<form action=\"\" method=\"GET\">");
			out.println("<input type=\"text\" name=\"data\" />");
			out.println("<input type=\"submit\" />");
			out.println("</form>");
			out.println("</body>");
			out.println("</html>");
		}
	}
	
}
