// Copyright (c) 1999  Dustin Sallings
//
// $Id: PhotoUser.java,v 1.31 2003/05/10 08:44:39 dustin Exp $

// This class stores an entry from the wwwusers table.
package net.spy.photo;

import java.io.Serializable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.SQLException;

import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.SortedSet;
import java.util.HashSet;
import java.util.TreeSet;
import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;

import net.spy.SpyDB;
import net.spy.cache.SpyCache;
import net.spy.db.DBSP;
import net.spy.db.AbstractSavable;
import net.spy.db.SaveException;
import net.spy.db.SaveContext;

import net.spy.photo.sp.ModifyUser;
import net.spy.photo.sp.InsertUser;
import net.spy.photo.sp.UpdateUser;
import net.spy.photo.sp.DeleteACLForUser;
import net.spy.photo.sp.InsertACLEntry;
import net.spy.photo.sp.GetGeneratedKey;
import net.spy.photo.sp.GetAllUsers;
import net.spy.photo.sp.GetACLForUser;

/**
 * Represents a user in the photo system.
 */
public class PhotoUser extends AbstractSavable
	implements Comparable, Serializable {

	private int id=-1;
	private String username=null;
	private String password=null;
	private String email=null;
	private String realname=null;
	private boolean canadd=false;
	private String persess=null;

	private ArrayList acl=null;
	private Set groups=null;

	private static final String CACHE_KEY="n.s.p.PhotoUserMap";
	private static final int CACHE_TIME=3600000; // one hour

	/**
	 * Get a new, empty user.
	 */
	public PhotoUser() {
		super();
		acl=new ArrayList();
		setNew(true);
		setModified(false);
	}

	// Get the user represented by the current row of this result set
	private PhotoUser(ResultSet rs) throws SQLException, PhotoException {
		this();
		setId(rs.getInt("id"));
		setUsername(rs.getString("username"));
		setPassword(rs.getString("password"));
		setEmail(rs.getString("email"));
		setRealname(rs.getString("realname"));
		canAdd(rs.getBoolean("canadd"));
		setPersess(rs.getString("persess"));

		setNew(false);
		setModified(false);
	}

	private void initACLs(int defaultId) throws PhotoUserException {
		try {
			GetACLForUser db=new GetACLForUser(new PhotoConfig());
			db.setUserId(getId());
			db.setDefaultUserId(defaultId);

			ResultSet rs=db.executeQuery();
			while(rs.next()) {
				int cat=rs.getInt("cat");
				if(rs.getBoolean("canview")) {
					addViewACLEntry(cat);
				}
				if(rs.getBoolean("canadd")) {
					addAddACLEntry(cat);
				}
			}
			rs.close();
			db.close();
		} catch(SQLException e) {
			throw new PhotoUserException("Error initializing ACLs", e);
		}
	}

	private static Map initUserMap() throws PhotoUserException {
		PhotoConfig conf=new PhotoConfig();
		Map rv=new HashMap();
		try {
			List users=new ArrayList();

			GetAllUsers db=new GetAllUsers(conf);
			ResultSet rs=db.executeQuery();
			while(rs.next()) {
				PhotoUser pu=new PhotoUser(rs);
				pu.setNew(false);
				pu.setModified(false);

				// Add it to the list so we can initialize the ACLs.
				users.add(pu);

				// Map the various thingies.
				rv.put(new Integer(pu.getId()), pu);
				rv.put(pu.getUsername().toLowerCase(), pu);
				rv.put(pu.getEmail().toLowerCase(), pu);
				// Map by persistent ID.
				String psid=pu.getPersess();
				if(psid != null) {
					rv.put(psid, pu);
				}
			}
			rs.close();
			db.close();

			// Find the default user so we can initialize the ACLs.
			String defUsername=conf.get("default_user", "guest");
			PhotoUser defaultUser=(PhotoUser)rv.get(defUsername);
			if(defaultUser==null) {
				throw new PhotoUserException("Default user not found.");
			}

			// Now that the base stuff is initialized, initialize the ACLs.
			for(Iterator i=users.iterator(); i.hasNext();) {
				PhotoUser pu=(PhotoUser)i.next();
				pu.initACLs(defaultUser.getId());
			}

		} catch(SQLException e) {
			throw new PhotoUserException("Problem initializing user map", e);
		} catch(PhotoException e) {
			throw new PhotoUserException("Problem initializing user map", e);
		}
		return(rv);
	}

	private static Map getUserMap() throws PhotoUserException {
		SpyCache sc=SpyCache.getInstance();
		Map rv=(Map)sc.get(CACHE_KEY);
		if(rv==null) {
			rv=initUserMap();
			sc.store(CACHE_KEY, rv, CACHE_TIME);
		}

		return(rv);
	}

	/** 
	 * Look up a user by name or email address.
	 * 
	 * @param spec the username or email address
	 * @return the user
	 *
	 * @throws NoSuchPhotoUserException if the user does not exist
	 * @throws PhotoUserException if there's a problem looking up the user
	 */
	public static PhotoUser getPhotoUser(String spec)
		throws PhotoUserException {

		if(spec==null) {
			throw new NoSuchPhotoUserException("There is no null user.");
		}

		Map m=getUserMap();
		PhotoUser rv=(PhotoUser)m.get(spec.toLowerCase());
		if(rv==null) {
			throw new NoSuchPhotoUserException("No such user:  " + spec);
		}

		return(rv);
	}

	/** 
	 * Look up a user by user ID.
	 * 
	 * @param id the user ID
	 * @return the user
	 *
	 * @throws NoSuchPhotoUserException if the user does not exist
	 * @throws PhotoUserException if there's a problem looking up the user
	 */
	public static PhotoUser getPhotoUser(int id)
		throws PhotoUserException {

		Map m=getUserMap();
		PhotoUser rv=(PhotoUser)m.get(new Integer(id));
		if(rv==null) {
			throw new NoSuchPhotoUserException("No such user (id):  " + id);
		}

		return(rv);
	}

	/** 
	 * Get all known users.
	 * @return an unmodifiable sorted set of users
	 * @throws PhotoUserException 
	 */
	public static SortedSet getAllPhotoUsers() throws PhotoUserException {
		Map m=getUserMap();
		SortedSet rv=new TreeSet();
		rv.addAll(m.values());
		return(Collections.unmodifiableSortedSet(rv));
	}

	/** 
	 * Uncache and recache the users.
	 */
	public static void recache() throws PhotoUserException {
		//  Uncache
		SpyCache sc=SpyCache.getInstance();
		sc.uncache(CACHE_KEY);
		// This will cause a recache.
		getUserMap();
	}

	/**
	 * String me.
	 */
	public String toString() {
		StringBuffer sb=new StringBuffer(64);

		sb.append("{PhotoUser: username=");
		sb.append(username);
		sb.append("}");

		return (sb.toString());
	}

	/** 
	 * Hashcode of this object.
	 * 
	 * @return the hashcode of the username
	 */
	public int hashCode() {
		return(username.hashCode());
	}

	/** 
	 * True if these objects are equal.
	 * 
	 * @param o the other object
	 * @return true if o is a PhotoUser with the same ID
	 */
	public boolean equals(Object o) {
		boolean rv=false;
		if(o instanceof PhotoUser) {
			PhotoUser u=(PhotoUser)o;
			rv= (id == u.id);
		}
		return(rv);
	}

	/** 
	 * Compare these users by username.
	 * 
	 * @param o the object to compare to
	 * @return this.username.compareTo(o.username)
	 */
	public int compareTo(Object o) {
		PhotoUser pu=(PhotoUser)o;
		int rv=username.compareTo(pu.username);
		return(rv);
	}

	/**
	 * Get the username.
	 */
	public String getUsername() {
		return(username);
	}

	/**
	 * Get the user's E-mail address.
	 */
	public String getEmail() {
		return(email);
	}

	/** 
	 * Set the persistent session ID.
	 */
	public void setPersess(String persess) {
		this.persess=persess;
		setModified(true);
	}

	/** 
	 * Get the persistent session ID.
	 * 
	 * @return 
	 */
	public String getPersess() {
		return(persess);
	}

	/**
	 * Get the ACL list.
	 */
	public Collection getACLEntries() {
		return(Collections.unmodifiableCollection(acl));
	}

	/**
	 * Get an ACL entry for the given category.
	 *
	 * @param cat the category ID for which you want the entry
	 *
	 * @return the entry, or null if there is no entry for the given cat.
	 */
	public PhotoACLEntry getACLEntryForCat(int cat) {
		PhotoACLEntry rv=null;

		for(Iterator i=getACLEntries().iterator(); rv==null && i.hasNext();) {
			PhotoACLEntry acl=(PhotoACLEntry)i.next();

			if(acl.getCat() == cat) {
				rv=acl;
			}
		}

		return(rv);
	}

	/**
	 * Same as above, but create a new (empty) one if it doesn't exist
	 */
	private PhotoACLEntry getACLEntryForCat2(int cat) {
		PhotoACLEntry rv=getACLEntryForCat(cat);

		if(rv==null) {
			rv=new PhotoACLEntry(getId(), cat);
			acl.add(rv);
		}

		return(rv);
	}

	/**
	 * Add an ACL entry permitting view access to a given category.
	 */
	public void addViewACLEntry(int cat) {
		PhotoACLEntry aclEntry=getACLEntryForCat2(cat);
		aclEntry.setCanView(true);
		setModified(true);
	}

	/**
	 * Add an ACL entry permitting add access to a given category.
	 */
	public void addAddACLEntry(int cat) {
		PhotoACLEntry aclEntry=getACLEntryForCat2(cat);
		aclEntry.setCanAdd(true);
		setModified(true);
	}

	/**
	 * Remove an ACL entry.
	 */
	public void removeACLEntry(int cat) {
		PhotoACLEntry entry=getACLEntryForCat(cat);
		if(entry!=null) {
			acl.remove(entry);
		}
		setModified(true);
	}

	/**
	 * Remove all ACL entries.
	 */
	public void removeAllACLEntries() {
		acl.clear();
		setModified(true);
	}

	/**
	 * Get a Collection of Strings describing all the groups this user is
	 * in.
	 */
	public Collection getGroups() {
		if(groups==null) {
			initGroups();
		}
		return(Collections.unmodifiableSet(groups));
	}

	/**
	 * True if the user is in the given group.
	 */
	public boolean isInGroup(String groupName) {
		if(groups==null) {
			initGroups();
		}
		return(groups.contains(groupName));
	}

	// Initialize the groups
	private synchronized void initGroups() {
		if(groups==null) {
			try {
				HashSet s=new HashSet();
				SpyDB db=new SpyDB(new PhotoConfig());
				PreparedStatement st=db.prepareStatement(
					"select * from show_group where username = ?");
				st.setString(1, getUsername());
				ResultSet rs=st.executeQuery();
				while(rs.next()) {
					s.add(rs.getString("groupname"));
				}
				rs.close();
				st.close();
				db.close();
				groups=s;
			} catch(Exception e) {
				// Spill your guts.
				e.printStackTrace();
			}
		}
	}

	/**
	 * True if the user can add.
	 */
	public boolean canAdd() {
		return(canadd);
	}

	/**
	 * True if the user can add to the specific category.
	 */
	public boolean canAdd(int cat) {
		boolean rv=false;
		PhotoACLEntry acl=getACLEntryForCat(cat);
		if(acl!=null && acl.canAdd()) {
			rv=true;
		}
		return(rv);
	}

	/**
	 * True if the user can view images in the specific category.
	 */
	public boolean canView(int cat) {
		boolean rv=false;
		PhotoACLEntry acl=getACLEntryForCat(cat);
		if(acl!=null && acl.canView()) {
			rv=true;
		}
		return(rv);
	}

	/**
	 * Set the user's addability.
	 */
	public void canAdd(boolean canadd) {
		this.canadd=canadd;
	}

	/**
	 * Get the ID of this user.
	 */
	public int getId() {
		return(id);
	}

	/**
	 * Set the ID of this user.
	 */
	public void setId(int id) {
		this.id=id;
		setModified(true);
	}

	/**
	 * Set the username of this user.
	 */
	public void setUsername(String username) {
		this.username=username.toLowerCase();
		setModified(true);
	}

	/**
	 * Set the real name of this user.
	 */
	public void setRealname(String realname) {
		this.realname=realname;
		setModified(true);
	}

	/**
	 * Get the real name of this user.
	 */
	public String getRealname() {
		return(realname);
	}

	/**
	 * Set the E-mail address of this  user.
	 */
	public void setEmail(String email) {
		this.email=email.toLowerCase();
		setModified(true);
	}

	// Savable implementation

	/**
	 * Save the user.
	 */
	public void save(Connection conn, SaveContext context)
		throws SaveException, SQLException {

		ModifyUser db=null;

		// Determine whether this is a new user or not.
		if(isNew()) {
			db=new InsertUser(conn);
		} else {
			db=new UpdateUser(conn);
			((UpdateUser)db).setUserId(getId());
		}

		// Set the common fields and update.
		db.setUsername(username);
		db.setRealname(realname);
		db.setEmail(email);
		db.setPassword(password);
		db.setCanadd(canadd);
		db.setPersess(persess);

		db.executeUpdate();
		db.close();
		db=null;

		// For new users, We need to fetch the ID
		if(id==-1) {
			GetGeneratedKey gkey=new GetGeneratedKey(conn);
			gkey.setSeq("wwwusers_id_seq");
			ResultSet rs=gkey.executeQuery();
			rs.next();
			id=rs.getInt("key");
			rs.close();
			gkey.close();
		}

		// OK, now let's save the ACL.

		// First, out with the old.
		DeleteACLForUser dacl=new DeleteACLForUser(conn);
		dacl.setUserId(getId());
		dacl.executeUpdate();
		dacl.close();

		// Then in with the new.
		InsertACLEntry ins=new InsertACLEntry(conn);
		ins.setUserId(getId());

		for(Iterator i=getACLEntries().iterator(); i.hasNext(); ) {
			PhotoACLEntry aclEntry=(PhotoACLEntry)i.next();

			ins.setCatId(aclEntry.getCat());
			ins.setCanView(aclEntry.canView());
			ins.setCanAdd(aclEntry.canAdd());
			ins.executeUpdate();
		}
		ins.close();

		setSaved();
	}

	/**
	 * Set the user's password.
	 */
	public void setPassword(String pass) throws PhotoException {
		// Make sure the password is hashed
		if(pass.length()<13) {
			PhotoSecurity security=new PhotoSecurity();
			try {
				pass=security.getDigest(pass);
			} catch(Exception e) {
				throw new PhotoException("Error digesting password", e);
			}
		}
		this.password=pass;
		setModified(true);
	}

	/**
	 * Get the user's hashed password.	Useful for administration forms and
	 * stuff.
	 */
	public String getPassword() {
		return(password);
	}

	/**
	 * Check the user's password.
	 */
	public boolean checkPassword(String pass) {
		boolean ret=false;
		try {
			PhotoSecurity security=new PhotoSecurity();
			String tpw=security.getDigest(pass);
			ret=tpw.equals(password);
		} catch(Exception e) {
			// Let it return false.
		}
		return(ret);
	}

	/**
	 * XML the user.
	 */
	public String toXML() {
		StringBuffer sb=new StringBuffer(128);

		sb.append("<photo_user>\n");

		sb.append("\t<id>");
		sb.append(id);
		sb.append("</id>\n");

		sb.append("\t<username>");
		sb.append(username);
		sb.append("</username>\n");

		sb.append("\t<email>");
		sb.append(email);
		sb.append("</email>\n");

		sb.append("\t<realname>");
		sb.append(realname);
		sb.append("</realname>\n");

		if(canadd) {
			sb.append("\t<canadd/>\n");
		}

		sb.append("</photo_user>\n");

		return(sb.toString());
	}
}