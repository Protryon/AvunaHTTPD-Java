package com.javaprophet.javawebserver.dns;

import java.io.ByteArrayOutputStream;

/**
 * Created by JavaProphet on 8/13/14 at 11:27 PM.
 */
public class Question extends Section {
	private String domain = "";
	private int type = 1;
	private int cls = 1;
	
	public Question() {
		
	}
	
	public void setDomain(String domain) {
		finalized = false;
		this.domain = domain;
	}
	
	public void setType(int type) {
		finalized = false;
		this.type = type;
	}
	
	public void setCls(int cls) {
		finalized = false;
		this.cls = cls;
	}
	
	public void refactor(byte[] pointers) {
		try {
			ByteArrayOutputStream tout = new ByteArrayOutputStream();
			ByteArrayOutputStream dom = new ByteArrayOutputStream();
			String[] dspl = getDomain().split("\\.");
			for (String spl : dspl) {
				dom.write(spl.length());
				dom.write(spl.getBytes());
			}
			dom.write(0);
			dom.flush();
			byte[] domainb = dom.toByteArray();
			int mLoc = Util.getLocationInArray(pointers, domainb);
			if (mLoc < 0) {
				ByteArrayOutputStream dom2 = new ByteArrayOutputStream();
				boolean subpo = false;
				for (int i = 0; i < dspl.length; i++) {
					dom = new ByteArrayOutputStream();
					for (int i2 = i; i2 < dspl.length; i2++) {
						dom.write(dspl[i2].length());
						dom.write(dspl[i2].getBytes());
					}
					dom.flush();
					byte[] tot = dom.toByteArray();
					int totl = Util.getLocationInArray(pointers, tot);
					if (totl > -1) {
						subpo = true;
						domainb = new byte[dom2.toByteArray().length + 2];
						System.arraycopy(dom2.toByteArray(), 0, domainb, 0, domainb.length - 2);
						System.arraycopy(Util.getUShort(Util.getByte(true, true, false, false, false, false, false, false, false, false, false, false, false, false, false, false) | totl), 0, domainb, domainb.length - 2, 2);
					}else {
						dom2.write(dspl[i].length());
						dom2.write(dspl[i].getBytes());
						dom2.flush();
					}
				}
				if (!subpo) dom2.write(0);
				dom2.flush();
				tout.write(domainb);
			}else if (mLoc > -1) {
				tout.write(Util.getUShort(Util.getByte(true, true, false, false, false, false, false, false, false, false, false, false, false, false, false, false) | mLoc));
			}
			tout.write(Util.getUShort(getType()));
			tout.write(Util.getUShort(getCls()));
			tout.flush();
			updateContent(tout.toByteArray());
		}catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public String getDomain() {
		return domain;
	}
	
	public int getType() {
		return type;
	}
	
	public int getCls() {
		return cls;
	}
	
}
