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
import org.json.JSONArray;
import org.json.JSONObject;

import com.gsk.sia.similarity.json.om.DistanceMatrix;
import com.gsk.sia.similarity.om.MeddraCode;
import com.gsk.sia.similarity.util.SimilarityDistance;
import com.gsk.sia.similarity.util.tools.UmlsConceptTool;

/**
 * @author painter
 */
public class MeddraHierarchy extends SecureScreen {

	// create an instance of the logging facility
	private static Log log = LogFactory.getLog(MeddraHierarchy.class);

	private static final String MY_ACTION = "methodCall";
	private static final String METRICS = "metrics";
	private static final String VIEW = "ontology";
	private static final String PT_FILTER = "pt_filter";
	private static final String FAERS_FILTER = "faers";
	private static final String DISTANCE_FILTER = "minDistance";
	private static final String EMPTY = "{ \"distance\": null }";
	
	// for normalized API call
	private static final String MIN_METRIC_RANGE = "minRange";
	private static final String MAX_METRIC_RANGE = "maxRange";

	@Override
	public void doBuildTemplate(PipelineData pipelineData, Context context) throws Exception {

		log.debug("MedDRA Hierarchy API called");

		RunData data = (RunData) pipelineData;

		try {
			// Required parameters
			String action = data.getParameters().getString(MY_ACTION, "");

			if (!StringUtils.isAllEmpty(action)) {

				/**
				 * Get codes that co-exist in the preferred HLT
				 */
				if (action.contentEquals("getRelatedHltCodes")) {
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

						// PT Only filter
						boolean ptOnly = false;
						if (ptFilter.contentEquals("PT_ONLY"))
							ptOnly = true;

						// Load the FAERS PT codes
						HashMap<String, com.gsk.sia.similarity.json.om.MeddraCode> faers_codes = concept
								.getFaersCodes();

						// MedDRA code
						MeddraCode mc = MeddraCode.getMeddraCodeByCode(code);
						List<MeddraCode> hltCodes = mc.getRelatedHltCodes();

						if (hltCodes != null) {

							// Create JSON msg to return
							String message;
							JSONObject json = new JSONObject();

							List<com.gsk.sia.similarity.json.om.MeddraCode> jCodes = new ArrayList<>();
							for (MeddraCode hltCode : hltCodes) {
								boolean addCode = true;
								if (ptOnly == true) {
									if (!hltCode.getMeddraCodeType().getCodeType().contentEquals("PT"))
										addCode = false;

								}

								if (addCode) {
									// Index by the PT code
									String lbl = hltCode.getMeddraCode();
									// Get SMQ's without codes attached
									com.gsk.sia.similarity.json.om.MeddraCode jMc = new com.gsk.sia.similarity.json.om.MeddraCode(
											hltCode);
									jCodes.add(jMc);
								}
							}

							json.put("codes", new JSONArray(jCodes));
							message = json.toString();

							// Add the output
							context.put("json", message);

						} else {
							context.put("json", "{ \"error\": \"Could not load related codes\"} ");
						}

					}
				}

				/**
				 * Get codes that co-exist in the preferred HLGT
				 */
				if (action.contentEquals("getRelatedHlgtCodes")) {
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

						// PT Only filter
						boolean ptOnly = false;
						if (ptFilter.contentEquals("PT_ONLY"))
							ptOnly = true;

						// Load the FAERS PT codes
						HashMap<String, com.gsk.sia.similarity.json.om.MeddraCode> faers_codes = concept
								.getFaersCodes();

						// MedDRA code
						MeddraCode mc = MeddraCode.getMeddraCodeByCode(code);
						List<MeddraCode> hlgtCodes = mc.getRelatedHlgtCodes();

						if (hlgtCodes != null) {

							// Create JSON msg to return
							String message;
							JSONObject json = new JSONObject();

							List<com.gsk.sia.similarity.json.om.MeddraCode> jCodes = new ArrayList<>();
							for (MeddraCode hlgtCode : hlgtCodes) {
								boolean addCode = true;
								if (ptOnly == true) {
									if (!hlgtCode.getMeddraCodeType().getCodeType().contentEquals("PT"))
										addCode = false;

								}

								if (addCode) {
									// Index by the PT code
									String lbl = hlgtCode.getMeddraCode();
									// Get SMQ's without codes attached
									com.gsk.sia.similarity.json.om.MeddraCode jMc = new com.gsk.sia.similarity.json.om.MeddraCode(
											hlgtCode);
									jCodes.add(jMc);
								}
							}

							json.put("codes", new JSONArray(jCodes));
							message = json.toString();

							// Add the output
							context.put("json", message);

						} else {
							context.put("json", "{ \"error\": \"Could not load related codes\"} ");
						}

					}
				}

				/**
				 * Get codes related to this code
				 * This method only works with a single Metric, Min/Max provided
				 * and the distance filter is provided without normalization
				 */
				if (action.contentEquals("getNormalizedRelatedCodes")) {

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

						// Get values for normalization
						double minValue = data.getParameters().getDouble(MIN_METRIC_RANGE, 0.0);
						double maxValue = data.getParameters().getDouble(MAX_METRIC_RANGE, 1.0);

						// PT Only filter
						boolean ptOnly = false;
						if (ptFilter.contentEquals("PT_ONLY"))
							ptOnly = true;

						// Set the distance filter if we have one
						String[] metricArray = metrics.split(",");
						double[] distFilter = new double[metricArray.length];
						setDistanceFilter(distanceFilter, metricArray, distFilter);
						
						// Only using a single metric
						if ( metricArray.length == 1 )
						{
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
								matrix = sd.getNormalizedRelatedCodeMatrix(mc, metricArray, distFilter, ptOnly, faers_codes, minValue, maxValue);
							} else {
								matrix = sd.getNormalizedRelatedCodeMatrix(mc, metricArray, distFilter, ptOnly, null, minValue, maxValue);
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

					}
				}				
				
				
				/**
				 * Get codes related to this code
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
				}

				/**
				 * Provide list of all SOC level terms
				 */
				if (action.contentEquals("listSOC")) {

					List<MeddraCode> socs = MeddraCode.getAllSOCs();

					// Create JSON msg to return
					String message;
					JSONObject json = new JSONObject();

					JSONObject items = new JSONObject();
					int id = 1;
					for (MeddraCode mc : socs) {
						// Index by the PT code
						String lbl = mc.getMeddraCode();
						items.put(lbl, new JSONObject(mc.toJson()));
						id++;
					}

					json.put("socs", items);
					message = json.toString();

					// Add the output
					context.put("json", message);
				}

				/**
				 * Provide list of all HLGT level terms
				 */
				if (action.contentEquals("listHLGT")) {

					List<MeddraCode> hlgts = MeddraCode.getAllHLGTs();

					// Create JSON msg to return
					String message;
					JSONObject json = new JSONObject();

					JSONObject items = new JSONObject();
					for (MeddraCode mc : hlgts) {
						// Index by the PT code
						String lbl = mc.getMeddraCode();
						items.put(lbl, new JSONObject(mc.toJson()));
					}

					json.put("hlgts", items);
					message = json.toString();

					// Add the output
					context.put("json", message);
				}

				/**
				 * Provide list of all PT's captured by an HLGT level terms
				 */
				if (action.contentEquals("getParents")) {

					// Required parameters
					String pt_code = data.getParameters().getString("code", "");
					if (!StringUtils.isEmpty(pt_code)) {
						MeddraCode myCode = MeddraCode.getMeddraCodeByCode(pt_code);
						if (myCode != null) {
							List<MeddraCode> parents = myCode.getParents();

							// Create JSON msg to return
							String message;
							JSONObject json = new JSONObject();

							JSONObject items = new JSONObject();
							for (MeddraCode mc : parents) {
								// Index by the PT code
								String lbl = mc.getMeddraCode();
								items.put(lbl, new JSONObject(mc.toJson()));
							}

							json.put("parents", items);
							message = json.toString();

							// Add the output
							context.put("json", message);
						}
					}
				}

				/**
				 * Provide list of all PT's captured by an HLGT level terms
				 */
				if (action.contentEquals("getHlgtTerms")) {

					// Required parameters
					String ptFilter = data.getParameters().getString(PT_FILTER, "PT_ONLY");

					// PT Only filter
					boolean ptOnly = false;
					if (ptFilter.contentEquals("PT_ONLY"))
						ptOnly = true;

					// Required parameters
					String hlgt_code = data.getParameters().getString("hlgt", "");
					if (!StringUtils.isEmpty(hlgt_code)) {
						MeddraCode hlgt = MeddraCode.getMeddraCodeByCode(hlgt_code);
						com.gsk.sia.similarity.json.om.HLGT jHlgt = new com.gsk.sia.similarity.json.om.HLGT(hlgt,
								ptOnly);
						JSONObject json = new JSONObject();
						json.put("hlgt", new JSONObject(jHlgt));
						String message = json.toString();
						context.put("json", message);
					}
				}

			} else {
				context.put("json", "{ \"error\": \"No action specified\"} ");
			}

		}catch(

	Exception e)
	{
			context.put("json", "{ \"error\": \"No action specified\"} ");
		}

	super.doBuildTemplate(data,context);
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
