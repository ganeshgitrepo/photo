// Copyright (c) 2003  Dustin Sallings <dustin@spy.net>
// arch-tag: 5F8E3B7A-5D6D-11D9-8100-000A957659CC

package net.spy.photo.filter;

import net.spy.photo.PhotoException;
import net.spy.photo.search.SearchResults;

/**
 * Filter that affects sorting.
 */
public abstract class SortingFilter extends Filter {

	/**
	 * Sort directions.
	 */
	public static enum Sort { FORWARD, REVERSE }

	/**
	 * Get an instance of SortingFilter.
	 */
	public SortingFilter() {
		super();
	}

	/** 
	 * Filter the results and use the FORWARD sort direction.
	 */
	public final SearchResults filter(SearchResults in) throws PhotoException {
		return(filter(in, Sort.FORWARD));
	}

	/**
	 * Filter a result set.
	 */
	public abstract SearchResults filter(SearchResults in, Sort direction)
		throws PhotoException;

}
