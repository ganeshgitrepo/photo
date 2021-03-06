// Copyright (c) 2001  Dustin Sallings <dustin@spy.net>

package net.spy.photo.struts;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.spy.db.Saver;
import net.spy.photo.Comment;
import net.spy.photo.Persistent;
import net.spy.photo.PhotoConfig;
import net.spy.photo.PhotoSessionData;
import net.spy.photo.User;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.DynaActionForm;

/**
 * Action that submits a comment.
 */
public class CommentAction extends PhotoAction {

	/**
	 * Process the comment thing.
	 */
	@Override
	public ActionForward spyExecute(
		ActionMapping mapping, ActionForm form,
		HttpServletRequest request, HttpServletResponse response) 
		throws Exception {

		DynaActionForm df=(DynaActionForm)form;

		System.err.println("Processing comment:  " + df);

		// Get the session data
		PhotoSessionData sessionData=getSessionData(request);
		// Get the user
		User user=sessionData.getUser();

		Integer imageInteger=(Integer)df.get("imageId");
		int imageId=imageInteger.intValue();

		// Check permission
		Persistent.getSecurity().checkAccess(user, imageId);

		// Construct the comment.
		Comment comment=new Comment();
		comment.setUser(user);
		comment.setPhotoId(imageId);
		comment.setRemoteAddr(request.getRemoteAddr());
		comment.setNote( (String)df.get("comment") );

		Saver s=new Saver(PhotoConfig.getInstance());
		s.save(comment);

		ActionForward rv=null;

		// Get the configured forward
		ActionForward forward=mapping.findForward("next");
		// If we got one, modify it to include the image ID.
		if(forward!=null) {
			String path=forward.getPath();
			path+="?id=" + imageId;
			// Duplicate the forward since modifying it does bad things.
			rv=new ActionForward(path);
			rv.setName(forward.getName());
			rv.setRedirect(forward.getRedirect());
		}
		return(rv);
	}

}
