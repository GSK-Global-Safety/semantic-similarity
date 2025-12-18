package com.gsk.sia.similarity.modules.screens.api;

import java.util.HashMap;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.turbine.pipeline.PipelineData;
import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;
import org.json.JSONArray;
import org.json.JSONObject;

import com.gsk.sia.similarity.om.MeddraCode;
import com.gsk.sia.similarity.util.SimilarityDistance;

/**
 * @author painter
 */
public class GetDistance extends SecureScreen {

	// create an instance of the logging facility
	private static Log log = LogFactory.getLog(GetDistance.class);

	private static final String CODE_1 = "code1";
	private static final String CODE_2 = "code2";
	private static final String METRIC = "metric";
	private static final String VIEW = "ontology";
	private static final String PATH_ID = "path";
	private static final String EMPTY = "{ \"distance\": null }";

	@Override
	public void doBuildTemplate(PipelineData pipelineData, Context context) throws Exception {

		RunData data = (RunData) pipelineData;
		try {

			// Required parameters
			String metric = data.getParameters().getString(METRIC, "INTRINSIC_LIN");
			String onto = data.getParameters().getString(VIEW, "mdr-umls");
			String code1 = data.getParameters().getString(CODE_1, "");
			String code2 = data.getParameters().getString(CODE_2, "");

			if (!StringUtils.isAllEmpty(metric) && !StringUtils.isAllEmpty(onto) && !StringUtils.isAllEmpty(code1)
					&& !StringUtils.isAllEmpty(code2)) {
				// Find meddra code from database
				MeddraCode mc1 = MeddraCode.getMeddraCodeByCode(code1);
				MeddraCode mc2 = MeddraCode.getMeddraCodeByCode(code2);
				if (mc1 != null && mc2 != null) {

					SimilarityDistance sd = new SimilarityDistance(onto);
					HashMap<String, Double> metrics = sd.computeDistance(onto, metric, mc1, mc2);
					if (metrics.containsKey(metric) == true) {
						double score = metrics.get(metric);

						// Create JSON msg to return
						String message;
						JSONObject json = new JSONObject();

						JSONObject items = new JSONObject();
						items.put("code1", new JSONObject(mc1.toJson()));
						items.put("code2", new JSONObject(mc2.toJson()));
						json.put("codes", items);

						// array = new JSONArray();
						items = new JSONObject();
						items.put("view", onto);
						items.put("distance_metric", metric);
						items.put("distance", score);
						// array.put(item);

						json.put("metrics", items);
						message = json.toString();

						// Add the output
						context.put("json", message);
					}
				} else {
					context.put("json", "{ \"error\": \"One of the codes provided is not a valid Meddra code\"} ");
				}
			}

		} catch (Exception e) {
			log.error("Could not load JSON screen for items: " + e.toString());
			context.put("json", EMPTY);
		}

		super.doBuildTemplate(data, context);
	}

}
