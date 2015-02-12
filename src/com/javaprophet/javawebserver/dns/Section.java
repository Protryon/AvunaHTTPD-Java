package com.javaprophet.javawebserver.dns;

/**
 * Created by JavaProphet on 8/13/14 at 11:26 PM.
 */
public abstract class Section {
	private byte[] content = new byte[0];
	protected boolean finalized = false;
	
	public void setContent(byte[] b) {
		content = b;
		finalized = false;
	}
	
	protected void updateContent(byte[] b) {
		content = b;
	}
	
	public void refactor(byte[] pointers) {
	}
	
	public boolean isFinalized() {
		return finalized;
	}
	
	public byte[] getContent() {
		return content;
	}
	
	public void finalize(byte[] pointers) {
		finalized = true;
		refactor(pointers);
	}
}
