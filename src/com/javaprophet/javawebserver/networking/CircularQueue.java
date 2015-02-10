package com.javaprophet.javawebserver.networking;

import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Queue;

public class CircularQueue<T> implements Queue<T> {
	private Object[] array;
	private int pointer = 0;
	private int mPointer = 0;
	
	public CircularQueue(int size) {
		this.array = new Object[size];
	}
	
	public CircularQueue() {
		this.array = new Object[65535];
	}
	
	@Override
	public int size() {
		return Math.abs(pointer - mPointer);
	}
	
	@Override
	public boolean isEmpty() {
		return pointer == mPointer && array[pointer] == null;
	}
	
	public boolean isFull() {
		return mPointer == pointer && array[pointer] != null;
	}
	
	@Override
	public synchronized boolean contains(Object o) {
		if (isEmpty()) return false;
		for (int i = mPointer; i != pointer; i++) {
			if (i == array.length) i = 0;
			if (i == pointer) i = mPointer;
			if (array[i].equals(o)) {
				return true;
			}
		}
		return false;
	}
	
	@Override
	public synchronized Iterator<T> iterator() {
		return new Iterator<T>() {
			private int pointer = mPointer;
			
			@Override
			public boolean hasNext() {
				return pointer < CircularQueue.this.pointer;
			}
			
			@Override
			public T next() {
				if (pointer == array.length) pointer = 0;
				if (pointer == CircularQueue.this.pointer) pointer = mPointer;
				return (T)CircularQueue.this.array[pointer++];
			}
			
			@Override
			public void remove() {
				if (pointer == mPointer) return;
				CircularQueue.this.array[pointer - 1] = null;
			}
			
		};
	}
	
	@Override
	public synchronized Object[] toArray() {
		Object[] n = new Object[Math.abs(pointer - mPointer)];
		int i2 = 0;
		for (int i = mPointer; i != pointer; i++) {
			if (i == array.length) i = 0;
			if (i == pointer) i = mPointer;
			n[i2++] = array[i];
		}
		return n;
	}
	
	@Override
	public synchronized Object[] toArray(Object[] a) {
		Object[] n = new Object[Math.abs(pointer - mPointer)];
		int i2 = 0;
		for (int i = mPointer; i != pointer; i++) {
			if (i == array.length) i = 0;
			if (i == pointer) i = mPointer;
			n[i2++] = array[i];
		}
		return n;
	}
	
	@Override
	public synchronized boolean remove(Object o) {
		boolean cc = false;
		for (int i = mPointer; i != pointer; i++) {
			if (i == array.length) i = 0;
			if (i == pointer) i = mPointer;
			if (o.equals(array[i])) {
				array[i] = null;
				for (int i2 = i + 1; i2 < pointer; i2++) {
					array[i2 - 1] = array[i2];
				}
				pointer--;
				if (pointer == array.length) pointer = 0;
				array[pointer] = null;
				cc = true;
			}
		}
		return cc;
	}
	
	@Override
	public synchronized boolean containsAll(Collection c) {
		top:
		for (Object cc : c) {
			for (int i = mPointer; i != pointer; i++) {
				if (i == array.length) i = 0;
				if (i == pointer) i = mPointer;
				if (array[i].equals(cc)) {
					continue top;
				}
			}
			return false;
		}
		return true;
	}
	
	@Override
	public synchronized boolean addAll(Collection c) {
		for (Object cc : c) {
			add((T)cc);
		}
		return c.size() > 0;
	}
	
	@Override
	public synchronized boolean removeAll(Collection c) {
		boolean cc = false;
		for (int i = mPointer; i != pointer; i++) {
			if (i == array.length) i = 0;
			if (i == pointer) i = mPointer;
			if (c.contains(array[i])) {
				array[i] = null;
				for (int i2 = i + 1; i2 < pointer; i2++) {
					array[i2 - 1] = array[i2];
				}
				pointer--;
				if (pointer == array.length) pointer = 0;
				array[pointer] = null;
				cc = true;
			}
		}
		return cc;
	}
	
	@Override
	public synchronized boolean retainAll(Collection c) {
		boolean cc = false;
		for (int i = mPointer; i != pointer; i++) {
			if (i == array.length) i = 0;
			if (i == pointer) i = mPointer;
			if (!c.contains(array[i])) {
				array[i] = null;
				for (int i2 = i + 1; i2 < pointer; i2++) {
					array[i2 - 1] = array[i2];
				}
				pointer--;
				if (pointer == array.length) pointer = 0;
				array[pointer] = null;
				cc = true;
			}
		}
		return cc;
	}
	
	@Override
	public synchronized void clear() {
		array = new Object[array.length];
		pointer = 0;
		mPointer = 0;
	}
	
	@Override
	public synchronized boolean add(T e) {
		if (isFull()) throw new IllegalStateException();
		array[pointer] = e;
		pointer++;
		if (pointer == array.length) pointer = 0;
		return true;
	}
	
	@Override
	public synchronized boolean offer(T e) {
		if (isFull()) return false;
		add(e);
		return true;
	}
	
	@Override
	public synchronized T remove() {
		if (isEmpty()) throw new NoSuchElementException();
		T etc = (T)array[mPointer];
		array[mPointer] = null;
		mPointer++;
		if (mPointer == array.length) mPointer = 0;
		return etc;
	}
	
	@Override
	public synchronized T poll() {
		if (isEmpty()) return null;
		T etc = (T)array[mPointer];
		array[mPointer] = null;
		mPointer++;
		if (mPointer == array.length) mPointer = 0;
		return etc;
	}
	
	@Override
	public synchronized T element() {
		if (isEmpty()) throw new NoSuchElementException();
		return (T)array[mPointer];
	}
	
	@Override
	public synchronized T peek() {
		if (isEmpty()) return null;
		return (T)array[mPointer];
	}
	
}
