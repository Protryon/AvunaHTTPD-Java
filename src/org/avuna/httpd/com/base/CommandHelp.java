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

import java.util.concurrent.ConcurrentHashMap;
import org.avuna.httpd.AvunaHTTPD;
import org.avuna.httpd.com.Command;
import org.avuna.httpd.com.CommandContext;

public class CommandHelp extends Command {
	
	@Override
	public String getHelp() {
		return null;
	}
	
	private static String pad(String text, int length) {
		String f = text;
		while (f.length() < length) {
			f += " ";
		}
		if (f.length() > length) {
			f = f.substring(0, length);
		}
		return f;
	}
	
	@Override
	public int processCommand(String[] args, CommandContext context) throws Exception {
		context.println("Avuna HTTPD Version " + AvunaHTTPD.VERSION);
		context.println("");
		ConcurrentHashMap<String[], Command> cmds = context.getRegistry().getCommands();
		context.println(pad("Command", 12) + "Function");
		for (String[] scmds : cmds.keySet()) {
			Command com = cmds.get(scmds);
			String hlp = com.getHelp();
			if (hlp == null) continue;
			String cmd = "";
			for (String tcmd : scmds) {
				cmd += (cmd.length() > 0 ? "/" : "") + tcmd;
			}
			context.println(pad(cmd, 12) + hlp);
		}
		return 0;
	}
	
}
