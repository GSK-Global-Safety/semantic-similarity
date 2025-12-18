package com.gsk.sia.similarity.util.tools;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.fulcrum.pool.Recyclable;
import org.apache.turbine.Turbine;
import org.apache.turbine.services.pull.ApplicationTool;

import com.gsk.sia.similarity.om.MeddraCode;

/**
 * UMLS Concept handling tool
 *
 * @author painter
 */
public class UmlsConceptTool implements ApplicationTool, Recyclable {

	// Data structures to store these once after
	// loading from the database
	private HashMap<String, Boolean> meddraConcepts;
	private HashMap<String, com.gsk.sia.similarity.json.om.MeddraCode> faersCodes;

	/** Logging */
	private static Log log = LogFactory.getLog(UmlsConceptTool.class);

	/**
	 * Constructor does initialization stuff
	 */
	public UmlsConceptTool() {
	}

	@Override
	public void init(Object arg0) {
	}

	@Override
	public void refresh() {
	}

	public HashMap<String, com.gsk.sia.similarity.json.om.MeddraCode> getFaersCodes() {
		if (this.faersCodes != null) {
			return this.faersCodes;
		} else {
			this.faersCodes = loadFaersCodes();
			return this.faersCodes;
		}
	}

	/**
	 * Reading from CSV is super fast
	 * 
	 * @return
	 */
	private HashMap<String, com.gsk.sia.similarity.json.om.MeddraCode> loadFaersCodes() {

		HashMap<String, com.gsk.sia.similarity.json.om.MeddraCode> faersCodes = new HashMap<>();
		try {
			String infile = getFaersDataFile();
			BufferedReader br = new BufferedReader(new FileReader(infile));
			CSVParser parser = new CSVParser(br, CSVFormat.EXCEL.withDelimiter(',').withQuote('\"').withHeader());

			for (CSVRecord record : parser) {
				// Data fields
				String ptCode = record.get("PT_CODE");
				String umlsCui = record.get("UMLS_CUI");
				String term = record.get("TERM");
				String tty = record.get("TTY");
				com.gsk.sia.similarity.json.om.MeddraCode mc = new com.gsk.sia.similarity.json.om.MeddraCode();
				mc.setCode(ptCode);
				mc.setTerm(term);
				mc.setTty(tty);
				mc.setCui(umlsCui);

				// Add to the dictionary
				faersCodes.put(ptCode, mc);
			}

		} catch (Exception e) {
		}
		return faersCodes;
	}

	/**
	 * Load the path to our concept graphs
	 * 
	 * @return
	 */
	private String getFaersDataFile() {
		// Get the real path
		String path = Turbine.getRealPath("/");
		String file = path + "/conceptGraphs/faers_umls.csv";
		return file;
	}

	/**
	 * Get all of the UMLS Cui's found for Meddra
	 * 
	 * @return
	 */
	public HashMap<String, Boolean> getMeddraConcepts() {
		if (this.meddraConcepts != null) {
			return this.meddraConcepts;
		} else {
			this.meddraConcepts = new HashMap<>();
			List<String> allCuis = MeddraCode.getAllPTCuis();
			for (String cui : allCuis) {
				this.meddraConcepts.put(cui, true);
			}
			return this.meddraConcepts;

		}
	}

	// ****************** Recyclable implementation ************************
	private boolean disposed;

	/**
	 * Recycles the object for a new client. Recycle methods with parameters must be
	 * added to implementing object and they will be automatically called by pool
	 * implementations when the object is taken from the pool for a new client. The
	 * parameters must correspond to the parameters of the constructors of the
	 * object. For new objects, constructors can call their corresponding recycle
	 * methods whenever applicable. The recycle methods must call their super.
	 */
	@Override
	public void recycle() {
		disposed = false;
	}

	/**
	 * Disposes the object after use. The method is called when the object is
	 * returned to its pool. The dispose method must call its super.
	 */
	@Override
	public void dispose() {
		disposed = true;
	}

	/**
	 * Checks whether the recyclable has been disposed.
	 *
	 * @return true, if the recyclable is disposed.
	 */
	@Override
	public boolean isDisposed() {
		return disposed;
	}

}
