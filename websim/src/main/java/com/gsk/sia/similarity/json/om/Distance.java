package com.gsk.sia.similarity.json.om;

import java.util.HashMap;

import lombok.Data;

@Data
public class Distance {

	private MeddraCode code1;
	private MeddraCode code2;
	HashMap<String, Double> distanceMetrics;
	private int maxMetric;

	public Distance() {
		this.code1 = null;
		this.code2 = null;
		this.distanceMetrics = new HashMap<String, Double>();
		this.maxMetric = 0;
	}

}
