package com.gsk.sia.similarity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.ctakes.ytex.kernel.metric.ConceptPair;

public class CuiPair {

	private String cui1;
	private String cui2;
	private HashMap<String, Double> metrics;
	private HashMap<String, String> lcs;

	public CuiPair() {
		this.metrics = new HashMap<>();
		this.lcs = new HashMap<>();
	}

	public void setCui1(String c) {
		this.cui1 = c;
	}

	public void setCui2(String c) {
		this.cui2 = c;
	}

	/**
	 * Return concepts for processing
	 * 
	 * @return
	 */
	public List<ConceptPair> getConcept() {

		List<ConceptPair> pairs = new ArrayList<>();
		if (this.cui1 != null && this.cui2 != null && this.cui1.startsWith("C") && this.cui2.startsWith("C")) {
			ConceptPair cp = new ConceptPair(this.cui1, this.cui2);
			pairs.add(cp);
			return pairs;
		} else
			return null;
	}

	public void addMetric(String m, double v) {
		this.metrics.put(m, v);
	}

	public HashMap<String, Double> getMetrics() {
		return this.metrics;
	}

	/**
	 * Lease common subsumer info
	 * 
	 * @param l
	 * @param p
	 */
	public void addLCS(String l, String p) {
		this.lcs.put(l, p);
	}

	public double getMetric(String m) {
		return this.metrics.get(m);
	}
}
