// Copyright (c) 2001  Dustin Sallings <dustin@spy.net>
//
// $Id: OnceAMonthFilter.java,v 1.6 2003/05/04 06:49:54 dustin Exp $

package net.spy.photo.filter;

import java.util.Calendar;
import java.util.Date;

/**
 * This filter randomly selects one picture per month from a group and
 * throws the rest away.
 */
public class OnceAMonthFilter extends DateFilter {

	/**
	 * Get an instance of OnceAMonthFilter.
	 */
	public OnceAMonthFilter() {
		super();
	}

	/** 
	 * Truncate the date to the beginning of the month.
	 */
	protected Date roundDate(Date d) {
		Calendar cal=Calendar.getInstance();
		cal.setTime(d);
		cal.set(Calendar.DAY_OF_MONTH, 1);
		return(cal.getTime());
	}

}
