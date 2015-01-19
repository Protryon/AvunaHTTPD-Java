package com.javaprophet.javawebserver.specialevent;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.CopyOnWriteArrayList;

import com.javaprophet.javawebserver.specialevent.types.Event;

/**
 * @author lucamasira
 * This class is the base class to handle events.<br>
 * 
 */
public class EventBus {
	
	/**
	 * The hashmap which contains all the listeners.
	 * The key of the map is the class of an event and the value of that key is a list of all listeners to that event.
	 */
	private final HashMap<Class<? extends Event>, List<EventSubscriber>> LISTENERS =
			new HashMap<Class<? extends Event>, List<EventSubscriber>>();
	
	/**
	 * Public.
	 */
	public EventBus() {

	}
	
	/**
	 * Fire an event.
	 * @param event the event to be fired.
	 * @return the actual event that was fired.
	 */
	public <T extends Event> T post(T event) {		
		if(!LISTENERS.containsKey(event.getClass()))
			return event;
		
		for(EventSubscriber subscriber : LISTENERS.get(event.getClass()))
			subscriber.invokeEvent(event);

		return event;
	}
	
	/**
	 * Register a listener.<br>
	 * This will register all eventlisteners found in the listener.
	 * @param listener the listener to register.
	 */
	public void register(final Listener listener) {
		for(Method possibleEvent : listener.getClass().getDeclaredMethods()) {
			//check if the method is correct/can be used as a listener.
			if(possibleEvent.isAnnotationPresent(Subscribe.class) && possibleEvent.getParameterTypes().length == 1) {		
				if(!possibleEvent.isAccessible())
					possibleEvent.setAccessible(true);
				
				Class<? extends Event> eventClass = possibleEvent.getParameterTypes()[0].asSubclass(Event.class);
				EventPriority priority = possibleEvent.getAnnotation(Subscribe.class).priority();
				
				EventSubscriber subscriberInfo = new EventSubscriber(listener, possibleEvent, priority);
				if(!LISTENERS.containsKey(eventClass))
					LISTENERS.put(eventClass, new CopyOnWriteArrayList<EventSubscriber>());
				
				LISTENERS.get(eventClass).add(subscriberInfo);
				sortEvent(eventClass);
			}
		}
	}
	
	/**
	 * Register only for a certain event.
	 * @param listener the listener to register
	 * @param eventClass the event to listen to
	 */
	public void register(final Listener listener, final Class<? extends Event> eventClass) {
		for(Method possibleEvent : listener.getClass().getDeclaredMethods()) {
			if(possibleEvent.isAnnotationPresent(Subscribe.class) && possibleEvent.getParameterTypes().length == 1) {
				if(possibleEvent.getParameterTypes()[0].equals(eventClass)) {
					if(!possibleEvent.isAccessible())
						possibleEvent.setAccessible(true);
					
					EventSubscriber eventSubscriber = new EventSubscriber(listener, possibleEvent,
							possibleEvent.getAnnotation(Subscribe.class).priority());
					
					if(!LISTENERS.containsKey(eventClass))
						LISTENERS.put(eventClass, new CopyOnWriteArrayList<EventSubscriber>());
					
					LISTENERS.get(eventClass).add(eventSubscriber);
					sortEvent(eventClass);
				}
			}
		}
	}
	
	/**
	 * Unregister all subscribers from a certain listener.
	 * @param listener the listener to unregister
	 */
	public void unregister(final Listener listener) {
		for(List<EventSubscriber> eventListeners : LISTENERS.values()) {//get all the listeners
			for(EventSubscriber subscriber : eventListeners) {			//loop through all the listeners to a certain event
				if(subscriber.getListener().equals(listener)) {			//do a check to see if the sources are the same
					eventListeners.remove(subscriber);					//remove the listener
				}
			}
		}
		cleanEmptyMaps();//remove all entries in the listeners map which are empty.
	}
	
	/**
	 * Unregister from a certain event.
	 * @param listener the listener to unregister the eventsubscriber.
	 * @param eventClass the event class to unregister from the listener.
	 */
	public void unregister(final Listener listener, Class<? extends Event> eventClass) {
		if(LISTENERS.containsKey(eventClass)) {
			for(EventSubscriber subscriber : LISTENERS.get(eventClass)) {
				if(subscriber.getListener().equals(listener)) {
					LISTENERS.get(eventClass).remove(subscriber);//we can do this since it's a concurrent arraylist.
																 //no return yet since there may be more EventSubscribers that listen to this event in the same class.
																 //It's no recommended to to such things though.
				}
			}
			
			if(LISTENERS.get(eventClass).isEmpty()) {
				LISTENERS.put(eventClass, null);//set the arraylist to null
				LISTENERS.remove(eventClass);	//no cleanEmptyMaps call required since we're dealing with a single event.
			}
		}
	}
	
	/**
	 * Removes all the listeners associated with a certain event.
	 * @param eventClass the event which gets all its listeners removed.
	 */
	public void unregister(final Class<? extends Event> eventClass) {
		if(!LISTENERS.containsKey(eventClass))
			return;
		
		LISTENERS.remove(eventClass);
	}
	
	/**
	 * Removes all the entries in the listeners hashmap which are empty.
	 */
	private void cleanEmptyMaps() {
		Iterator<Entry<Class<? extends Event>, List<EventSubscriber>>> iterator = LISTENERS.entrySet().iterator();
		while(iterator.hasNext()) {					  //loop through all entries
			if(iterator.next().getValue().isEmpty()) {//check if empty
				iterator.remove();					  //remove the entry
			}
		}
		
	}
	
	/**
	 * This method will sort the listeners of a certain event on priority.
	 * @param eventClass the event to sort
	 */
	private void sortEvent(final Class<? extends Event> eventClass) {
		//the sorted list
		List<EventSubscriber> sorted = new CopyOnWriteArrayList<EventSubscriber>();
		
		for(EventPriority priority : EventPriority.values()) {//could probably be made more efficient.
			for(EventSubscriber subscriber : LISTENERS.get(eventClass)) {
				if(subscriber.getPriority() == priority) {
					sorted.add(subscriber);
				}
			}
		}
		
		//set the subscriber list to the new sorted list.
		LISTENERS.put(eventClass, sorted);
	}	

}
