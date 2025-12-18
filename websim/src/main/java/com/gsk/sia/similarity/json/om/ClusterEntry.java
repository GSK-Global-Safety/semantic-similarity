package com.gsk.sia.similarity.json.om;

import lombok.Data;

@Data
public class ClusterEntry {

	private String metric;
	private String ontology;
	private double level;
	private int clusterId;
	private String ptCode;
	private String tty;
	private String ptTerm;

	public ClusterEntry() {
		this.metric = "";
		this.ontology = "";
		this.level = 0.0;
		this.clusterId = 0;
		this.ptCode = "";
		this.tty = "";
		this.ptTerm = "";
	}

	public ClusterEntry(int clusterId, String metric, String ontology, double currentSSMLevel) {
		this.clusterId = clusterId;
		this.metric = metric;
		this.ontology = ontology;
		this.level = currentSSMLevel;
		this.ptCode = "";
		this.tty = "";
		this.ptTerm = "";
	}

	public void setCode(MeddraCode code) {
		this.ptCode = code.getCode();
		this.tty = code.getTty();
		this.ptTerm = code.getTerm();
	}

}
