package org.avuna.httpd.http.plugins.avunaagent;

import org.avuna.httpd.util.ConfigFormat;

public abstract class AvunaAgentConfigFormat extends ConfigFormat {
	public final AvunaAgent us;
	
	public AvunaAgentConfigFormat(AvunaAgent us) {
		super();
		this.us = us;
	}
}
