/*
 * Copyright (c) 1999 Dustin Sallings
 *
 * $Id: PhotoCache.java,v 1.1 2000/07/05 01:03:41 dustin Exp $
 */

package net.spy.photo;

import java.util.*;

public class PhotoCache extends Object {
	protected static Hashtable cacheStore=null;
	protected static PhotoCacheCleaner cacheCleaner=null;

	public PhotoCache() {
		super();

		init();
	}

	public void store(Object key, Object value, long cache_time) {
		PhotoCacheItem i=new PhotoCacheItem(key, value, cache_time);
		synchronized(cacheStore) {
			cacheStore.put(key, i);
		}
	}

	public Object get(Object key) {
		Object ret=null;
		synchronized(cacheStore) {
			PhotoCacheItem i=(PhotoCacheItem)cacheStore.get(key);
			if(i!=null && (!i.expired())) {
				ret=i.getObject();
			}
		}
		return(ret);
	}

	protected synchronized void init() {
		if(cacheStore==null) {
			cacheStore=new Hashtable();
		}

		if(cacheCleaner==null) {
			cacheCleaner=new PhotoCacheCleaner(cacheStore);
			cacheCleaner.setDaemon(true);
			cacheCleaner.start();
		}
	}
}