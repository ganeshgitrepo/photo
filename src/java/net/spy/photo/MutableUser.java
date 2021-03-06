// Copyright (c) 2005  Dustin Sallings <dustin@spy.net>

package net.spy.photo;

import net.spy.db.Savable;

/**
 * Mutable user interface.
 */
public interface MutableUser extends Mutable, User, Savable {

	/** 
	 * Set the username.
	 */
	void setName(String to);

	/** 
	 * Set the real name of this user.
	 */
	void setRealname(String to);

	/** 
	 * Set the password for this user.
	 */
	void setPassword(String to);

	/** 
	 * Set the email address.
	 */
	void setEmail(String to);

	/** 
	 * Set the persistent session ID.
	 */
	void setPersess(String to);

	/** 
	 * Set the ``add'' flag for this user.
	 */
	void setCanAdd(boolean to);

	/** 
	 * Add a role.
	 */
	void addRole(String role);

	/** 
	 * Clear all of the roles.
	 */
	void clearRoles();
	
	/** 
	 * Remove a role for this user.
	 */
	void removeRole(String role);

}
