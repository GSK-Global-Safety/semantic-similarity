package com.gsk.sia.similarity.modules.screens.api;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.turbine.om.security.User;
import org.apache.turbine.pipeline.PipelineData;
import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;
import org.json.JSONArray;
import org.json.JSONObject;

import com.gsk.sia.similarity.json.om.MeddraCode;
import com.gsk.sia.similarity.util.TermMatcher;

/**
 * @author painter
 */
public class Meddra extends SecureScreen {

	// create an instance of the logging facility
	private static Log log = LogFactory.getLog(MeddraHierarchy.class);

	private static final String MY_ACTION = "methodCall";
	private static final String TERM = "term";
	private static final String PT_CODE = "pt_code";
	private static final String PT_FILTER = "pt_filter";
	private static final String THRESHOLD = "probability";
	private static final String EMPTY = "{ \"distance\": null }";

	@Override
	public void doBuildTemplate(PipelineData pipelineData, Context context) throws Exception {

		log.debug("MedDRA API called");

		RunData data = (RunData) pipelineData;

		try {
			// Required parameters
			String action = data.getParameters().getString(MY_ACTION, "");

			if (!StringUtils.isAllEmpty(action)) {

				/**
				 * Get codes that co-exist in the preferred HLT
				 */
				if (action.contentEquals("code_search")) {

					// get the current user
					User user = data.getUser();

					// Required parameters
					String code = data.getParameters().getString(PT_CODE, "");

					if (StringUtils.isAllEmpty(code) == false) {

						com.gsk.sia.similarity.om.MeddraCode mc = com.gsk.sia.similarity.om.MeddraCode.getMeddraCodeByCode(code);
						
						if (mc != null) {

							MeddraCode jsonCode = new MeddraCode(mc);
							jsonCode.setMatchProbability(1.0);
							
							// Create JSON msg to return
							String message;
							JSONObject json = new JSONObject();
							json.put("code", new JSONObject(jsonCode));
							message = json.toString();

							// Add the output
							context.put("json", message);

						} else {
							context.put("json", "{ \"error\": \"Could not load code\"} ");
						}

					}

				}				
				
				/**
				 * Get codes that co-exist in the preferred HLT
				 */
				if (action.contentEquals("term_search")) {

					// get the current user
					User user = data.getUser();

					// Required parameters
					String term = data.getParameters().getString(TERM, "");
					String ptFilter = data.getParameters().getString(PT_FILTER, "");
					boolean ptOnly = false;
					if (ptFilter.contentEquals("PT_ONLY"))
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
						if (results != null) {

							// Create JSON msg to return
							String message;
							JSONObject json = new JSONObject();

							json.put("codes", new JSONArray(results));
							message = json.toString();

							// Add the output
							context.put("json", message);

						} else {
							context.put("json", "{ \"error\": \"Could not load related codes\"} ");
						}

					}

				}
			}

		} catch (Exception e) {
			context.put("json", "{ \"error\": \"No action specified\"} ");
		}

		super.doBuildTemplate(data, context);
	}

}
