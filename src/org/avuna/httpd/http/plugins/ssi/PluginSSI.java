/* Avuna HTTPD - General Server Applications Copyright (C) 2015 Maxwell Bruce This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>. */

package org.avuna.httpd.http.plugins.ssi;

import java.io.File;
import java.util.Date;
import java.util.HashMap;
import org.avuna.httpd.AvunaHTTPD;
import org.avuna.httpd.event.Event;
import org.avuna.httpd.event.EventBus;
import org.avuna.httpd.http.ResponseGenerator;
import org.avuna.httpd.http.event.EventGenerateResponse;
import org.avuna.httpd.http.event.HTTPEventID;
import org.avuna.httpd.http.networking.RequestPacket;
import org.avuna.httpd.http.networking.ResponsePacket;
import org.avuna.httpd.http.plugins.Plugin;
import org.avuna.httpd.http.plugins.PluginRegistry;
import org.avuna.httpd.http.plugins.ssi.directives.ConfigDirective;
import org.avuna.httpd.http.plugins.ssi.directives.EchoDirective;
import org.avuna.httpd.http.plugins.ssi.directives.ElifDirective;
import org.avuna.httpd.http.plugins.ssi.directives.ElseDirective;
import org.avuna.httpd.http.plugins.ssi.directives.EndIfDirective;
import org.avuna.httpd.http.plugins.ssi.directives.ExecDirective;
import org.avuna.httpd.http.plugins.ssi.directives.FlastmodDirective;
import org.avuna.httpd.http.plugins.ssi.directives.FsizeDirective;
import org.avuna.httpd.http.plugins.ssi.directives.IfDirective;
import org.avuna.httpd.http.plugins.ssi.directives.IncludeDirective;
import org.avuna.httpd.http.plugins.ssi.directives.PrintEnvDirective;
import org.avuna.httpd.http.plugins.ssi.directives.SetDirective;
import org.avuna.httpd.http.plugins.ssi.functions.Base64Function;
import org.avuna.httpd.http.plugins.ssi.functions.EnvFunction;
import org.avuna.httpd.http.plugins.ssi.functions.EscapeFunction;
import org.avuna.httpd.http.plugins.ssi.functions.FileFunction;
import org.avuna.httpd.http.plugins.ssi.functions.FileSizeFunction;
import org.avuna.httpd.http.plugins.ssi.functions.HTTPFunction;
import org.avuna.httpd.http.plugins.ssi.functions.HTTP_NoVaryFunction;
import org.avuna.httpd.http.plugins.ssi.functions.MD5Function;
import org.avuna.httpd.http.plugins.ssi.functions.NoteFunction;
import org.avuna.httpd.http.plugins.ssi.functions.OSENVFunction;
import org.avuna.httpd.http.plugins.ssi.functions.ReqEnvFunction;
import org.avuna.httpd.http.plugins.ssi.functions.RespFunction;
import org.avuna.httpd.http.plugins.ssi.functions.SHA1Function;
import org.avuna.httpd.http.plugins.ssi.functions.ToLowerFunction;
import org.avuna.httpd.http.plugins.ssi.functions.ToUpperFunction;
import org.avuna.httpd.http.plugins.ssi.functions.Unbase64Function;
import org.avuna.httpd.http.plugins.ssi.functions.UnescapeFunction;
import org.avuna.httpd.http.plugins.ssi.unaryop.UnaryOPAnyExists;
import org.avuna.httpd.http.plugins.ssi.unaryop.UnaryOPDirExists;
import org.avuna.httpd.http.plugins.ssi.unaryop.UnaryOPFileExists;
import org.avuna.httpd.http.plugins.ssi.unaryop.UnaryOPFileIsSymlink;
import org.avuna.httpd.http.plugins.ssi.unaryop.UnaryOPFileNotEmpty;
import org.avuna.httpd.http.plugins.ssi.unaryop.UnaryOPIPMatch;
import org.avuna.httpd.http.plugins.ssi.unaryop.UnaryOPStringEmpty;
import org.avuna.httpd.http.plugins.ssi.unaryop.UnaryOPStringNotEmpty;
import org.avuna.httpd.http.plugins.ssi.unaryop.UnaryOPStringTrue;
import org.avuna.httpd.http.plugins.ssi.unaryop.UnaryOPValidatePath;
import org.avuna.httpd.http.plugins.ssi.unaryop.UnaryOPValidateURL;

public class PluginSSI extends Plugin {
	public final SSIEngine engine = new SSIEngine(new SSIParser());
	
	public PluginSSI(String name, PluginRegistry registry, File config) {
		super(name, registry, config);
		engine.addDirective(new ConfigDirective(engine));
		engine.addDirective(new EchoDirective(engine));
		IfDirective id;
		engine.addDirective(id = new IfDirective(engine));
		engine.addDirective(new ElifDirective(id, engine));
		engine.addDirective(new ElseDirective(engine));
		engine.addDirective(new EndIfDirective(engine));
		engine.addDirective(new ExecDirective(engine));
		engine.addDirective(new FlastmodDirective(engine));
		engine.addDirective(new FsizeDirective(engine));
		engine.addDirective(new IncludeDirective(engine));
		engine.addDirective(new PrintEnvDirective(engine));
		engine.addDirective(new SetDirective(engine));
		HTTPFunction hf = new HTTPFunction();
		engine.addFunction("req", hf);
		engine.addFunction("http", hf);
		engine.addFunction("req_novary", new HTTP_NoVaryFunction());
		engine.addFunction("resp", new RespFunction());
		engine.addFunction("reqenv", new ReqEnvFunction());
		engine.addFunction("osenv", new OSENVFunction());
		engine.addFunction("note", new NoteFunction());
		engine.addFunction("env", new EnvFunction());
		engine.addFunction("tolower", new ToLowerFunction());
		engine.addFunction("toupper", new ToUpperFunction());
		engine.addFunction("escape", new EscapeFunction());
		engine.addFunction("unescape", new UnescapeFunction());
		engine.addFunction("base64", new Base64Function());
		engine.addFunction("unbase64", new Unbase64Function());
		engine.addFunction("md5", new MD5Function());
		engine.addFunction("sha1", new SHA1Function());
		engine.addFunction("file", new FileFunction());
		engine.addFunction("filesize", new FileSizeFunction());
		engine.addUnaryOP('d', new UnaryOPDirExists()); // not impl for security
		engine.addUnaryOP('e', new UnaryOPAnyExists()); // not impl for security
		engine.addUnaryOP('f', new UnaryOPFileExists()); // not impl for security
		engine.addUnaryOP('s', new UnaryOPFileNotEmpty()); // not impl for security
		UnaryOPFileIsSymlink uopfis = new UnaryOPFileIsSymlink(); // not impl for security
		engine.addUnaryOP('L', uopfis); // not impl for security
		engine.addUnaryOP('h', uopfis); // not impl for security
		engine.addUnaryOP('F', new UnaryOPValidatePath());
		UnaryOPValidateURL uopvu = new UnaryOPValidateURL();
		engine.addUnaryOP('U', uopvu);
		engine.addUnaryOP('A', uopvu);
		engine.addUnaryOP('n', new UnaryOPStringNotEmpty());
		engine.addUnaryOP('z', new UnaryOPStringEmpty());
		engine.addUnaryOP('T', new UnaryOPStringTrue());
		engine.addUnaryOP('R', new UnaryOPIPMatch());
	}
	
	@Override
	public void receive(EventBus bus, Event event) {
		if (event instanceof EventGenerateResponse) {
			EventGenerateResponse egr = (EventGenerateResponse) event;
			ResponsePacket response = egr.getResponse();
			RequestPacket request = egr.getRequest();
			if (!response.headers.hasHeader("Content-Type") || !response.hasContent()) return;
			String ct = response.headers.getHeader("Content-Type");
			if (ct == null || !ct.startsWith("application/x-ssi")) return;
			response.headers.updateHeader("Content-Type", "text/html; charset=utf-8");
			if (response.body.data.length == 0) return;
			String body = new String(response.body.data);
			Page sp = engine.getParser().parsePage(body);
			String get = request.target;
			if (get.contains("#")) {
				get = get.substring(0, get.indexOf("#"));
			}
			String rq = get;
			if (get.contains("?")) {
				rq = get.substring(0, get.indexOf("?"));
				get = get.substring(get.indexOf("?") + 1);
			}else {
				get = "";
			}
			sp.variables.put("SERVER_ADDR", "");
			sp.variables.put("REQUEST_URI", rq + (get.length() > 0 ? "?" + get : ""));
			
			rq = AvunaHTTPD.fileManager.correctForIndex(rq, request);
			
			sp.variables.put("CONTENT_LENGTH", (request.body == null || request.body.data == null) ? "0" : request.body.data.length + "");
			if (request.body != null && request.body.type != null) sp.variables.put("CONTENT_TYPE", request.body.type);
			sp.variables.put("QUERY_STRING", get);
			sp.variables.put("REMOTE_ADDR", request.userIP);
			sp.variables.put("REMOTE_HOST", request.userIP);
			sp.variables.put("REMOTE_PORT", request.userPort + "");
			sp.variables.put("REQUEST_METHOD", request.method.name);
			sp.variables.put("REDIRECT_STATUS", response.statusCode + "");
			String oabs = response.body.oabs.replace("\\", "/");
			String htds = request.host.getHTDocs().getAbsolutePath().replace("\\", "/");
			sp.variables.put("SCRIPT_NAME", oabs.substring(htds.length()));
			if (request.headers.hasHeader("Host")) sp.variables.put("SERVER_NAME", request.headers.getHeader("Host"));
			int port = request.host.getHost().getPort();
			sp.variables.put("SERVER_PORT", port + "");
			sp.variables.put("SERVER_PROTOCOL", request.httpVersion);
			sp.variables.put("SERVER_SOFTWARE", "Avuna/" + AvunaHTTPD.VERSION);
			sp.variables.put("DOCUMENT_ROOT", htds);
			sp.variables.put("SCRIPT_FILENAME", oabs);
			HashMap<String, String[]> hdrs = request.headers.getHeaders();
			for (String key : hdrs.keySet()) {
				if (key.equalsIgnoreCase("Accept-Encoding")) continue;
				for (String val : hdrs.get(key)) {
					sp.variables.put("HTTP_" + key.toUpperCase().replace("-", "_"), val); // TODO: will break if multiple same-nameed headers are received
				}
			}
			sp.variables.put("DATE_GMT", ResponseGenerator.sdf.format(new Date())); // local time
			sp.variables.put("DATE_LOCAL", sp.variables.get("DATE_GMT"));
			sp.variables.put("DOCUMENT_NAME", rq.contains("/") ? rq.substring(rq.lastIndexOf("/") + 1) : "");
			sp.variables.put("DOCUMENT_URI", rq);
			sp.variables.put("LAST_MODIFIED", "");
			sp.variables.put("QUERY_STRING_UNESCAPED", get.replace("\\", "\\\\").replace("&", "\\&"));
			StringBuilder res = new StringBuilder();
			int le = 0;
			if (sp != null && sp.directives != null) {
				sp.data = request;
				for (ParsedSSIDirective pd : sp.directives) {
					if (sp.shouldOutputNextBlock()) {
						res.append(body.substring(le, pd.start));
					}
					String lr = engine.callDirective(sp, pd);
					
					le = pd.end;
					if (lr == null) {
						response.body.data = sp.variables.get("error").getBytes();
						return;
					}else if (lr.length() > 0) {
						res.append(lr);
					}
				}
				sp.data = null;
				res.append(body.substring(le, body.length()));
				response.body.data = res.toString().getBytes();
			}
		}
	}
	
	@Override
	public void register(EventBus bus) {
		bus.registerEvent(HTTPEventID.GENERATERESPONSE, this, -700);
		bus.registerEvent(HTTPEventID.CLEARCACHE, this, 0);
	}
	
}
