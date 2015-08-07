/* Avuna HTTPD - General Server Applications Copyright (C) 2015 Maxwell Bruce This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>. */

package org.avuna.httpd.mail.imap.command;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import org.avuna.httpd.hosts.HostMail;
import org.avuna.httpd.mail.imap.IMAPCommand;
import org.avuna.httpd.mail.imap.IMAPWork;
import org.avuna.httpd.mail.mailbox.Email;
import org.avuna.httpd.mail.util.StringFormatter;
import org.avuna.httpd.util.Logger;

public class IMAPCommandSearch extends IMAPCommand {
	
	public IMAPCommandSearch(String comm, int minState, int maxState, HostMail host) {
		super(comm, minState, maxState, host);
	}
	
	private static int getDateInt(String imapdate) {
		String[] spl = imapdate.split(imapdate.contains("-") ? "-" : "/");
		if (spl.length != 3) {
			return -1;
		}
		String mos = spl[1];
		int mo = 0;
		if (mos.equals("jan")) {
			mo = 0;
		}else if (mos.equals("feb")) {
			mo = 1;
		}else if (mos.equals("mar")) {
			mo = 2;
		}else if (mos.equals("apr")) {
			mo = 3;
		}else if (mos.equals("may")) {
			mo = 4;
		}else if (mos.equals("jun")) {
			mo = 5;
		}else if (mos.equals("jul")) {
			mo = 6;
		}else if (mos.equals("aug")) {
			mo = 7;
		}else if (mos.equals("sep")) {
			mo = 8;
		}else if (mos.equals("oct")) {
			mo = 9;
		}else if (mos.equals("nov")) {
			mo = 10;
		}else if (mos.equals("dec")) {
			mo = 11;
		}else {
			mo = Integer.parseInt(mos);
		}
		return Integer.parseInt(spl[0]) + (mo * 40) + (Integer.parseInt(spl[2]) * 400);
	}
	
	private static int getDateRFC(String sdate) {
		String date = sdate.substring(sdate.indexOf(",") + 1).trim();
		int day = Integer.parseInt(date.substring(0, date.indexOf(" ")));
		date = date.substring(date.indexOf(" ") + 1);
		String mos = date.substring(0, date.indexOf(" ")).toLowerCase();
		int mo = 0;
		if (mos.equals("jan")) {
			mo = 0;
		}else if (mos.equals("feb")) {
			mo = 1;
		}else if (mos.equals("mar")) {
			mo = 2;
		}else if (mos.equals("apr")) {
			mo = 3;
		}else if (mos.equals("may")) {
			mo = 4;
		}else if (mos.equals("jun")) {
			mo = 5;
		}else if (mos.equals("jul")) {
			mo = 6;
		}else if (mos.equals("aug")) {
			mo = 7;
		}else if (mos.equals("sep")) {
			mo = 8;
		}else if (mos.equals("oct")) {
			mo = 9;
		}else if (mos.equals("nov")) {
			mo = 10;
		}else if (mos.equals("dec")) {
			mo = 11;
		}
		date = date.substring(date.indexOf(" ") + 1);
		int yr = Integer.parseInt(date.substring(0, date.indexOf(" ")));
		return day + (mo * 40) + (yr * 400);
	}
	
	private static boolean processList(IMAPWork focus, String bcom, ArrayList<Email> emls) {
		String com = bcom;
		boolean inverted = false;
		ArrayList<Email> emails = emls;
		if (com.startsWith("not")) {
			inverted = true;
			if (com.length() >= 4) com = com.substring(4);
			else return false;
			emails = new ArrayList<Email>();
		}
		if (com.equals("all")) {
			
		}else if (com.equals("answered")) {
			for (int i = 0; i < emails.size(); i++) {
				if (!emails.get(i).flags.contains("\\Answered")) {
					emails.remove(i--);
				}
			}
		}else if (com.startsWith("bcc ")) {
			for (int i = 0; i < emails.size(); i++) {
				if (emails.get(i).headers.hasHeader("BCC") && emails.get(i).headers.getHeader("BCC").contains(com.substring(4))) emails.remove(i--);
			}
		}else if (com.startsWith("before ")) {
			String wd = com.substring(7);
			int wval = getDateInt(wd);
			if (wval == -1) return false;
			for (int i = 0; i < emails.size(); i++) {
				String date = emails.get(i).headers.getHeader("Date");
				boolean hb = date == null;
				if (!hb) {
					int eval = getDateRFC(date);
					if (eval <= wval) hb = true;
				}
				if (hb) emails.remove(i--);
			}
		}else if (com.startsWith("since ")) {
			String wd = com.substring(6);
			int wval = getDateInt(wd);
			if (wval == -1) return false;
			for (int i = 0; i < emails.size(); i++) {
				String date = emails.get(i).headers.getHeader("Date");
				boolean hb = date == null;
				if (!hb) {
					int eval = getDateRFC(date);
					if (eval >= wval) hb = true;
				}
				if (hb) emails.remove(i--);
			}
		}else if (com.equals("deleted")) {
			for (int i = 0; i < emails.size(); i++) {
				if (!emails.get(i).flags.contains("\\Deleted")) emails.remove(i--);
			}
		}else if (com.equals("flagged")) {
			for (int i = 0; i < emails.size(); i++) {
				if (!emails.get(i).flags.contains("\\Flagged")) emails.remove(i--);
			}
		}else if (com.equals("draft")) {
			for (int i = 0; i < emails.size(); i++) {
				if (!emails.get(i).flags.contains("\\Draft")) emails.remove(i--);
			}
		}else if (com.equals("seen")) {
			for (int i = 0; i < emails.size(); i++) {
				if (!emails.get(i).flags.contains("\\Seen")) emails.remove(i--);
			}
		}else if (com.equals("answered")) {
			for (int i = 0; i < emails.size(); i++) {
				if (!emails.get(i).flags.contains("\\Answered")) emails.remove(i--);
			}
		}else if (com.equals("unseen")) {
			for (int i = 0; i < emails.size(); i++) {
				if (!emails.get(i).flags.contains("\\Unseen")) emails.remove(i--);
			}
		}else if (com.equals("new")) {
			for (int i = 0; i < emails.size(); i++) {
				if (!emails.get(i).flags.contains("\\Seen") && emails.get(i).flags.contains("\\Recent")) emails.remove(i--);
			}
		}else if (com.equals("old")) {
			for (int i = 0; i < emails.size(); i++) {
				if (!emails.get(i).flags.contains("\\Recent")) emails.remove(i--);
			}
		}else if (com.equals("recent")) {
			for (int i = 0; i < emails.size(); i++) {
				if (emails.get(i).flags.contains("\\Recent")) emails.remove(i--);
			}
		}else if (com.equals("unanswered")) {
			for (int i = 0; i < emails.size(); i++) {
				if (emails.get(i).flags.contains("\\Answered")) emails.remove(i--);
			}
		}else if (com.equals("undeleted")) {
			for (int i = 0; i < emails.size(); i++) {
				if (emails.get(i).flags.contains("\\Deleted")) emails.remove(i--);
			}
		}else if (com.equals("undraft")) {
			for (int i = 0; i < emails.size(); i++) {
				if (emails.get(i).flags.contains("\\Draft")) emails.remove(i--);
			}
		}else if (com.equals("unflagged")) {
			for (int i = 0; i < emails.size(); i++) {
				if (emails.get(i).flags.contains("\\Flagged")) emails.remove(i--);
			}
		}else if (com.startsWith("uid ")) {
			try {
				ArrayList<Email> toFetch = focus.selectedMailbox.getByIdentifier(com.substring(4));
				for (int i = 0; i < emails.size(); i++) {
					if (!toFetch.contains(emails.get(i))) {
						emails.remove(i--);
					}
				}
			}catch (Exception e) {
				return false;
			}
		}else if (com.startsWith("header ")) {
			com = com.substring(7);
			String hn = com.substring(0, com.indexOf(" "));
			com = com.substring(hn.length() + 1); // hd
			for (int i = 0; i < emails.size(); i++) {
				Email eml = emails.get(i);
				if (eml.headers.hasHeader(hn)) {
					if (com.length() > 0 && !eml.headers.getHeader(hn).equals(com)) {
						emails.remove(i--);
					}
				}else {
					emails.remove(i--);
				}
			}
		}else {
			try {
				ArrayList<Email> toFetch = focus.selectedMailbox.getByIdentifier(com);
				for (int i = 0; i < emails.size(); i++) {
					if (!toFetch.contains(emails.get(i))) {
						emails.remove(i--);
					}
				}
			}catch (Exception e) {
				return false;
			}
		}
		if (inverted) {
			for (int i = 0; i < emls.size(); i++) {
				if (emails.contains(emls.get(i))) {
					emls.remove(i--);
				}
			}
		}
		return true;
	}
	
	@Override
	public void run(IMAPWork focus, String letters, String[] cargs) throws IOException {
		String[] args = StringFormatter.congealBySurroundings(cargs, "\"", "\"");
		ArrayList<Email> emails;
		synchronized (focus.selectedMailbox.emails) {
			emails = new ArrayList<Email>(Arrays.asList(focus.selectedMailbox.emails));
		}
		for (int i = 0; i < emails.size(); i++) {
			if (emails.get(i) == null) emails.remove(i--);
		}
		for (int i = 0; i < args.length; i++) {
			String arg = args[i].toLowerCase().replace("\"", "");
			String targ = arg;
			if (arg.equals("not")) {
				targ += " " + args[++i].toLowerCase().replace("\"", "");
				arg = args[i];
			}
			if (arg.equals("before") || arg.equals("since") || arg.equals("uid") || arg.equals("header")) {
				targ += " " + args[++i].toLowerCase().replace("\"", "");
			}
			if (arg.equals("header")) {
				targ += " " + args[++i].replace("\"", ""); // header value is case sensitive
			}
			Logger.log(targ);
			if (!processList(focus, targ, emails)) {
				focus.writeLine(focus, letters, "NO Invalid search.");
				return;
			}
		}
		String resp = "SEARCH";
		for (Email email : emails) {
			resp += " " + email.uid;
		}
		focus.writeLine(focus, "*", resp);
		focus.writeLine(focus, letters, "OK Search completed.");
	}
}
