package com.gsk.sia.similarity.json.om;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class HLGT {
	// Class Properties
	String code;
	String name;
	List<com.gsk.sia.similarity.json.om.MeddraCode> codes;

	public HLGT() {
		this.code = "";
		this.name = "";
		this.codes = new ArrayList<>();
	}

	public HLGT(com.gsk.sia.similarity.om.MeddraCode obj, boolean ptOnly) throws Exception {
		if (obj != null) {
			this.code = obj.getMeddraCode();
			this.name = obj.getMeddraTerm();
			this.codes = new ArrayList<>();
			for (com.gsk.sia.similarity.om.MeddraCode mc : obj.getFaersHlgtPtCodes(ptOnly)) {
				this.codes.add(new com.gsk.sia.similarity.json.om.MeddraCode(mc));
			}
		}
	}

}
