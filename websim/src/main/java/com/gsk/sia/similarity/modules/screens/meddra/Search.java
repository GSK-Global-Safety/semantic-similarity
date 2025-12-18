package com.gsk.sia.similarity.modules.screens.meddra;

import java.util.List;

import org.apache.turbine.om.security.User;
import org.apache.turbine.pipeline.PipelineData;
import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;

import com.gsk.sia.similarity.json.om.MeddraCode;
import com.gsk.sia.similarity.util.TermMatcher;

import nl.basjes.parse.useragent.yauaa.shaded.org.apache.commons.lang3.StringUtils;

/**
 * This class provides the data required for displaying content in the Velocity
 * page.
 */
public class Search extends SecureScreen {

	// Form variables
	private static final String TERM = "term";
	private static final String PT_FILTER = "pt_filter";
	private static final String THRESHOLD = "probability";
	
	/**
	 * Add all SOCs to the template
	 */
	@Override
	protected void doBuildTemplate(PipelineData pipelineData, Context context) throws Exception {

		RunData data = (RunData) pipelineData;
		// get the current user
		User user = data.getUser();

		try {

			// Required parameters
			String term = data.getParameters().getString(TERM, "");
			String ptFilter = data.getParameters().getString(PT_FILTER, "");
			boolean ptOnly = false;
			if ( ptFilter.contentEquals("PT_ONLY") )
				ptOnly = true;
			
			String threshold = data.getParameters().getString(THRESHOLD, "0.7");
			double minThreshold = Double.parseDouble(threshold);
			if (StringUtils.isAllEmpty(term) == false) {

				// Test if the TermMatcher has been instantiated
				TermMatcher tm = (TermMatcher) user.getTemp("termMatcher");
				if (tm == null) {
					tm = TermMatcher.getInstance();
					user.setTemp("termMatcher", tm);
				}

				// Find MedDRA codes
				List<MeddraCode> results = tm.findClosestMatchingMeddraCodes(term, minThreshold, ptOnly);
				context.put("results", results);

			}

		} catch (Exception e) {
			log.error("Error on meddra search: " + e.toString());
		}

	}

}
