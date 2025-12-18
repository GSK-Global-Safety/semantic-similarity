package com.gsk.sia.similarity.modules.actions;

import java.util.Date;

import org.apache.commons.configuration2.Configuration;
import org.apache.fulcrum.security.util.PasswordMismatchException;
import org.apache.turbine.annotation.TurbineConfiguration;
import org.apache.turbine.annotation.TurbineService;
import org.apache.turbine.modules.actions.VelocityAction;
import org.apache.turbine.om.security.User;
import org.apache.turbine.pipeline.PipelineData;
import org.apache.turbine.services.security.SecurityService;
import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;

import com.gsk.sia.similarity.om.EventLog;
import com.gsk.sia.similarity.om.InsightUser;

/**
 * This class provides a simple set of methods to reset the user's password
 *
 * @author painter
 */
public class PasswordReset extends VelocityAction {

	/** Injected service instance */
	@TurbineService
	private SecurityService security;

	/** Injected configuration instance */
	@TurbineConfiguration
	private Configuration conf;

	/**
	 * force update on user password
	 */
	public void doChange(PipelineData pipelineData, Context context) throws Exception {

		// Get the rundata
		RunData data = (RunData) pipelineData;

		Date today = new Date();

		// get the current user
		User user = data.getUser();
		InsightUser admin = (InsightUser) user.getTemp("myUser");

		String newPassword = data.getParameters().getString("newPassword").trim();
		String confirmNewPassword = data.getParameters().getString("confirmNewPassword").trim();

		if (newPassword.length() < 4) {
			data.setMessage("Your password is too short, must be greater than 4 characters long.");
			return;
		}

		if (!newPassword.equals(confirmNewPassword)) {
			data.setMessage("Your password does not match the confirmation, please try again.");
			return;
		} else {

			try {
				String oldPassword = user.getPassword();

				try {
					security.changePassword(user, oldPassword, newPassword);
					data.setMessage(
							"Your password has been changed. You can log out and log back in using your new password.");

					// Log the event
					EventLog eLog = new EventLog();
					eLog.setInsightUser(admin);
					eLog.setMessage("User set new password");
					eLog.setTxDate(today);
					eLog.setNew(true);
					eLog.save();

				} catch (PasswordMismatchException e) {
					log.error("PasswordReset doChange error: " + e.toString());
					data.setMessage("Your password was not changed due to some error in the system. "
							+ "Please alert the administrator.");
				}

			} catch (Exception e) {
				log.error("PasswordReset doChange error: " + e.toString());
				data.setMessage("Your password was not changed due to some error in the system. "
						+ "Please alert the administrator.");
			}
		}
	}

	/**
	 * default action
	 */
	public void doPerform(PipelineData pipelineData, Context arg1) throws Exception {
		RunData data = (RunData) pipelineData;
		data.setMessage("Invalid button!");
	}

}
