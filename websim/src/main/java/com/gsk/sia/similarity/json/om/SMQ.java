package com.gsk.sia.similarity.json.om;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class SMQ {

	// Class Properties
	String code;
	String name;
	boolean narrow;
	boolean broad;
	List<com.gsk.sia.similarity.json.om.MeddraCode> codes;

	public SMQ() {
		this.code = "";
		this.name = "";
		this.narrow = false;
		this.broad = false;
		this.codes = new ArrayList<>();
	}

	public SMQ(com.gsk.sia.similarity.om.MeddraSmq obj, boolean withCodes, boolean ptOnly) throws Exception {
		if (obj != null) {
			this.code = obj.getCode();
			this.name = obj.getName();
			this.narrow = obj.isNarrow();
			this.broad = obj.isBroad();
			this.codes = new ArrayList<>();
			if (withCodes == true) {
				for (com.gsk.sia.similarity.om.MeddraCode mc : obj.getMeddraCodes(ptOnly)) {
					this.codes.add(new com.gsk.sia.similarity.json.om.MeddraCode(mc));
				}
			}
		}
	}

	public SMQ(com.gsk.sia.similarity.om.MeddraSmq obj, boolean ptOnly) throws Exception {
		if (obj != null) {
			this.code = obj.getCode();
			this.name = obj.getName();
			this.narrow = obj.isNarrow();
			this.broad = obj.isBroad();
			this.codes = new ArrayList<>();
			for (com.gsk.sia.similarity.om.MeddraCode mc : obj.getMeddraCodes(ptOnly)) {
				this.codes.add(new com.gsk.sia.similarity.json.om.MeddraCode(mc));
			}
		}
	}

}