// Copyright (c) 2001  Dustin Sallings <dustin@spy.net>
//
// $Id: Comment.java,v 1.11 2002/07/10 04:00:17 dustin Exp $

package net.spy.photo;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import net.spy.SpyDB;

import net.spy.photo.sp.FindImagesByComments;

/**
 * Comments on photos.
 */
public class Comment extends Object implements java.io.Serializable {

	private int commentId=-1;

	private PhotoUser user=null;

	private int photoId=-1;
	private String note=null;
	private String remoteAddr=null;
	private Timestamp timestamp=null;
	private String timestampString=null;

	/**
	 * Get an instance of Comment.
	 */
	public Comment() {
		super();
		timestamp=new Timestamp(System.currentTimeMillis());
		timestampString=timestamp.toString();
	}

	// Get a comment from a result row
	private Comment(PhotoSecurity sec, ResultSet rs) throws Exception {
		super();

		commentId=rs.getInt("comment_id");
		photoId=rs.getInt("photo_id");
		note=rs.getString("note");
		timestampString=rs.getString("ts");
		remoteAddr=rs.getString("remote_addr");
		user=sec.getUser(rs.getInt("wwwuser_id"));
	}

	/**
	 * Get a Collection of Comment objects for all of the comments on a
	 * given image.
	 */
	public static Collection getCommentsForPhoto(int imageId)
		throws Exception {

		PhotoSecurity security=new PhotoSecurity();
		ArrayList al=new ArrayList();
		SpyDB db=new SpyDB(new PhotoConfig());
		PreparedStatement pst=db.prepareStatement(
			"select * from commentary where photo_id=? order by ts desc");
		pst.setInt(1, imageId);
		ResultSet rs=pst.executeQuery();

		while(rs.next()) {
			al.add(new Comment(security, rs));
		}

		rs.close();
		pst.close();
		db.close();

		return(al);
	}

	/**
	 * Get a List of GroupedComments objects that represent the
	 * comments this user can see.  Each object may represent multiple
	 * comments, but they will all refer to a single image.
	 *
	 * @see GroupedComments
	 */
	public static List getAllComments(PhotoUser user) throws Exception {

		PhotoSecurity security=new PhotoSecurity();
		ArrayList al=new ArrayList();
		FindImagesByComments db=new FindImagesByComments(new PhotoConfig());
		db.set("user_id", user.getId());
		ResultSet rs=db.executeQuery();

		if(rs.next()) {
			GroupedComments gc=new GroupedComments(rs.getInt("photo_id"));
			gc.addComment(new Comment(security, rs));
			while(rs.next()) {
				int newid=rs.getInt("photo_id");
				if(gc.getPhotoId()!=newid) {
					// Add what we have so far
					al.add(gc);
					// Create a new one
					gc=new GroupedComments(rs.getInt("photo_id"));
				}
				// Add the current entry
				gc.addComment(new Comment(security, rs));
			}
			al.add(gc);
		}

		rs.close();
		db.close();

		return(al);
	}

	/**
	 * Save a new comment.
	 */
	public void save() throws Exception {
		if(commentId!=-1) {
			throw new Exception("You can only save *new* comments.");
		}
		if(user.getUsername().equals("guest")) {
			throw new Exception("Guest is not allowed to comment.");
		}
		SpyDB db=new SpyDB(new PhotoConfig());
		PreparedStatement pst=db.prepareStatement(
			"insert into commentary(wwwuser_id,photo_id,note,remote_addr,ts)\n"
			+ " values(?,?,?,?,?)");
		pst.setInt(1, user.getId());
		pst.setInt(2, photoId);
		pst.setString(3, note);
		pst.setString(4, remoteAddr);
		pst.setTimestamp(5, timestamp);

		int updated=pst.executeUpdate();
		if(updated!=1) {
			throw new Exception("No rows updated?");
		}
		pst.close();

		ResultSet rs=db.executeQuery(
			"select currval('commentary_comment_id_seq')");
		if(!rs.next()) {
			System.err.println("*** Couldn't get comment ID ***");
			commentId=-2;
		} else {
			commentId=rs.getInt(1);
		}
		rs.close();
		db.close();
	}

	/**
	 * Get the comment ID of this comment record.
	 */
	public int getCommentId() {
		return(commentId);
	}

	/**
	 * Set the user who'll own this comment.
	 */
	public void setUser(PhotoUser user) {
		this.user=user;
	}

	/**
	 * Get the user who owns this comment.
	 */
	public PhotoUser getUser() {
		return(user);
	}

	/**
	 * Set the ID of the photo to which this comment belongs.
	 */
	public void setPhotoId(int photoId) {
		this.photoId=photoId;
	}

	/**
	 * Get the ID of the photo to which this comment belongs.
	 */
	public int getPhotoId() {
		return(photoId);
	}

	/**
	 * Set the actual note.
	 */
	public void setNote(String note) {
		this.note=note;
	}

	/**
	 * Get the note.
	 */
	public String getNote() {
		return(note);
	}

	/**
	 * Set the remote address of the user at the time this note was added.
	 */
	public void setRemoteAddr(String remoteAddr) {
		this.remoteAddr=remoteAddr;
	}

	/**
	 * Get the remote address of the user at the time this note was added.
	 */
	public String getRemoteAddr() {
		return(remoteAddr);
	}

	/**
	 * Get the timestamp of when this entry was created.
	 */
	public String getTimestamp() {
		return(timestampString);
	}

	/**
	 * String me!
	 */
	public String toString() {
		StringBuffer sb=new StringBuffer();
		sb.append("Comment ");
		sb.append(getCommentId());
		sb.append(" from ");
		sb.append(getUser());
		sb.append(" on ");
		sb.append(getRemoteAddr());
		sb.append(" at ");
		sb.append(getTimestamp());
		sb.append(":\n");
		sb.append(getNote());
		return(sb.toString());
	}

}
