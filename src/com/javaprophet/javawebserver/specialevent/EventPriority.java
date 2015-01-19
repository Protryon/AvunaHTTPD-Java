package com.javaprophet.javawebserver.specialevent;

/**
 * @author lucamasira
 * A list of byte values which represents all the priorities an eventlistener can have.<br>
 * The LOWEST value will be invoked first and the MONITOR last.<br>
 * MONITOR should only be used to check the final state of the event.<br>
 * HIGHEST should be used to do the last things to an event.<br>
 * <br>
 * This class doesn't use enums because they take up alot more memory.
 */
public enum EventPriority {
	
	/**
	 * This is the lowest event priority and will be called first.
	 */
	LOWEST,
	
	/**
	 * This priority will be called after the LOWEST priority.
	 */
	LOW,
	
	/**
	 * This is the normal priority, if there isn't a priority set in the annotation this priority will be used.
	 */
	NORMAL,
	
	/**
	 * This priority will be called after the normal priority.
	 */
	HIGH,
	
	/**
	 * This is the last priority where changes to the event should happen.
	 */
	HIGHEST,
	
	/**
	 * This priority is mainly to monitor the event outcome.<br>
	 * Changes should not be made to the event at this priority.
	 */
	MONITOR;
	
	/**
	 * Private constructor
	 */
	private EventPriority() {
	}

}
