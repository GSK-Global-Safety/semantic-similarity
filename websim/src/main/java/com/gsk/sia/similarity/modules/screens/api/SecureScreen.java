package com.gsk.sia.similarity.modules.screens.api;


import javax.servlet.http.HttpServletResponse;

import org.apache.turbine.annotation.TurbineService;
import org.apache.turbine.modules.screens.VelocitySecureScreen;
import org.apache.turbine.pipeline.PipelineData;
import org.apache.turbine.services.security.SecurityService;
import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;

/**
 * API Parent has no security
 * Output in JSON format
 */
public class SecureScreen extends VelocitySecureScreen {
	@TurbineService
	protected SecurityService securityService;

	// JSON data
	private static String CONTENT_TYPE = "application/json";
	private static String DEFAULT_LAYOUT_VM = "/text.vm";

	@Override
	public void doBuildTemplate(PipelineData pipelineData, Context context) throws Exception {
		
		RunData data = (RunData) pipelineData;
		data.setLayout(null);

		// log.debug("Changing content type to " + CONTENT_TYPE );
		data.setContentType(CONTENT_TYPE);
		data.setLayoutTemplate(DEFAULT_LAYOUT_VM);

		// log.debug("Set response header type");
		HttpServletResponse resp = data.getResponse();
		resp.setHeader("Content-Type", CONTENT_TYPE);
	}

	@Override
	/**
	 * Open API - no restrictions
	 */
	protected boolean isAuthorized(PipelineData pipelineData) throws Exception {
		return true;
	}
}