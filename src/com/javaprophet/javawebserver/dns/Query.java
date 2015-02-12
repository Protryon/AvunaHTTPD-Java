package com.javaprophet.javawebserver.dns;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Created by JavaProphet on 8/14/14 at 1:33 AM.
 */
public class Query {
	
	public Header getHeader() {
		return header;
	}
	
	public Question[] getQd() {
		return qd;
	}
	
	public ResourceRecord[] getRr() {
		return rr;
	}
	
	public ResourceRecord[] getNs() {
		return ns;
	}
	
	public ResourceRecord[] getAr() {
		return ar;
	}
	
	public byte[] encode() {
		try {
			ByteArrayOutputStream tout = new ByteArrayOutputStream();
			if (!header.finalized) {
				header.finalize(new byte[0]);
			}
			tout.write(header.getContent());
			for (Question q : qd) {
				if (!q.finalized) {
					q.finalize(tout.toByteArray());
				}
				tout.write(q.getContent());
			}
			for (ResourceRecord r : rr) {
				if (!r.finalized) {
					r.finalize(tout.toByteArray());
				}
				tout.write(r.getContent());
			}
			if (ns != null) for (Section n : ns) {
				if (!n.finalized) {
					n.finalize(tout.toByteArray());
				}
				tout.write(n.getContent());
			}
			if (ar != null) for (Section a : ar) {
				if (!a.finalized) {
					a.finalize(tout.toByteArray());
				}
				tout.write(a.getContent());
			}
			return tout.toByteArray();
		}catch (Exception e) {
			e.printStackTrace();
			return new byte[0];
		}
	}
	
	private Header header;
	private Question[] qd;
	private ResourceRecord[] rr;
	private ResourceRecord[] ns;
	private ResourceRecord[] ar;
	
	public Query(byte[] data) {
		try {
			DataInputStream in = new DataInputStream(new ByteArrayInputStream(data));
			ByteArrayOutputStream pointerStream = new ByteArrayOutputStream();
			Header header = new Header();
			short id = in.readShort();
			header.setId(id);
			byte[] na = new byte[2];
			in.read(na);
			header.setQr(Util.getBit(0, na));
			header.setOpcode(Util.getNibblet(1, 4, na));
			header.setAa(Util.getBit(5, na));
			header.setTc(Util.getBit(6, na));
			header.setRd(Util.getBit(7, na));
			header.setRa(Util.getBit(8, na));
			header.setZ(Util.getNibblet(9, 3, na));
			header.setRcode(Util.getNibblet(12, 4, na));
			na = new byte[2];
			in.read(na);
			header.setQdcount(Util.getUShort(na));
			na = new byte[2];
			in.read(na);
			header.setAncount(Util.getUShort(na));
			na = new byte[2];
			in.read(na);
			header.setNscount(Util.getUShort(na));
			na = new byte[2];
			in.read(na);
			header.setArcount(Util.getUShort(na));
			byte[] content = new byte[12];
			System.arraycopy(data, 0, content, 0, content.length);
			header.setContent(content);
			header.finalize(new byte[0]);
			pointerStream.write(header.getContent());
			pointerStream.flush();
			int loc = 12;
			Question[] qd = new Question[header.getQdcount()];
			ResourceRecord[] an = new ResourceRecord[header.getAncount()];
			ResourceRecord[] ns = new ResourceRecord[header.getNscount()];
			ResourceRecord[] ar = new ResourceRecord[header.getArcount()];
			for (int i = 0; i < qd.length; i++) {
				int sloc = loc;
				qd[i] = new Question();
				int ploc = loc;
				ByteArrayOutputStream dout = new ByteArrayOutputStream();
				int reRead = 0;
				boolean pointering = false;
				boolean hasNext = true;
				while (hasNext) {
					byte b = data[ploc];
					int ct = (b & 0xff);
					if (Util.getBit(0, b) && Util.getBit(1, b)) {
						ByteBuffer buf = ByteBuffer.allocate(2);
						buf.order(ByteOrder.BIG_ENDIAN);
						buf.put(0, (byte)((data[ploc] & 0xff) - 192));
						buf.put(1, data[ploc + 1]);
						if (!pointering) reRead += 2;
						ploc = buf.getShort(0);
						pointering = true;
					}else if (ct > 0) {
						ploc++;
						if (dout.toByteArray().length > 0) {
							dout.write(".".getBytes());
						}
						byte[] tmp = new byte[ct];
						System.arraycopy(data, ploc, tmp, 0, tmp.length);
						dout.write(tmp);
						ploc += ct;
						if (!pointering) reRead += ct + 1;
					}else {
						hasNext = false;
						ploc += 1;
						if (!pointering) reRead += 1;
					}
				}
				in.read(new byte[reRead]);
				String domain = new String(dout.toByteArray());
				loc += reRead;
				qd[i].setDomain(domain);
				na = new byte[2];
				in.read(na);
				qd[i].setType(Util.getUShort(na));
				na = new byte[2];
				in.read(na);
				qd[i].setCls(Util.getUShort(na));
				loc += 4;
				content = new byte[loc - sloc];
				System.arraycopy(data, sloc, content, 0, content.length);
				qd[i].setContent(content);
				qd[i].finalize(pointerStream.toByteArray());
				pointerStream.write(qd[i].getContent());
				pointerStream.flush();
			}
			// we don't care, we are purely authoritative DNS.
			// for (int i = 0; i < an.length; i++) {
			// int sloc = loc;
			// an[i] = new ResourceRecord();
			// int ploc = loc;
			// ByteArrayOutputStream dout = new ByteArrayOutputStream();
			// int reRead = 0;
			// boolean pointering = false;
			// boolean hasNext = true;
			// while (hasNext) {
			// byte b = data[ploc];
			// int ct = (b & 0xff);
			// if (Util.getBit(0, b) && Util.getBit(1, b)) {
			// ByteBuffer buf = ByteBuffer.allocate(2);
			// buf.order(ByteOrder.BIG_ENDIAN);
			// buf.put(0, (byte)((data[ploc] & 0xff) - 192));
			// buf.put(1, data[ploc + 1]);
			// if (!pointering) reRead += 2;
			// ploc = buf.getShort(0);
			// pointering = true;
			// }else if (ct > 0) {
			// ploc++;
			// if (dout.toByteArray().length > 0) {
			// dout.write(".".getBytes());
			// }
			// byte[] tmp = new byte[ct];
			// System.arraycopy(data, ploc, tmp, 0, tmp.length);
			// dout.write(tmp);
			// ploc += ct;
			// if (!pointering) reRead += ct + 1;
			// }else {
			// hasNext = false;
			// ploc += 1;
			// if (!pointering) reRead += 1;
			// }
			// }
			// in.read(new byte[reRead]);
			// loc += reRead;
			// String domain = new String(dout.toByteArray());
			// an[i].setDomain(domain);
			// na = new byte[2];
			// in.read(na);
			// an[i].setType(Util.getUShort(na));
			// na = new byte[2];
			// in.read(na);
			// an[i].setCls(Util.getUShort(na));
			// na = new byte[4];
			// in.read(na);
			// an[i].setTtl(Util.getUInt(na));
			// na = new byte[2];
			// in.read(na);
			// an[i].setLength(Util.getUShort(na));
			// loc += 10;
			// na = new byte[an[i].getLength()];
			// in.read(na);
			// an[i].setData(na);
			// loc += na.length;
			// an[i].finalize(pointerStream.toByteArray());
			// pointerStream.write(an[i].getContent());
			// pointerStream.flush();
			// }
			// for (int i = 0; i < ns.length; i++) {
			// int sloc = loc;
			// ns[i] = new ResourceRecord();
			// int ploc = loc;
			// ByteArrayOutputStream dout = new ByteArrayOutputStream();
			// int reRead = 0;
			// boolean pointering = false;
			// boolean hasNext = true;
			// while (hasNext) {
			// byte b = data[ploc];
			// int ct = (b & 0xff);
			// if (Util.getBit(0, b) && Util.getBit(1, b)) {
			// ByteBuffer buf = ByteBuffer.allocate(2);
			// buf.order(ByteOrder.BIG_ENDIAN);
			// buf.put(0, (byte)((data[ploc] & 0xff) - 192));
			// buf.put(1, data[ploc + 1]);
			// if (!pointering) reRead += 2;
			// ploc = buf.getShort(0);
			// pointering = true;
			// }else if (ct > 0) {
			// ploc++;
			// if (dout.toByteArray().length > 0) {
			// dout.write(".".getBytes());
			// }
			// byte[] tmp = new byte[ct];
			// System.arraycopy(data, ploc, tmp, 0, tmp.length);
			// dout.write(tmp);
			// ploc += ct;
			// if (!pointering) reRead += ct + 1;
			// }else {
			// hasNext = false;
			// ploc += 1;
			// if (!pointering) reRead += 1;
			// }
			// }
			// in.read(new byte[reRead]);
			// loc += reRead;
			// String domain = new String(dout.toByteArray());
			// ns[i].setDomain(domain);
			// na = new byte[2];
			// in.read(na);
			// ns[i].setType(Util.getUShort(na));
			// na = new byte[2];
			// in.read(na);
			// ns[i].setCls(Util.getUShort(na));
			// na = new byte[4];
			// in.read(na);
			// ns[i].setTtl(Util.getUInt(na));
			// na = new byte[2];
			// in.read(na);
			// ns[i].setLength(Util.getUShort(na));
			// loc += 10;
			// na = new byte[ns[i].getLength()];
			// in.read(na);
			// ns[i].setData(na);
			// loc += na.length;
			// ns[i].finalize(pointerStream.toByteArray());
			// pointerStream.write(ns[i].getContent());
			// pointerStream.flush();
			// }
			// for (int i = 0; i < ar.length; i++) {
			// int sloc = loc;
			// ar[i] = new ResourceRecord();
			// int ploc = loc;
			// ByteArrayOutputStream dout = new ByteArrayOutputStream();
			// int reRead = 0;
			// boolean pointering = false;
			// boolean hasNext = true;
			// while (hasNext) {
			// byte b = data[ploc];
			// int ct = (b & 0xff);
			// if (Util.getBit(0, b) && Util.getBit(1, b)) {
			// ByteBuffer buf = ByteBuffer.allocate(2);
			// buf.order(ByteOrder.BIG_ENDIAN);
			// buf.put(0, (byte)((data[ploc] & 0xff) - 192));
			// buf.put(1, data[ploc + 1]);
			// if (!pointering) reRead += 2;
			// ploc = buf.getShort(0);
			// pointering = true;
			// }else if (ct > 0) {
			// ploc++;
			// if (dout.toByteArray().length > 0) {
			// dout.write(".".getBytes());
			// }
			// byte[] tmp = new byte[ct];
			// System.arraycopy(data, ploc, tmp, 0, tmp.length);
			// dout.write(tmp);
			// ploc += ct;
			// if (!pointering) reRead += ct + 1;
			// }else {
			// hasNext = false;
			// ploc += 1;
			// if (!pointering) reRead += 1;
			// }
			// }
			// in.read(new byte[reRead]);
			// loc += reRead;
			// String domain = new String(dout.toByteArray());
			// ar[i].setDomain(domain);
			// na = new byte[2];
			// in.read(na);
			// ar[i].setType(Util.getUShort(na));
			// na = new byte[2];
			// in.read(na);
			// ar[i].setCls(Util.getUShort(na));
			// na = new byte[4];
			// in.read(na);
			// ar[i].setTtl(Util.getUInt(na));
			// na = new byte[2];
			// in.read(na);
			// ar[i].setLength(Util.getUShort(na));
			// loc += 10;
			// na = new byte[ar[i].getLength()];
			// in.read(na);
			// ar[i].setData(na);
			// loc += na.length;
			// ar[i].finalize(pointerStream.toByteArray());
			// pointerStream.write(ar[i].getContent());
			// pointerStream.flush();
			// }
			this.header = header;
			this.qd = qd;
			this.rr = an;
			this.ns = ns;
			this.ar = ar;
		}catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public Query(Header header, Question[] qd, ResourceRecord[] rr, ResourceRecord[] ns, ResourceRecord[] ar) {
		this.header = header;
		this.qd = qd;
		this.rr = rr;
		this.ns = ns;
		this.ar = ar;
	}
	
}
