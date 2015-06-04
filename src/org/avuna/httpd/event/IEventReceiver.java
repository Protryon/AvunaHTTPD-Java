package org.avuna.httpd.event;

public interface IEventReceiver {
	public void receive(EventBus bus, Event event);
}
