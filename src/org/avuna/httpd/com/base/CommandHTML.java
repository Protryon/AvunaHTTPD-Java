/*	Avuna HTTPD - General Server Applications
    Copyright (C) 2015 Maxwell Bruce

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.*/

package org.avuna.httpd.com.base;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.Scanner;
import org.avuna.httpd.AvunaHTTPD;
import org.avuna.httpd.com.Command;
import org.avuna.httpd.com.CommandContext;
import org.avuna.httpd.hosts.Host;
import org.avuna.httpd.hosts.HostHTTP;
import org.avuna.httpd.hosts.VHost;

public class CommandHTML extends Command {
	
	@Override
	public String getHelp() {
		return "Converts a HTML file into a compilable java file.";
	}
	
	@Override
	public int processCommand(String[] args, CommandContext context) throws Exception {
		if (args.length != 2 && args.length != 1) {
			context.println("Usage: <html input> <output[optional]>");
			return 1;
		}
		Host ghost = (Host)AvunaHTTPD.hosts.get(context.getSelectedHost());
		if (ghost == null) {
			context.println("Invalid Selected Host! (select)");
			return 2;
		}
		if (!(ghost instanceof HostHTTP)) {
			context.println("Not a http host! (select)");
			return 4;
		}
		HostHTTP phost = (HostHTTP)ghost;
		VHost host = phost.getVHostByName(context.getSelectedVHost());
		if (host == null) {
			context.println("Invalid Selected VHost! (select)");
			return 3;
		}
		File sc2 = null;
		Scanner scan2 = new Scanner(new FileInputStream(sc2 = new File(host.getHTDocs(), args[0])));
		PrintStream ps;
		File temp = null;
		if (args.length == 2) {
			temp = new File(host.getHTSrc(), args[1]);
		}else {
			temp = new File(host.getHTSrc(), args[0].substring(0, args[0].indexOf(".")) + ".java");
		}
		temp.getParentFile().mkdirs();
		temp.createNewFile();
		ps = new PrintStream(new FileOutputStream(temp));
		ps.println("import org.avuna.httpd.http.plugins.javaloader.HTMLBuilder;");
		ps.println("import org.avuna.httpd.http.networking.packets.RequestPacket;");
		ps.println("import org.avuna.httpd.http.networking.packets.ResponsePacket;");
		ps.println("import org.avuna.httpd.http.plugins.javaloader.JavaLoaderPrint;");
		ps.println();
		ps.println("public class " + (args.length == 2 ? temp.getName().substring(0, temp.getName().indexOf(".")) : sc2.getName().substring(0, sc2.getName().indexOf("."))) + " extends JavaLoaderPrint {");
		ps.println("    public boolean generate(HTMLBuilder out, ResponsePacket response, RequestPacket request) {");
		while (scan2.hasNextLine()) {
			String line = scan2.nextLine().trim();
			ps.println("        " + "out.println(\"" + line.replace("\\", "\\\\").replace("\"", "\\\"") + "\");");
		}
		ps.println("    return true;");
		ps.println("    }");
		ps.println("}");
		ps.flush();
		ps.close();
		scan2.close();
		context.println("HTML completed.");
		return 0;
	}
	
}
