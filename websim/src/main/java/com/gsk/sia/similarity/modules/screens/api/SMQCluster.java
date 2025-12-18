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

import com.gsk.sia.similarity.json.om.ClusterEntry;
import com.gsk.sia.similarity.om.MeddraCode;
import com.gsk.sia.similarity.om.MeddraSmq;
import com.gsk.sia.similarity.util.SimilarityDistance;
import com.gsk.sia.similarity.util.tools.UmlsConceptTool;

/**
 * @author painter
 */
public class SMQCluster extends SecureScreen {

	// create an instance of the logging facility
	private static Log log = LogFactory.getLog(GetDistance.class);

	// Parameters the user can set
	private static final String SMQ_CODE = "smq";
	private static final String SCOPE = "scope";
	private static final String METRICS = "metrics";
	private static final String VIEWS = "ontologies";
	private static final String EMPTY = "{ \"distance\": null }";

	@Override
	public void doBuildTemplate(PipelineData pipelineData, Context context) throws Exception {

		RunData data = (RunData) pipelineData;

		try {

			// Load the concept tool
			UmlsConceptTool concept = (UmlsConceptTool) context.get("concept");

			// Required parameters
			String smqCode = data.getParameters().getString(SMQ_CODE, "");
			String scope = data.getParameters().getString(SCOPE, "narrow");
			String metrics = data.getParameters().getString(METRICS, "INTRINSIC_LIN");
			String ontologies = data.getParameters().getString(VIEWS, "mdr-umls");

			// Default is to return everything
			String ptFilter = data.getParameters().getString("pt_filter", "ALL");
			boolean ptOnly = false;
			if (ptFilter.contentEquals("PT_ONLY"))
				ptOnly = true;
			
			
			// Set the metrics and ontologies to compute clusters on
			String[] metricArray = metrics.split(",");
			String[] viewArray = ontologies.split(",");

			// Initialize the similarity distance services once to load the
			// concept graphs into memory
			HashMap<String, SimilarityDistance> sds = new HashMap<>();
			for ( String view : viewArray ) {
				SimilarityDistance sd = new SimilarityDistance(view);
				sds.put(view,  sd);
			}
			
			
			if (!StringUtils.isAllEmpty(smqCode) && !StringUtils.isAllEmpty(scope) && !StringUtils.isAllEmpty(metrics)
					&& !StringUtils.isAllEmpty(ontologies)) {

				// Load the Meddra SMQ
				MeddraSmq ms = MeddraSmq.getSmq(smqCode, scope);

				// Test that we found the SMQ
				if (ms != null) {

					// Get the Meddra Codes related to this SMQ
					List<MeddraCode> smqCodes = ms.getMeddraCodes(false);
					// System.out.println("Codes: " + smqCodes.size());

					// Instantiate the distance compute methods
					SimilarityDistance sd = new SimilarityDistance();
					// com.gsk.sia.similarity.json.om.SMQCluster cluster =
					// sd.computeDirectCluster(ms, metricArray, viewArray);

					List<ClusterEntry> cluster = sd.computeLevelClusters(ms, metricArray, viewArray, ptOnly);

					// Create JSON msg to return
					String message;
					JSONObject json = new JSONObject();
					json.put("clusters", new JSONArray(cluster));
					message = json.toString();

					// Add the output
					context.put("json", message);

				} else {
					context.put("json", "{ \"error\": \"SMQ not found\"} ");
				}
			}

		} catch (

		Exception e) {
			log.error("Could not load JSON screen for items: " + e.toString());
			context.put("json", EMPTY);
		}

		super.doBuildTemplate(data, context);
	}



}
