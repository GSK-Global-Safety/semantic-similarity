package com.gsk.sia.similarity.modules.screens.meddra;

import org.apache.commons.configuration2.Configuration;
import org.apache.turbine.TurbineConstants;
import org.apache.turbine.annotation.TurbineConfiguration;
import org.apache.turbine.annotation.TurbineService;
import org.apache.turbine.modules.screens.VelocitySecureScreen;
import org.apache.turbine.pipeline.PipelineData;
import org.apache.turbine.services.security.SecurityService;
import org.apache.velocity.context.Context;

/**
 * This class provides a sample implementation for creating a secured screen
 */
public class SecureScreen extends VelocitySecureScreen {
	@TurbineService
	protected SecurityService securityService;

	@TurbineConfiguration(TurbineConstants.TEMPLATE_LOGIN)
	private Configuration templateLogin;

	@TurbineConfiguration(TurbineConstants.TEMPLATE_HOMEPAGE)
	private Configuration templateHomepage;

	@Override
	protected boolean isAuthorized(PipelineData pipelineData) throws Exception {
		return true;
	}

	@Override
	protected void doBuildTemplate(PipelineData pipelineData, Context context) throws Exception {

	}
}
