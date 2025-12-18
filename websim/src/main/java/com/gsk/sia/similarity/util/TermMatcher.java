package com.gsk.sia.similarity.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.torque.criteria.Criteria;

import com.gsk.sia.similarity.om.MeddraCode;
import com.gsk.sia.similarity.om.MeddraCodePeer;

/**
 * This is a utility class to help match terms to UMLS concepts
 */
public class TermMatcher {

	/** Logging */
	private static Log log = LogFactory.getLog(TermMatcher.class);

	private static final TermMatcher instance = new TermMatcher();

	// Local UMLS
	private String MRCONSO = "";

	// MedDRA
	private HashMap<String, com.gsk.sia.similarity.json.om.MeddraCode> MEDDRA_ATOMS = null;
	private HashMap<String, BiGramSequence> MEDDRA_BIGRAM = null;

	public static TermMatcher getInstance() {
		return instance;
	}

	/**
	 * Setup data structures
	 */
	private TermMatcher() {

		log.info("Creating TermMatcher instance");
		// this.MRCONSO = "/home/painter/umls/mdr_msh_sct_2024AA/2024AA/META/MRCONSO.RRF";
		// Only load if no atoms are in memory
		if (this.MEDDRA_ATOMS == null) {
			this.MEDDRA_ATOMS = new HashMap<>();
			this.MEDDRA_BIGRAM = new HashMap<>();

			// Load meddra
			this.loadMedDRA();
			// this.loadMedDRAFromFile();
		}
		return;
	}

	/**
	 * Load MedDRA PT and LLTs from the UMLS MRCONSO export
	 */
	private void loadMedDRAFromFile() {
		try {
			log.info("Loading PT codes");

			HashMap<String, Boolean> validTtys = new HashMap<>();
			validTtys.put("PT", true);
			validTtys.put("LLT", true);
			validTtys.put("OS", true);
			validTtys.put("HT", true);
			validTtys.put("HG", true);
			System.out.println("Valid TTYs: " + validTtys.size());

			// update hierarchy with terms/codes
			File infile = new File(MRCONSO);
			final Reader reader = new InputStreamReader(new FileInputStream(infile));
			CSVParser parser = new CSVParser(reader, CSVFormat.DEFAULT.withDelimiter('|').withQuote(null));
			for (CSVRecord record : parser) {

				// Source
				String sourceName = record.get(11);
				if (sourceName.contentEquals("MDR")) {
					String cui = record.get(0);
					String aui = record.get(7);
					String tty = record.get(12);
					String term = record.get(14);
					term = term.replace("'", "\\'");
					String code = record.get(13);

					if (validTtys.containsKey(tty) == true) {
						// Create an atom to store the object
						com.gsk.sia.similarity.json.om.MeddraCode atom = new com.gsk.sia.similarity.json.om.MeddraCode();
						atom.setAui(aui);
						;
						atom.setCui(cui);
						;
						atom.setCode(code);
						atom.setTerm(term);
						atom.setTty(tty);
						this.MEDDRA_ATOMS.put(atom.getAui(), atom);
					}
				}
			}

			// close input stream
			reader.close();
			parser.close();

			System.out.println("Loaded: " + this.MEDDRA_ATOMS.size());
			// log.info("Loaded: " + this.MEDDRA_ATOMS.size());

		} catch (Exception e) {
			System.err.println("Error loading MedDRA: " + e.toString());
			return;
		}
	}

	/**
	 * Load MedDRA PT and LLTs from the database
	 */
	private void loadMedDRA() {
		try {
			log.info("Loading PT codes");
			Criteria criteria = new Criteria();
			criteria.where(MeddraCodePeer.CODE_ID, 1, Criteria.EQUAL);
			List<MeddraCode> ptTerms = MeddraCodePeer.doSelect(criteria);
			for (MeddraCode mc : ptTerms) {
				com.gsk.sia.similarity.json.om.MeddraCode atom = new com.gsk.sia.similarity.json.om.MeddraCode();
				atom.setAui(mc.getUmlsAui());
				atom.setCui(mc.getUmlsCui());
				atom.setCode(mc.getMeddraCode());
				atom.setTerm(mc.getMeddraTerm());
				atom.setTty("PT");
				this.MEDDRA_ATOMS.put(atom.getAui(), atom);
			}
			log.info("Loaded: " + this.MEDDRA_ATOMS.size());

			log.info("Loading LLT terms...");
			criteria = new Criteria();
			criteria.where(MeddraCodePeer.CODE_ID, 2, Criteria.EQUAL);
			List<MeddraCode> lltTerms = MeddraCodePeer.doSelect(criteria);
			for (MeddraCode mc : lltTerms) {
				com.gsk.sia.similarity.json.om.MeddraCode atom = new com.gsk.sia.similarity.json.om.MeddraCode();
				atom.setAui(mc.getUmlsAui());
				atom.setCui(mc.getUmlsCui());
				atom.setCode(mc.getMeddraCode());
				atom.setTerm(mc.getMeddraTerm());
				atom.setTty("LLT");
				this.MEDDRA_ATOMS.put(atom.getAui(), atom);
			}
			log.info("Loaded: " + this.MEDDRA_ATOMS.size());

		} catch (Exception e) {
			System.err.println("Error loading MedDRA: " + e.toString());
			return;
		}
	}

	/**
	 * Returns list of matching MedDRA codes
	 * 
	 * @param term
	 * @param THRESHOLD
	 * @return
	 */
	public List<com.gsk.sia.similarity.json.om.MeddraCode> findClosestMatchingMeddraCodes(String term, double THRESHOLD,
			boolean ptOnly) {

		List<com.gsk.sia.similarity.json.om.MeddraCode> results = new ArrayList<>();

		// check that MedDRA is loaded
		if (this.MEDDRA_ATOMS.size() == 0)
			this.loadMedDRA();

		// compute all bigrams just once
		if (this.MEDDRA_BIGRAM.size() == 0) {
			log.info("Computing all bigrams...");
			for (String aui : this.MEDDRA_ATOMS.keySet()) {
				com.gsk.sia.similarity.json.om.MeddraCode ma = this.MEDDRA_ATOMS.get(aui);
				if (ma.getTerm().length() > 2)
					this.MEDDRA_BIGRAM.put(aui, new BiGramSequence(ma.getTerm()));
			}
			log.info("Bigrams computed: " + this.MEDDRA_BIGRAM.size());
		}

		// find closest match to this term
		if (term.length() > 2) {
			BiGramSequence b1 = new BiGramSequence(term);
			for (String aui : this.MEDDRA_BIGRAM.keySet()) {
				BiGramSequence b2 = this.MEDDRA_BIGRAM.get(aui);
				double score = round(b1.compare(b2), 6);
				if (score > THRESHOLD) {
					com.gsk.sia.similarity.json.om.MeddraCode atom = this.MEDDRA_ATOMS.get(aui);

					// Test PT Only inclusion criteria
					boolean add = true;
					if (ptOnly == true) {
						if (atom.getTty().contentEquals("PT") == false)
							add = false;
					}

					if (add) {
						atom.setMatchProbability(score);
						results.add(atom);
					}
				}
			}
		}

		return results;
	}

	/**
	 * Put error handling in to catch potential bad numbers
	 * 
	 * @param value
	 * @param places
	 * @return
	 */
	private static double round(double value, int places) {
		try {
			if (places < 0)
				throw new IllegalArgumentException();

			BigDecimal bd = new BigDecimal(Double.toString(value));
			bd = bd.setScale(places, RoundingMode.HALF_UP);
			return bd.doubleValue();
		} catch (Exception e) {
			return 0.0;
		}
	}

}
