// Copyright (c) 2001  Dustin Sallings <dustin@spy.net>
//
// $Id: AdminSelectUserAction.java,v 1.4 2002/06/22 23:27:45 dustin Exp $

package net.spy.photo.struts;

import java.io.*;
import java.util.*;

import javax.servlet.*;
import javax.servlet.http.*;

import org.apache.struts.action.*;

import net.spy.photo.*;

/**
 * Action used to begin editing a new user.
 */
public class AdminSelectUserAction extends AdminAction {

	/**
	 * Get an instance of AdminSelectUserAction.
	 */
	public AdminSelectUserAction() {
		super();
	}

	/**
	 * Perform the action.
	 */
	public ActionForward perform(ActionMapping mapping,
		ActionForm form,
		HttpServletRequest request,HttpServletResponse response)
		throws IOException, ServletException {

		// Verify the user is an admin
		checkAdmin(request);

		// Get the form
		AdminUserForm auf=(AdminUserForm)form;

		// Get the user id from the form
		int userid=Integer.parseInt(auf.getUserId());

		// The form can remain blank if the user id is -1 (new user)
		if(userid==-1) {
			// Empty user, fill it out with some defaults
			auf.setUsername("newuser");
			auf.setPassword("");
			auf.setRealname("");
			auf.setEmail("");
			auf.setCanadd(false);
			auf.setCatAclAdd(new String[0]);
			auf.setCatAclView(new String[0]);
		} else {
			// Look up the user
			PhotoSecurity sec=null;
			try {
				sec=new PhotoSecurity();
			} catch(Exception e) {
				throw new ServletException("Couldn't get security", e);
			}
			PhotoUser user=sec.getUser(userid);

			if(user==null) {
				throw new ServletException("No such user:  " + userid);
			}

			// Set the easy stuff
			auf.setUsername(user.getUsername());
			auf.setPassword(user.getPassword());
			auf.setRealname(user.getRealname());
			auf.setEmail(user.getEmail());
			auf.setCanadd(user.canAdd());

			// Populate the ACL stuff
			Vector viewable=new Vector();
			Vector addable=new Vector();
			for(Enumeration e=user.getACLEntries(); e.hasMoreElements();) {
				PhotoACLEntry acl=(PhotoACLEntry)e.nextElement();

				int id=acl.getCat();
				if(acl.canAdd()) {
					addable.addElement("" + id);
				}
				if(acl.canView()) {
					viewable.addElement("" + id);
				}
			}

			// Store them
			auf.setCatAclAdd((String[])addable.toArray(new String[0]));
			auf.setCatAclView((String[])viewable.toArray(new String[0]));
		}

		return(mapping.findForward("success"));
	}

}