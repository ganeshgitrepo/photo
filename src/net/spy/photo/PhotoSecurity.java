/*
 * Copyright (c) 1999 Dustin Sallings <dustin@spy.net>
 *
 * $Id: PhotoSecurity.java,v 1.31 2003/01/07 09:38:50 dustin Exp $
 */

package net.spy.photo;

import java.security.MessageDigest;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

import java.util.Collection;
import java.util.ArrayList;
import java.util.List;

import net.spy.SpyDB;

import net.spy.cache.SpyCache;

import net.spy.util.Base64;

/**
 * Security dispatch type stuff happens here.
 */
public class PhotoSecurity extends PhotoHelper {

	/**
	 * Get a PhotoSecurity object.
	 */
	public PhotoSecurity() {
		super();
	}

	/**
	 * Get a digest for a given string.
	 */
	public String getDigest(String input) throws Exception {
		// We need the trim so we can ignore whitespace.
		byte dataB[]=input.trim().getBytes();
		PhotoConfig conf=getConfig();
		MessageDigest md = MessageDigest.getInstance(conf.get("cryptohash"));
		md.update(dataB);
		Base64 base64=new Base64();
		String out = base64.encode(md.digest());
		out = out.replace('+', '/');
		out=out.trim();
		// Get rid of = signs.
		while(out.endsWith("=")) {
			out=out.substring(0,out.length()-1);
		}
		return(out.trim());
	}

	// Get the default user
	PhotoUser getDefaultUser() {
		PhotoUser ret=null;

		// Get the username from the config
		String username=getConfig().get("default_user", "guest");

		try {
			ret=PhotoUser.getPhotoUser(username);
		} catch(PhotoUserException e) {
			// Ignore, return null
		}

		return(ret);
	}
	
	/**
	 * Get a user by integer ID.
	 */
	public PhotoUser getUser(int id) throws PhotoUserException {
		return(PhotoUser.getPhotoUser(id));
	}

	/** 
	 * Get a user by username or email address.
	 */
	public PhotoUser getUser(String spec) throws PhotoUserException {
		return(PhotoUser.getPhotoUser(spec));
	}

	/**
	 * The preferred way to check a user's access to an image.
	 */
	public static void checkAccess(PhotoUser user, int imageId) throws
		Exception {

		boolean ok=false;

		try {
			PhotoImageData pid=PhotoImageData.getData(imageId);
			ok=user.canView(pid.getCatId());
			
			if(!ok) {
				PhotoUser u=PhotoUtil.getDefaultUser();
				ok=u.canView(pid.getCatId());
			}
		} catch(Exception e) {
			// Will return false
			e.printStackTrace();
		}

		if(!ok) {
			throw new Exception("Access to image " + imageId
				+ " is not allowed by user " + user);
		}
	}

	/**
	 * Check to see if the given uid has access to the given image ID.
	 *
	 * <b>Note</b>:  This is not generally the right way to do this.
	 */
	public static void checkAccess(int uid, int imageId) throws Exception {
		PhotoSecurity sec=new PhotoSecurity();
		PhotoUser u=sec.getUser(uid);

		checkAccess(u, imageId);
	}

	/**
	 * List all users in alphabetical order by username.
	 *
	 * @return an List of PhotoUser objects
	 */
	public static Collection getAllUsers() throws Exception {
		return(PhotoUser.getAllPhotoUsers());
	}
}