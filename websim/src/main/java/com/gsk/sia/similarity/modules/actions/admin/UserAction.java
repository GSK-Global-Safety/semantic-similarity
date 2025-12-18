package com.gsk.sia.similarity.modules.actions.admin;

import java.util.Date;
import java.util.List;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.fulcrum.security.SecurityService;
import org.apache.fulcrum.security.entity.Group;
import org.apache.fulcrum.security.entity.Role;
import org.apache.fulcrum.security.model.turbine.TurbineAccessControlList;
import org.apache.fulcrum.security.model.turbine.TurbineModelManager;
import org.apache.fulcrum.security.util.GroupSet;
import org.apache.fulcrum.security.util.RoleSet;
import org.apache.turbine.annotation.TurbineConfiguration;
import org.apache.turbine.annotation.TurbineService;
import org.apache.turbine.om.security.User;
import org.apache.turbine.pipeline.PipelineData;
import org.apache.turbine.services.ServiceManager;
import org.apache.turbine.services.TurbineServices;
import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;

import com.gsk.sia.similarity.flux.modules.actions.FluxAction;
import com.gsk.sia.similarity.om.InsightUser;
import com.gsk.sia.similarity.om.TurbineUser;
import com.gsk.sia.similarity.om.TurbineUserGroupRole;
import com.gsk.sia.similarity.om.TurbineUserGroupRolePeer;
import com.gsk.sia.similarity.util.SysLog;
import com.gsk.sia.similarity.wrapper.TurbineUserWrapper;

public class UserAction extends FluxAction {

	/** Logging */
	private static Log log = LogFactory.getLog(UserAction.class);

	/** Injected service instance */
	@TurbineService
	private org.apache.turbine.services.security.SecurityService securityService;

	SecurityService fulcrumSecurityService;

	/** Injected configuration instance */
	@TurbineConfiguration
	private Configuration conf;

	private void setup() {
		ServiceManager serviceManager = TurbineServices.getInstance();
		fulcrumSecurityService = (SecurityService) serviceManager.getService(SecurityService.ROLE);
	}

	public void doUnlock(PipelineData pipelineData, Context context) throws Exception {
		// get the current user
		RunData data = (RunData) pipelineData;

		try {
			List<InsightUser> users = InsightUser.getAllUsers();
			for (InsightUser user : users) {
				// Reset logins and set back to enabled
				user.setLoginAttempts(0);
				user.setEnabled(true);
				user.setActive(true);
				user.save();
			}

			data.setMessage("Unlocked all users");
			return;

		} catch (Exception e) {
			context.put("error", true);
			data.setMessage("An error occurred trying to unlock all users. Please contact the admin");
			return;
		}

	}

	/**
	 * ActionEvent responsible for inserting a new user into the Turbine security
	 * system.
	 */
	public void doInsert(PipelineData pipelineData, Context context) throws Exception {

		// get the current user
		RunData data = (RunData) pipelineData;

		try {

			// logged in user
			User luser = data.getUser();

			if (!data.getParameters().containsKey("username") || !data.getParameters().containsKey("password")) {
				context.put("error", true);
				data.setMessage("Must provide username and password to add a new user");
				return;
			}

			// get the username/password to add
			String username = data.getParameters().getString("username");
			String password = data.getParameters().getString("password");

			/*
			 * Make sure this account doesn't already exist. If the account already exists
			 * then alert the user and make them change the username.
			 */
			if (securityService.accountExists(username) == true) {
				data.setMessage("Account username is invalid, please choose another.");
				return;
			} else {

				if (!StringUtils.isEmpty(username) && !StringUtils.isEmpty(password)) {
					// Create a new user instance and set the default values

					// create the turbine user object directly
					TurbineUser tu = new TurbineUser();
					data.getParameters().setProperties(tu);

					// make sure username is set
					tu.setEntityName(username);

					// save
					tu.setNew(true);
					tu.save();

					// Assign the user to the company
					InsightUser newUser = new InsightUser();

					// copy from the user
					newUser.setFirstName(tu.getFirstName());
					newUser.setLastName(tu.getLastName());
					newUser.setEmail(tu.getEmail());

					// not a vendor, active and enabled
					newUser.setActive(true);
					newUser.setEnabled(true);

					newUser.setTurbineUserId((int) tu.getId());
					newUser.setNew(true);
					newUser.save();

					// Make new users part of the sales team by default
					setRole(data, username, "sales");

					// Use security to force the password
					// not working right now
					// securityService.forcePassword((User) tu, password);

					// create log
					SysLog.log(data, SysLog.ADD_USER, "New user added: " + username);

				} else {
					context.put("error", true);
					data.setMessage("Username or password were blank");
				}
			}
		} catch (Exception e) {
			log.error("Error adding new user: " + e.toString());
		}
	}

	/**
	 * ActionEvent responsible for removing a user
	 */
	public void doDelete(PipelineData pipelineData, Context context) throws Exception {
		try {
			// get the current user
			RunData data = (RunData) pipelineData;
			User luser = data.getUser();
			String username = data.getParameters().getString("username");

			if (!StringUtils.isEmpty(username) && securityService.accountExists(username)) {

				// user the wrapper to access the related objects stored with the acount
				TurbineUserWrapper oldUser = new TurbineUserWrapper(securityService.getUser(username));
				InsightUser insightUser = oldUser.getMyUser();
				if (insightUser != null) {

					// don't delete this as it is linked to orders, etc
					insightUser.setActive(false);
					insightUser.setEnabled(false);
					insightUser.save();

				}

				// find the user object and remove using security mgr
				User user = securityService.getUser(username);
				securityService.removeUser(user);

				data.setMessage("Removed user account successfully: " + username);

				// create log
				SysLog.log(data, SysLog.DELETE_USER, "Deleted user: " + username);

			} else {
				data.setMessage("Could not remove user: " + username);
			}

		} catch (Exception e) {
			log.error("Could not remove user: " + e.toString());
		}
	}

	/**
	 * ActionEvent responsible updating a user in the Tambora system. Must check the
	 * input for integrity before allowing the user info to be update in the
	 * database.
	 */
	public void doUpdate(PipelineData pipelineData, Context context) throws Exception {

		try {
			// get the current user
			RunData data = (RunData) pipelineData;
			User luser = data.getUser();
			InsightUser admin = (InsightUser) luser.getTemp("myUser");

			String username = data.getParameters().getString("username");
			String plainPassword = data.getParameters().getString("password");
			boolean active = data.getParameters().getBoolean("active");

			if (!StringUtils.isEmpty(username)) {

				User pwUser = securityService.getUser(username);
				TurbineUserWrapper oldUser = new TurbineUserWrapper(pwUser);
				InsightUser modifyUser = oldUser.getMyUser();

				// store old password to be safe
				String oldPassword = oldUser.getPassword();
				data.getParameters().setProperties(modifyUser);

				// set boolean values
				modifyUser.setActive(active);

				// did we update password?
				if (StringUtils.isEmpty(plainPassword) != true) {
					// set new encrypted
					securityService.changePassword(pwUser, oldPassword, plainPassword);

					// try again
					securityService.forcePassword(pwUser, plainPassword);
				} else {
					// don't change the password
					oldUser.setPassword(oldPassword);
				}

				modifyUser.setModifiedDate(new Date());
				modifyUser.save();

				// create log
				SysLog.log(data, SysLog.UPDATE_USER, "Updated user: " + username);

			} else {
				log.error("Can not update empty user");
			}
		} catch (Exception e) {
			((RunData) pipelineData).setMessage("Could not update user");
			log.error("Error updating user: " + e.toString());
		}
	}

	/**
	 * Enable a user who was marked disabled
	 */
	public void doEnable(PipelineData pipelineData, Context context) throws Exception {

		RunData data = (RunData) pipelineData;

		try {
			// get the current user
			User luser = data.getUser();
			TurbineAccessControlList acl = data.getACL();
			if (acl.hasRole("administrator")) {
				String username = data.getParameters().getString("username");
				if (securityService.accountExists(username)) {
					TurbineUserWrapper user = new TurbineUserWrapper(securityService.getUser(username));
					InsightUser insightUser = user.getMyUser();

					if (insightUser.isEnabled()) {
						data.setMessage("The user is already enabled");
					} else {
						// reset the login attempts and set back to enabled
						insightUser.setLoginAttempts(0);
						insightUser.setEnabled(true);
						insightUser.save();
						data.setMessage(username + ": Account has been re-enabled.");

						// create log
						SysLog.log(data, SysLog.UPDATE_USER, "Account enabled: " + username);
					}
				} else {
					data.setMessage("The specified user is invalid.");
				}
			} else {
				data.setMessage("You do not have access to perform this action.");
			}

		} catch (Exception e) {
			data.setMessage("The specified user is invalid.");
			log.error(e);
		}
	}

	/**
	 * Disable a user account manually (admin can disable an HCP)
	 */
	public void doDisable(PipelineData pipelineData, Context context) throws Exception {

		RunData data = (RunData) pipelineData;

		try {
			// get the current user
			User luser = data.getUser();
			InsightUser admin = (InsightUser) luser.getTemp("myUser");

			// Get the Turbine ACL implementation
			TurbineAccessControlList acl = data.getACL();
			if (acl.hasRole("administrator")) {
				String username = data.getParameters().getString("username");
				if (securityService.accountExists(username)) {
					TurbineUserWrapper user = new TurbineUserWrapper(securityService.getUser(username));
					InsightUser InsightUser = user.getMyUser();

					if (!InsightUser.isEnabled()) {
						data.setMessage("The user was already disabled");
					} else {
						// reset the login attempts and set back to enabled
						InsightUser.setEnabled(false);
						InsightUser.save();
						data.setMessage(username + ": Account has been disabled.");

						// create log
						SysLog.log(data, SysLog.UPDATE_USER, "Account disabled: " + username);
					}
				} else {
					data.setMessage("The specified user is invalid.");
				}
			} else {
				data.setMessage("You do not have access to perform this action.");
			}

		} catch (Exception e) {
			data.setMessage("The specified user is invalid.");
			log.error(e);
		}

	}

	/**
	 * Update the roles that are to assigned to a user for a project.
	 */
	public void doRoles(PipelineData pipelineData, Context context) throws Exception {

		try {
			RunData data = (RunData) pipelineData;

			// get fulcrum security working
			setup();

			// Get the Turbine ACL implementation for our current user
			TurbineAccessControlList adminAcl = data.getACL();
			if (adminAcl.hasRole("administrator")) {
				String username = data.getParameters().getString("username");
				if (securityService.accountExists(username)) {

					// Must downcast for setting the role
					User ab = securityService.getUserInstance();
					String type = ab.getClass().getTypeName();
					log.debug("Default: " + type);

					User user = securityService.getUser(username);
					type = user.getClass().getTypeName();
					log.debug("User: " + type);

					// try manual build
					// Fulcrum security service returns a raw
					// org.apache.fulcrum.security.model.turbine.entity.impl.TurbineUserImpl,
					org.apache.fulcrum.security.UserManager userManager = fulcrumSecurityService.getUserManager();
					org.apache.fulcrum.security.entity.User fulcrumUser = userManager.getUserInstance(username);
					type = fulcrumUser.getClass().getTypeName();
					log.debug("fulcrumUser: " + type);

					// Get the Turbine ACL implementation
					TurbineAccessControlList acl = securityService.getACL(user);

					/*
					 * Grab all the Groups and Roles in the system.
					 */
					GroupSet groups = securityService.getAllGroups();
					RoleSet roles = securityService.getAllRoles();

					for (Group group : groups) {
						String groupName = group.getName();
						for (Role role : roles) {
							String roleName = role.getName();

							/*
							 * In the UserRoleForm.vm we made a checkbox for every possible Group/Role
							 * combination so we will compare every possible combination with the values
							 * that were checked off in the form. If we have a match then we will grant the
							 * user the role in the group.
							 */
							String groupRole = groupName + roleName;
							String formGroupRole = data.getParameters().getString(groupRole);

							if (formGroupRole != null && !acl.hasRole(role, group)) {
								// add the role for this user
								if (acl.hasRole(role) == false) {
									log.debug("Adding new role to user: " + role.getName());

									// try using fulcrum service
									((TurbineModelManager) fulcrumSecurityService.getModelManager()).grant(fulcrumUser,
											group, role);

									securityService.grant(user, group, role);

								}
							} else if (formGroupRole == null && acl.hasRole(role, group)) {
								// revoke the role for this user
								log.debug("Revoke role: " + role.getName());

								securityService.revoke(user, group, role);
							}
						}
					}

				} else {
					log.error("User does not exist!");
				}
			} else {
				data.setMessage("You do not have access to perform this action.");
			}
		} catch (Exception e) {
			log.error("Error setting roles: " + e.toString());
		}

	}

	/**
	 * Update the roles that are to assigned to a user for a project.
	 */
	public void setRole(RunData data, String username, String roleName) {

		try {
			if (!StringUtils.isEmpty(username)) {
				if (securityService.accountExists(username)) {
					User user = securityService.getUser(username);

					// Get the Turbine ACL implementation
					TurbineAccessControlList acl = securityService.getUserManager().getACL(user);
					GroupSet groups = securityService.getAllGroups();
					RoleSet roles = securityService.getAllRoles();

					for (Group group : groups) {
						for (Role role : roles) {
							if (roleName.equals(role.getName())) {
								TurbineUserGroupRole tugr = new TurbineUserGroupRole();
								tugr.setRoleId((Integer) role.getId());
								tugr.setGroupId((Integer) group.getId());
								tugr.setUserId((Integer) user.getId());
								tugr.setNew(true);
								TurbineUserGroupRolePeer.doInsert(tugr);
							}
						}
					}

				} else {
					log.error("User does not exist!");
				}
			}

		} catch (Exception e) {
			log.error("Error on role assignment: " + e);
		}
	}

	/**
	 * Implement this to add information to the context.
	 */
	public void doPerform(RunData data, Context context) throws Exception {
		log.info("Running do perform!");
		data.setMessage("Can't find the requested action!");
	}

}
