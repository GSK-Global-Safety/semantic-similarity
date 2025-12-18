package com.gsk.sia.similarity.json.om;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class Cluster {

	private String metric;
	private String ontology;
	private double level;
	private int clusterId;
	private List<MeddraCode> codes;

	public Cluster() {
		this.metric = "";
		this.ontology = "";
		this.level = 0.0;
		this.codes = new ArrayList<>();
	}

	public void addCode(MeddraCode code) {
		this.codes.add(code);
	}

	/**
	 * Test if the cluster contains the code
	 * @param code
	 * @return
	 */
	public boolean containsCode(MeddraCode code) {
		if (this.codes != null) {
			for (MeddraCode mc : this.codes) {
				if (mc.getCode().contentEquals(code.getCode()))
					return true;
			}
			return false;
		} else {
			this.codes = new ArrayList<>();
			return false;
		}
	}
}
