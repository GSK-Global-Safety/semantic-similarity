package com.gsk.sia.similarity.util.tools;


/*
 *  ------------------------------------------------------------------
 *  JiveCast
 *  301 Fayetteville St Unit 2301
 *  Raleigh, NC 27601
 *  https://jivecast.com
 *
 *  Copyright (c) 2018-2019 JiveCast. All Rights Reserved. Permission
 *  to copy, modify and distribute this software and code
 *  included and its documentation (collectively, the "PROGRAM") for
 *  any purpose is hereby prohibited.
 *
 *  THE PROGRAM IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 *  EITHER EXPRESSED OR IMPLIED, INCLUDING, WITHOUT
 *  LIMITATION, WARRANTIES THAT THE PROGRAM IS FREE OF
 *  DEFECTS, MERCHANTABLE, FIT FOR A PARTICULAR PURPOSE OR
 *  NON-INFRINGING. THE ENTIRE RISK AS TO THE QUALITY AND
 *  PERFORMANCE OF THE PROGRAM IS WITH YOU. SHOULD ANY PART
 *  OF THE PROGRAM PROVE DEFECTIVE IN ANY RESPECT, YOU
 *  (NOT JIVECAST) ASSUME THE COST OF ANY NECESSARY SERVICING,
 *  REPAIR OR CORRECTION. THIS DISCLAIMER OF WARRANTY CONSTITUTES
 *  AN ESSENTIAL PART OF THIS LICENSE. NO USE OF
 *  THE PROGRAM IS AUTHORIZED HEREUNDER EXCEPT
 *  UNDER THIS DISCLAIMER.
 *
 *  ------------------------------------------------------------------
 */

import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.fulcrum.pool.Recyclable;
import org.apache.turbine.services.pull.ApplicationTool;

import com.gsk.sia.similarity.util.LocalDate;



/**
 * Date handling tool
 *
 * @author painter
 */
public class DateTool implements ApplicationTool, Recyclable {

	/** Logging */
	private static Log log = LogFactory.getLog(DateTool.class);

	/**
	 * Constructor does initialization stuff
	 */
	public DateTool() {
	}

	@Override
	public void init(Object arg0) {
	}

	@Override
	public void refresh() {
	}

	/**
	 * Get a nicely formatted date for today
	 *
	 * @return The today value
	 */
	public String getToday() {
		return LocalDate.reportDate(new Date());
	}

	/*
	 * get a date object for now
	 */
	public Date getTodayDate() {
		return new Date();
	}

	/**
	 * Get a nicely formatted date for reports
	 */
	public String reportDate(Date date) {
		return LocalDate.reportDate(date);
	}

	/**
	 * Get a nicely formatted date for display with time
	 */
	public String displayDate(Date date) {
		return LocalDate.display(date);
	}

	// ****************** Recyclable implementation ************************
	private boolean disposed;

	/**
	 * Recycles the object for a new client. Recycle methods with parameters must be
	 * added to implementing object and they will be automatically called by pool
	 * implementations when the object is taken from the pool for a new client. The
	 * parameters must correspond to the parameters of the constructors of the
	 * object. For new objects, constructors can call their corresponding recycle
	 * methods whenever applicable. The recycle methods must call their super.
	 */
	@Override
	public void recycle() {
		disposed = false;
	}

	/**
	 * Disposes the object after use. The method is called when the object is
	 * returned to its pool. The dispose method must call its super.
	 */
	@Override
	public void dispose() {
		disposed = true;
	}

	/**
	 * Checks whether the recyclable has been disposed.
	 *
	 * @return true, if the recyclable is disposed.
	 */
	@Override
	public boolean isDisposed() {
		return disposed;
	}

}
