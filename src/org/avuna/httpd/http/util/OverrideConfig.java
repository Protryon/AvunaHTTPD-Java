package org.avuna.httpd.http.util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;
import org.avuna.httpd.util.Logger;

public class OverrideConfig {
	private final File file;
	
	public OverrideConfig(File file) {
		this.file = file;
	}
	
	private final ArrayList<CompiledDirective> directives = new ArrayList<CompiledDirective>();
	
	public ArrayList<CompiledDirective> getDirectives() {
		return directives;
	}
	
	public void load() throws IOException {
		Scanner scan = new Scanner(file);
		while (scan.hasNextLine()) {
			String line = scan.nextLine().trim();
			if (line.contains("#")) line = line.substring(0, line.indexOf("#")).trim();
			if (line.length() == 0) continue;
			boolean ha = line.contains(" ");
			String drv = ha ? line.substring(0, line.indexOf(" ")) : line;
			line = ha ? line.substring(drv.length() + 1) : "";
			Directive drvv = Directive.getDirective(drv);
			if (drvv == null) {
				Logger.log("[WARNING] null directive(" + drv + ") in " + file.getAbsolutePath());
				continue;
			}
			try {
				directives.add(new CompiledDirective(drvv, line));
			}catch (IllegalArgumentException e) { // thrown intentionally to prevent the adding of the directive.
			}
		}
		scan.close();
	}
}
