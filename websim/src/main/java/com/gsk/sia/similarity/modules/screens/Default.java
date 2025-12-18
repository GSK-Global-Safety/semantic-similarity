package com.gsk.sia.similarity.modules.screens;

import javax.servlet.http.HttpServletResponse;

// include the logging facility
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/*
 *  ------------------------------------------------------------------
 *  Jeffery L Painter, <jeffery.l.painter@gsk.com>
 *
 *  Copyright (c) 2010-2015 GlaxoSmithKline
 *  All Rights Reserved.
 *
 *  THE PROGRAM IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 *  EITHER EXPRESSED OR IMPLIED, INCLUDING, WITHOUT
 *  LIMITATION, WARRANTIES THAT THE PROGRAM IS FREE OF
 *  DEFECTS, MERCHANTABLE, FIT FOR A PARTICULAR PURPOSE OR
 *  NON-INFRINGING. THE ENTIRE RISK AS TO THE QUALITY AND
 *  PERFORMANCE OF THE PROGRAM IS WITH YOU. SHOULD ANY PART
 *  OF THE PROGRAM PROVE DEFECTIVE IN ANY RESPECT, YOU
 *  (NOT NCSU) ASSUME THE COST OF ANY NECESSARY SERVICING,
 *  REPAIR OR CORRECTION. THIS DISCLAIMER OF WARRANTY CONSTITUTES
 *  AN ESSENTIAL PART OF THIS LICENSE. NO USE OF
 *  THE PROGRAM IS AUTHORIZED HEREUNDER EXCEPT
 *  UNDER THIS DISCLAIMER.
 *
 *  ------------------------------------------------------------------
 */

import org.apache.turbine.modules.screens.VelocityScreen;
import org.apache.turbine.pipeline.PipelineData;
import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;

/**
 * The homepage is not a restricted resource.
 */
public class Default extends VelocityScreen {
	// create an instance of the logging facility
	private static Log log = LogFactory.getLog(Default.class);

	/**
	 * Place all the data object in the context for use in the template.
	 */
	public void doBuildTemplate(PipelineData pipelineData, Context context) {
		RunData data = (RunData) pipelineData;

		// generate a log for each time
		// any page is accessed
		// log.info("Home page accessed");

		// Update the character set to support multiple languages in display
		data.setCharSet("UTF-8");
		HttpServletResponse response = data.getResponse();
		response.setContentType("text/html; charset=UTF-8");
		response.setCharacterEncoding("UTF-8");

	}

}
