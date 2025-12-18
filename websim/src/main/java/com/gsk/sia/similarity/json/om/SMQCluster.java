package com.gsk.sia.similarity.json.om;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class SMQCluster {

	private SMQ smq;
	private List<ClusterResult> entries;

	public SMQCluster() {
		this.smq = null;
		this.entries = new ArrayList<>();
	}

}
