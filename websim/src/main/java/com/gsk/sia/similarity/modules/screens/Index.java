package com.gsk.sia.similarity.modules.screens;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.fulcrum.json.JsonService;
import org.apache.turbine.modules.screens.VelocitySecureScreen;
import org.apache.turbine.pipeline.PipelineData;
import org.apache.turbine.services.TurbineServices;
import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;

import com.gsk.sia.similarity.om.MeddraCode;
import com.gsk.sia.similarity.util.SimilarityDistance;
import com.gsk.sia.similarity.util.tools.UmlsConceptTool;

/**
 * This class provides the data required for displaying content in the Velocity
 * page.
 */
public class Index extends VelocitySecureScreen {

	private static final String FAERS_FILTER = "faers";

	/**
	 * This method is called by the Turbine framework when the associated Velocity
	 * template, Index.vm is requested
	 * 
	 * @param data    the Turbine request data
	 * @param context the Velocity context
	 * @throws Exception a generic Exception
	 */
	@Override
	protected void doBuildTemplate(PipelineData pipelineData, Context context) throws Exception {

		RunData data = (RunData) pipelineData;

		// Load the concept tool
		UmlsConceptTool concept = (UmlsConceptTool) context.get("concept");

		//
		// Find related codes
		//
		String findRelated = data.getParameters().getString("matchRelated", "");
		if (StringUtils.isAllEmpty(findRelated) == false) {
			String code1 = data.getParameters().getString("code1", "");
			if (StringUtils.isAllEmpty(code1) == false) {

				String filter = data.getParameters().getString("filter", "PT_ONLY");
				boolean ptOnly = false;
				if (filter.contentEquals("PT_ONLY"))
					ptOnly = true;

				// Filter to only FEARS PT codes
				boolean filterFaers = true;
				String strFilterFears = data.getParameters().getString(FAERS_FILTER, "true");
				if (!strFilterFears.contentEquals("true"))
					filterFaers = false;

				// Load the FAERS PT codes
				HashMap<String, com.gsk.sia.similarity.json.om.MeddraCode> faers_codes = concept.getFaersCodes();

				double limit = 0.95;
				String metric = data.getParameters().getString("metric", "INTRINSIC_LIN");
				String str_lowerLimit = data.getParameters().getString("limit", "0.95");
				try {
					limit = Double.parseDouble(str_lowerLimit);
				} catch (Exception e) {

				}

				// Selected ontology
				String onto = data.getParameters().getString("ontology", "mdr-umls");
				MeddraCode mc1 = MeddraCode.getMeddraCodeByCode(code1);
				context.put("matchCode", mc1);
				context.put("limit", limit);
				context.put("onto", onto);
				context.put("metric", metric);

				// Find matching codes!
				SimilarityDistance sd = new SimilarityDistance(onto);
				HashMap<MeddraCode, Double> results = null;

				if (filterFaers == true) {
					results = sd.getRelatedCodes(mc1, metric, limit, ptOnly, faers_codes);
				} else {
					results = sd.getRelatedCodes(mc1, metric, limit, ptOnly, null);
				}

				context.put("matchResults", results);
			}
		}

		// Ontology view
		String onto = data.getParameters().getString("ontology", "mdr-umls");
		String code1 = data.getParameters().getString("code1", "");
		String code2 = data.getParameters().getString("code2", "");
		if (!StringUtils.isAllEmpty(code1) && !StringUtils.isAllEmpty(code2)) {

			// Find meddra code from database
			MeddraCode mc1 = MeddraCode.getMeddraCodeByCode(code1);
			MeddraCode mc2 = MeddraCode.getMeddraCodeByCode(code2);

			SimilarityDistance sd = new SimilarityDistance(onto);
			HashMap<String, Double> metrics = sd.computeDistance(mc1, mc2);
			List<MetricScore> scores = new ArrayList<>();
			for (String m : metrics.keySet()) {
				MetricScore ms = new MetricScore();
				ms.setMetric(m);
				ms.setScore(metrics.get(m));
				scores.add(ms);
			}

			// Populate results
			context.put("onto", onto);
			context.put("results", scores);
			context.put("code1", mc1);
			context.put("code2", mc2);
		}
	}

	/**
	 * This method is called bythe Turbine framework to determine if the current
	 * user is allowed to use this screen. If this method returns false, the
	 * doBuildTemplate() method will not be called. If a redirect to some "access
	 * denied" page is required, do the necessary redirect here.
	 * 
	 * return always <code>true</code>true, to show this screen as default
	 */
	@Override
	protected boolean isAuthorized(PipelineData pipelineData) throws Exception {
		// use data.getACL()
		return true;
	}

	public class MetricScore {
		private String metric = "NA";
		private double score = 0.0;

		public void setMetric(String m) {
			this.metric = m;
		}

		public void setScore(double s) {
			this.score = s;
		}

		public String getMetric() {
			return this.metric;
		}

		public double getScore() {
			return this.score;
		}
	}
}
