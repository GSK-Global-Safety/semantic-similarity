package com.gsk.sia.similarity.modules.screens.csv;

import java.util.HashMap;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.turbine.pipeline.PipelineData;
import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;
import org.json.JSONObject;

import com.gsk.sia.similarity.json.om.DistanceMatrix;
import com.gsk.sia.similarity.om.MeddraCode;
import com.gsk.sia.similarity.util.SimilarityDistance;
import com.gsk.sia.similarity.util.tools.UmlsConceptTool;

/**
 * @author painter
 */
public class MeddraHierarchy extends CsvScreen {

	// create an instance of the logging facility
	private static Log log = LogFactory.getLog(MeddraHierarchy.class);

	private static final String MY_ACTION = "methodCall";
	private static final String METRICS = "metrics";
	private static final String VIEW = "ontology";
	private static final String PT_FILTER = "pt_filter";
	private static final String FAERS_FILTER = "faers";
	private static final String DISTANCE_FILTER = "minDistance";
	private static final String EMPTY = "{ \"distance\": null }";

	@Override
	public void doBuildTemplate(PipelineData pipelineData, Context context) {

		log.debug("MedDRA Hierarchy API called");

		RunData data = (RunData) pipelineData;

		try {
			// Required parameters
			String action = data.getParameters().getString(MY_ACTION, "");

			if (!StringUtils.isAllEmpty(action)) {

				/**
				 * Provide list of all SOC level terms
				 */
				if (action.contentEquals("getRelatedCodes")) {

					//
					// Find related codes
					//
					String code = data.getParameters().getString("code", "");
					if (StringUtils.isAllEmpty(code) == false) {

						// Load the concept tool
						UmlsConceptTool concept = (UmlsConceptTool) context.get("concept");

						// Required parameters
						String ptFilter = data.getParameters().getString(PT_FILTER, "PT_ONLY");
						String faers_filter = data.getParameters().getString(FAERS_FILTER, "true").toLowerCase().trim();
						String metrics = data.getParameters().getString(METRICS, "INTRINSIC_LIN");
						String onto = data.getParameters().getString(VIEW, "mdr-umls");
						String distanceFilter = data.getParameters().getString(DISTANCE_FILTER, "");

						// PT Only filter
						boolean ptOnly = false;
						if (ptFilter.contentEquals("PT_ONLY"))
							ptOnly = true;

						// Set the distance filter if we have one
						String[] metricArray = metrics.split(",");
						double[] distFilter = new double[metricArray.length];
						setDistanceFilter(distanceFilter, metricArray, distFilter);

						// Update FAERS filter
						boolean filterFaers = true;
						if (!faers_filter.contentEquals("true"))
							filterFaers = false;

						// Load the FAERS PT codes
						HashMap<String, com.gsk.sia.similarity.json.om.MeddraCode> faers_codes = concept
								.getFaersCodes();

						// MedDRA code
						MeddraCode mc = MeddraCode.getMeddraCodeByCode(code);

						// Initialize the similarity distance services once to load the
						// concept graphs into memory
						SimilarityDistance sd = new SimilarityDistance(onto);

						DistanceMatrix matrix = null;

						if (filterFaers == true) {
							matrix = sd.getRelatedCodeMatrix(mc, metricArray, distFilter, ptOnly, faers_codes);
						} else {
							matrix = sd.getRelatedCodeMatrix(mc, metricArray, distFilter, ptOnly, null);
						}

						if (matrix != null) {

							// for building the CSV file
							context.put("metrics",  metricArray);
							context.put("distances",  matrix.getDistances());

						} else {
							context.put("csv", "{ \"error\": \"Could not compute distance matrix\"} ");
						}

					}
				}

			} else {
				context.put("json", "{ \"error\": \"No action specified\"} ");
			}

		} catch (Exception e) {
			context.put("json", "{ \"error\": \"No action specified\"} ");
		}

	}

	/**
	 * Set the distance filter as an array
	 * 
	 * @param distanceFilter
	 * @param metricArray
	 * @param distFilter
	 */
	private void setDistanceFilter(String distanceFilter, String[] metricArray, double[] distFilter) {
		for (int i = 0; i < metricArray.length; i++)
			distFilter[i] = -1;
		if (!StringUtils.isAllEmpty(distanceFilter)) {
			String[] filter = distanceFilter.split(",");
			int idx = 0;
			for (String f : filter) {
				f = f.replace("[", "");
				f = f.replace("]", "");
				f = f.trim();

				try {
					distFilter[idx] = Double.parseDouble(f);
				} catch (Exception f1) {

				}
				idx++;
			}
		}
	}
}
