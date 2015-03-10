package com.javaprophet.javawebserver.dns;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.HashMap;
import com.javaprophet.javawebserver.networking.CircularQueue;
import com.javaprophet.javawebserver.util.Logger;

public class ThreadDNSWorker extends Thread {
	
	public static RecordHolder holder;
	
	public ThreadDNSWorker() {
		this.setDaemon(true);
		workers.add(this);
	}
	
	public static void clearWork() {
		workQueue.clear();
	}
	
	private static ArrayList<ThreadDNSWorker> workers = new ArrayList<ThreadDNSWorker>();
	private static CircularQueue<Work> workQueue;
	private static HashMap<String, Integer> connIPs = new HashMap<String, Integer>();
	
	public static void initQueue(int connlimit) {
		workQueue = new CircularQueue<Work>(connlimit);
	}
	
	public static int getConnectionsForIP(String ip) {
		return connIPs.get(ip);
	}
	
	public static void addWork(Work work) {
		workQueue.add(work);
	}
	
	private boolean keepRunning = true;
	
	public void close() {
		keepRunning = false;
	}
	
	public static int getQueueSize() {
		return workQueue.size();
	}
	
	public void run() {
		while (keepRunning) {
			Work focus = workQueue.poll();
			if (focus == null) {
				try {
					Thread.sleep(10L);
				}catch (InterruptedException e) {
					Logger.logError(e);
				}
				continue;
			}
			try {
				long s = System.nanoTime();
				Query query = null;
				if (focus.UDP) {
					query = new Query(((WorkUDP)focus).query);
				}else {
					WorkTCP wt = ((WorkTCP)focus);
					byte[] qb = new byte[wt.in.readShort()];
					wt.in.readFully(qb);
					query = new Query(qb);
				}
				if (query != null && !query.getHeader().isQr()) {
					for (Question q : query.getQd()) {
						// System.out.println("Request for " + q.getDomain());
					}
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
						for (DNSRecord r : holder.getRecords()) {
							if (!(r.getDomain().equals(q.getDomain()) || r.getDomain().equals("@")) || !r.getType().matches(q.getType())) continue;
							ResourceRecord responseRecord = new ResourceRecord();
							responseRecord.setDomain(q.getDomain());
							responseRecord.setType(r.getType().id);
							responseRecord.setCls(1);
							responseRecord.setTtl(r.getTimeToLive());
							byte[] data = r.getData();
							responseRecord.setLength(data.length);
							responseRecord.setData(data);
							resps.add(responseRecord);
							// System.out.println("Response for " + responseRecord.getDomain() + " data: " + new String(responseRecord.getData()));
						}
					}
					header.setAncount(resps.size());
					// System.out.println(resps.size());
					Query response = new Query(header, query.getQd(), resps.toArray(new ResourceRecord[]{}), null, null);
					byte[] rb = response.encode();
					if (rb.length >= 512) {
						response.getHeader().setTc(true);
						rb = response.encode();
						System.arraycopy(rb, 0, rb, 0, 512);
					}
					// System.out.println("2: " + Util.bytesToHex(rb));
					if (focus.UDP) {
						WorkUDP wu = (WorkUDP)focus;
						DatagramPacket sendPacket2 = new DatagramPacket(rb, rb.length, wu.rip, wu.rport);
						wu.server.send(sendPacket2);
					}else {
						WorkTCP wt = ((WorkTCP)focus);
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
						((WorkTCP)focus).s.close();
					}catch (IOException e1) {
						Logger.logError(e1);
					}
				}
			}catch (Exception e) {
				Logger.logError(e);
			}finally {
				if (!focus.UDP) {
					try {
						((WorkTCP)focus).s.close();
					}catch (IOException e) {
						Logger.logError(e);
					}
				}
			}
		}
	}
}
