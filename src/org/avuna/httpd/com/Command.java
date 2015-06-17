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

public abstract class Command {
	private boolean registered = false;
	private CommandRegistry registry = null;
	protected int registeredID = -1;
	
	public CommandRegistry getRegistry() {
		return registry;
	}
	
	protected void setRegistry(CommandRegistry registry, int id) {
		if (this.registered) throw new IllegalArgumentException("Command already registered!");
		this.registry = registry;
		this.registered = true;
		this.registeredID = id;
	}
	
	public Command() {
		
	}
	
	/**
	 * -2 = critical error (exception)
	 * -1 = invalid command
	 * 0 = OK
	 * 1 = invalid arguments
	 * 2 = no host selected
	 * 3 = no vhost selected
	 * 4 = bad host-type selected
	 * 5 = invalid permission
	 * 6 = undefined...
	 * 
	 * @throws Exception
	 */
	public abstract int processCommand(String[] args, CommandContext context) throws Exception;
	
	public abstract String getHelp();
}
