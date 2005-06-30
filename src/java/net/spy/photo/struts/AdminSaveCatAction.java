// Copyright (c) 2001  Dustin Sallings <dustin@spy.net>
// arch-tag: 0E0A7866-5D6E-11D9-BB32-000A957659CC

package net.spy.photo.struts;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

import net.spy.photo.CategoryFactory;
import net.spy.photo.MutableCategory;

/**
 * Action used to save a category.
 */
public class AdminSaveCatAction extends PhotoAction {

	/**
	 * Get an instance of AdminSaveCatAction.
	 */
	public AdminSaveCatAction() {
		super();
	}

	/**
	 * Perform the action.
	 */
	public ActionForward spyExecute(ActionMapping mapping,
		ActionForm form,
		HttpServletRequest request,HttpServletResponse response)
		throws Exception {

		// Get the form
		AdminCategoryForm acf=(AdminCategoryForm)form;

		// Get the category
		CategoryFactory cf=CategoryFactory.getInstance();
		MutableCategory cat=null;
		try {
			cat=cf.getMutable(Integer.parseInt(acf.getCatId()));
		} catch(Exception e) {
			e.printStackTrace();
		}
		// If we didn't get a category, make a new one.
		if(cat==null) {
			cat=cf.createNew();
		}

		// OK, now set the new stuff
		cat.setName(acf.getName());

		// Set the ACLs

		// Get rid of the old.
		cat.getACL().removeAllEntries();

		// Add the view entries
		String acls[]=acf.getCatAclView();
		for(int i=0; i<acls.length; i++) {
			int uid=Integer.parseInt(acls[i]);
			cat.getACL().addViewEntry(uid);
		}
		// Add the add entries
		acls=acf.getCatAclAdd();
		for(int i=0; i<acls.length; i++) {
			int uid=Integer.parseInt(acls[i]);
			cat.getACL().addAddEntry(uid);
		}

		cf.persist(cat);

		return(mapping.findForward("next"));
	}

}
