
/*
 *  ------------------------------------------------------------------
 *  JiveCast.com
 *  3205 Randall Parkway
 *  Wilmington, NC 28403
 *  http://JiveCast.com.com
 *
 *  Copyright (c) 2003-2004 JiveCast.com All Rights Reserved. Permission
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
 *  (NOT JiveCast.com) ASSUME THE COST OF ANY NECESSARY SERVICING,
 *  REPAIR OR CORRECTION. THIS DISCLAIMER OF WARRANTY CONSTITUTES
 *  AN ESSENTIAL PART OF THIS LICENSE. NO USE OF
 *  THE PROGRAM IS AUTHORIZED HEREUNDER EXCEPT
 *  UNDER THIS DISCLAIMER.
 *
 *  ------------------------------------------------------------------
 */
/*
 *  ------------------------------------------------------------------
 *  originally from org.apache.turbine.utils.HttpUtils - fixed for own
 *  use... original can be found in turbine cvs
 *  ------------------------------------------------------------------
 */
package com.gsk.sia.similarity.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 *  This class provides utilities for handling some semi-trivial HTTP stuff that
 *  would othterwize be handled elsewhere.
 *
 *@author     <a href="mailto:magnus@handpoint.com">Magnús Þór Torfason</a>
 */
public class HttpUtils {
    /**
     *  The date format to use for HTTP Dates.
     */
    private static SimpleDateFormat httpDateFormat;

    static {
        httpDateFormat = new SimpleDateFormat(
            "EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
        // "EEE, dd MMM yyyyy HH:mm:ss z", Locale.US); -- fixed

        httpDateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
    }


    /**
     *  Formats a java Date according to rfc 1123, the rfc standard for dates in
     *  http.
     *
     *@param  date  The Date to format
     *@return       A String represeentation of the date
     */
    public static String formatHttpDate(Date date) {
        synchronized (httpDateFormat) {
            return httpDateFormat.format(date);
        }
    }

}

