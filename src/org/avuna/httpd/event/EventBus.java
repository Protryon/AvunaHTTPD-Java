/* Avuna HTTPD - General Server Applications Copyright (C) 2015 Maxwell Bruce This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>. */

package org.avuna.httpd.event;

public class EventBus {
	
	private IEventReceiver[][] recvs = new IEventReceiver[0][];
	private int[][] priorities = new int[0][];
	
	public void registerEvent(int id, IEventReceiver recv, int priority) {
		synchronized (recvs) {
			if (id >= recvs.length) {
				IEventReceiver[][] nr = new IEventReceiver[id + 1][];
				System.arraycopy(recvs, 0, nr, 0, recvs.length);
				int ol = recvs.length;
				recvs = nr;
				for (; ol < id + 1; ol++) {
					recvs[ol] = new IEventReceiver[0];
				}
				int[][] np = new int[id + 1][];
				System.arraycopy(priorities, 0, np, 0, priorities.length);
				ol = priorities.length;
				priorities = np;
				for (; ol < id + 1; ol++) {
					priorities[ol] = new int[0];
				}
			}
			IEventReceiver[] ida = new IEventReceiver[recvs[id].length + 1];
			System.arraycopy(recvs[id], 0, ida, 0, ida.length - 1);
			ida[ida.length - 1] = recv;
			int[] idp = new int[priorities[id].length + 1];
			System.arraycopy(priorities[id], 0, idp, 0, idp.length - 1);
			idp[idp.length - 1] = priority;
			int[] i2r = new int[idp.length];
			int i2ri = 0;
			int[] pool = new int[idp.length];
			System.arraycopy(idp, 0, pool, 0, pool.length);
			for (int pi = 0; pi < pool.length; pi++) {
				int li = -1;
				int lv = Integer.MIN_VALUE;
				for (int i = 0; i < pool.length; i++) {
					if (lv < pool[i]) {
						lv = pool[i];
						li = i;
					}
				}
				i2r[i2ri++] = li;
				pool[li] = Integer.MIN_VALUE;
			}
			IEventReceiver[] ida2 = new IEventReceiver[ida.length];
			int[] idp2 = new int[idp.length];
			int idp2i = 0;
			for (int r : i2r) {
				idp2[idp2i] = idp[r];
				ida2[idp2i++] = ida[r];
			}
			recvs[id] = ida2;
			priorities[id] = idp2;
		}
	}
	
	public void removeListener(IEventReceiver recv) {
		for (int i = 0; i < recvs.length; i++) {
			for (int i2 = 0; i2 < recvs[i].length; i2++) {
				if (recvs[i][i2] == recv) {
					recvs[i][i2] = null;
				}
			}
		}
	}
	
	public void callEvent(Event event) {
		int id = event.getEID();
		if (id >= recvs.length || recvs[id].length == 0) {
			return;
		}
		for (int r = 0; r < recvs[id].length; r++) {
			IEventReceiver ir = recvs[id][r];
			if (ir != null) ir.receive(this, event);
			if (event.isCanceled()) break;
		}
	}
}
