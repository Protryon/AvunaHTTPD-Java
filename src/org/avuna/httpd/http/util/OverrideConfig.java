/*
 * Avuna HTTPD - General Server Applications Copyright (C) 2015 Maxwell Bruce This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>. */

package org.avuna.httpd.http.util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;
import org.avuna.httpd.hosts.VHost;

public class OverrideConfig {
	private final File file;
	
	public OverrideConfig(File file) {
		this.file = file;
	}
	
	private final ArrayList<CompiledDirective> directives = new ArrayList<CompiledDirective>();
	
	public ArrayList<CompiledDirective> getDirectives() {
		return directives;
	}
	
	public void load(VHost vhost) throws IOException {
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
				vhost.logger.log("[WARNING] null directive(" + drv + ") in " + file.getAbsolutePath());
				continue;
			}
			try {
				directives.add(new CompiledDirective(vhost, drvv, line));
			}catch (IllegalArgumentException e) { // thrown intentionally to prevent the adding of the directive.
			}
		}
		scan.close();
	}
}
