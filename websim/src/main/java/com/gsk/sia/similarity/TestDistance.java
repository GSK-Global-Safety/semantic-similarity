package com.gsk.sia.similarity;

import java.sql.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.ctakes.ytex.kernel.dao.ConceptDao;
import org.apache.ctakes.ytex.kernel.dao.ConceptDaoImpl;
import org.apache.ctakes.ytex.kernel.metric.ConceptPair;
import org.apache.ctakes.ytex.kernel.metric.ConceptPairSimilarity;
import org.apache.ctakes.ytex.kernel.metric.ConceptSimilarityService.SimilarityMetricEnum;
import org.apache.ctakes.ytex.kernel.metric.ConceptSimilarityServiceImpl;

public class TestDistance {

	// Where are our network graphs?
	private static final String CONCEPT_GRAPH_PATH = "~/websim/src/main/webapp/conceptGraphs/";

	private static ConceptDao conceptDao = null;

	public static void main(String[] args) {

		// test1();

		// Find a concept near me
		test2();

	}

	/**
	 * Test stuff!
	 */
	private static void test2() {
		try {

			String dbDriver = "com.mysql.cj.jdbc.Driver";
			String dbUrl = "jdbc:mysql://localhost:3306/websim?serverTimezone=US/Eastern";
			String dbUser = "dbuser";
			String password = "dbpass";

			// Capture concepts above this limit!
			double scoreLimit = 0.95;
			HashMap<String, Double> matchedConcepts = new HashMap<>();

			// MI setup
			String mi_cui = "C0027051";
			String onto = "sct-mdr";

			// The metrics we want to test
			String metrics = "INTRINSIC_LIN";
			String[] metricArray = metrics.split(",");
			List<SimilarityMetricEnum> metricList = parseMetrics(metrics);

			List<String> umlsCuis = new ArrayList<>();

			Class.forName(dbDriver);
			Connection con = DriverManager.getConnection(dbUrl, dbUser, password);
			Statement stmt = con.createStatement();
			// Select distinct CUIs at the PT/LLT level
			ResultSet rs = stmt.executeQuery(
					"SELECT DISTINCT(UMLS_CUI) as UMLS_CUI from websim.MEDDRA_CODE WHERE CODE_ID in (1,2)");
			while (rs.next()) {
				String cui = rs.getString("UMLS_CUI");
				if (cui.contentEquals(mi_cui) == false) {
					umlsCuis.add(cui);
				}
			}

			System.out.println("UMLS CUIs: " + umlsCuis.size());

			// Default parameters
			boolean lcs = false;

			// Start this ONCE at the beginning
			ConceptDao dao = new ConceptDaoImpl();
			dao.setGraphPath(CONCEPT_GRAPH_PATH);

			ConceptSimilarityServiceImpl simSvc = new ConceptSimilarityServiceImpl();
			simSvc.setConceptDao(dao);
			simSvc.setConceptGraphName(onto);
			simSvc.init();

			// Test all CUIs!
			for (String cui2 : umlsCuis) {
				CuiPair cp = new CuiPair();
				cp.setCui1(mi_cui);
				cp.setCui2(cui2);

				List<ConceptPair> conceptPairs = cp.getConcept();
				// List<SimilarityInfo> simInfos = lcs ? new
				// ArrayList<SimilarityInfo>(conceptPairs.size())
				// : null;
				List<ConceptPairSimilarity> conceptSimMap = simSvc.similarity(conceptPairs, metricList, null, lcs);

				// Add the labels
				List<String> metricLabels = new ArrayList<>();
				for (SimilarityMetricEnum metric : metricList) {
					metricLabels.add(metric.name());
				}

				int idx = 0;
				for (ConceptPairSimilarity csim : conceptSimMap) {
					for (Double sim : csim.getSimilarities()) {
						cp.addMetric(metricLabels.get(idx), sim);
						idx++;
					}
				}

				for (String m : metricArray) {
					double score = round(cp.getMetric(m), 6);
					if (score > scoreLimit) {
						matchedConcepts.put(cui2, score);
					}
				}

			}

			System.out.println("Matched CUIs: " + matchedConcepts.size());
			HashMap<String, Boolean> seenCode = new HashMap<>();
			int counter = 1;
			for (String cui : matchedConcepts.keySet()) {
				double score = matchedConcepts.get(cui);
				// Search for PT matches only
				rs = stmt.executeQuery(
						"SELECT MEDDRA_CODE, MEDDRA_TERM from websim.MEDDRA_CODE WHERE UMLS_CUI = '" + cui + "' AND CODE_ID = 1");
				while (rs.next()) {
					String mCode = rs.getString("MEDDRA_CODE");
					String mTerm = rs.getString("MEDDRA_TERM");
					if (seenCode.containsKey(mCode) == false) {
						System.out.println(counter +
								" CUI: [" + cui + "]\tMeddra code: " + mCode + " \t " + mTerm + "\t\tMatch Score = " + score);
						seenCode.put(mCode, true);
						counter++;
					}
				}

			}
			con.close();
			System.out.println("Done!");
		} catch (Exception e) {
			System.out.println(e);
		}
	}

	private static void test1() {
		// Let's try to run these in a loop and see the outputs
		List<String> cuis = new ArrayList<>();
		// cuis.add("C1167697");
		cuis.add("C0857468");
		cuis.add("C0020544");
		// cuis.add("C0020538");

		//
		// The metrics we want to compute
		//

		String metrics = "LCH,INTRINSIC_LCH,WUPALMER,INTRINSIC_LIN,JACCARD,SOKAL,INTRINSIC_RADA,DICE,SIMPSON,BRAUN_BLANQUET,OCHIAI";
		// String metrics =
		// "PATH,LCH,INTRINSIC_LCH,WUPALMER,INTRINSIC_LIN,JACCARD,SOKAL,INTRINSIC_RADA,DICE,SIMPSON,BRAUN_BLANQUET,OCHIAI,RESNIK,INTRINSIC_RESNIK";
		String[] metricArray = metrics.split(",");

		// Default parameters
		boolean lcs = false;

		// Start this ONCE at the beginning
		ConceptDao dao = new ConceptDaoImpl();
		dao.setGraphPath(CONCEPT_GRAPH_PATH);

		ConceptSimilarityServiceImpl simSvc = new ConceptSimilarityServiceImpl();
		simSvc.setConceptDao(dao);
		simSvc.setConceptGraphName("sct-msh-mdr");
		simSvc.init();

		// Get list of metrics to compute
		List<SimilarityMetricEnum> metricList = parseMetrics(metrics);

		// From UMLS::Similarity test run

		// Old code from deerfield
		// MeshCodeA|MeshTermA|MeshCodeB|MeshTermB|LinScore|ResnikScore
		// Resnik: 0.1448<>C0029128(Drusen, Optic Disc)<>C0085542(Pravastatin)
		// Lin: 0.0246<>C0029128(Drusen, Optic Disc)<>C0085542(Pravastatin)

		// Resnik: 1.7027<>C0008286(Chlorpromazine)<>C0017861(Glycerine)

		/*
		 * IC1: 14.6131563626951 IC2: 14.028193861974 ICLCS: 9.95494487994334
		 * 0.6951<>(Withdrawal hypertension)<>(Hypertension renal)
		 */

		// String cui1 = "C0857468";
		// String cui2 = "C0020544";
		// Perl Resnik: 3.1590<>C0027497(Nausea NOS)<>C0027498(Nausea with vomiting)

		// String cui1 = "C0035320"; // Retinal neovascularization, NOS
		// String cui2 = "C0393570"; // corticobasal degeneration
		double expectedLinDistance = 0.3352;

		HashMap<String, Boolean> seenCuiPair = new HashMap<>();
		for (String cui1 : cuis) {
			for (String cui2 : cuis) {

				if (cui1.contentEquals(cui2) == false) {
					String cp1 = cui1 + "<>" + cui2;
					String cp2 = cui2 + "<>" + cui1;
					if (seenCuiPair.containsKey(cp1) || seenCuiPair.containsKey(cp2)) {
						// pass
					} else {
						System.out.println(cui1 + "<>" + cui2);
						printDistances(metrics, metricArray, lcs, simSvc, metricList, cui1, cui2);
						System.out.println("---------------\n");
						seenCuiPair.put(cp1, true);
					}
				}
			}
		}
		System.out.println("Done!");
	}

	private static void printDistances(String metrics, String[] metricArray, boolean lcs,
			ConceptSimilarityServiceImpl simSvc, List<SimilarityMetricEnum> metricList, String cui1, String cui2) {
		CuiPair cp = new CuiPair();
		cp.setCui1(cui1);
		cp.setCui2(cui2);

		List<ConceptPair> conceptPairs = cp.getConcept();
		// List<SimilarityInfo> simInfos = lcs ? new
		// ArrayList<SimilarityInfo>(conceptPairs.size())
		// : null;
		List<ConceptPairSimilarity> conceptSimMap = simSvc.similarity(conceptPairs, metricList, null, lcs);

		// Add the labels
		List<String> metricLabels = new ArrayList<>();
		for (SimilarityMetricEnum metric : metricList) {
			metricLabels.add(metric.name());
		}

		int idx = 0;
		for (ConceptPairSimilarity csim : conceptSimMap) {
			for (Double sim : csim.getSimilarities()) {
				cp.addMetric(metricLabels.get(idx), sim);
				idx++;
			}
		}

		String output = "";
		for (String m : metricArray) {
			double score = round(cp.getMetric(m), 6);
			output += score + "|";
		}

		// output to file
		System.out.println(metrics);
		System.out.println(output);
	}

	private static double round(double value, int places) {
		if (places < 0)
			throw new IllegalArgumentException();

		BigDecimal bd = new BigDecimal(Double.toString(value));
		bd = bd.setScale(places, RoundingMode.HALF_UP);
		return bd.doubleValue();
	}

	private static List<SimilarityMetricEnum> parseMetrics(String metrics) {
		String ms[] = metrics.split(",");
		List<SimilarityMetricEnum> metricSet = new ArrayList<SimilarityMetricEnum>();
		for (String metric : ms) {
			SimilarityMetricEnum m = SimilarityMetricEnum.valueOf(metric);
			if (m == null)
				System.err.println("invalid metric: " + ms);
			else
				metricSet.add(m);
		}
		return metricSet;
	}

}
