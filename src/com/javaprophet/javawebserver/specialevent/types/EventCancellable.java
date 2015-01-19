package com.javaprophet.javawebserver.specialevent.types;



/**
 * @author lucamasira
 * Base class for cancellable events.
 */
public class EventCancellable extends Event {
	
	/**
	 * The state which indicates if the event is cancelled.
	 */
	private boolean cancelled;
	
	/**
	 * Just a normal constructor
	 */
	public EventCancellable() {
	}
	
	/**
	 * The state which indicates if the event is cancelled.
	 * @return the cancelled state
	 */
	public final boolean isCancelled() {
		return cancelled;
	}
	
	/**
	 * Set the cancelled state.
	 * @param cancelled the state which indicates if the event is cancelled
	 */
	public final void setCancelled(boolean cancelled) {
		this.cancelled = cancelled;
	}

}
