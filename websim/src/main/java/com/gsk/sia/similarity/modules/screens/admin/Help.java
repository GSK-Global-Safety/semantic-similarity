package com.gsk.sia.similarity.modules.screens.admin;

import java.util.List;

import org.apache.turbine.pipeline.PipelineData;

/*
 *  ------------------------------------------------------------------
 *  Jeffery L Painter, <jeffery.l.painter@gsk.com>
 *
 *  Copyright (c) 2016 GlaxoSmithKline
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

//Turbine objects
import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;

import com.gsk.sia.similarity.om.HelpFile;

public class Help extends SecureScreen {
	/**
	 * Place all the data object in the context for use in the template.
	 */
	public void doBuildTemplate(PipelineData pipelineData, Context context) {
		RunData data = (RunData) pipelineData;
		try {
			List<HelpFile> files = HelpFile.getAllFiles();
			context.put("files", files);
		} catch (Exception e) {
		}
	}

}
