package com.javaprophet.javawebserver.specialevent;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author lucamasira
 * This is the annotation which is required to register as an event listener.
 */
@Target(ElementType.METHOD)		   //only use this annotation on methods.
@Retention(RetentionPolicy.RUNTIME)//be able to use this annotation at runtime.
public @interface Subscribe {

	/**
	 * This is the priority that the subscriber is listening to the event.
	 * @return the event subscriber priority.
	 */
	public EventPriority priority() default EventPriority.NORMAL;
}
