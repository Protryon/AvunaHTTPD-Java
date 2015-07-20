/* Avuna HTTPD - General Server Applications Copyright (C) 2015 Maxwell Bruce This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>. */

package org.avuna.httpd.dns;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.SocketException;
import java.util.ArrayList;
import org.avuna.httpd.dns.zone.DNSRecord;
import org.avuna.httpd.dns.zone.IDirective;
import org.avuna.httpd.dns.zone.ImportDirective;
import org.avuna.httpd.dns.zone.ZoneDirective;
import org.avuna.httpd.dns.zone.ZoneFile;
import org.avuna.httpd.hosts.HostDNS;
import org.avuna.httpd.hosts.ITerminatable;
import org.avuna.httpd.util.Logger;

public class ThreadDNSWorker extends Thread implements ITerminatable {
	
	private static int nid = 1;
	private final HostDNS host;
	
	public ThreadDNSWorker(HostDNS host) {
		super("Avuna DNS Accept Thread #" + nid++);
		host.workers.add(this);
		this.host = host;
	}
	
	private boolean keepRunning = true;
	
	public boolean domainMatches(String dom, String match, boolean hadType) {
		if (match.equals("@")) return true;
		if (match.equals("~@") && !hadType) return true;
		if (dom.equalsIgnoreCase(match)) return true;
		String[] spl = match.split("\\.");
		String[] dspl = dom.split("\\.");
		if (spl.length == dspl.length) {
			boolean bm = true;
			for (int i = 0; i < spl.length; i++) {
				if (!spl[i].equalsIgnoreCase(dspl[i]) && !spl[i].equals("*")) {
					bm = false;
				}
			}
			return bm;
		}
		return false;
	}
	
	private boolean processZoneForRecord(Question q, ZoneFile zf, ArrayList<ResourceRecord> records, boolean ht) {
		boolean hasType = ht;
		for (IDirective d : zf.getDirectives(true)) {
			if (d instanceof DNSRecord) {
				DNSRecord r = (DNSRecord) d;
				if (!r.getType().matches(q.getType())) continue;
				boolean matches = domainMatches(q.getDomain(), r.getDomain(), hasType);
				if (matches) {
					hasType = true;
					ResourceRecord responseRecord = new ResourceRecord();
					responseRecord.setDomain(q.getDomain());
					responseRecord.setType(r.getType().id);
					responseRecord.setCls(1);
					responseRecord.setTtl(r.getTimeToLive());
					byte[] data = r.getData();
					responseRecord.setLength(data.length);
					responseRecord.setData(data);
					// Logger.log("DNS: " + q.getDomain() + " for type# " + q.getType() + " returned " + AvunaHTTPD.fileManager.bytesToHex(responseRecord.getData()));
					records.add(responseRecord);
					if (r.getType() == Type.PTR || r.getType() == Type.CNAME || r.getType() == Type.DNAME || r.getType() == Type.NS) {
						Question q2 = new Question();
						q2.setDomain(r.getArgs()[0]);
						q2.setType(Type.A.id);
						processZoneForRecord(q2, zf, records, true);
					}else if (r.getType() == Type.MX) {
						Question q2 = new Question();
						q2.setDomain(r.getArgs()[1]);
						q2.setType(Type.A.id);
						processZoneForRecord(q2, zf, records, true);
					}// TODO: what else to recurse?
				}
			}else if (d instanceof ZoneDirective) {
				boolean matches = q.getDomain().matches(((ZoneDirective) d).zr);
				if (matches) {
					if (processZoneForRecord(q, ((ZoneDirective) d).zf, records, hasType)) hasType = true;
				}
			}else if (d instanceof ImportDirective) {
				if (processZoneForRecord(q, ((ImportDirective) d).zf, records, hasType)) hasType = true;
			}
			// System.out.println("Response for " + responseRecord.getDomain() + " data: " + new String(responseRecord.getData()));
		}
		return hasType;
	}
	
	public void run() {
		while (keepRunning) {
			Work focus = host.workQueue.poll();
			if (focus == null) {
				try {
					Thread.sleep(2L, 500000);
				}catch (InterruptedException e) {
					// Logger.logError(e);
				}
				continue;
			}
			try {
				Query query = null;
				if (focus.UDP) {
					query = new Query(((WorkUDP) focus).query);
				}else {
					WorkTCP wt = ((WorkTCP) focus);
					byte[] qb = new byte[wt.in.readShort()];
					wt.in.readFully(qb);
					query = new Query(qb);
				}
				if (query != null && !query.getHeader().isQr()) {
					Header qh = query.getHeader();
					Header header = new Header();
					header.setId(qh.getId());
					header.setQr(true);
					header.setOpcode(qh.getOpcode());
					header.setAa(true);
					header.setTc(false); // TODO: allows TCP stuff
					header.setRd(false);
					header.setRa(false);
					header.setZ(0);
					header.setRcode(0); // TODO: failed?
					header.setQdcount(qh.getQdcount());
					header.setNscount(0);
					header.setArcount(0);
					ArrayList<ResourceRecord> resps = new ArrayList<ResourceRecord>();
					for (Question q : query.getQd()) {
						processZoneForRecord(q, host.getZone(), resps, false);
						// Logger.log("DNS: " + q.getDomain() + " for type# " + q.getType());
						
					}
					header.setAncount(resps.size());
					// System.out.println(resps.size());
					Query response = new Query(header, query.getQd(), resps.toArray(new ResourceRecord[] {}), null, null);
					byte[] rb = response.encode();
					if (rb.length >= 512) {
						response.getHeader().setTc(true);
						rb = response.encode();
						System.arraycopy(rb, 0, rb, 0, 512);
					}
					// System.out.println("2: " + Util.bytesToHex(rb));
					if (focus.UDP) {
						WorkUDP wu = (WorkUDP) focus;
						DatagramPacket sendPacket2 = new DatagramPacket(rb, rb.length, wu.rip, wu.rport);
						wu.server.send(sendPacket2);
					}else {
						WorkTCP wt = ((WorkTCP) focus);
						wt.out.writeShort(rb.length);
						wt.out.write(rb);
						wt.out.flush();
						wt.s.close();
					}
				}
				// System.out.println((System.nanoTime() - s) / 1000000D + " ms");
			}catch (SocketException e) {
				if (!focus.UDP) {
					try {
						((WorkTCP) focus).s.close();
					}catch (IOException e1) {
						Logger.logError(e1);
					}
				}
			}catch (Exception e) {
				Logger.logError(e);
			}finally {
				if (!focus.UDP) {
					try {
						((WorkTCP) focus).s.close();
					}catch (IOException e) {
						Logger.logError(e);
					}
				}
			}
		}
	}
	
	@Override
	public void terminate() {
		keepRunning = false;
		this.interrupt();
	}
}
