package com.gsk.sia.similarity.modules.screens.meddra;


import org.apache.turbine.pipeline.PipelineData;
import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;

import com.gsk.sia.similarity.om.MeddraCode;

/**
 * This class provides the data required for displaying content in the Velocity
 * page.
 */
public class Hlgt extends SecureScreen {

	/**
	 * Add all SOCs to the template
	 */
	@Override
	protected void doBuildTemplate(PipelineData pipelineData, Context context) throws Exception {

		RunData data = (RunData) pipelineData;
		context.put("hlgts", MeddraCode.getAllHLGTs());
	}

}
