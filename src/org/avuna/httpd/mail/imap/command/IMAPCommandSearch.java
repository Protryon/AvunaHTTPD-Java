package org.avuna.httpd.mail.imap.command;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;
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
	
	private static int getDateRFC(String date) {
		date = date.substring(date.indexOf(",") + 1).trim();
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
	
	private static boolean processList(IMAPWork focus, String com, ArrayList<Email> emails) {
		if (com.equals("all")) {
			
		}else if (com.equals("answered")) {
			for (int i = 0; i < emails.size(); i++) {
				if (!emails.get(i).flags.contains("\\Answered")) {
					emails.remove(i--);
				}
			}
		}else if (com.startsWith("bcc ")) {
			for (int i = 0; i < emails.size(); i++) {
				Scanner hs = new Scanner(emails.get(i).data);
				boolean hb = false;
				while (hs.hasNextLine()) {
					String line = hs.nextLine();
					if (line.length() == 0) break;
					if (line.startsWith("BCC")) {
						if (!line.substring(5).contains(com.substring(4))) {
							emails.remove(i--);
						}
						hb = true;
					}
				}
				if (!hb) emails.remove(i--);
			}
		}else if (com.startsWith("before ")) {
			String wd = com.substring(7);
			int wval = getDateInt(wd);
			if (wval == -1) return false;
			for (int i = 0; i < emails.size(); i++) {
				Scanner hs = new Scanner(emails.get(i).data);
				boolean hb = false;
				while (hs.hasNextLine()) {
					String line = hs.nextLine();
					if (line.length() == 0) break;
					if (line.startsWith("Date")) {
						String date = line.substring(6);
						int eval = getDateRFC(date);
						if (eval <= wval) emails.remove(i--);
						hb = true;
					}
				}
				if (!hb) emails.remove(i--);
			}
		}else if (com.startsWith("since ")) {
			String wd = com.substring(6);
			int wval = getDateInt(wd);
			for (int i = 0; i < emails.size(); i++) {
				Scanner hs = new Scanner(emails.get(i).data);
				boolean hb = false;
				while (hs.hasNextLine()) {
					String line = hs.nextLine();
					if (line.length() == 0) break;
					if (line.startsWith("Date")) {
						String date = line.substring(6);
						int eval = getDateRFC(date);
						if (eval >= wval) emails.remove(i--);
						hb = true;
					}
				}
				if (!hb) emails.remove(i--);
			}
		}else {
			try {
				ArrayList<Email> toFetch = focus.selectedMailbox.getByIdentifier(com);
				for (int i = 0; i < emails.size(); i++) {
					if (!toFetch.contains(emails.get(i))) {
						emails.remove(i--);
					}
				}
				return true;
			}catch (Exception e) {
				
			}
			return false;
		}
		return true;
	}
	
	@Override
	public void run(IMAPWork focus, String letters, String[] args) throws IOException {
		args = StringFormatter.congealBySurroundings(args, "\"", "\"");
		ArrayList<Email> emails = (ArrayList<Email>)focus.selectedMailbox.emails.clone();
		for (int i = 0; i < args.length; i++) {
			String arg = args[i].toLowerCase().replace("\"", "");
			String targ = arg;
			if (arg.equals("before") || arg.equals("since")) {
				targ += " " + args[++i].toLowerCase().replace("\"", "");
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
