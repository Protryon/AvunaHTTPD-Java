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
