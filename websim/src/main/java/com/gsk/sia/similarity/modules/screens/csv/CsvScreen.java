package com.gsk.sia.similarity.modules.screens.csv;

import javax.servlet.http.HttpServletResponse;

import org.apache.turbine.modules.screens.VelocitySecureScreen;
import org.apache.turbine.pipeline.PipelineData;
import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;

/**
 * set the content type header for a csv export screen
 *
 * @author painter
 */
public class CsvScreen extends VelocitySecureScreen {

	// CSV
	private static String CONTENT_TYPE = "application/vnd.ms-excel";
	private static String DEFAULT_LAYOUT_VM = "/text.vm";

	@Override
	public void doBuildTemplate(PipelineData pipelineData, Context context) throws Exception {

		RunData data = (RunData) pipelineData;
		String filename = data.getParameters().getString("outfile");
		if (filename != null) {
			filename.trim();
		} else {
			filename = "export.csv";
		}

		data.setLayout(null);

		// log.debug("Changing content type to " + CONTENT_TYPE );
		data.setContentType(CONTENT_TYPE);
		data.setLayoutTemplate(DEFAULT_LAYOUT_VM);

		// log.debug("Set response header type");
		HttpServletResponse resp = data.getResponse();
		resp.setContentType("application/download"); // see if this makes Chrome happy
		resp.setHeader("Content-Type", CONTENT_TYPE);
		resp.setHeader("Content-Disposition", "inline; filename=" + filename);
	}

	@Override
	/**
	 * Open API - no restrictions
	 */
	protected boolean isAuthorized(PipelineData pipelineData) throws Exception {
		return true;
	}

}
