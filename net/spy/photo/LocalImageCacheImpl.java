// Copyright (c) 2001  Dustin Sallings <dustin@spy.net>
//
// $Id: LocalImageCacheImpl.java,v 1.1 2002/06/17 03:52:32 dustin Exp $

package net.spy.photo;

import java.io.*;

import net.spy.cache.*;

/**
 * ImageCache that uses local files.
 */
public class LocalImageCacheImpl extends Object implements ImageCache {

	private DiskCache cache=null;

	/**
	 * Get an instance of LocalImageCacheImpl.
	 */
	public LocalImageCacheImpl() {
		super();

		PhotoConfig conf=new PhotoConfig();
		cache=new DiskCache(conf.get("cache.dir", "/var/tmp/photocache"));
	}

	/**
	 * @see ImageCache
	 */
	public PhotoImage getImage(String key) throws PhotoException {
		PhotoImage rv=(PhotoImage)cache.getObject(key);
		return(rv);
	}

	/**
	 * @see ImageCache
	 */
	public void putImage(String key, PhotoImage image) throws PhotoException {
		try {
			cache.storeObject(key, image);
		} catch(IOException e) {
			throw new PhotoException("Error storing image", e);
		}
	}

}
