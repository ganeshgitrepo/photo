// Copyright (c) 2004  Dustin Sallings <dustin@spy.net>

package net.spy.photo.impl;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.TreeSet;

import net.spy.db.AbstractSavable;
import net.spy.db.SaveContext;
import net.spy.db.SaveException;
import net.spy.db.SpyDB;
import net.spy.photo.AnnotatedRegion;
import net.spy.photo.CategoryFactory;
import net.spy.photo.Format;
import net.spy.photo.ImageServer;
import net.spy.photo.Keyword;
import net.spy.photo.KeywordFactory;
import net.spy.photo.Persistent;
import net.spy.photo.PhotoConfig;
import net.spy.photo.PhotoDimensions;
import net.spy.photo.PhotoException;
import net.spy.photo.PhotoImage;
import net.spy.photo.PhotoImageFactory;
import net.spy.photo.PhotoParser;
import net.spy.photo.PhotoUtil;
import net.spy.photo.Place;
import net.spy.photo.User;
import net.spy.photo.Votes;
import net.spy.photo.sp.DeleteAnnotations;
import net.spy.photo.sp.DeleteAnnotationsMap;
import net.spy.photo.sp.DeleteKeywordMap;
import net.spy.photo.sp.DeleteVariants;
import net.spy.photo.sp.InsertAnnotation;
import net.spy.photo.sp.InsertAnnotationKeyword;
import net.spy.photo.sp.InsertImage;
import net.spy.photo.sp.InsertKeywordMap;
import net.spy.photo.sp.InsertVariant;
import net.spy.photo.sp.UpdateImage;
import net.spy.photo.util.ReferenceTracker;
import net.spy.util.CloseUtil;

/**
 * Savable implementation of PhotoImage.
 */
public class SavablePhotoImage extends AbstractSavable implements PhotoImage {

	private Collection<AnnotatedRegion> annotations=null;
	private Collection<Keyword> keywords=null;
	private Collection<PhotoImage> variants=null;
	private Votes votes=null;
	private String descr=null;
	private String md5=null;
	private int catId=-1;
	private int size=-1;
	private PhotoDimensions dimensions=null;
	private PhotoDimensions tnDims=null;
	private User addedBy=null;
	private String catName=null;
	private Date taken=null;
	private Date timestamp=null;
	private int id=-1;
	private Format format=null;
	private byte[] imageData=null;
	private Place place=null;

	/**
	 * Get a new savable photo.
	 *
	 * The following attributes will be initialized:
	 * <ul>
	 *  <li>dimensions</li>
	 *  <li>size</li>
	 *  <li>format</li>
	 *  <li>timestamp</li>
	 *  <li>id</li>
	 * </ul>
	 *
	 * @param data the data for the image
	 * @throws PhotoException If a new photo id cannot be obtained
	 */
	public SavablePhotoImage(byte[] data) throws PhotoException {
		super();

		imageData=data;
		ReferenceTracker.getInstance().addObject(imageData);
		PhotoParser.Result res=PhotoParser.getInstance().parseImage(data);
		dimensions=new PhotoDimensionsImpl(res.getWidth(), res.getHeight());
		size=data.length;
		format=res.getFormat();
		md5=res.getMd5();
		timestamp=new Date();
		votes=new Votes();

		keywords=new TreeSet<Keyword>();
		annotations=new HashSet<AnnotatedRegion>();
		variants=new HashSet<PhotoImage>();

		id=getNewImageId();
		setNew(true);

		// this is a sort of ugly hack, but tell the image factory there's a
		// new image.
		// XXX:  Should probably just get SavablePhotoImage from
		// PhotoImageFactory.  Then it could all live there.
		PhotoImageFactory.getInstance().registerNewImage(id);
	}

	/** 
	 * Get an editable wrapper around the given PhotoImage.
	 */
	public SavablePhotoImage(PhotoImage proto) {
		super();

		this.keywords=proto.getKeywords();
		this.variants=new HashSet<PhotoImage>(proto.getVariants());
		getLogger().info("New savable image has %d variants", variants.size());
		this.annotations=proto.getAnnotations();
		this.votes=proto.getVotes();
		this.descr=proto.getDescr();
		this.catId=proto.getCatId();
		this.size=proto.getSize();
		this.dimensions=proto.getDimensions();
		this.tnDims=proto.getTnDims();
		this.addedBy=proto.getAddedBy();
		this.catName=proto.getCatName();
		this.taken=proto.getTaken();
		this.timestamp=proto.getTimestamp();
		this.id=proto.getId();
		this.format=proto.getFormat();
		this.place=proto.getPlace();
		this.md5=proto.getMd5();

		setNew(false);
		setModified(false);
	}

	private void saveNew(Connection conn) throws SaveException, SQLException {
		// Check access
		if(!addedBy.canAdd(catId)) {
			throw new SaveException("User " + addedBy + " cannot add to cat "
				+ catId);
		}
		InsertImage db=new InsertImage(conn);
		db.setImageId(id);
		db.setDescription(descr);
		db.setMd5(md5);
		db.setCatId(catId);
		db.setTaken(new java.sql.Date(taken.getTime()));
		db.setSize(size);
		db.setAddedBy(addedBy.getId());
		db.setTimestamp(new java.sql.Timestamp(timestamp.getTime()));
		db.setWidth(dimensions.getWidth());
		db.setHeight(dimensions.getHeight());
		db.setFormatId(format.getId());
		db.setPlaceId(place==null?null:place.getId());
		int aff=db.executeUpdate();
		if(aff != 1) {
			throw new SaveException("Expected to update 1 row, updated " + aff);
		}
		db.close();

		saveKeywords(conn);
		saveAnnotations(conn);
		saveVariants(conn);

		// Get the photo cached
		try {
			ImageServer server=Persistent.getImageServer();
			server.storeImage(this, imageData);
			// Kill the reference.
			imageData=null;
		} catch(Exception e) {
			throw new SaveException("Cannot cache photo", e);
		}
	}

	private void saveVariants(Connection conn) throws SQLException {
		DeleteVariants dv=null;
		InsertVariant iv=null;
		try {
			dv=new DeleteVariants(conn);
			dv.setOriginalId(id);
			dv.executeUpdate();
			CloseUtil.close(dv);
			dv=null;

			iv=new InsertVariant(conn);
			iv.setOriginalId(id);
			getLogger().info("Saving %d variants", variants.size());
			for(PhotoImage pid : variants) {
				iv.setVariantId(pid.getId());
				int aff=iv.executeUpdate();
				getLogger().info("Saved variant %d -> %d", id, pid.getId());
				assert aff == 1;
			}
		} finally {
			CloseUtil.close(dv);
			CloseUtil.close(iv);
		}
	}

	private void saveKeywords(Connection conn)
		throws SaveException, SQLException {

		InsertKeywordMap ikm=new InsertKeywordMap(conn);
		ikm.setPhotoId(id);

		for(Keyword k : keywords) {
			ikm.setWordId(k.getId());
			int aff=ikm.executeUpdate();
			if(aff != 1) {
				throw new SaveException("Expected to update 1 row, updated "
					+ aff);
			}
		}

		ikm.close();
	}

	private void saveAnnotations(Connection conn)
		throws SaveException, SQLException {
		InsertAnnotation ia=new InsertAnnotation(conn);
		ia.setPhotoId(id);

		for(AnnotatedRegion ar : annotations) {

			ia.setAnnotationId(ar.getId());
			ia.setTitle(ar.getTitle());
			ia.setX(ar.getX());
			ia.setY(ar.getY());
			ia.setWidth(ar.getWidth());
			ia.setHeight(ar.getHeight());
			ia.setUserId(ar.getUser().getId());
			ia.setTs(new java.sql.Timestamp(ar.getTimestamp().getTime()));

			int aff=ia.executeUpdate();
			if(aff != 1) {
				throw new SaveException("Expected to update 1 row, updated "
					+ aff);
			}

			InsertAnnotationKeyword iak=new InsertAnnotationKeyword(conn);
			iak.setAnnotationId(ar.getId());

			for(Keyword kw : ar.getKeywords()) {
				iak.setWordId(kw.getId());
				aff=iak.executeUpdate();
				if(aff != 1) {
					throw new SaveException("Expected to update 1 row, updated "
						+ aff);
				}
			}
			iak.close();
		}

		ia.close();
	}

	private void saveUpd(Connection conn) throws SaveException, SQLException {
		UpdateImage db=new UpdateImage(conn);
		db.setDescr(descr);
		db.setCat(catId);
		db.setTaken(new java.sql.Date(taken.getTime()));
		db.setId(id);
		db.setMd5(md5);
		db.setPlaceId(place==null?null:place.getId());
		int aff=db.executeUpdate();
		if(aff != 1) {
			throw new SaveException("Expected to update 1 row, updated " + aff);
		}
		db.close();

		// Delete the old keyword map
		DeleteKeywordMap dkm=new DeleteKeywordMap(conn);
		dkm.setPhotoId(id);
		dkm.executeUpdate();
		dkm.close();

		// Insert a new keyword map
		saveKeywords(conn);

		// Delete the old annotations
		DeleteAnnotationsMap dam=new DeleteAnnotationsMap(conn);
		dam.setPhotoId(id);
		dam.executeUpdate();
		dam.close();
		DeleteAnnotations da=new DeleteAnnotations(conn);
		da.setPhotoId(id);
		da.executeUpdate();
		da.close();

		// Insert a new keyword map
		saveAnnotations(conn);
		// Also save the variants.
		saveVariants(conn);
	}

	public void save(Connection conn, SaveContext ctx)
		throws SaveException, SQLException {

		if(isNew()) {
			saveNew(conn);
		} else {
			saveUpd(conn);
		}
	}

	/**
	 * Get a new ID for a photo.
	 */
	private static int getNewImageId() throws PhotoException {
		int rv=0;
		try {
			SpyDB db=new SpyDB(PhotoConfig.getInstance());
			ResultSet rs=db.executeQuery("select nextval('album_id_seq')");
			if(!rs.next()) {
				throw new PhotoException("No result for new album ID");
			}
			rv=rs.getInt(1);
			if(rs.next()) {
				throw new PhotoException("Too many results for new album ID");
			}
			rs.close();
			db.close();
		} catch(PhotoException e) {
			throw e;
		} catch(Exception e) {
			throw new PhotoException("Error getting image ID", e);
		}
		return(rv);
	}

	/** 
	 * Set the keywords from a keyword string.
	 */
	public void setKeywords(String kw) throws Exception {
		keywords.clear();
		KeywordFactory kf=KeywordFactory.getInstance();
		KeywordFactory.Keywords kws=kf.getKeywords(kw, true);
		keywords.addAll(kws.getPositive());

		// XXX: Remove any keywords from any annotations that aren't in the
		// keywords set
	}

	/** 
	 * Add an annotation.
	 * 
	 * @param x x position of the region
	 * @param y y position of the region
	 * @param width width of the region
	 * @param height height of the region
	 * @param kw keyword string
	 * @param title title of the region
	 * @param u user who's adding this annotation
	 * @throws Exception 
	 */
	public void addAnnotation(final int x, final int y,
		final int width, final int height,
		final String kw, final String title,
		final User u) throws Exception {

		int newId=PhotoUtil.getNewIdForSeq("region_region_id_seq");

		NewAnnotatedRegion nar=new NewAnnotatedRegion(newId, x, y,
			width, height, title, u);

		KeywordFactory kf=KeywordFactory.getInstance();
		KeywordFactory.Keywords kws=kf.getKeywords(kw, true);
		for(Keyword k : kws.getPositive()) {
			nar.addKeyword(k);
		}

		annotations.add(nar);
	}

	public void setKeywords(Collection<Keyword> to) {
		this.keywords=to;
	}

	public Collection<Keyword> getKeywords() {
		return(keywords);
	}

	public Collection<AnnotatedRegion> getAnnotations() {
		return(annotations);
	}

	public Votes getVotes() {
		return(votes);
	}

	public void setDescr(String to) {
		this.descr=to;
	}

	public String getDescr() {
		return(descr);
	}

	public void setCatId(int to) {
		catName=CategoryFactory.getInstance().getObject(to).getName();
		this.catId=to;
	}

	public int getCatId() {
		return(catId);
	}

	public void setSize(int to) {
		this.size=to;
	}

	public int getSize() {
		return(size);
	}

	public void setDimensions(PhotoDimensions to) {
		this.dimensions=to;
	}

	public PhotoDimensions getDimensions() {
		return(dimensions);
	}

	public void setTnDims(PhotoDimensions to) {
		this.tnDims=to;
	}

	public PhotoDimensions getTnDims() {
		return(tnDims);
	}

	public void setAddedBy(User to) {
		this.addedBy=to;
	}

	public User getAddedBy() {
		return(addedBy);
	}

	public String getCatName() {
		return(catName);
	}

	/** 
	 * Get the hash code for this object.
	 * 
	 * @return the id
	 */
	@Override
	public int hashCode() {
		return(id);
	}

	/**
	 * True if the given object is a PhotoImage object representing the
	 * same image.
	 */
	@Override
	public boolean equals(Object o) {
		boolean rv=false;

		if(o instanceof PhotoImage) {
			PhotoImage pid=(PhotoImage)o;

			if(id == pid.getId()) {
				rv=true;
			}
		}

		return(rv);
	}

	/** 
	 * Set the taken date from a string in one of the photo date formats.
	 */
	public void setTaken(String to) {
		Date d=PhotoUtil.parseDate(to);
		if(d == null) {
			throw new RuntimeException("Invalid date format:  " + to);
		}
		setTaken(d);
	}

	public void setTaken(Date to) {
		this.taken=to;
	}

	public Date getTaken() {
		return(taken);
	}

	public void setTimestamp(Date to) {
		this.timestamp=to;
	}

	public String getMd5() {
		return md5;
	}

	public void setMd5(String to) {
		md5=to;
	}

	public Date getTimestamp() {
		return(timestamp);
	}

	public void setId(int to) {
		this.id=to;
	}

	public int getId() {
		return(id);
	}

	public void setFormat(Format to) {
		this.format=to;
	}

	public Format getFormat() {
		return(format);
	}

	public Collection<PhotoImage> getVariants() {
		return variants;
	}

	public void addVariant(PhotoImage v) {
		getLogger().info("Adding variant:  %s", v);
		variants.add(v);
	}

	public void removeVariant(PhotoImage v) {
		getLogger().info("Removing variant:  %s", v);
		variants.remove(v);
	}

	public Map<String, String> getMetaData() {
		return(Collections.emptyMap());
	}

	public Place getPlace() {
		return place;
	}

	public void setPlace(Place to) {
		place=to;
	}

	// New annotated region implementation
	private static final class NewAnnotatedRegion extends AnnotatedRegionImpl {
		public NewAnnotatedRegion(final int id, final int x, final int y,
		        final int width, final int height, final String title, User u) {
			super();
			setId(id);
			setX(x);
			setY(y);
			setWidth(width);
			setHeight(height);
			setTitle(title);
			setUser(u);
			setTimestamp(new Date());
		}

		@Override
		public void addKeyword(Keyword k) {
			super.addKeyword(k);
		}
	}

}
