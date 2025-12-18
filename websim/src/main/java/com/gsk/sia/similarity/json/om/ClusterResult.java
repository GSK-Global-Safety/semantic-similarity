package com.gsk.sia.similarity.json.om;

import lombok.Data;

@Data
public class ClusterResult {

	private String ontology;
	private String metric;
	private double value;
	private int cluster;
	private MeddraCode code;

	public ClusterResult() {
		this.ontology = "";
		this.metric = "";
		this.value = 999.0;
		this.cluster = -1;
		this.code = null;
	}
}
