// Copyright (c) 2001  Dustin Sallings <dustin@spy.net>
//
// $Id: Comment.java,v 1.1 2002/02/23 07:51:29 dustin Exp $

package net.spy.photo;

import java.sql.*;
import java.util.*;

import net.spy.*;

/**
 * Comments on photos.
 */
public class Comment extends Object {

	private int commentId=-1;

	private PhotoUser user=null;

	private int photoId=-1;
	private String note=null;
	private String remoteAddr=null;
	private Timestamp timestamp=null;

	/**
	 * Get an instance of Comment.
	 */
	public Comment() {
		super();
		timestamp=new java.sql.Timestamp(System.currentTimeMillis());
	}

	// Get a comment from a result row
	private Comment(PhotoSecurity sec, ResultSet rs) throws Exception {
		super();

		commentId=rs.getInt("comment_id");
		photoId=rs.getInt("photo_id");
		note=rs.getString("note");
		timestamp=rs.getTimestamp("ts");
		remoteAddr=rs.getString("remote_addr");
		user=sec.getUser(rs.getInt("wwwuser_id"));
	}

	/**
	 * Get an Enumeration of Comment objects for all of the comments on a
	 * given image.
	 */
	public static Enumeration getCommentsForPhoto(int image_id)
		throws Exception {

		PhotoSecurity security=new PhotoSecurity();
		Vector v=new Vector();
		SpyDB db=new SpyDB(new PhotoConfig());
		PreparedStatement pst=db.prepareStatement(
			"select * from commentary where photo_id=? order by ts desc");
		pst.setInt(1, image_id);
		ResultSet rs=pst.executeQuery();

		while(rs.next()) {
			v.addElement(new Comment(security, rs));
		}

		rs.close();
		pst.close();
		db.close();

		return(v.elements());
	}

	/**
	 * Save a new comment.
	 */
	public void save() throws Exception {
		if(commentId!=-1) {
			throw new Exception("You can only save *new* comments.");
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
	public Timestamp getTimestamp() {
		return(timestamp);
	}

	/**
	 * XML me.
	 */
	public String toXML() {
		StringBuffer sb=new StringBuffer();
		sb.append("<photo_comment>\n");
		sb.append("<comment_id>");
		sb.append(getCommentId());
		sb.append("</comment_id>\n");
		sb.append("<remote_addr>");
		sb.append(getRemoteAddr());
		sb.append("</remote_addr>\n");
		sb.append("<timestamp>");
		sb.append(getTimestamp());
		sb.append("</timestamp>\n");
		sb.append("<note>");
		sb.append(PhotoXML.normalize(getNote(), false));
		sb.append("</note>\n");
		sb.append(getUser().toXML());
		sb.append("</photo_comment>\n");
		return(sb.toString());
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

	/**
	 * Testing and what not.
	 */
	public static void main(String args[]) throws Exception {
		if(args.length==0) {
			PhotoSecurity sec=new PhotoSecurity();
			PhotoUser me=sec.getUser("dustin");
			System.out.println("Got user:  " + me);

			Comment comment=new Comment();
			comment.setUser(me);
			comment.setNote("This image has been up for a *really* long time.");
			comment.setRemoteAddr("192.168.1.139");
			comment.setPhotoId(2156);

			comment.save();
			System.out.println(comment);
		} else {
			int img=Integer.parseInt(args[0]);
			for(Enumeration e=Comment.getCommentsForPhoto(img);
				e.hasMoreElements();) {

				Comment c=(Comment)e.nextElement();
				System.out.println(c.toXML());
				System.out.println("--");
			}
		}
	}

}
