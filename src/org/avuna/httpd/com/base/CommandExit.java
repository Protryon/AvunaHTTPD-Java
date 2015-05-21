package org.avuna.httpd.com.base;

import org.avuna.httpd.com.Command;
import org.avuna.httpd.com.CommandContext;

public class CommandExit extends Command {
	
	@Override
	public int processCommand(String[] args, CommandContext context) throws Exception {
		System.exit(0);
		return 0;
	}
	
	@Override
	public String getHelp() {
		return "Exits Avuna.";
	}
	
}
