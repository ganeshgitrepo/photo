/*
 * Copyright (c) 1999 Dustin Sallings <dustin@spy.net>
 *
 * $Id: PhotoAdmin.java,v 1.13 2001/07/03 08:08:01 dustin Exp $
 */

package net.spy.photo;

import java.security.*;
import java.util.*;
import java.sql.*;

import javax.servlet.*;
import javax.servlet.http.*;

import net.spy.*;
import net.spy.cache.*;

public class PhotoAdmin extends PhotoHelper {

	protected boolean debug=true;

	PhotoSession ps=null;

	public PhotoAdmin(PhotoSession ps) throws Exception {
		super();
		this.ps=ps;
	}

	public String process(String func) throws Exception {
		String out="";

		if(func.equals("admcat")) {
			out=admShowCategories();
		} else if(func.equals("admcatedit")) {
			out=admEditCategoryForm();
		} else if(func.equals("admsavecat")) {
			out=admSaveCategory();
		} else if(func.equals("admuser")) {
			out=admShowUsers();
		} else if(func.equals("admuseredit")) {
			out=admEditUserForm();
		} else if(func.equals("admsaveuser")) {
			out=admSaveUser();
		} else if(func.equals("admedittext")) {
			saveImageInfo();
			out=ps.doDisplay();
		} else {
			throw new ServletException("Not an Admin function:  "+func);
		}
		return(out);
	}

	// Show the user edit form
	public String admShowUsers() throws ServletException {
		ServletException se=null;
		if(!isAdmin()) {
			throw new ServletException("Not admin");
		}
		String out="";
		Connection photo=null;
		try {
			photo=getDBConn();
			String users="";
			Hashtable h = new Hashtable();

			Statement st=photo.createStatement();
			ResultSet rs=st.executeQuery(
				"select id, username from wwwusers"
				);
			while(rs.next()) {
				users+="\t\t<option value=\""
					+ rs.getInt("id") + "\">"
					+ rs.getString("username") + "</option>\n";
			}
			h.put("USERS", users);
			out=tokenize("admin/admuser.inc", h);
		} catch(Exception e) {
			se=new ServletException("Error showing users", e);
		} finally {
			freeDBConn(photo);
		}

		if(se!=null) {
			throw(se);
		}

		return(out);
	}

	// Show a user to be edited
	protected String admEditUserForm() throws ServletException {
		String output="";
		ServletException se=null;
		if(!isAdmin()) {
			throw new ServletException("Not admin");
		}
		Connection photo=null;
		try {
			String s_user_id=ps.request.getParameter("userid");
			int userid=Integer.parseInt(s_user_id);
			Hashtable h=new Hashtable();
			Hashtable cats=new Hashtable();
			String acltable="";

			// Defaults -- Basically for a new uesr
			h.put("USERID", s_user_id);
			h.put("USER", "New User");
			h.put("REALNAME", "New User");
			h.put("EMAIL", "");
			h.put("PASS", "");
			h.put("CANADD", "");
			h.put("CANNOTADD", "checked");

			photo=getDBConn();

			// First DB query is to get the user info
			PreparedStatement st=photo.prepareStatement(
				"select * from wwwusers where id=?"
				);
			st.setInt(1, userid);
			ResultSet rs=st.executeQuery();
			while(rs.next()) {
				h.put("USERID", rs.getString("id"));
				h.put("USER", rs.getString("username"));
				h.put("PASS", rs.getString("password"));
				h.put("EMAIL", rs.getString("email"));
				h.put("REALNAME", rs.getString("realname"));

				if(rs.getBoolean("canadd")) {
					h.put("CANADD", "checked");
					h.put("CANNOTADD", "");
				} else {
					h.put("CANADD", "");
					h.put("CANNOTADD", "checked");
				}
			}

			// Second DB query gets a list of the categories the user can
			// see
			st=photo.prepareStatement(
				"select cat from wwwacl where userid=?"
				);
			st.setInt(1, userid);
			rs=st.executeQuery();
			while(rs.next()) {
				cats.put(rs.getString("cat"), "1");
			}

			// Third DB query gets all of the categories
			st=photo.prepareStatement(
				"select * from cat order by name"
				);
			rs=st.executeQuery();
			while(rs.next()) {
				String catname=rs.getString("name");
				String cat_id_s=rs.getString("id");
				int cat_id=rs.getInt("id");

				if(cats.containsKey(cat_id_s)) {
					acltable+="<tr>\n\t<td><font color=\"green\">"
						+ rs.getString("name")
						+ "</font></td>";
					acltable+="<td><input type=\"checkbox\" name=\"catacl\" "
						+ "checked=\"1\" " + "value=\"" + cat_id
							+ "\"></td></tr>\n";
				} else {
					acltable+="<tr>\n\t<td><font color=\"red\">"
						+ rs.getString("name")
						+ "</font></td>";
					acltable+="<td><input type=\"checkbox\" name=\"catacl\" "
						+ "value=\"" + cat_id + "\"></td></tr>\n";
				}
			}
			h.put("ACLTABLE", acltable);
			output=tokenize("admin/userform.inc", h);
		} catch(Exception e) {
			se=new ServletException("Error displaying user:  " + e);
		} finally {
			freeDBConn(photo);
		}
		if(se!=null) {
			throw(se);
		}
		return(output);
	}

	// Save a user
	protected String admSaveUser() throws ServletException {
		Connection photo=null;

		try {
			PhotoSecurity security=new PhotoSecurity();
			String user_id_s=ps.request.getParameter("userid");
			int user_id=Integer.parseInt(user_id_s);

			// Try to see if'n we can get one of these already done
			PhotoUser user = security.getUser(user_id);

			// If we didn't get one, we will this time.
			if(user==null) {
				user=new PhotoUser();
			}

			// Username
			String tmp=ps.request.getParameter("username");
			if(tmp!=null) {
				user.setUsername(tmp);
			}

			// Password
			tmp=ps.request.getParameter("password");
			if(tmp!=null) {
				user.setPassword(tmp);
			}

			// Real name
			tmp=ps.request.getParameter("realname");
			if(tmp!=null) {
				user.setRealname(tmp);
			}

			// Email
			tmp=ps.request.getParameter("email");
			if(tmp!=null) {
				user.setEmail(tmp);
			}

			// CanAdd
			tmp=ps.request.getParameter("canadd");
			if(tmp!=null) {
				boolean tmpb=false;
				if(tmp.startsWith("t")) {
					tmpb=true;
				}
				user.canAdd(tmpb);
			}

			// Save the settings.
			user.save();

			// Now save the ACLs and stuff.
			photo=getDBConn();
			photo.setAutoCommit(false);

			PreparedStatement st=null;

			// Delete all of the ACLs
			st=photo.prepareStatement("delete from wwwacl where userid=?");
			st.setInt(1, user_id);
			st.executeUpdate();

			// Add the new ACLs for the user
			String acls[]=ps.request.getParameterValues("catacl");
			if(acls!=null) {
				for(int i=0; i<acls.length; i++) {
					int cat_id=Integer.parseInt(acls[i]);
					st=photo.prepareStatement(
						"insert into wwwacl(userid,cat) values(?,?)"
						);
					st.setInt(1, user_id);
					st.setInt(2, cat_id);
					st.executeUpdate();
				}
			}

			photo.commit();
		} catch(Exception e) {
			log("Error saving user:  " + e);
			e.printStackTrace();
			try {
				if(photo!=null) {
					photo.rollback();
				}
			} catch(Exception e2) {
				// Don't mind this...
			}
		} finally {
			if(photo != null) {
				try {
					photo.setAutoCommit(true);
					freeDBConn(photo);
				} catch(Exception e) {
					log(e.getMessage());
				}
			}
			freeDBConn(photo);
		}
		return(admShowUsers());
	}

	// Show the category edit form
	protected String admShowCategories() throws ServletException {
		if(!isAdmin()) {
			throw new ServletException("Not admin");
		}
		Hashtable h = new Hashtable();
		h.put("CATS", ps.getCatList(-1));
		return(tokenize("admin/admcat.inc", h));
	}

	// Show the form to edit categories.
	protected String admEditCategoryForm() throws ServletException {
		String output="";
		ServletException se=null;
		if(!isAdmin()) {
			throw new ServletException("Not admin");
		}
		Connection photo=null;
		try {
			String s_cat_id=ps.request.getParameter("cat");
			int cat_id=Integer.parseInt(s_cat_id);
			Hashtable h=new Hashtable();
			h.put("CATID", "-1");
			h.put("CATNAME", "");
			photo=getDBConn();
			PreparedStatement st=photo.prepareStatement(
				"select name from cat where id=?"
				);
			st.setInt(1, cat_id);
			ResultSet rs=st.executeQuery();
			while(rs.next()) {
				h.put("CATID", s_cat_id);
				h.put("CATNAME", rs.getString("name"));
			}
			output=tokenize("admin/editcat.inc", h);
		} catch(Exception e) {
			se=new ServletException("Error displaying cat:  " + e);
		} finally {
			freeDBConn(photo);
		}
		if(se!=null) {
			throw(se);
		}
		return(output);
	}

	// Show the category edit form
	protected String admSaveCategory() throws ServletException {
		if(!isAdmin()) {
			throw new ServletException("Not admin");
		}
		Hashtable h = new Hashtable();
		ServletException se=null;
		Connection photo=null;
		try {
			String stmp=ps.request.getParameter("id");
			int cat_id=Integer.parseInt(stmp);
			String cat_name=ps.request.getParameter("name");

			photo=getDBConn();
			if(cat_id>0) {
				PreparedStatement st=photo.prepareStatement(
					"update cat set name=? where id=?"
					);
				st.setString(1, ps.request.getParameter("name"));
				st.setInt(2, cat_id);
				st.executeUpdate();
			} else {
				PreparedStatement st=photo.prepareStatement(
					"insert into cat(name) values(?)"
					);
				st.setString(1, cat_name);
				st.executeUpdate();
				// Find out what the ID of that was so that we can grant
				// ourselves access to it.
				st=photo.prepareStatement("select currval('cat_id_seq')");
				ResultSet rs=st.executeQuery();
				rs.next();
				cat_id=rs.getInt(1);
				// OK, grant ourselves access to it.
				st=photo.prepareStatement("insert into wwwacl values(?,?)");
				st.setInt(1, ps.remote_uid.intValue());
				st.setInt(2, cat_id);
				st.executeUpdate();

				// Now, let's uncache any cached category lists we have.
				SpyCache cache=new SpyCache();
				// Get rid of anything that starts with catList_
				cache.uncacheLike("catList_");
			}
		} catch(Exception e) {
			se=new ServletException("Error in admEditCategoryForm", e);
		} finally {
			freeDBConn(photo);
		}
		if(se!=null) {
			throw(se);
		}
		// If we make it to the bottom, show the categories again
		return(admShowCategories());
	}

	// Save the image stuff
	protected void saveImageInfo() throws ServletException {
		if(!isAdmin()) {
			throw new ServletException("Must be an admin to do this.");
		}
		String keywords="", info="", taken="";
		String out="", stmp=null;
		int id=-1;
		int category=-1;

		// We need a short lifetime for whatever page this produces
		long l=new java.util.Date().getTime();
		l+=10000L;
		ps.response.setDateHeader("Expires", l);

		// Get the ID
		stmp=ps.request.getParameter("id");
		id=Integer.parseInt(stmp);

		// Get the category ID
		stmp=ps.request.getParameter("cat");
		category=Integer.parseInt(stmp);

		// Get the string data
		taken=ps.request.getParameter("taken");
		keywords=ps.request.getParameter("keywords");
		if (keywords == null) {
			debug ("keywords null, setting to blank string");
			keywords = "";
		}

		info=ps.request.getParameter("info");
		if (info == null) {
			debug ("info/desc null, setting to blank string");
			info = "";
		}

		Connection photo=null;
		try {
			photo=getDBConn();
			PreparedStatement st=photo.prepareStatement(
				"update album set cat=?, keywords=?, descr=?, taken=?\n"
				+ " where id=?"
				);
			st.setInt(1, category);
			st.setString(2, keywords);
			st.setString(3, info);
			st.setString(4, taken);
			st.setInt(5, id);
			st.executeUpdate();
		} catch(Exception e) {
			log("Error updating information:  " + e);
		} finally {
			freeDBConn(photo);
		}
	}

	// Shortcut to isAdmin();
	protected boolean isAdmin() {
		return(ps.isAdmin());
	}

	protected void debug(String msg) {
		if (debug) {
			log ("PhotoAdmin debug: " + msg);
		}
	}

	// Shortcut to tokenize()
	protected String tokenize(String what, Hashtable h)
		throws ServletException {

		String rv=PhotoUtil.tokenize(ps, what, h);

		if(rv==null) {
			throw new ServletException("Tokenizer returned null, perhaps the "
				+ " template " + what + " could not be found?");
		}
		return(rv);
	}
}
