/*
 * Avuna HTTPD - General Server Applications Copyright (C) 2015 Maxwell Bruce This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>. */

package org.avuna.httpd.com;

import java.io.PrintStream;
import java.util.Scanner;

public class CommandContext {
	private String selectedHost = "main", selectedVHost = "main";
	private PrintStream out = null;
	private Scanner in = null;
	private int[] status = new int[0];
	private int[] cmdRan = new int[0];
	private final CommandRegistry registry;
	
	public final CommandRegistry getRegistry() {
		return registry;
	}
	
	// TODO: finish command logs
	protected synchronized final void logCommand(int cmd, int res) {
		int[] ns = new int[cmdRan.length + 1];
		System.arraycopy(cmdRan, 0, ns, 0, cmdRan.length);
		ns[cmdRan.length] = cmd;
		cmdRan = ns;
		ns = new int[status.length + 1];
		System.arraycopy(status, 0, ns, 0, status.length);
		ns[status.length] = res;
		status = ns;
	}
	
	public void println(String line) {
		if (out != null) out.println(line);
	}
	
	protected CommandContext(CommandRegistry registry, PrintStream out, Scanner in) {
		this.out = out;
		this.in = in;
		this.registry = registry;
	}
	
	public String getSelectedHost() {
		return selectedHost;
	}
	
	public void setSelectedHost(String selectedHost) {
		this.selectedHost = selectedHost;
	}
	
	public String getSelectedVHost() {
		return selectedVHost;
	}
	
	public void setSelectedVHost(String selectedVHost) {
		this.selectedVHost = selectedVHost;
	}
	
	public PrintStream getOut() {
		return out;
	}
	
	public Scanner getIn() {
		return in;
	}
	
	public void setOut(PrintStream out) {
		this.out = out;
	}
	
	public void setIn(Scanner in) {
		this.in = in;
	}
}
