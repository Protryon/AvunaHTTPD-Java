package com.javaprophet.javawebserver.plugins.base.fcgi.packets;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import com.javaprophet.javawebserver.plugins.base.fcgi.PStatus;
import com.javaprophet.javawebserver.plugins.base.fcgi.Type;

public class End extends FCGIPacket {
	public int appStatus = -1;
	public PStatus pstatus = null;
	
	public End(DataInputStream in) throws IOException {
		super(in);
	}
	
	public End(int appStatus, PStatus pstatus, int id) {
		super(Type.FCGI_BEGIN_REQUEST, id);
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
