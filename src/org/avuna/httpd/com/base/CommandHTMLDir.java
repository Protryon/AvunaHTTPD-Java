package org.avuna.httpd.com.base;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Scanner;
import org.avuna.httpd.com.Command;
import org.avuna.httpd.com.CommandContext;

public class CommandHTMLDir extends Command {
	private static void recurseHTML(PrintWriter sw, File f, File ng) throws Exception {
		for (File sf : f.listFiles()) {
			if (sf.getAbsolutePath().equals(ng.getAbsolutePath())) {
				continue;
			}
			if (sf.isDirectory()) {
				recurseHTML(sw, sf, ng);
			}else {
				String ext = sf.getName().substring(sf.getName().lastIndexOf(".") + 1).toLowerCase();
				if (!(ext.equals("html") || ext.equals("json") || ext.equals("xml") || ext.equals("xhtml") || ext.equals("htm") || ext.equals("css") || ext.equals("js") || ext.equals("txt") || ext.equals("php"))) continue;
				Scanner s = new Scanner(sf);
				int l = 0;
				ArrayList<String> lines = new ArrayList<String>(64);
				while (s.hasNextLine()) {
					lines.add(s.nextLine());
					l++;
				}
				sw.println(sf.getName() + ":" + l);
				for (String line : lines) {
					sw.println(line);
				}
				s.close();
			}
		}
	}
	
	@Override
	public String getHelp() {
		return "Processes a directory of HTML/other files into a HTMLCache file for JavaLoaders.";
	}
	
	@Override
	public int processCommand(String[] args, CommandContext context) throws Exception {
		if (args.length != 2) {
			context.println("Usage: <input directory> <output html>");
			return 1;
		}
		File temp = null;
		PrintWriter ps = new PrintWriter(new FileOutputStream(args[1]));
		recurseHTML(ps, temp = new File(args[0]), temp);
		ps.flush();
		ps.close();
		context.println("HTMLDir completed.");
		return 0;
	}
	
}
