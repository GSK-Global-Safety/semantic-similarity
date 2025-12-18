package com.gsk.sia.similarity.json.om;

import lombok.Data;

@Data
public class MeddraCode {

	// Class Properties
	String aui;
	String code;
	String term;
	String tty;
	String cui;
	double matchProbability;

	public MeddraCode() {
		this.code = "";
		this.term = "";
		this.tty = "";
		this.cui = "";
		this.matchProbability = -1.0;
	}

	public MeddraCode(com.gsk.sia.similarity.om.MeddraCode obj) throws Exception {
		if (obj != null) {
			this.code = obj.getMeddraCode();
			this.term = obj.getMeddraTerm();
			this.tty = obj.getMeddraCodeType().getCodeType().toString();
			this.cui = obj.getUmlsCui();
		}
	}

}