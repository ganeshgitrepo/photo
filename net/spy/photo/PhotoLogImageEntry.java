/*
 * Copyright (c) 1999 Dustin Sallings
 *
 * $Id: PhotoLogImageEntry.java,v 1.3 2000/11/10 07:17:18 dustin Exp $
 */

package net.spy.photo;

import java.util.*;
import java.text.*;
import java.io.*;

import javax.servlet.*;
import javax.servlet.http.*;

import net.spy.*;
import net.spy.log.*;

public class PhotoLogImageEntry extends SpyLogEntry {

	private int photo_id;
	private int wwwuser_id;
	private Date timestamp;
	private String remote_addr;
	private String server_host;
	private String user_agent;
	private boolean cached;

	public PhotoLogImageEntry(int u, int p, boolean c,
		HttpServletRequest request) {
		super();
		photo_id=p;
		wwwuser_id=u;
		cached=c;
		remote_addr=request.getRemoteAddr();
		server_host=request.getServerName();
		user_agent=request.getHeader("User-Agent");
		timestamp = new Date();
	}

	public String toString() {
		String r;
		SimpleDateFormat f = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
		PhotoConfig conf = new PhotoConfig();

		r="insert into photo_log(photo_id, wwwuser_id, remote_addr, "
			+ "server_host, user_agent, cached, ts) values("
			+ photo_id + ", " + wwwuser_id + ", '" + remote_addr 
			+ "', '" + server_host
			+ "', get_agent('" + PhotoUtil.dbquote_str(user_agent) + "'), '"
			+ cached + "', '" + f.format(timestamp) + " "
			+ conf.get("timezone") + "')";

		return(r);
	}
}
