package com.gsk.sia.similarity.modules.screens.api;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.ctakes.ytex.kernel.metric.ConceptSimilarityService.SimilarityMetricEnum;
import org.apache.turbine.pipeline.PipelineData;
import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;
import org.json.JSONArray;
import org.json.JSONObject;

import com.gsk.sia.similarity.json.om.ClusterMetric;
import com.gsk.sia.similarity.json.om.DistanceMatrix;
import com.gsk.sia.similarity.om.MeddraCode;
import com.gsk.sia.similarity.om.MeddraSmq;
import com.gsk.sia.similarity.util.SimilarityDistance;
import com.gsk.sia.similarity.util.tools.UmlsConceptTool;

/**
 * @author painter
 */
public class SMQMatrix extends SecureScreen {

	// create an instance of the logging facility
	private static Log log = LogFactory.getLog(GetDistance.class);

	private static final String MY_ACTION = "methodCall";
	private static final String SMQ_CODE = "smq";
	private static final String INNER_OUTER = "inner";
	private static final String SCOPE = "scope";
	private static final String METRICS = "metrics";
	private static final String VIEW = "ontology";
	private static final String PT_FILTER = "pt_filter";
	private static final String FAERS_FILTER = "faers";
	private static final String DISTANCE_FILTER = "minDistance";
	private static final String EMPTY = "{ \"distance\": null }";

	@Override
	public void doBuildTemplate(PipelineData pipelineData, Context context) throws Exception {

		RunData data = (RunData) pipelineData;

		try {

			// Load the concept tool
			UmlsConceptTool concept = (UmlsConceptTool) context.get("concept");

			String action = data.getParameters().getString(MY_ACTION, "");

			// Required parameters
			String smqCode = data.getParameters().getString(SMQ_CODE, "");
			String innerOuter = data.getParameters().getString(INNER_OUTER, "inner");
			String scope = data.getParameters().getString(SCOPE, "narrow");
			String metrics = data.getParameters().getString(METRICS, "INTRINSIC_LIN");
			String onto = data.getParameters().getString(VIEW, "mdr-umls");
			String distanceFilter = data.getParameters().getString(DISTANCE_FILTER, "");
			
			String ptFilter = data.getParameters().getString(PT_FILTER, "PT_ONLY");
			boolean ptOnly = false;
			if (ptFilter.contentEquals("PT_ONLY"))
				ptOnly = true;
			
			// Filter to only FEARS PT codes
			boolean filterFaers = true;
			String strFilterFears = data.getParameters().getString(FAERS_FILTER, "true");
			if (!strFilterFears.contentEquals("true"))
				filterFaers = false;

			// All the ontologies we want to compute over
			String[] viewArray = onto.split(",");
			
			// Initialize the similarity distance services once to load the
			// concept graphs into memory
			HashMap<String, SimilarityDistance> sds = new HashMap<>();
			for ( String view : viewArray ) {
				SimilarityDistance sd = new SimilarityDistance(view);
				sds.put(view,  sd);
			}

			// Set the distance filter if we have one
			String[] metricArray = metrics.split(",");
			double[] distFilter = new double[metricArray.length];
			setDistanceFilter(distanceFilter, metricArray, distFilter);

			// Compute FAERS filter counts
			if (action.contentEquals("countFaersCodes")) {
				if (!StringUtils.isAllEmpty(smqCode) && !StringUtils.isAllEmpty(scope)
						&& !StringUtils.isAllEmpty(metrics) && !StringUtils.isAllEmpty(onto)) {

					// Load the Meddra SMQ
					MeddraSmq smq = MeddraSmq.getSmq(smqCode, scope);
					if (smq != null) {

						// Get the Meddra Codes related to this SMQ
						List<MeddraCode> smqCodes = smq.getMeddraCodes(ptOnly);

						// Load the FAERS PT codes
						HashMap<String, com.gsk.sia.similarity.json.om.MeddraCode> faers_codes = concept
								.getFaersCodes();

						List<ClusterMetric> results = new ArrayList<>();
						for (String view : viewArray) {

							// Get the SD for this view
							SimilarityDistance sd = sds.get(view);

							if (filterFaers == true) {
								List<MeddraCode> filteredCodes = new ArrayList<>();
								for (MeddraCode mc : smqCodes) {
									if (faers_codes.containsKey(mc.getMeddraCode()) == true) {
										filteredCodes.add(mc);
									}
								}

								// Compute metrics and then add to all results
								List<ClusterMetric> tmpResults = sd.computeOuterMatrixFiltered(smq, metrics, distFilter,
										filteredCodes, faers_codes, ptOnly);
								for (ClusterMetric cm : tmpResults)
									results.add(cm);

							} else {
								// Compute metrics and then add to all results								
								List<ClusterMetric> tmpResults = sd.computeOuterMatrixFiltered(smq, metrics, distFilter,
										smqCodes, faers_codes, ptOnly);
								for (ClusterMetric cm : tmpResults)
									results.add(cm);
							}
						}

						// Create JSON msg to return
						String message;
						JSONObject json = new JSONObject();
						json.put("results", new JSONArray(results));
						message = json.toString();

						// Add the output
						context.put("json", message);
					}
				}

			}

			// Compute distance matrix
			if (action.contentEquals("computeDistanceMatrix")) {
				if (!StringUtils.isAllEmpty(smqCode) && !StringUtils.isAllEmpty(scope)
						&& !StringUtils.isAllEmpty(metrics) && !StringUtils.isAllEmpty(onto)) {

					
					// Load the Meddra SMQ
					MeddraSmq ms = MeddraSmq.getSmq(smqCode, scope);

					// Test that we found the SMQ
					if (ms != null) {

						// Get the Meddra Codes related to this SMQ
						List<MeddraCode> smqCodes = ms.getMeddraCodes(ptOnly);

						// Load the FAERS PT codes
						HashMap<String, com.gsk.sia.similarity.json.om.MeddraCode> faers_codes = concept
								.getFaersCodes();

						// Instantiate the distance compute methods
						SimilarityDistance sd = new SimilarityDistance();
						DistanceMatrix matrix = null;

						if (innerOuter.contentEquals("inner")) {

							if (filterFaers == true) {
								List<MeddraCode> filteredCodes = new ArrayList<>();
								for (MeddraCode mc : smqCodes) {
									if (faers_codes.containsKey(mc.getMeddraCode()) == true) {
										filteredCodes.add(mc);
									}
								}
								sd.initOntology(onto);
								matrix = sd.computeInnerDistanceMatrix(metrics, filteredCodes, distFilter);
							} else {
								sd.initOntology(onto);
								matrix = sd.computeInnerDistanceMatrix(metrics, smqCodes, distFilter);
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
							// Compute outer matrix
							if (filterFaers == true) {
								List<MeddraCode> filteredCodes = new ArrayList<>();
								for (MeddraCode mc : smqCodes) {
									if (faers_codes.containsKey(mc.getMeddraCode()) == true) {
										filteredCodes.add(mc);
									}
								}
								sd.initOntology(onto);
								matrix = sd.computeOuterDistanceMatrix(metrics, filteredCodes, faers_codes, distFilter, ptOnly);
							} else {
								sd.initOntology(onto);
								matrix = sd.computeOuterDistanceMatrix(metrics, smqCodes, faers_codes, distFilter, ptOnly);
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
						}

					} else {
						context.put("json", "{ \"error\": \"SMQ not found\"} ");
					}
				}
			}

		} catch (

		Exception e) {
			log.error("Could not load JSON screen for items: " + e.toString());
			context.put("json", EMPTY);
		}

		super.doBuildTemplate(data, context);
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
