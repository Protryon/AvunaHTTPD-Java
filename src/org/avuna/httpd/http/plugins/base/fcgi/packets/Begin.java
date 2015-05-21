package org.avuna.httpd.http.plugins.base.fcgi.packets;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import org.avuna.httpd.http.plugins.base.fcgi.Role;
import org.avuna.httpd.http.plugins.base.fcgi.Type;

public class Begin extends FCGIPacket {
	public Role role = Role.FCGI_RESPONDER;
	
	public Begin(DataInputStream in, int l) throws IOException {
		super(Type.FCGI_BEGIN_REQUEST);
		readContent(in, l);
	}
	
	public Begin(Role role, int id) {
		super(Type.FCGI_BEGIN_REQUEST, id);
		this.role = role;
	}
	
	@Override
	protected void readContent(DataInputStream in, int l) throws IOException {
		role = Role.fromID(in.readUnsignedShort());
		in.read();// flags
		in.readFully(new byte[5]); // reserved
	}
	
	@Override
	protected void writeContent(DataOutputStream out) throws IOException {
		out.writeShort(role.id);
		out.write(1);// no shutdown
		out.write(new byte[5]); // reserved
	}
	
}
