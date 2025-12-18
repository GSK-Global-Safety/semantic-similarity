package com.gsk.sia.similarity.modules.screens.api;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.turbine.pipeline.PipelineData;
import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;
import org.json.JSONArray;
import org.json.JSONObject;

import com.gsk.sia.similarity.om.MeddraCode;
import com.gsk.sia.similarity.om.MeddraSmq;
import com.gsk.sia.similarity.util.tools.UmlsConceptTool;

/**
 * @author painter
 */
public class SMQ extends SecureScreen {

	// create an instance of the logging facility
	private static Log log = LogFactory.getLog(GetDistance.class);

	private static final String MY_ACTION = "methodCall";
	private static final String PT_FILTER = "pt_filter";

	@Override
	public void doBuildTemplate(PipelineData pipelineData, Context context) throws Exception {

		log.debug("SMQ API called");

		RunData data = (RunData) pipelineData;

		try {

			// Load the concept tool
			UmlsConceptTool concept = (UmlsConceptTool) context.get("concept");

			// Required parameters
			String action = data.getParameters().getString(MY_ACTION, "");

			if (!StringUtils.isAllEmpty(action)) {

				/**
				 * Provide list of all current SMQs available in the database
				 */
				if (action.contentEquals("listAll")) {
					// Create JSON msg to return
					String message;
					JSONObject json = new JSONObject();

					List<MeddraSmq> smqs = MeddraSmq.getAllSmqs();
					List<com.gsk.sia.similarity.json.om.SMQ> jsonSmqList = new ArrayList<>();
					for (MeddraSmq smq : smqs) {

						// Get SMQ's without codes attached
						com.gsk.sia.similarity.json.om.SMQ jSmq = new com.gsk.sia.similarity.json.om.SMQ(smq, false);
						jsonSmqList.add(jSmq);
					}

					json.put("smqs", new JSONArray(jsonSmqList));
					message = json.toString();

					// Add the output
					context.put("json", message);

				}

				/**
				 * Load a single SMQ
				 */
				if (action.contentEquals("getSMQ")) {
					String ptFilter = data.getParameters().getString(PT_FILTER, "PT_ONLY");
					boolean ptOnly = false;
					if (ptFilter.contentEquals("PT_ONLY"))
						ptOnly = true;

					String smqCode = data.getParameters().getString("smq", "");
					String scope = data.getParameters().getString("scope", "");
					if (!StringUtils.isAllEmpty(smqCode)) {
						MeddraSmq smq = MeddraSmq.getSmq(smqCode, scope);
						com.gsk.sia.similarity.json.om.SMQ jSmq = new com.gsk.sia.similarity.json.om.SMQ(smq, ptOnly);
						JSONObject json = new JSONObject();
						json.put("smq", new JSONObject(jSmq));
						String message = json.toString();
						context.put("json", message);

					}
				}

				/**
				 * Find codes related to another by an SMQ
				 */
				if (action.contentEquals("getRelatedSMQ")) {
					String ptFilter = data.getParameters().getString(PT_FILTER, "PT_ONLY");
					boolean ptOnly = false;
					if (ptFilter.contentEquals("PT_ONLY"))
						ptOnly = true;

					String ptCode = data.getParameters().getString("pt_code", "");
					String scope = data.getParameters().getString("scope", "");
					if (!StringUtils.isAllEmpty(ptCode)) {
						MeddraCode mc = MeddraCode.getMeddraCodeByCode(ptCode);
						List<MeddraCode> smqCodes = mc.getCodesRelatedBySMQ(scope, ptOnly);
						List<com.gsk.sia.similarity.json.om.MeddraCode> results = new ArrayList<>();
						for (MeddraCode smqCode : smqCodes) {
							com.gsk.sia.similarity.json.om.MeddraCode jsonCode = new com.gsk.sia.similarity.json.om.MeddraCode(
									smqCode);
							results.add(jsonCode);

						}

						if (results.size() > 0) {
							JSONObject json = new JSONObject();
							json.put("codes", new JSONArray(results));
							String message = json.toString();
							context.put("json", message);
						} else {
							context.put("json", "{ \"error\": \"No codes found\"} ");
						}

					}
				}

			} else {
				context.put("json", "{ \"error\": \"No action specified\"} ");
			}

		} catch (Exception e) {
			context.put("json", "{ \"error\": \"No action specified\"} ");
		}

		super.doBuildTemplate(data, context);
	}

}
