// Copyright (c) 2001  Dustin Sallings <dustin@spy.net>
//
// $Id: Category.java,v 1.9 2002/11/10 09:41:59 dustin Exp $

package net.spy.photo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

import net.spy.SpyDB;
import net.spy.db.DBSP;
import net.spy.cache.SpyCache;

import net.spy.photo.sp.GetAllCategories;
import net.spy.photo.sp.DeleteACLForCat;
import net.spy.photo.sp.InsertACLEntry;
import net.spy.photo.sp.InsertCategory;
import net.spy.photo.sp.UpdateCategory;
import net.spy.photo.sp.GetACLsForCategory;
import net.spy.photo.sp.GetGeneratedKey;

/**
 * Category representation.
 */
public class Category extends Object implements Comparable {

	/**
	 * Flag to list categories that can be read by the user.
	 */
	public static final int ACCESS_READ=1;
	/**
	 * Flag to list categories to which the user may add.
	 */
	public static final int ACCESS_WRITE=2;

	private static final String CACHE_KEY="photo_cat_map";
	private static final long CACHE_TIME=86400000;

	private int id=-1;
	private String name=null;

	private List acl=null;

	/**
	 * Get an instance of Category.
	 */
	public Category() {
		super();
		acl=new ArrayList();
	}

	private Category(ResultSet rs) throws SQLException {
		id=rs.getInt("id");
		name=rs.getString("name");
	}

	private static Map getCategoryMap() throws SQLException {
		Map rv=null;
		
		SpyCache sc=SpyCache.getInstance();

		rv=(Map)sc.get(CACHE_KEY);
		if (rv==null) {
			rv=new HashMap();

			GetAllCategories db=new GetAllCategories(new PhotoConfig());

			ResultSet rs=db.executeQuery();
			while(rs.next()) {
				Category cat=new Category(rs);

				// Map it by name and id
				rv.put(cat.getName(), cat);
				rv.put(new Integer(cat.getId()), cat);
			}

			db.close();

			// Now, flip through the categories and get their ACLs initified.
			loadACLs(rv.values());

			// remember it
			sc.store(CACHE_KEY, rv, CACHE_TIME);
		}

		return (rv);
	}

	/**
	 * Lookup a category by integer ID.
	 */
	public static Category lookupCategory(int catId) throws Exception {
		Map catMap=getCategoryMap();

		Category rv=(Category)catMap.get(new Integer(catId));

		if(rv == null) {
			throw new Exception("No such category:  " + catId);
		}

		return(rv);
	}

	/**
	 * Look up a category by name.
	 */
	public static Category lookupCategory(String catName) throws Exception {
		Map catMap=getCategoryMap();

		Category rv=(Category)catMap.get(catName);

		if(rv == null) {
			throw new Exception("No such category:  " + catName);
		}

		return(rv);
	}

	/** 
	 * Compare categories by name.
	 */
	public int compareTo(Object o) {
		Category cat=(Category)o;

		String thisName=getName().toLowerCase();
		String thatName=cat.getName().toLowerCase();

		return (thisName.compareTo(thatName));
	}

	/** 
	 * True if the categories have the same name.
	 */
	public boolean equals(Object o) {
		boolean rv=false;

		try {
			rv=compareTo(o) == 0;
		} catch(ClassCastException cce) {
			// Remain false
		}

		return (rv);
	}

	/** 
	 * Get the hash code.
	 * 
	 * @return the ID of this category
	 */
	public int hashCode() {
		return (getId());
	}

	// Overwrite the ACL entry
	private void setAcl(List to) {
		acl=to;
	}

	/**
	 * Load the ACLs for this Category instance.
	 */
	private static void loadACLs(Collection categories) throws SQLException {
		
		GetACLsForCategory db=new GetACLsForCategory(new PhotoConfig());

		for (Iterator i=categories.iterator(); i.hasNext(); ) {
			Category cat=(Category)i.next();

			db.setCat(cat.getId());
			ResultSet rs=db.executeQuery();

			// (re)set the ACL set for this category
			cat.setAcl(new ArrayList());

			while(rs.next()) {
				int uid=rs.getInt("userid");
				if(rs.getBoolean("canview")) {
					cat.addViewACLEntry(uid);
				}
				if(rs.getBoolean("canadd")) {
					cat.addAddACLEntry(uid);
				}
			}
			rs.close();
		}
		db.close();
	}

	/**
	 * Save this category and ACL entries.
	 */
	public void save() throws Exception {
		SpyDB db=new SpyDB(new PhotoConfig());
		Connection conn=null;
		try {
			conn=db.getConn();
			conn.setAutoCommit(false);

			DBSP saveCat=null;

			// What to do here depends on whether it's a new category or a
			// modification of an existing category.
			if(id>=0) {
				// Existing user
				saveCat=new UpdateCategory(conn);
				saveCat.set("id", id);
			} else {
				saveCat=new InsertCategory(conn);
			}

			// Set the common fields
			saveCat.set("name", name);
			// Save the category proper
			saveCat.executeUpdate();
			saveCat.close();
			saveCat=null;

			// If'n it's a new category, let's look up the ACL we just saved.
			if(id==-1) {
				GetGeneratedKey ggk=new GetGeneratedKey(conn);
				ggk.setSeq("cat_id_seq");
				ResultSet rs=ggk.executeQuery();
				if(!rs.next()) {
					throw new PhotoException(
						"No results returned while looking up new cat id");
				}
				id=rs.getInt(1);
				rs.close();
				ggk.close();
				System.err.println("New category:  " + id);
			}

			// OK, now deal with the ACLs

			// Out with the old
			DeleteACLForCat dacl=new DeleteACLForCat(conn);
			dacl.setCat(id);
			dacl.executeUpdate();
			dacl.close();
			dacl=null;

			// In with the new

			InsertACLEntry iacl=new InsertACLEntry(conn);

			for(Iterator i=getACLEntries().iterator(); i.hasNext(); ) {
				PhotoACLEntry aclEntry=(PhotoACLEntry)i.next();

				iacl.setUserId(aclEntry.getUid());
				iacl.setCatId(id);
				iacl.setCanView(aclEntry.canView());
				iacl.setCanAdd(aclEntry.canAdd());
				iacl.executeUpdate();
			}
			iacl.close();
			iacl=null;

			conn.commit();
		} catch(Exception e) {
			if(conn!=null) {
				conn.rollback();
			}
			throw e;
		} finally {
			if(conn!=null) {
				conn.setAutoCommit(true);
			}
			// Tell it we're done
			db.close();
		}
	}

	/**
	 * Get the ACL entries for this category.
	 */
	public Collection getACLEntries() {
		return(Collections.unmodifiableCollection(acl));
	}

	/**
	 * Get an ACL entry for the given user.
	 *
	 * @param userid the numeric user ID of the user to look up
	 *
	 * @return the entry, or null if there's no entry for the given user
	 */
	public PhotoACLEntry getACLEntryForUser(int userid) {
		PhotoACLEntry rv=null;

		for(Iterator i=getACLEntries().iterator(); rv==null && i.hasNext();) {
			PhotoACLEntry acl=(PhotoACLEntry)i.next();

			if(acl.getUid() == userid) {
				rv=acl;
			}
		}

		return(rv);
	}

	// Same as above, but create a new entry if there isn't one.
	private PhotoACLEntry getACLEntryForUser2(int userid) {
		PhotoACLEntry rv=getACLEntryForUser(userid);
		if(rv==null) {
			rv=new PhotoACLEntry(userid, getId());
			acl.add(rv);
		}
		return(rv);
	}

	/**
	 * Add an ACL entry permitting the given user ID to view this
	 * category.
	 */
	public void addViewACLEntry(int userid) {
		PhotoACLEntry aclEntry=getACLEntryForUser2(userid);
		aclEntry.setCanView(true);
	}

	/**
	 * Add an ACL entry permitting the given user ID to add to this
	 * category.
	 */
	public void addAddACLEntry(int userid) {
		PhotoACLEntry aclEntry=getACLEntryForUser2(userid);
		aclEntry.setCanAdd(true);
	}

	/**
	 * Remove an entry for a given user.
	 */
	public void removeACLEntry(int userid) {
		PhotoACLEntry entry=getACLEntryForUser(userid);
		if(entry!=null) {
			acl.remove(entry);
		}
	}

	/**
	 * Remove all ACL entries.
	 */
	public void removeAllACLEntries() {
		acl.clear();
	}

	private static SortedSet getInternalCatList(int uid, int access)
		throws PhotoException {

		boolean seeksRead=((access&ACCESS_READ)>0);
		boolean seeksWrite=((access&ACCESS_WRITE)>0);

		// Make sure some access method is given.
		if(access==0) {
			throw new PhotoException("No access method given.");
		}

		SortedSet rv=getAdminCatList();

		for(Iterator i=rv.iterator(); i.hasNext();) {
			Category cat=(Category)i.next();

			PhotoACLEntry aclE=cat.getACLEntryForUser(uid);

			boolean keep=false;
			// If we got an entry, see if it works for us.
			if(aclE != null) {
				if(seeksRead && aclE.canView()) {
					keep=true;
				}
				if(seeksWrite && aclE.canAdd()) {
					keep=true;
				}
			}

			if(!keep) {
				i.remove();
			}
		}

		return(rv);
	}

	/**
	 * Get a category list.
	 *
	 * @param uid The numeric UID of the user.
	 * @param access A bitmask describing the access required for the
	 * search (ord together).
	 */
	public static SortedSet getCatList(int uid, int access)
		throws PhotoException {

		// The set for this user
		SortedSet baseSet=getInternalCatList(uid, access);
		// The set for the default user
		SortedSet defSet=getInternalCatList(PhotoUtil.getDefaultId(), access);

		// Add all of the default set to the base set.
		baseSet.addAll(defSet);

		return(baseSet);
	} // getCatList(int,int)

	/**
	 * Get a Collection of all categories (for administrative actions).
	 * @return a new SortedSet of Categories.
	 * @throws PhotoException if it can't find the categories
	 */
	public static SortedSet getAdminCatList() throws PhotoException {
		SortedSet rv=null;
		try {
			Map catMap=getCategoryMap();
			rv=new TreeSet();
			rv.addAll(catMap.values());
		} catch(Exception e) {
			throw new PhotoException("Error getting admin category list", e);
		}
		return(rv);
	}

	/**
	 * Get the ID of this category.
	 */
	public int getId() {
		return(id);
	}

	/**
	 * Get the name of this category.
	 */
	public String getName() {
		return(name);
	}

	/**
	 * Set the name of this category.
	 */
	public void setName(String to) {
		this.name=to;
	}

	/**
	 * String me.
	 */
	public String toString() {
		StringBuffer sb=new StringBuffer(64);

		sb.append("{Category name=");
		sb.append(name);
		sb.append(", id=");
		sb.append(id);
		sb.append("}");

		return (sb.toString());
	}

	/**
	 * Testing and what not.
	 */
	public static void main(String args[]) throws Exception {
		Collection c=getCatList(Integer.parseInt(args[0]), ACCESS_READ);
		for(Iterator i=c.iterator(); i.hasNext(); ) {
			Category cat=(Category)i.next();
			System.out.println(cat);
		}
	}

}
