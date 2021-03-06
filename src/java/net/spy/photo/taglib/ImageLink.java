// Copyright (c) 2001  Dustin Sallings <dustin@spy.net>

package net.spy.photo.taglib;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.jsp.JspException;

import net.spy.photo.PhotoDimensions;
import net.spy.photo.PhotoImage;
import net.spy.photo.PhotoImageFactory;
import net.spy.photo.PhotoImageHelper;
import net.spy.photo.PhotoSessionData;
import net.spy.photo.PhotoUtil;

/**
 * Taglib to link to an image.
 */
public class ImageLink extends PhotoTag {

	private int id=0;
	private boolean showThumbnail=false;
	private boolean showOptimal=false;
	private String width=null;
	private String height=null;
	private boolean scale=false;
	private String altText=null;

	private String onMouseDown=null;
	private String usemap=null;
	private String className=null;

	/**
	 * Get an instance of ImageLink.
	 */
	public ImageLink() {
		super();
		release();
	}

	/**
	 * Set the id of the image to which we want to link.
	 */
	@Override
	public void setId(String to) {
		id=Integer.parseInt(to);
	}

	/**
	 * Set the id of the image to which we want to link.
	 */
	public void setId(int to) {
		id=to;
	}

	/**
	 * If ``true'' show a thumbnail.
	 */
	public void setShowThumbnail(String to) {
		this.showThumbnail=Boolean.valueOf(to).booleanValue();
	}

	/**
	 * If ``true'' show the optimal size.
	 */
	public void setShowOptimal(String to) {
		this.showOptimal=Boolean.valueOf(to).booleanValue();
	}

	/**
	 * If ``true'' scale the image to the given width and height.
	 */
	public void setScale(String to) {
		this.scale=Boolean.valueOf(to).booleanValue();
	}

	/**
	 * Set the image width.
	 */
	public void setWidth(String to) {
		this.width=to;
	}

	/**
	 * Set the image height.
	 */
	public void setHeight(String to) {
		this.height=to;
	}

	/**
	 * Set the alt text for the thumbnail (if provided).
	 */
	public void setAlt(String to) {
		this.altText=to;
	}

	/** 
	 * Set the onMouseDown script.
	 */
	public void setOnMouseDown(String to) {
		this.onMouseDown=to;
	}

	/** 
	 * Set the usemap.
	 */
	public void setUsemap(String to) {
		this.usemap=to;
	}

	/** 
	 * Set the class name.
	 */
	public void setStyleClass(String to) {
		this.className=to;
	}

	/**
	 * Start link.
	 */
	@Override
	public int doStartTag() throws JspException {

		StringBuffer href=new StringBuffer();
		StringBuffer url=new StringBuffer();

		href.append("<img src=\"");

		HttpServletRequest req=(HttpServletRequest)pageContext.getRequest();
		url.append(PhotoUtil.getRelativeUri(req, "/PhotoServlet/"));

		// Get the PhotoSessionData so we can figure out the width and height
		HttpSession session=req.getSession(false);
		PhotoSessionData sessionData=
			(PhotoSessionData)session.getAttribute(PhotoSessionData.SES_ATTR);
		if(sessionData==null) {
			throw new JspException("No photoSession in session.");
		}

		if(showOptimal) {
			scale=true;
			try {
				PhotoImageFactory pidf=PhotoImageFactory.getInstance();
				PhotoImage pid=pidf.getObject(id);
				PhotoDimensions optdims=sessionData.getOptimalDimensions();
				PhotoDimensions newDims=PhotoUtil.scaleTo(
					pid.getDimensions(), optdims);

				width=String.valueOf(newDims.getWidth());
				height=String.valueOf(newDims.getHeight());
			} catch(Exception e) {
				JspException e2=new JspException("Couldn't get image");
				e2.initCause(e);
				throw e2;
			}
		}
		
		url.append(id);
		url.append(".jpg?id=");
		url.append(id);
		if(scale && (width!=null) && (height!=null)) {
			url.append("&amp;scale=");
			url.append(width);
			url.append("x");
			url.append(height);
		}

		if(showThumbnail) {
			url.append("&amp;thumbnail=1");
		}

		// Get the response for rewriting
		HttpServletResponse res=(HttpServletResponse)pageContext.getResponse();
		href.append(res.encodeURL(url.toString()));

		// Finish the src attribute.
		href.append("\"");

		String tmpAlt=altText;
		if(altText==null) {
			tmpAlt="image " + id;
		}
		href.append(" alt=\"");
		href.append(tmpAlt);
		href.append("\"");

		// if no width or height was provided, figure out out
		if( (width == null) && (height == null) ) {
			try {
				if(showThumbnail) {
					PhotoImageHelper ph=PhotoImageHelper.getInstance();
					PhotoDimensions size=ph.getThumbnailSize(
							PhotoImageFactory.getInstance().getObject(id));
					width="" + size.getWidth();
					height="" + size.getHeight();
				}
			} catch(Exception e) {
				// Just print the stack trace, leave the width and height
				// blank.
				e.printStackTrace();
			}
		}

		if(width!=null) {
			href.append(" width=\"");
			href.append(width);
			href.append("\"");
		}
		if(height!=null) {
			href.append(" height=\"");
			href.append(height);
			href.append("\"");
		}

		if(onMouseDown!=null) {
			href.append(" onmousedown=\"");
			href.append(onMouseDown);
			href.append("\"");
		}

		if(usemap!=null) {
			href.append(" usemap=\"");
			href.append(usemap);
			href.append("\"");
		}

		if(className!=null) {
			href.append(" class=\"");
			href.append(className);
			href.append("\"");
		}

		href.append("/>");

		try {
			pageContext.getOut().write(href.toString());
		} catch(Exception e) {
			e.printStackTrace();
			throw new JspException("Error sending output:  " + e);
		}

		return(EVAL_BODY_INCLUDE);
	}

	/**
	 * Reset all values.
	 */
	@Override
	public void release() {
		id=0;
		showThumbnail=false;
		width=null;
		height=null;
		scale=false;
		altText=null;
	}

}
