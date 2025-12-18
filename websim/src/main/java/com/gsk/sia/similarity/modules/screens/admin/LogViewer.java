package com.gsk.sia.similarity.modules.screens.admin;

import org.apache.turbine.pipeline.PipelineData;

/*
 *  ------------------------------------------------------------------
 *  Jeffery L Painter, <jeffery.l.painter@gsk.com>
 *
 *  Copyright (c) 2010 GlaxoSmithKline
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

import com.gsk.sia.similarity.om.EventLog;

//OM Layer

public class LogViewer extends SecureScreen {
	/**
	 * Place all the data object in the context for use in the template.
	 */
	public void doBuildTemplate(PipelineData pipelineData, Context context) {
		RunData data = (RunData) pipelineData;
		try {
			int idx_id = 0;
			try {
				idx_id = data.getParameters().getInt("idx");
				if (idx_id > 0) {
					idx_id--;
				}
			} catch (Exception e) {
			}

			// Put the editors only in the list
			int PAGE_LIMIT = 50;
			context.put("entries", EventLog.getAllLogs(PAGE_LIMIT, idx_id));
			context.put("current_page", idx_id + 1);
			context.put("page_links", EventLog.getEventPages(PAGE_LIMIT));

		} catch (Exception e) {
			data.setMessage("No transactions found.");
		}
	}

}
