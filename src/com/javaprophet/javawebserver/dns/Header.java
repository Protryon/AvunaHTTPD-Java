package com.javaprophet.javawebserver.dns;

import java.io.ByteArrayOutputStream;

/**
 * Created by JavaProphet on 8/13/14 at 11:27 PM.
 */
public class Header extends Section {
	public short getId() {
		return id;
	}
	
	public void setId(short id) {
		finalized = false;
		this.id = id;
	}
	
	public boolean isQr() {
		return qr;
	}
	
	public void setQr(boolean qr) {
		finalized = false;
		this.qr = qr;
	}
	
	public int getOpcode() {
		return opcode;
	}
	
	public void setOpcode(int opcode) {
		finalized = false;
		this.opcode = opcode;
	}
	
	public boolean isAa() {
		return aa;
	}
	
	public void setAa(boolean aa) {
		finalized = false;
		this.aa = aa;
	}
	
	public boolean isTc() {
		return tc;
	}
	
	public void setTc(boolean tc) {
		finalized = false;
		this.tc = tc;
	}
	
	public boolean isRd() {
		return rd;
	}
	
	public void setRd(boolean rd) {
		finalized = false;
		this.rd = rd;
	}
	
	public boolean isRa() {
		return ra;
	}
	
	public void setRa(boolean ra) {
		finalized = false;
		this.ra = ra;
	}
	
	public int getZ() {
		return z;
	}
	
	public void setZ(int z) {
		finalized = false;
		this.z = z;
	}
	
	public int getRcode() {
		return rcode;
	}
	
	public void setRcode(int rcode) {
		finalized = false;
		this.rcode = rcode;
	}
	
	public int getQdcount() {
		return qdcount;
	}
	
	public void setQdcount(int qdcount) {
		finalized = false;
		this.qdcount = qdcount;
	}
	
	public int getAncount() {
		return ancount;
	}
	
	public void setAncount(int ancount) {
		finalized = false;
		this.ancount = ancount;
	}
	
	public int getNscount() {
		return nscount;
	}
	
	public void setNscount(int nscount) {
		finalized = false;
		this.nscount = nscount;
	}
	
	public int getArcount() {
		return arcount;
	}
	
	public void setArcount(int arcount) {
		finalized = false;
		this.arcount = arcount;
	}
	
	public void refactor(byte[] pointers) {
		try {
			ByteArrayOutputStream tout = new ByteArrayOutputStream();
			tout.write(Util.getUShort(getId()));
			boolean[] nib = Util.toNibble(getOpcode(), 4);
			int b1 = Util.getByte(isQr(), nib[0], nib[1], nib[2], nib[3], isAa(), isTc(), isRd());
			nib = Util.toNibble(getZ(), 3);
			boolean[] nib2 = Util.toNibble(getRcode(), 4);
			int b2 = Util.getByte(isRa(), nib[0], nib[1], nib[2], nib2[0], nib2[1], nib2[2], nib2[3]);
			tout.write(b1);
			tout.write(b2);
			tout.write(Util.getUShort(getQdcount()));
			tout.write(Util.getUShort(getAncount()));
			tout.write(Util.getUShort(getNscount()));
			tout.write(Util.getUShort(getArcount()));
			tout.flush();
			updateContent(tout.toByteArray());
		}catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private short id = 0;
	private boolean qr = false;
	private int opcode = 0;
	private boolean aa, tc, rd, ra = false;
	private int z = 0;
	private int rcode = 0;
	private int qdcount, ancount, nscount, arcount = 0;
	
	public Header() {
		
	}
	
}
