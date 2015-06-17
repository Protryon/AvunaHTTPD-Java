/*	Avuna HTTPD - General Server Applications
    Copyright (C) 2015 Maxwell Bruce

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.*/

package org.avuna.httpd.dns;

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
