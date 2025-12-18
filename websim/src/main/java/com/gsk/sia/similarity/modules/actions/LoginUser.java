package com.gsk.sia.similarity.modules.actions;

import java.util.Date;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.fulcrum.security.util.DataBackendException;
import org.apache.fulcrum.security.util.FulcrumSecurityException;
import org.apache.fulcrum.security.util.PasswordMismatchException;
import org.apache.fulcrum.security.util.UnknownEntityException;
import org.apache.torque.criteria.Criteria;
import org.apache.turbine.TurbineConstants;
import org.apache.turbine.annotation.TurbineConfiguration;
import org.apache.turbine.annotation.TurbineService;
import org.apache.turbine.modules.Action;
import org.apache.turbine.om.security.User;
import org.apache.turbine.pipeline.PipelineData;
import org.apache.turbine.services.security.SecurityService;
import org.apache.turbine.util.RunData;

import com.gsk.sia.similarity.om.EventLog;
import com.gsk.sia.similarity.om.InsightUser;
import com.gsk.sia.similarity.om.InsightUserPeer;
import com.gsk.sia.similarity.util.SysLog;
import com.gsk.sia.similarity.wrapper.TurbineUserWrapper;

/**
 * This is where we authenticate the user logging into the system against a user
 * in the database. If the user exists in the database that users last login
 * time will be updated.
 *
 * @version $Id$
 */
public class LoginUser implements Action {

	/** CGI Parameter for the user name */
	public static final String CGI_USERNAME = "username";

	/** CGI Parameter for the password */
	public static final String CGI_PASSWORD = "password";

	/** Logging */
	private static Log log = LogFactory.getLog(LoginUser.class);

	/** Injected service instance */
	@TurbineService
	private SecurityService security;

	@TurbineConfiguration(TurbineConstants.LOGIN_ERROR)
	private String loginError = "";

	@TurbineConfiguration(TurbineConstants.TEMPLATE_LOGIN)
	private String templateLogin;

	@TurbineConfiguration(TurbineConstants.SCREEN_LOGIN)
	private String screenLogin;

	/** Maximum number of login attempts **/
	private static int MAX_LOGIN_ATTEMPTS = 4;
	private static String LOCKOUT_MSG = "Your account has been locked out. Please contact your admin, or send an email to support@jivecast.com to have your account reset.";

	/**
	 * Checks for anonymous user, else calls parent method.
	 *
	 * @param pipelineData Turbine information.
	 * @exception FulcrumSecurityException could not get instance of the anonymous
	 *                                     user
	 */
	@Override
	public void doPerform(PipelineData pipelineData) throws FulcrumSecurityException {
		RunData data = (RunData) pipelineData;

		// make this variable accessible to the exception handler
		String username = data.getParameters().getString(CGI_USERNAME, "");
		String password = data.getParameters().getString(CGI_PASSWORD, "");

		if (StringUtils.isEmpty(username)) {
			reset(data, "Enter your username");
			return;
		}

		if (StringUtils.isEmpty(password)) {
			reset(data, "Enter your password");
			return;
		}

		if (username.equals(security.getAnonymousUser().getName())) {

			((RunData) data).setMessage("Anonymous user cannot login");
			reset(data, "");
			return;
		}

		try {

			// Authenticate the user and get the object.
			TurbineUserWrapper user = security.getAuthenticatedUser(username, password);
			setDatabaseUserMap(user);

			// use the InsightUser map to see if we are active/enabled
			InsightUser InsightUser = (InsightUser) user.getTemp("myUser");
			if (InsightUser != null) {

				if (InsightUser.getLoginAttempts() > MAX_LOGIN_ATTEMPTS) {
					// disable the account
					try {
						InsightUser.setEnabled(false);
						InsightUser.save();
					} catch (Exception e) {
						log.error("Error disabling account for user [" + username + "]: " + e.toString());
					}

					reset(data, LOCKOUT_MSG);
					return;

				} else {

					if (InsightUser.isActive() == true && InsightUser.isEnabled() == true) {

						// reset the login attempt counter
						try {
							InsightUser.setLoginAttempts(0);
							InsightUser.save();
						} catch (Exception ce) {
							log.error("Error resetting user's login attempts");
						}

						// Store the user object.
						data.setUser(user);

						// Mark the user as being logged in.
						user.setHasLoggedIn(Boolean.TRUE);

						// Set the last_login date in the database.
						user.updateLastLogin();

						// This only happens if the user is valid; otherwise, we
						// will get a valueBound in the User object when we don't
						// want to because the username is not set yet. Save the
						// User object into the session.
						data.save();

						// record the login
						SysLog.log(data, SysLog.LOGIN_SUCCESS, "Logged in!");

						data.setScreenTemplate("Index.vm");
						return;
						
					} else {
						reset(data, LOCKOUT_MSG);
						return;
					}
				}

			} else {
				reset(data, "Could not locate user details");
				return;
			}

		} catch (PasswordMismatchException pme) {

			// invalid password, do some logging an increase the login attempt counter
			if (!StringUtils.isEmpty(username)) {
				if (security.accountExists(username)) {
					TurbineUserWrapper user = security.getUser(username);
					setDatabaseUserMap(user);

					// use the InsightUser map to see if we are active/enabled
					InsightUser InsightUser = (InsightUser) user.getTemp("myUser");
					if (InsightUser != null) {

						// record the login attempt
						logAuthenticationError(username);

						try {

							// update login attempts for this user
							InsightUser.setLoginAttempts(InsightUser.getLoginAttempts() + 1);

							// lock out
							if (InsightUser.getLoginAttempts() > MAX_LOGIN_ATTEMPTS)
								InsightUser.setEnabled(false);

							InsightUser.save();
						} catch (Exception ipme) {
						}
					}
				}
			}

			// redirect failed login attempt
			reset(data, "Login failed");

		} catch (Exception e) {
			if (e instanceof DataBackendException) {
				log.error(e);
			}

			reset(data, "");
		}
	}

	private void reset(RunData data, String msg) throws UnknownEntityException {

		// Set message if provided
		if (!StringUtils.isEmpty(msg))
			data.setMessage(msg);

		User anonymousUser = security.getAnonymousUser();
		data.setUser(anonymousUser);

		if (StringUtils.isNotEmpty(templateLogin)) {
			// We're running in a templating solution
			data.setScreenTemplate(templateLogin);
		} else {
			data.setScreen(screenLogin);
		}
	}

	/**
	 * Set this user's InsightUser and company on login
	 * 
	 * @param user
	 */
	private void setDatabaseUserMap(User user) {
		try {

			Criteria criteria = new Criteria();
			criteria.where(InsightUserPeer.TURBINE_USER_ID, user.getId());
			InsightUser myUser = InsightUserPeer.doSelectSingleRecord(criteria);
			if (myUser != null) {

				// update login timestamp
				myUser.setLastLogin(new Date());
				myUser.save();
				user.setTemp("myUser", myUser);

			}
			return;
		} catch (Exception e) {
			return;
		}
	}

	/**
	 * Log authentication errors
	 *
	 * @param username
	 * @throws DataBackendException
	 */
	private void logAuthenticationError(String username) {
		try {
			TurbineUserWrapper user = security.getUser(username);
			setDatabaseUserMap(user);

			EventLog t_log = new EventLog();
			t_log.setInsightUser((InsightUser) user.getTemp("myUser"));
			t_log.setTransactionType(SysLog.LOGIN_FAILURE);
			t_log.setMessage("denied login for: " + username);
			t_log.setTxDate(new Date());
			t_log.setNew(true);
			t_log.save();
		} catch (Exception e) {
			log.error(e);
		}
	}

}
