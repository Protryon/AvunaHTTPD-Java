package org.avuna.httpd.util;

import java.util.LinkedHashMap;

public class Benchmark {
	private final boolean enabled;
	
	public Benchmark(boolean enabled) {
		this.enabled = enabled;
	}
	
	public synchronized void log() {
		if (!enabled) return;
		Logger.log(toString());
	}
	
	private LinkedHashMap<String, Long> runningSections = new LinkedHashMap<String, Long>();
	private LinkedHashMap<String, Long> endedSections = new LinkedHashMap<String, Long>();
	
	public synchronized void startSection(String name) {
		if (!enabled) return;
		long start = System.nanoTime();
		if (runningSections.containsKey(name)) {
			// already running
			return;
		}
		runningSections.put(name, start);
	}
	
	public synchronized void endSection(String name) {
		if (!enabled) return;
		long end = System.nanoTime();
		if (!runningSections.containsKey(name)) {
			// already ended
			return;
		}
		long ea = 0L;
		if (endedSections.containsKey(name)) {
			ea = endedSections.get(name);
		}
		long ntd = (end - runningSections.get(name)) + ea;
		endedSections.put(name, ntd);
		runningSections.remove(name);
	}
	
	public synchronized void endAllSections() {
		if (!enabled) return;
		for (String name : runningSections.keySet()) {
			endSection(name);
		}
	}
	
	public synchronized long getSectionTime(String name) {
		if (!enabled) return -1L;
		if (!endedSections.containsKey(name)) {
			throw new IllegalArgumentException("No ended section named '" + name + "'!");
		}
		return endedSections.get(name);
	}
	
	public synchronized String toString() {
		if (!enabled) return null;
		StringBuilder sb = new StringBuilder();
		for (String name : endedSections.keySet()) {
			sb.append(name + " = " + (getSectionTime(name) / 1000000D) + " ms\r\n");
		}
		return sb.toString();
	}
	
	public synchronized String[] getEndedSections() {
		if (!enabled) return null;
		return endedSections.keySet().toArray(new String[0]);
	}
}
