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

package org.avuna.httpd.com;

import java.io.PrintStream;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;
import org.avuna.httpd.util.Logger;

public class CommandRegistry {
	public CommandRegistry() {
		
	}
	
	private final ConcurrentHashMap<String[], Command> cmds = new ConcurrentHashMap<String[], Command>();
	private int nextID = 0;
	
	// TODO: not following my security of this class.
	public ConcurrentHashMap<String[], Command> getCommands() {
		return cmds;
	}
	
	public void registerCommand(Command command, String... cmd) {
		command.setRegistry(this, nextID++);
		cmds.put(cmd, command);
	}
	
	public int processCommand(String cmd, String[] args, CommandContext context) {
		Command com = null;
		m:
		for (String[] ks : cmds.keySet()) {
			for (String k : ks) {
				if (k.equalsIgnoreCase(cmd)) {
					com = cmds.get(ks);
					break m;
				}
			}
		}
		if (com == null) {
			context.println("Unknown Command! Try using the help command.");
			context.logCommand(-1, -1);
			return -1;
		}
		int i = -2;
		try {
			i = com.processCommand(args, context);
		}catch (Exception e) {
			e.printStackTrace(context.getOut());
			Logger.logError(e);
		}
		context.logCommand(com.registeredID, i);
		return i;
	}
	
	public CommandContext newContext(PrintStream out, Scanner in) {
		return new CommandContext(this, out, in);
	}
	
	public int processCommand(String cmd, CommandContext context) {
		String[] cargs = cmd.contains(" ") ? cmd.substring(cmd.indexOf(" ") + 1).split(" ") : new String[0];
		if (cargs.length > 0) {
			String[] tcargs = new String[cargs.length];
			int nl = 0;
			boolean iq = false;
			String tmp = "";
			for (int i = 0; i < cargs.length; i++) {
				boolean niq = false;
				String ct = cargs[i].trim();
				if (!iq && ct.startsWith("\"")) {
					iq = true;
					niq = true;
				}
				if (iq) {
					tmp += (niq ? ct.substring(1) : ct) + " ";
				}else {
					tcargs[nl++] = ct;
				}
				if ((!niq || ct.length() > 3) && iq && ct.endsWith("\"")) {
					iq = false;
					String n = tmp.trim();
					if (n.endsWith("\"")) n = n.substring(0, n.length() - 1);
					tcargs[nl++] = n;
					tmp = "";
				}
			}
			cargs = new String[nl];
			System.arraycopy(tcargs, 0, cargs, 0, nl);
		}
		String command = cmd.contains(" ") ? cmd.substring(0, cmd.indexOf(" ")) : cmd;
		return processCommand(command, cargs, context);
	}
}
