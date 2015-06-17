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

package org.avuna.httpd.http2.networking;

public enum ErrorCode {
	NO_ERROR(0), PROTOCOL_ERROR(1), INTERNAL_ERROR(2), FLOW_CONTROL_ERROR(3), SETTINGS_TIMEOUT(4), STREAM_CLOSED(5), FRAME_SIZE_ERROR(6), REFUSED_STREAM(7), CANCEL(8), COMPRESSION_ERROR(10), ENHANCE_YOUR_CALM(11), INADEQUATE_SECURITY(12), HTTP_1_1_REQUIRED(13);
	public final int id;
	
	private ErrorCode(int id) {
		this.id = id;
	}
}
