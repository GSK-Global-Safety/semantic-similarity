package com.gsk.sia.similarity.util;


import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.turbine.util.RunData;

import com.gsk.sia.similarity.om.EventLog;
import com.gsk.sia.similarity.om.InsightUser;

/**
 * Create standard system loggin interface
 * 
 * @author painter
 *
 */
public class SysLog {

	/** Logging */
	private static Log log = LogFactory.getLog(SysLog.class);

	/** Transaction Types **/

	// user actions
	public static final int ADD_USER = 5;
	public static final int UPDATE_USER = 6;
	public static final int DELETE_USER = 7;
	public static final int LOGIN_SUCCESS = 11;
	public static final int LOGIN_FAILURE = 12;
	public static final int ACCOUNT_DISABLED = 13;
	public static final int USER_PASSWORD_CHANGE = 14;
	
	public static final int PASSWORD_RESET_REQUEST = 300;
	public static final int PASSWORD_RESET_BAD_LINK = 301;
	public static final int PASSWORD_RESET_SUCCESS = 302;

	/**
	 * Standard logging interface for smartorder event monitoring
	 * 
	 * @param data
	 * @param txType
	 * @param msg
	 */
	public static void log(RunData data, int txType, String msg) {
		try {
			// get the user and company
			InsightUser user = (InsightUser) data.getUser().getTemp("myUser");

			// get the client IP address
			String ip = data.getRequest().getRemoteAddr();

			// Log the event
			EventLog txLog = new EventLog();
			txLog.setInsightUser(user);
			txLog.setTransactionType(txType);
			txLog.setMessage(msg);
			txLog.setTxDate(new Date());
			txLog.setNew(true);
			txLog.save();

		} catch (Exception e) {
			log.error("Could not add system event log: " + e.toString());
		}
	}
}
