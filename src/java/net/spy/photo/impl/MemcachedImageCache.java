package net.spy.photo.impl;

import java.net.InetSocketAddress;
import java.util.List;

import net.spy.SpyObject;
import net.spy.memcached.AddrUtil;
import net.spy.memcached.MemcachedClient;
import net.spy.memcached.SerializingTranscoder;
import net.spy.photo.ImageCache;
import net.spy.photo.Persistent;
import net.spy.photo.PhotoConfig;
import net.spy.photo.PhotoException;
import net.spy.photo.ShutdownHook;
import net.spy.stat.ComputingStat;
import net.spy.stat.Stats;

/**
 * ImageCache implementation that uses Memcached.
 */
public class MemcachedImageCache extends SpyObject
	implements ImageCache, ShutdownHook {

	private MemcachedClient memcached=null;
	private int expirationTime=3600;
	private String prefix=null;

	private ComputingStat hitStat=null;
	private ComputingStat missStat=null;

	public MemcachedImageCache() {
		super();
		try {
			prefix=Persistent.getContextPath();
			PhotoConfig conf=PhotoConfig.getInstance();
			int gzipsize=conf.getInt("memcached.transcoder.gzipsize",
					Integer.MAX_VALUE);
			getLogger().info("Compression threshold:  %d", gzipsize);
			SerializingTranscoder transcoder = new SerializingTranscoder();
			transcoder.setCompressionThreshold(gzipsize);

			int bufSize=conf.getInt("memcached.bufsize",
					MemcachedClient.DEFAULT_BUF_SIZE);
			getLogger().info("Buffer size:  %d", bufSize);

			expirationTime=conf.getInt("memcached.cachetime", 3600);
			getLogger().info("Cache time:  %d", expirationTime);

			List<InetSocketAddress> addrs=AddrUtil.getAddresses(
					conf.get("memcached.servers"));

			memcached=new MemcachedClient(bufSize, addrs);
			memcached.setTranscoder(transcoder);

			hitStat=Stats.getComputingStat("memcached.hit");
			missStat=Stats.getComputingStat("memcached.miss");

			Persistent.addShutdownHook(this);
		} catch(Exception e) {
			getLogger().info("Error initializing memcached cache", e);
		}
	}

	public byte[] getImage(String key) throws PhotoException {
		byte[] rv=null;
		if(memcached != null) {
			long start=System.currentTimeMillis();
			rv=(byte[])memcached.get(prefix + "/" + key);
			long end=System.currentTimeMillis();
			// Add our stats.
			(rv==null?missStat:hitStat).add(end-start);
			// Log if we got something.
			if(rv != null) {
				getLogger().debug("Got %s from memcached (%d bytes)", key,
						rv.length);
			}
		}
		return rv;
	}

	public void putImage(String key, byte[] image) throws PhotoException {
		if(memcached != null) {
			String k=prefix + "/" + key;
			memcached.add(k, expirationTime, image);
			getLogger().debug("Memcached stored %s", k);
		} else {
			getLogger().debug("No memcached, can't store %s", key);
		}
	}

	public void onShutdown() {
		if(memcached != null) {
			memcached.shutdown();
		}
	}

}