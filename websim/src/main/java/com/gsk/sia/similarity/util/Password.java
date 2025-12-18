
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
package com.gsk.sia.similarity.util;

// see http://ostermiller.org/utils for more info
import com.Ostermiller.util.RandPass;

/*
 *  Password Generator, uses Ostermiller Random password
 *  generator to create a new 8 character password
 *  for our system
 */
/**
 *  Description of the Class
 *
 *@author     painter
 */
public class Password {
    private RandPass rand = new RandPass();


    // default constructor
    /**
     *  Gets the password attribute of the Password object
     *
     *@return    The password value
     */
    public String getPassword() {
        return getPassword(8);
    }


    // can specify password to be of size
    /**
     *  Gets the password attribute of the Password object
     *
     *@param  size  Description of the Parameter
     *@return       The password value
     */
    public String getPassword(int size) {
        rand.setAlphabet(com.Ostermiller.util.RandPass.LOWERCASE_LETTERS_ALPHABET);
        rand.setMaxRepetition(2);
        return rand.getPass(size);
    }

}

