package com.javaprophet.javawebserver.specialevent;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import com.javaprophet.javawebserver.specialevent.types.Event;

/**
 * @author lucamasira
 * This class will be used by the EventBus to save information about a single event subscriber such as the method and the source.
 */
public class EventSubscriber {
	
	/**
	 * The object where the listener is in
	 */
	private final Listener listener;
	
	/**
	 * The method which has the Subscribe annotation.
	 */
	private final Method method;
	
	/**
	 * The priority of the listener.
	 */
	private final EventPriority priority;
	
	/**
	 * The constructor for this class.
	 * @param source the object which listens for this event.
	 * @param method the method which listens to the event.
	 * @param priority the priority of the listener.
	 */
	public EventSubscriber(final Listener source, final Method method, final EventPriority priority) {
		this.listener = source;
		this.method = method;
		this.priority = priority;
	}
	
	/**
	 * Invoke the event on the method.<br>
	 * Synchronized method since we don't want this method invoked at the same time since it might mess up stuff.
	 * @param event the event to invoke.
	 */
	public synchronized final void invokeEvent(final Event event)  {
		try {
			//the method is already made accessable in the EventBus.
			getMethod().invoke(getListener(), event);
			//print stacktrace here so it can still invoke the other events.
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Get the method which listens to the event.
	 * @return the method which listens to an event.
	 */
	public final Method getMethod() {
		return method;
	}
	
	/**
	 * Get the object where the eventlistener is in.
	 * @return the object where the method which listens for the event is in.
	 */
	public final Listener getListener() {
		return listener;
	}
	
	/**
	 * Get the priority of the listener.
	 * @return the priority of the listener.
	 */
	public final EventPriority getPriority() {
		return priority;	
	}

}
