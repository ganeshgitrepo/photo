// Copyright (c) 2001  Dustin Sallings <dustin@spy.net>
//
// $Id: PhotoACLEntry.java,v 1.4 2002/11/04 03:11:24 dustin Exp $

package net.spy.photo;

/**
 * An ACL entry.
 */
public class PhotoACLEntry extends Object implements java.io.Serializable {

	private int what=0;
	private boolean canview=false;
	private boolean canadd=false;

	/**
	 * Get an instance of PhotoACLEntry.
	 * @param id the user id or category or to whatever this ACL is relevant
	 */
	public PhotoACLEntry(int id) {
		super();
		this.what=what;
	}

	/** 
	 * String me.
	 */
	public String toString() {
		StringBuffer sb=new StringBuffer(64);

		sb.append("{PhotoACLEntry what=");
		sb.append(what);
		sb.append(", canview=");
		sb.append(canview);
		sb.append(", canadd=");
		sb.append(canadd);
		sb.append("}");

		return (sb.toString());
	}

	/**
	 * True for PhotoACLEntry instances that reference the same ``what''
	 * regardless of flags.
	 */
	public boolean equals(Object o) {
		boolean rv=false;

		if(o instanceof PhotoACLEntry) {
			PhotoACLEntry entry=(PhotoACLEntry)o;
			rv = (what==entry.what);
		}

		return(rv);
	}

	/** 
	 * Get the hash code of this object.
	 */
	public int hashCode() {
		return (what);
	}

	/**
	 * Get the id of the thing to which this category points.
	 */
	public int getWhat() {
		return(what);
	}

	/**
	 * Does this ACL entry permit adding?
	 */
	public boolean canAdd() {
		return(canadd);
	}

	/**
	 * Does this ACL entry permit viewing?
	 */
	public boolean canView() {
		return(canview);
	}

	/**
	 * Set the CanView value of this ACL entry.
	 */
	public void setCanView(boolean to) {
		this.canview=to;
	}

	/**
	 * Set the CanAdd value of this ACL entry.
	 */
	public void setCanAdd(boolean to) {
		this.canadd=to;
	}

}
