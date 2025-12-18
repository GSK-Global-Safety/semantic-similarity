package com.gsk.sia.similarity.modules.screens.api;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.turbine.pipeline.PipelineData;
import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;
import org.json.JSONObject;

import com.gsk.sia.similarity.json.om.ClusterMetric;
import com.gsk.sia.similarity.json.om.DistanceMatrix;
import com.gsk.sia.similarity.om.MeddraCode;
import com.gsk.sia.similarity.util.SimilarityDistance;
import com.gsk.sia.similarity.util.tools.UmlsConceptTool;

/**
 * Given a list of PT's compute the distance matrix
 */
public class ComputeMatrix extends SecureScreen {

	// create an instance of the logging facility
	private static Log log = LogFactory.getLog(GetDistance.class);

	private static final String MY_ACTION = "methodCall";
	private static final String HLGT_CODE = "hlgt_code";
	private static final String PT_CODES = "pt_codes";
	private static final String FAERS_FILTER = "faers";
	private static final String PT_FILTER = "pt_filter";
	private static final String METRICS = "metrics";
	private static final String VIEW = "ontology";
	private static final String DISTANCE_FILTER = "minDistance";
	private static final String EMPTY = "{ \"distance\": null }";

	@Override
	public void doBuildTemplate(PipelineData pipelineData, Context context) throws Exception {

		RunData data = (RunData) pipelineData;

		try {
			// Load the concept tool
			UmlsConceptTool concept = (UmlsConceptTool) context.get("concept");

			String action = data.getParameters().getString(MY_ACTION, "ptMatrix");

			String ptFilter = data.getParameters().getString(PT_FILTER, "PT_ONLY");
			boolean ptOnly = false;
			if (ptFilter.contentEquals("PT_ONLY"))
				ptOnly = true;

			// Filter to only FEARS PT codes
			boolean filterFaers = true;
			String strFilterFears = data.getParameters().getString(FAERS_FILTER, "true").toLowerCase().trim();
			if (!strFilterFears.contentEquals("true"))
				filterFaers = false;

			/**
			 * Compute matrix of HLGT codes
			 */
			if (action.contentEquals("hlgtMatrix")) {

				String hlgtCode = data.getParameters().getString(HLGT_CODE, "");
				if (!StringUtils.isEmpty(hlgtCode)) {
					// Test that this is an HLGT code first
					MeddraCode hlgt = MeddraCode.getMeddraCodeByCode(hlgtCode);
					if (hlgt.getMeddraCodeType().getCodeType().contentEquals("HG")) {

						String metrics = data.getParameters().getString(METRICS, "INTRINSIC_LIN");
						String onto = data.getParameters().getString(VIEW, "mdr-umls");
						String distanceFilter = data.getParameters().getString(DISTANCE_FILTER, "");

						// All the ontologies we want to compute over
						String[] viewArray = onto.split(",");

						// Initialize the similarity distance services once to load the
						// concept graphs into memory
						HashMap<String, SimilarityDistance> sds = new HashMap<>();
						for (String view : viewArray) {
							SimilarityDistance sd = new SimilarityDistance(view);
							sds.put(view, sd);
						}

						// Set the distance filter if we have one
						String[] metricArray = metrics.split(",");
						double[] distFilter = new double[metricArray.length];
						setDistanceFilter(distanceFilter, metricArray, distFilter);

						// Compute Matrix
						if (!StringUtils.isAllEmpty(metrics) && !StringUtils.isAllEmpty(onto)) {

							// Get the list of HLGT codes to compute across
							List<MeddraCode> myPTCodes = hlgt.getFaersHlgtPtCodes(ptOnly);

							// Load the FAERS PT codes
							HashMap<String, com.gsk.sia.similarity.json.om.MeddraCode> faers_codes = concept
									.getFaersCodes();

							// Instantiate the distance compute methods
							SimilarityDistance sd = new SimilarityDistance();

							sd.initOntology(onto);
							DistanceMatrix matrix = null;
							if (filterFaers == true) {

								List<MeddraCode> filteredCodes = new ArrayList<>();
								for (MeddraCode mc : myPTCodes) {
									if (faers_codes.containsKey(mc.getMeddraCode()) == true) {
										filteredCodes.add(mc);
									}
								}

								matrix = sd.computeInnerDistanceMatrix(metrics, filteredCodes, distFilter);

							} else {
								matrix = sd.computeInnerDistanceMatrix(metrics, myPTCodes, distFilter);
							}

							if (matrix != null) {

								// Create JSON msg to return
								String message;
								JSONObject json = new JSONObject();
								json.put("matrix", new JSONObject(matrix));
								message = json.toString();

								// Add the output
								context.put("json", message);

							} else {
								context.put("json", "{ \"error\": \"Could not compute distance matrix\"} ");
							}

						} else {
							context.put("json", "{ \"error\": \"Could not compute distance matrix\"} ");
						}

					} else {
						context.put("json", "{ \"error\": \"Could not compute distance matrix\"} ");
					}

				} else {
					context.put("json", "{ \"error\": \"Could not compute distance matrix\"} ");
				}

			}

			/**
			 * Compute matrix of provided list of PT codes
			 */
			if (action.contentEquals("ptMatrix")) {

				// Required parameters
				String ptCodes = data.getParameters().getString(PT_CODES, "");
				String metrics = data.getParameters().getString(METRICS, "INTRINSIC_LIN");
				String onto = data.getParameters().getString(VIEW, "mdr-umls");
				String distanceFilter = data.getParameters().getString(DISTANCE_FILTER, "");

				// All the PTs we want in the matrix
				String[] ptArray = ptCodes.split(",");

				// All the ontologies we want to compute over
				String[] viewArray = onto.split(",");

				// Initialize the similarity distance services once to load the
				// concept graphs into memory
				HashMap<String, SimilarityDistance> sds = new HashMap<>();
				for (String view : viewArray) {
					SimilarityDistance sd = new SimilarityDistance(view);
					sds.put(view, sd);
				}

				// Set the distance filter if we have one
				String[] metricArray = metrics.split(",");
				double[] distFilter = new double[metricArray.length];
				setDistanceFilter(distanceFilter, metricArray, distFilter);

				// Compute Matrix
				if (!StringUtils.isAllEmpty(metrics) && !StringUtils.isAllEmpty(onto)) {
					// Get the Meddra Codes related to this SMQ
					List<MeddraCode> myPTCodes = getMeddraCodes(ptArray, ptOnly);
					// System.out.println("Codes: " + smqCodes.size());

					// Instantiate the distance compute methods
					SimilarityDistance sd = new SimilarityDistance();

					sd.initOntology(onto);
					DistanceMatrix matrix = sd.computeInnerDistanceMatrix(metrics, myPTCodes, distFilter);

					if (matrix != null) {

						// Create JSON msg to return
						String message;
						JSONObject json = new JSONObject();
						json.put("matrix", new JSONObject(matrix));
						message = json.toString();

						// Add the output
						context.put("json", message);

					} else {
						context.put("json", "{ \"error\": \"Could not compute distance matrix\"} ");
					}

				} else {
					context.put("json", "{ \"error\": \"SMQ not found\"} ");
				}
			}

		} catch (Exception e) {
			log.error("Could not load JSON screen for items: " + e.toString());
			context.put("json", EMPTY);
		}

		super.doBuildTemplate(data, context);
	}

	/**
	 * Get list of Meddra Codes based on PTs
	 * 
	 * @param ptCodes
	 * @return
	 */
	private static List<MeddraCode> getMeddraCodes(String[] ptCodes, boolean ptOnly) {
		List<MeddraCode> codes = new ArrayList<>();
		for (String ptCode : ptCodes) {
			MeddraCode mc = MeddraCode.getMeddraCodeByCode(ptCode);
			if (mc != null) {
				if (ptOnly == true) {
					if (mc.getTty().contentEquals("PT"))
						codes.add(mc);
				} else {
					codes.add(mc);
				}
			}
		}
		return codes;
	}

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
