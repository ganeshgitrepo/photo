// Copyright (c) 2005  Dustin Sallings <dustin@spy.net>

package net.spy.photo.impl;

import net.spy.SpyObject;
import net.spy.photo.PhotoRegion;

/**
 * Concrete implementation of a region.
 */
public class PhotoRegionImpl extends SpyObject implements PhotoRegion {

	private int x=0;
	private int y=0;
	private int width=0;
	private int height=0;

	/**
	 * Get an instance of PhotoRegionImpl.
	 */
	public PhotoRegionImpl(int ex, int why, int w, int h) {
		super();
		this.x=ex;
		this.y=why;
		this.width=w;
		this.height=h;
	}

	public int getX() {
		return(x);
	}

	public int getY() {
		return(y);
	}

	public int getWidth() {
		return(width);
	}

	public int getHeight() {
		return(height);
	}

	@Override
	public int hashCode() {
		return(x ^ y ^ width ^ height);
	}

	@Override
	public String toString() {
		return("{PhotoRegionImpl " + width + "x" + height
			+ " at " + x + "," + y + "}");
	}

	@Override
	public boolean equals(Object o) {
		boolean rv=false;
		if(o instanceof PhotoRegion) {
			PhotoRegion r=(PhotoRegion)o;
			// Assume true unless some number is different
			rv=true;
			rv &= (getX() == r.getX());
			rv &= (getY() == r.getY());
			rv &= (getWidth() == r.getWidth());
			rv &= (getHeight() == r.getHeight());
		}
		return(rv);
	}

}
