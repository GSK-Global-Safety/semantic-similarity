package com.gsk.sia.similarity.json.om;

import lombok.Data;

@Data
public class ClusterMetric {

	private String smq;
	private String ontology;
	private String ssm;
	private double ssmMax;
	private int outerPtCount;
	private int filteredOuterPtCount;

	public ClusterMetric() {
		this.smq = "";
		this.ontology = "";
		this.ssm = "";
		this.ssmMax = 0.0;
		this.outerPtCount = 0;
		this.filteredOuterPtCount = 0;
	}

}
