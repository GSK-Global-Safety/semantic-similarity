package com.gsk.sia.similarity.util;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class Atom {

	String aui;
	String cui;
	String term;
	String tty;
	String code;
	String lat;
	String ts;
	String lui;
	String stt;
	String sui;
	String isPref;
	String saui;
	String scui;
	String sdui;
	String sab;
	String srl;
	String suppress;
	
	// path to root
	String [] ptr;
	
	long caseCount;
	
	List<Long> parents;
	List<Long> children;
	
	private long refId;
	
	public Atom()
	{
		this.parents = new ArrayList<>();
		this.children = new ArrayList<>();
		this.ptr = null;
		this.caseCount = 0l;
	}
}
