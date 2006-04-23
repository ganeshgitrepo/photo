// Copyright (c) 1999 Dustin Sallings
// arch-tag: 8755C444-5D6E-11D9-A26E-000A957659CC

package net.spy.photo.util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import net.spy.db.DBSPLike;
import net.spy.db.SpyDB;
import net.spy.jwebkit.SAXAble;
import net.spy.jwebkit.ThrowableElement;
import net.spy.jwebkit.XMLUtils;
import net.spy.photo.PhotoConfig;
import net.spy.photo.PhotoImage;
import net.spy.photo.PhotoImageDataFactory;
import net.spy.photo.PhotoImageHelper;
import net.spy.photo.sp.GetImagesToFlush;
import net.spy.util.Base64;
import net.spy.util.CloseUtil;
import net.spy.util.LoopingThread;
import net.spy.util.RingBuffer;

/**
 * Store images in the DB.	Uploaded images go directly into the cache and
 * are referenced in the album table.  They're usable without being stored,
 * and may in fact never be pulled from the DB for display unless the cache
 * is cleared.	It's quite important to make sure the images make it into
 * the database for long-term storage, however.
 */
public class PhotoStorerThread extends LoopingThread implements SAXAble {

	// chunks should be divisible by 57
	private static final int CHUNK_SIZE=2052;

	private int added=0;
	private int addNotifications=0;
	private AtomicBoolean flushPending=new AtomicBoolean(false);
	private int totalExceptions=0;
	private RingBuffer<Throwable> lastExceptions=null;
	private boolean flushing=false;

	private int flushes;

	/**
	 * Get a PhotoStorerThread.
	 */
	public PhotoStorerThread() {
		super("storer_thread");
		this.setDaemon(true);
		lastExceptions=new RingBuffer<Throwable>(10);
		setMsPerLoop(86400000);
	}

	private void recordException(Throwable t) {
		totalExceptions++;
		lastExceptions.add(t);
	}

	/**
	 * Invoked when a new image is added.
	 */
	public synchronized void addedImage() {
		addNotifications++;
		performFlush();
	}

	/**
	 * 
	 */
	public synchronized void performFlush() {
		flushPending.set(true);
		notifyAll();
	}

	/**
	 * Get the total number of images added.
	 */
	public int getAdded() {
		return added;
	}

	/**
	 * Get the total number of flushes performed.
	 */
	public int getFlushes() {
		return flushes;
	}

	/**
	 * Is a flush currently pending?
	 */
	public boolean getFlushPending() {
		return flushPending.get();
	}

	/**
	 * Get the number of images that have been requested to be added.
	 */
	public int getAddNotifications() {
		return addNotifications;
	}

	/**
	 * Get a snapshot of the last ten exceptions that occurred during
	 * processing.
	 */
	public Collection<Throwable> getLastExceptions() {
		return new ArrayList<Throwable>(lastExceptions);
	}

	/**
	 * Get the total number of exceptions that has occurred during flushing.
	 */
	public int getTotalExceptions() {
		return totalExceptions;
	}

	public String toString() {
		Throwable recentException=null;
		for(Throwable t : lastExceptions) {
			recentException=t;
		}
		return super.toString() + " - " + flushes + " flushes, "
			+ added + " adds, " + addNotifications + " add notifications, "
			+ totalExceptions + " total exceptions"
			+ (recentException != null ? " - latest exception: "
					+ recentException : "");
	}


	/**
	 * XMLificate.
	 */
	public void writeXml(ContentHandler h) throws SAXException {
		XMLUtils x=XMLUtils.getInstance();
		x.startElement(h, "storer");
		x.doElement(h, "flushes", String.valueOf(flushes));
		x.doElement(h, "added", String.valueOf(added));
		x.doElement(h, "notifications", String.valueOf(addNotifications));
		x.doElement(h, "totalExceptions", String.valueOf(totalExceptions));
		x.doElement(h, "flushing", String.valueOf(flushing));
		x.startElement(h, "exceptions");
		for(Throwable t : lastExceptions) {
			new ThrowableElement(t).writeXml(h);
		}
		x.endElement(h, "exceptions");
		x.endElement(h, "storer");
	}

	// Takes an imageId, pulls in the image from cache, and goes about
	// encoding it to put it into the database in a transaction.  The last
	// query in the transaction records the image having been stored.
	private void storeImage(int imageId) throws Exception {
		PhotoImageHelper p = new PhotoImageHelper(imageId);
		SpyDB pdb = new SpyDB(PhotoConfig.getInstance());
		PhotoImage pi = p.getImage();
		getLogger().info("Storer: Got image for " + imageId + " " 
			+ pi.size() + " bytes of data to store.");
		// This is an awkward way of doing this.
		ArrayList<byte[]> al=new ArrayList<byte[]>();
		byte data[]=pi.getData();

		// i will be incremented inside the loop
		for(int i=0; i<data.length; ) {
			// How much we have left
			int max=data.length-i;

			// Make sure we don't get more than 2k at a time.
			if(max>CHUNK_SIZE) {
				max=CHUNK_SIZE;
			}

			// Get the thing to store.
			byte b[]=new byte[max];
			for(int j=0; j<max; j++) {
				b[j]=data[i++];
			}

			al.add(b);
		}

		Connection db=null;
		try {
			int n=0;
			db = pdb.getConn();
			db.setAutoCommit(false);
			PreparedStatement pst=db.prepareStatement(
				"insert into image_store (id, line, data) values(?,?,?)");

			Base64 base64=new Base64();
			StringBuffer sdata = new StringBuffer();

			pst.setInt(1, imageId);

			// Get the encoded data
			for(byte b[] : al) {
				String tmp = base64.encode(b);
				tmp=tmp.trim();
				if(sdata.length() < CHUNK_SIZE) {
					sdata.append(tmp);
					sdata.append("\n");
				} else {
					pst.setInt(2, n++);
					pst.setString(3, sdata.toString());
					pst.executeUpdate();
					sdata=new StringBuffer(tmp);
				}
			}

			// OK, this is sick, but another one right now for the spare.
			if(sdata.length() > 0) {
				getLogger().debug("Storer:  Storing spare.");
				pst.setInt(2, n++);
				pst.setString(3, sdata.toString());
				pst.executeUpdate();
			}
			getLogger().debug("Storer:  Stored " + n + " lines of data for "
				+ imageId + ".");
			pst.close();
			pst=null;
			PreparedStatement pst2=db.prepareStatement(
				"update photo_logs set extra_info=text(now())\n"
					+ "  where photo_id = ?\n"
					+ "  and log_type = get_log_type('Upload')");
			pst2.setInt(1, imageId);
			int rows=pst2.executeUpdate();
			// We should update exactly one row.  We can live with 0, but
			// more than one could be bad.
			switch(rows) {
				case 0:
					getLogger().warn("WARNING:  No upload log entry was "
						+ "found for " + imageId);
					break;
				case 1:
					// Expected
					break;
				default:
					throw new Exception(
						"Expected to update 1 upload log entry , updated "
						+ rows);
			}
			db.commit();
			// Go ahead and generate a thumbnail.
			p.getThumbnail();
		} catch(Exception e) {
			// If anything happens, roll it back.
			getLogger().warn("Problem saving image", e);
			try {
				if(db!=null) {
					db.rollback();
				}
			} catch(Exception e3) {
				getLogger().warn("Problem rolling back transaction", e3);
			}
		} finally {
			if(db!=null) {
				try {
					db.setAutoCommit(true);
				} catch(Exception e) {
					getLogger().warn("Problem restoring autocommit", e);
				}
			}
			pdb.close();
		}
	}

	// Get a list of images that have been added, but not yet added into
	// the database.
	//
	// If decrement is true, this also decrements the addNotifications counter
	// so we can make sure we've done a run for each addition.
	//
	// Returns the number of records found in the DB needing to be stored.
	private int doFlush() {
		getLogger().info("Flushing");
		if(flushing) {
			getLogger().warn("Failed to clear previous flushing state");
		}
		flushes++;
		flushing=true;
		GetImagesToFlush db = null;
		ArrayList<Integer> ids = new ArrayList<Integer>();
		try {
			db=new GetImagesToFlush(PhotoConfig.getInstance());
			ResultSet rs=db.executeQuery();
			while(rs.next()) {
				ids.add(rs.getInt("image_id"));
			}
			rs.close();
		} catch(Exception e) {
			// Do nothing, we'll try again later.
			getLogger().warn("Exception while loading images to flush", e);
			recordException(e);
		} finally {
			CloseUtil.close((DBSPLike)db);
		}

		// Got the IDs, now store the actual images.  This is done so
		// that we don't hold the database connection open while we're
		// making the list *and* getting another database connection to act
		// on it.
		for(int i : ids) {
			try {
				storeImage(i);
				added++;
			} catch(Exception e) {
				getLogger().warn("Exception while storing images", e);
				recordException(e);
				// In the case of an exception, make sure we perform another
				// flush
				flushPending.set(true);
			}
		}

		getLogger().info("Flush complete:  " + ids.size() + " found.");
		flushing=false;

		// Return the number of flushable elements found.
		return ids.size();
	}

	/**
	 * When starting up, run an initial flush to catch up with anything that
	 * may not have ben flushed from a previous run.
	 */
	protected void startingUp() {
		try {
			// Sleep for at least one minute, at most five.
			long sleepTime = new Random().nextInt(240000) + 60000;
			getLogger().info(
					"Starting up...performing initial flush in %d seconds",
					(sleepTime / 1000));
			Thread.sleep(sleepTime);
			doFlush();
		} catch (Throwable t) {
			getLogger().warn("Initial run had an exception", t);
			recordException(t);
		}		
	}

	/**
	 * Run through as many flushes as we see fit.
	 */
	protected void runLoop() {
		getLogger().info("Beginning flush loop.");
		// Loop immediately until there's nothing left to flush.
		try {
			// Continue to go as long as addNotifications reports stuff.
			while(flushPending.getAndSet(false)) {
				// Sleep a bit before each flush to present it from looping
				// out of control.  This also gives us enough time to get the
				// recaching done before trying to pull image data in the
				// normal case.  If we don't make some, there'll be an exception
				// and we'll keep running until it goes away.
				Thread.sleep(PhotoImageDataFactory.RECACHE_DELAY + 5000);
				// Execute a flush.  We expect it to find at least one record
				// because a flush was pending when we entered here.  However,
				// there are two circumstances under which there would be
				// nothing to flush once it got here:
				// 1) The flush was requested before startingUp completed.
				// 2) A manual flush was requested.
				if(doFlush() == 0) {
					getLogger().warn("doFlush() found no images to flush");
				}
			} // Flush loop
		} catch(Throwable t) {
			recordException(t);
			getLogger().warn("Exception while flushing", t);
		}
		getLogger().info("Completed flush loop.");
	}
}
