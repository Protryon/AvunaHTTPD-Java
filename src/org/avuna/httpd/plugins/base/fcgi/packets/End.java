package org.avuna.httpd.plugins.base.fcgi.packets;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import org.avuna.httpd.plugins.base.fcgi.PStatus;
import org.avuna.httpd.plugins.base.fcgi.Type;

public class End extends FCGIPacket {
	public int appStatus = -1;
	public PStatus pstatus = null;
	
	public End(DataInputStream in, int l) throws IOException {
		super(Type.FCGI_END_REQUEST);
		readContent(in, l);
	}
	
	public End(int appStatus, PStatus pstatus, int id) {
		super(Type.FCGI_END_REQUEST, id);
		this.appStatus = appStatus;
		this.pstatus = pstatus;
	}
	
	@Override
	protected void readContent(DataInputStream in, int l) throws IOException {
		appStatus = in.readInt();
		pstatus = PStatus.fromID(in.read());
		in.readFully(new byte[3]); // reserved
	}
	
	@Override
	protected void writeContent(DataOutputStream out) throws IOException {
		out.writeInt(appStatus);
		out.write(pstatus.id);
		out.write(new byte[3]); // reserved
	}
	
}
