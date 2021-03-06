// Copyright (c) 2006  Dustin Sallings <dustin@spy.net>

package net.spy.photo;

import net.spy.db.Savable;

/**
 * Mutable place.
 */
public interface MutablePlace extends Mutable, Place, Savable {

	/**
	 * Set the name of this place.
	 */
	void setName(String to);

	/**
	 * Set the longitude of this place.
	 */
	void setLongitude(double to);

	/**
	 * Set the latitude of this place.
	 */
	void setLatitude(double to);
}
