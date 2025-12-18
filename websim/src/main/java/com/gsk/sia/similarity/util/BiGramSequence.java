package com.gsk.sia.similarity.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/*
 *  ------------------------------------------------------------------
 *  Jivecast
 *
 *  Copyright (c) 2003-2018 Jivecast. All Rights Reserved. Permission
 *  to copy, modify and distribute this software and code
 *  included and its documentation (collectively, the "PROGRAM") for
 *  any purpose is hereby prohibited.
 *
 *  THE PROGRAM IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 *  EITHER EXPRESSED OR IMPLIED, INCLUDING, WITHOUT
 *  LIMITATION, WARRANTIES THAT THE PROGRAM IS FREE OF
 *  DEFECTS, MERCHANTABLE, FIT FOR A PARTICULAR PURPOSE OR
 *  NON-INFRINGING. THE ENTIRE RISK AS TO THE QUALITY AND
 *  PERFORMANCE OF THE PROGRAM IS WITH YOU. SHOULD ANY PART
 *  OF THE PROGRAM PROVE DEFECTIVE IN ANY RESPECT, YOU
 *  (NOT JIVECAST) ASSUME THE COST OF ANY NECESSARY SERVICING,
 *  REPAIR OR CORRECTION. THIS DISCLAIMER OF WARRANTY CONSTITUTES
 *  AN ESSENTIAL PART OF THIS LICENSE. NO USE OF
 *  THE PROGRAM IS AUTHORIZED HEREUNDER EXCEPT
 *  UNDER THIS DISCLAIMER.
 *
 *  ------------------------------------------------------------------
 */
public class BiGramSequence {

	private String word;
	private List<String> bigrams;

	// default constructor
	public BiGramSequence() {
		this.word = null;
		this.bigrams = new ArrayList<String>();
	}

	// Create a bigram set based on a word
	public BiGramSequence(String word) {

		// error check first
		if (word.length() < 2) {
			System.err.println("Word length must be at least two characters long");
		} else {
			this.word = word;
			this.bigrams = new ArrayList<String>();
			bigrams = this.getBiGrams(word);
		}
	}

	public String getWord() {
		return this.word;
	}

	public void setWord(String val) {
		this.word = val;
	}

	/// <summary>
	/// Override the to string method to output a bigram sequence,
	/// used for generating the sequences to be stored in the database.
	/// </summary>
	/// <returns></returns>
	@Override
	public String toString() {
		String bigramSequence = "";

		// sort the bigrams
		Collections.sort(this.bigrams);

		for (String biGram : this.bigrams)
			bigramSequence += biGram;

		return bigramSequence;
	}

	/// <summary>
	/// For hashing a bigram sequence, unique bigram sets
	/// </summary>
	/// <returns></returns>
	@Override
	public int hashCode() {
		return this.toString().hashCode();
	}

	public void addBiGram(String b) {
		this.bigrams.add(b);
		return;
	}

	/// <summary>
	/// Convert a word to a list of its bigram pairs
	/// </summary>
	/// <param name="word"></param>
	/// <returns></returns>
	public List<String> getBiGrams(String word) {
		word = word.toLowerCase();
		List<String> uniqueSet = new ArrayList<String>();

		HashMap<String, Boolean> bigrams = new HashMap<>();
		int count = word.length();
		int i = 0;
		while (i < (count - 1)) {
			String bigram = word.substring(i, i + 2);
			if (!bigrams.containsKey(bigram))
				bigrams.put(bigram, true);
			i++;
		}

		// insure that we only have unique bigram entries
		for (String bg : bigrams.keySet())
			uniqueSet.add(bg);

		return uniqueSet;

	}

	/// <summary>
	/// Compare this bigram sequence to another and return a value from
	/// 0 to 1 based on the probability of a match
	/// </summary>
	/// <param name="cSet"></param>
	/// <returns></returns>
	public double compare(BiGramSequence cSet) {

		if (this.word != null && this.bigrams.size() > 0) {
			double count = 0;
			double denom = this.bigrams.size() + cSet.bigrams.size();
			for (String b : this.bigrams)
				if (cSet.bigrams.contains(b))
					count++;
			double result = (2 * count) / denom;
			return result;
		} else {
			return 0;
		}
	}

}
