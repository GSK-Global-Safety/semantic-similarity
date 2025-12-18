package com.gsk.sia.similarity.modules.actions.admin;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.HashMap;

import javax.servlet.http.Part;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.turbine.Turbine;
import org.apache.turbine.om.security.User;
import org.apache.turbine.pipeline.PipelineData;
import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;

import com.gsk.sia.similarity.om.EventLog;
import com.gsk.sia.similarity.om.FaersCode;
import com.gsk.sia.similarity.om.InsightUser;
import com.gsk.sia.similarity.om.MeddraCode;
import com.gsk.sia.similarity.om.MeddraSmq;
import com.gsk.sia.similarity.om.MeddraSmqCode;

public class DataImportSQL extends SecureAction {

	/** Logging */
	private static Log log = LogFactory.getLog(DataImportSQL.class);

	private static HashMap<String, Boolean> FILE_TYPES = new HashMap<>();

	/**
	 * Correct file name handling with Internet Explorer users
	 */
	public static String fixFileName(String longFile) {
		int loc = longFile.lastIndexOf("\\");
		return longFile.substring((loc + 1), longFile.length());
	}

	private void initFileTypes() {
		FILE_TYPES.put("xlsx", true);
		FILE_TYPES.put("xls", true);
		FILE_TYPES.put("txt", true);
		FILE_TYPES.put("csv", true);
		FILE_TYPES.put("tsv", true);
		FILE_TYPES.put("html", true);
		FILE_TYPES.put("htm", true);
		return;
	}

	public void doFaers(PipelineData pipelineData, Context context) {

		RunData data = (RunData) pipelineData;
		initFileTypes();

		try {
			// get the current user
			User user = data.getUser();
			InsightUser admin = (InsightUser) user.getTemp("myUser");

			// get our text tool
			Date today = new Date();

			// Log the event
			EventLog eLog = new EventLog();
			eLog.setInsightUser(admin);
			eLog.setMessage("Importing new MedDRA SMQ data");
			eLog.setTxDate(today);
			eLog.setNew(true);
			eLog.save();

			//
			//
			// unpack the file and save to database
			//
			Part fileItem = data.getParameters().getPart("file");

			if (fileItem != null) {
				String fileName = fileItem.getSubmittedFileName();
				String[] f = fileName.split("\\.");
				if (f.length > 0) {
					String extension = f[f.length - 1];
					if (FILE_TYPES.containsKey(extension) == true) {

						// save the uploaded data into a byte array
						InputStream is = fileItem.getInputStream();
						byte[] byteArray = IOUtils.toByteArray(is);

						// save the file to disk
						String path = Turbine.getRealPath("/");
						String file = path + "/imports/" + fileName;

						File targetFile = new File(file);
						OutputStream outStream = new FileOutputStream(targetFile);
						outStream.write(byteArray);

						outStream.flush();
						outStream.close();

						// Output vars
						int importCnt = 0;
						int skip = 0;

						// -------------------------------------------------------------------------------
						//
						// Defaults for meta-data requirements
						//
						// -------------------------------------------------------------------------------

						// -------------------------------------------------------------------------------
						// This is our import file
						// -------------------------------------------------------------------------------
						BufferedReader br = new BufferedReader(new FileReader(file));

						// -------------------------------------------------------------------------------
						// All files from Epidemico are comma delimited
						// Used PIPE delimit for internal data
						// -------------------------------------------------------------------------------
						CSVParser parser = new CSVParser(br,
								CSVFormat.EXCEL.withDelimiter(',').withQuote('\"').withHeader());

						// Check data fields before break
						boolean field_check = false; // have we tested that all the required
														// fields are present?
						boolean field_pass = true; // did we have a fault when testing the
													// required fields? (Assume true)

						// -------------------------------------------------------------------------------
						// Parse data line by line
						// -------------------------------------------------------------------------------
						for (CSVRecord record : parser) {
							// -------------------------------------------------------------------------------
							//
							// Have we checked the data for all required fields?
							//
							// -------------------------------------------------------------------------------
							if (field_check == false) {
								log.debug("Computing field check for data integrity");

								// Fields expected for input
								final String[] required_fields = { "PT_CODE", "COUNT" };
								for (String rf : required_fields) {
									if (record.isMapped(rf) == false) {
										// If any required field is missing, end the
										// processing and alert user
										field_pass = false;
										String err = "Import file is missing a required field: " + rf;
										log.error(err);
										data.setMessage("err");
										return;
									}
								}

								// Completed field check - set flag
								field_check = true;
								log.debug("Data check completed successfully");
							}

							// -------------------------------------------------------------------------------
							// Do we have all the fields necessary to import?
							// -------------------------------------------------------------------------------
							if (field_pass == true) {
								// Data fields
								String ptCode = "";

								try {
									ptCode = record.get("PT_CODE");
								} catch (Exception f1) {
								}

								// Case entry date
								int count = 0;
								try {
									count = Integer.parseInt(record.get("COUNT"));
								} catch (Exception f1) {
								}

								if (!StringUtils.isAllBlank(ptCode)) {
									MeddraCode mc = MeddraCode.getMeddraCodeByCode(ptCode);
									if (mc != null) {
										FaersCode fc = new FaersCode();
										fc.setMeddraCode(mc);
										fc.setCount(count);
										fc.setNew(true);
										fc.save();

										// Add FAERS code count
										importCnt++;

									} else {
										// Already exists
										skip++;
									}
								}
							}

						} // Next CSV record

						// Tell us how many records were imported
						String msg = "<b>FAERS Count data to the database: [" + importCnt + "] successful</b> <br/>";
						msg = msg + "<b>Records Skipped [" + skip + "]</b> <br/>";
						data.setMessage(msg);
						return;
					}
				}
			}

		} catch (Exception e) {
			log.error("An error occured importing records: " + e.toString());
			data.setMessage("An error occured trying to import FAERS count data");
			return;
		}
	}

	/**
	 * Import data file
	 */
	public void doImport(PipelineData pipelineData, Context context) {

		RunData data = (RunData) pipelineData;
		initFileTypes();

		try {
			// get the current user
			User user = data.getUser();
			InsightUser admin = (InsightUser) user.getTemp("myUser");

			// get our text tool
			Date today = new Date();

			// Log the event
			EventLog eLog = new EventLog();
			eLog.setInsightUser(admin);
			eLog.setMessage("Importing new MedDRA SMQ data");
			eLog.setTxDate(today);
			eLog.setNew(true);
			eLog.save();

			//
			// unpack the file and save to database
			//
			Part fileItem = data.getParameters().getPart("file");

			if (fileItem != null) {
				String fileName = fileItem.getSubmittedFileName();
				String[] f = fileName.split("\\.");
				if (f.length > 0) {
					String extension = f[f.length - 1];
					if (FILE_TYPES.containsKey(extension) == true) {

						// save the uploaded data into a byte array
						InputStream is = fileItem.getInputStream();
						byte[] byteArray = IOUtils.toByteArray(is);

						// save the file to disk
						String path = Turbine.getRealPath("/");
						String file = path + "/imports/" + fileName;

						File targetFile = new File(file);
						OutputStream outStream = new FileOutputStream(targetFile);
						outStream.write(byteArray);

						outStream.flush();
						outStream.close();

						// Output vars
						int importCnt = 0;
						int skip = 0;

						// -------------------------------------------------------------------------------
						//
						// Defaults for meta-data requirements
						//
						// -------------------------------------------------------------------------------

						// -------------------------------------------------------------------------------
						// This is our import file
						// -------------------------------------------------------------------------------
						BufferedReader br = new BufferedReader(new FileReader(file));

						// -------------------------------------------------------------------------------
						// All files from Epidemico are comma delimited
						// Used PIPE delimit for internal data
						// -------------------------------------------------------------------------------
						CSVParser parser = new CSVParser(br,
								CSVFormat.EXCEL.withDelimiter('|').withQuote('\"').withHeader());

						// Check data fields before break
						boolean field_check = false; // have we tested that all the required
														// fields are present?
						boolean field_pass = true; // did we have a fault when testing the
													// required fields? (Assume true)

						// -------------------------------------------------------------------------------
						// Parse data line by line
						// -------------------------------------------------------------------------------
						for (CSVRecord record : parser) {
							// -------------------------------------------------------------------------------
							//
							// Have we checked the data for all required fields?
							//
							// -------------------------------------------------------------------------------
							if (field_check == false) {
								log.debug("Computing field check for data integrity");

								// Fields expected for input
								final String[] required_fields = { "SMQ_CODE", "SMQ", "SCOPE", "PT_CODES" };
								for (String rf : required_fields) {
									if (record.isMapped(rf) == false) {
										// If any required field is missing, end the
										// processing and alert user
										field_pass = false;
										String err = "Import file is missing a required field: " + rf;
										log.error(err);
										data.setMessage("err");
										return;
									}
								}

								// Completed field check - set flag
								field_check = true;
								log.debug("Data check completed successfully");
							}

							// -------------------------------------------------------------------------------
							// Do we have all the fields necessary to import?
							// -------------------------------------------------------------------------------
							if (field_pass == true) {
								// Data fields
								String smqCode = "";
								String smq = "";
								String scope = "";
								String activePtCodes = "";

								try {
									smqCode = record.get("SMQ_CODE");
								} catch (Exception f1) {
								}

								// Case entry date
								try {
									smq = record.get("SMQ");
								} catch (Exception f1) {
								}

								try {
									// English products
									scope = record.get("SCOPE");
								} catch (Exception f1) {
								}

								try {
									// English conditions
									activePtCodes = record.get("PT_CODES");
								} catch (Exception f1) {
								}

								String[] codes = activePtCodes.split(",");
								if (codes.length > 0) {
									if (!StringUtils.isAllBlank(smqCode) && !StringUtils.isAllBlank(smq)
											&& !StringUtils.isAllBlank(scope)) {

										// Check that the SMQ has not been previously added to the database
										if (MeddraSmq.checkSmqExists(smqCode, smq, scope) == false) {
											// Create the SMQ group
											MeddraSmq mSmq = new MeddraSmq();
											mSmq.setCode(smqCode);
											mSmq.setName(smq);
											mSmq.setScope(scope);

											mSmq.setNew(true);
											mSmq.save();

											for (String ptCode : codes) {
												ptCode = ptCode.replaceAll("'", "");
												MeddraCode mc = MeddraCode.getMeddraCodeByCode(ptCode);
												if (mc != null) {

													// Add code to the SMQ
													MeddraSmqCode msc = new MeddraSmqCode();
													msc.setMeddraSmq(mSmq);
													msc.setMeddraCode(mc);
													msc.setNew(true);
													msc.save();
												}
											}
											// Create new SMQ entries
											importCnt++;

										} else {
											// Already exists
											skip++;
										}

									}
								}
							}

						} // Next CSV record

						// Tell us how many records were imported
						String msg = "<b>MedDRA SMQs added to the database: [" + importCnt + "] successful</b> <br/>";
						msg = msg + "<b>Records Skipped [" + skip + "]</b> <br/>";
						data.setMessage(msg);
						return;
					}
				}
			}

		} catch (Exception e) {
			log.error("An error occured importing records: " + e.toString());
			data.setMessage("An error occured trying to import MedDRA SMQs");
			return;
		}
	}

	/**
	 * Implement this to add information to the context.
	 *
	 * @param data    Turbine information.
	 * @param context Context for web pages.
	 * @exception Exception, a generic exception.
	 */
	public void doPerform(PipelineData pipelineData, Context context) {
		RunData data = (RunData) pipelineData;
		data.setMessage("Invalid button!");
	}

}