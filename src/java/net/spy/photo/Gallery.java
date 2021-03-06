// Copyright (c) 2001  Dustin Sallings <dustin@spy.net>

package net.spy.photo;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;

import net.spy.db.AbstractSavable;
import net.spy.db.SaveContext;
import net.spy.db.SaveException;
import net.spy.photo.sp.DeleteGalleryMappings;
import net.spy.photo.sp.GetGalleriesForUser;
import net.spy.photo.sp.GetGalleryForUser;
import net.spy.photo.sp.GetGeneratedKey;
import net.spy.photo.sp.InsertGallery;
import net.spy.photo.sp.InsertGalleryMap;
import net.spy.photo.sp.LookupGallery;
import net.spy.photo.sp.ModifyGallery;
import net.spy.photo.sp.UpdateGallery;

/**
 * A named collection of images.
 */
public class Gallery extends AbstractSavable implements Serializable {

	private int id=-1;
	private String name=null;
	private ArrayList<PhotoImage> images=null;
	private boolean isPublic=false;
	private User owner=null;
	private Date timestamp=null;

	/**
	 * Get an instance of Gallery belonging to a given owner.
	 */
	public Gallery(User o, String nm) {
		super();
		this.name=nm;
		this.owner=o;
		this.images=new ArrayList<PhotoImage>();

		setNew(true);
		setModified(false);
	}

	/**
	 * Get an instance of Gallery belonging to a given owner.
	 */
	public Gallery(User o) {
		this(o, null);
	}

	// Get the gallery for the current row in the given resultset
	private Gallery(ResultSet rs) throws SQLException, PhotoException {
		super();
		this.id=rs.getInt("gallery_id");
		this.name=rs.getString("gallery_name");
		this.owner=Persistent.getSecurity().getUser(
			rs.getInt("user_id"));
		this.isPublic=rs.getBoolean("ispublic");
		this.timestamp=rs.getTimestamp("ts");

		if(owner==null) {
			throw new PhotoException("User " + rs.getInt("wwwuser_id")
				+ "not found");
		}

		setNew(false);
		setModified(false);
	}

	/**
	 * Get a list of all galleries visible by the user.
	 */
	public static Cursor<Gallery> getGalleries(User user)
		throws PhotoException {

		Cursor<Gallery> rv=null;

		try {
			GetGalleriesForUser db=
				new GetGalleriesForUser(PhotoConfig.getInstance());
			db.setUserId(user.getId());
			ResultSet rs=db.executeQuery();
			rv=new Cursor<Gallery>();
			while(rs.next()) {
				Gallery g=new Gallery(rs);
				g.loadMap(user);
				rv.add(g);
			}
			rs.close();
			db.close();
		} catch(Exception e) {
			throw new PhotoException("Error getting gallery list", e);
		}

		return(rv);
	}

	private void loadMap(User user) throws Exception {
		LookupGallery lg=new LookupGallery(PhotoConfig.getInstance());
		lg.setGalleryId(id);
		lg.setCurrentUser(user.getId());
		lg.setDefaultUser(PhotoUtil.getDefaultId());

		ResultSet rs=lg.executeQuery();

		images=new ArrayList<PhotoImage>();
		while(rs.next()) {
			addImage(rs.getInt("album_id"));
		}

		rs.close();
		lg.close();
	}

	/**
	 * Get the given gallery by ID as it will be seen by the given user.
	 *
	 * @return the Gallery or null if there's no match.
	 */
	public static Gallery getGallery(User user, int id)
		throws PhotoException {

		Gallery rv=null;

		try {
			GetGalleryForUser db=
				new GetGalleryForUser(PhotoConfig.getInstance());
			db.setGalleryId(id);
			db.setUserId(user.getId());
			ResultSet rs=db.executeQuery();
			if(rs.next()) {
				rv=new Gallery(rs);
			}
			rs.close();
			db.close();

			// Load the map
			if(rv != null) {
				rv.loadMap(user);
			}

		} catch(Exception e) {
			throw new PhotoException("Couldn't look up gallery", e);
		}

		return(rv);
	}

	// Savable implementation

	/**
	 * Save a new gallery.
	 */
	public void save(Connection conn, SaveContext context)
		throws SaveException, SQLException {

		if(name==null) {
			throw new SaveException("Gallery name not provided.");
		}

		ModifyGallery db=null;

		// Different prepared statement for new vs. update
		if(isNew()) {
			db=new InsertGallery(conn);
		} else {
			db=new UpdateGallery(conn);
			((UpdateGallery)db).setGalleryId(id);
		}
		// Set the common fields
		db.setGalleryName(getName());
		db.setUserId(getOwner().getId());
		db.setIsPublic(isPublic());

		// Perform the update
		int affected=db.executeUpdate();

		// Make sure we didn't mess up
		if(affected!=1) {
			throw new SaveException(
				"Expected to affect one row, affected " + affected);
		}

		// Close the statement.
		db.close();

		// If it's a new record, get the ID
		if(isNew()) {
			GetGeneratedKey gkey=new GetGeneratedKey(conn);
			gkey.setSeq("galleries_gallery_id_seq");
			ResultSet rs=gkey.executeQuery();
			rs.next();
			id=rs.getInt("key");
			rs.close();
			gkey.close();
		}

		// Save the mappings

		// Delete any old mappings that might exist
		DeleteGalleryMappings dgm=new DeleteGalleryMappings(conn);
		dgm.setGalleryId(id);
		dgm.executeUpdate();
		dgm.close();

		// Load the new gallery
		InsertGalleryMap igm=new InsertGalleryMap(conn);
		igm.setGalleryId(id);

		for(PhotoImage pid : images) {
			igm.setAlbumId(pid.getId());
			affected=igm.executeUpdate();
			if(affected!=1) {
				throw new SaveException(
					"Expected to affect one row, affected " + affected);
			}
		}
	}

	/**
	 * Add a new image to the gallery.
	 */
	public void addImage(PhotoImage pid) {
		removeImage(pid);
		images.add(pid);
	}

	/**
	 * Add a new image to the gallery.
	 */
	public void addImage(int imageId) throws Exception {
		PhotoImageFactory pidf=PhotoImageFactory.getInstance();
		PhotoImage pid=pidf.getObject(imageId);
		addImage(pid);
	}

	/**
	 * String me.
	 */
	@Override
	public String toString() {
		return("{Gallery name=\"" + name + "\", id=" + id
			+ ", images:  " + images + "}");
	}

	/**
	 * Remove an image from the given gallery.
	 */
	public void removeImage(PhotoImage pid) {
		images.remove(pid);
	}

	/**
	 * Get this gallery's ID.
	 */
	public int getId() {
		return(id);
	}

	/**
	 * Get the name of this gallery.
	 */
	public String getName() {
		return(name);
	}

	/**
	 * Set the name of this Gallery.
	 */
	public void setName(String to) {
		this.name=to;
	}

	/**
	 * Get a Collection of PhotoImage objects describing the contents
	 * of this gallery.
	 */
	public Collection<PhotoImage> getImages() {
		return(Collections.unmodifiableCollection(images));
	}

	/**
	 * If true, the gallery is public.
	 */
	public boolean isPublic() {
		return(isPublic);
	}

	/**
	 * If true, the gallery is public.
	 */
	public void setPublic(boolean to) {
		this.isPublic=to;
	}

	/**
	 * Get the timestamp this gallery was created.
	 */
	public Date getTimestamp() {
		return(timestamp);
	}

	/**
	 * Get the owner of this gallery.
	 */
	public User getOwner() {
		return(owner);
	}

	/**
	 * Return the number of images in this gallery.
	 */
	public int getSize() {
		return(images.size());
	}

}
