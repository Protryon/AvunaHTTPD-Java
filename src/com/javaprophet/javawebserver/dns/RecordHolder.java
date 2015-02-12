package com.javaprophet.javawebserver.dns;

import java.util.ArrayList;

public class RecordHolder {
	public RecordHolder() {
		
	}
	
	private final ArrayList<DNSRecord> records = new ArrayList<DNSRecord>();
	
	public void addRecord(DNSRecord record) {
		records.add(record);
	}
	
	protected ArrayList<DNSRecord> getRecords() {
		return records;
	}
}
