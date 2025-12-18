package com.gsk.sia.similarity.json.om;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import lombok.Data;

@Data
public class DistanceMatrix {

	private String view;
	private List<Distance> distances;

	public DistanceMatrix() {
		this.view = "";
		this.distances = new ArrayList<>();
	}

	/**
	 * Given a pt code, return all distances that include this code
	 * 
	 * @param ptCode
	 * @return
	 */
	public List<Distance> getAllDistancesWithCode(MeddraCode ptcode, String metric, double level) {
		List<Distance> matchedDistances = new ArrayList<>();
		for (Distance d : this.distances) {
			if (d.getDistanceMetrics().containsKey(metric)) {
				if (d.getDistanceMetrics().get(metric) >= level) {
					MeddraCode ptCode1 = d.getCode1();
					MeddraCode ptCode2 = d.getCode2();

					// Ignore (a,a) pairs
					if (ptCode1.getCode().contentEquals(ptCode2.getCode()) == false) {
						if (ptCode1.getCode().contentEquals(ptcode.getCode())) {
							matchedDistances.add(d);
						} else if (ptCode2.getCode().contentEquals(ptcode.getCode())) {
							matchedDistances.add(d);
						}
					}
				}
			}
		}
		return matchedDistances;
	}

	/**
	 * Compute submatrix for a given metric and similarity level
	 * 
	 * @param metric
	 * @param p
	 * @return
	 */
	public List<Distance> getSubmatrix(String metric, double p) {

		List<Distance> subMatrix = new ArrayList<>();
		for (Distance d : this.distances) {
			// See if this distance is above our test level
			double testVal = d.getDistanceMetrics().get(metric).doubleValue();
			if (testVal >= p) {

				subMatrix.add(d);
			}
		}

		return subMatrix;
	}

	/**
	 * Get the unique set of codes in the matrix
	 * @return
	 */
	public HashMap<String, MeddraCode> getJsonCodes() {
		HashMap<String, MeddraCode> codes = new HashMap<>();
		if (this.distances != null) {
			for (Distance d : this.distances) {
				codes.put(d.getCode1().getCode(), d.getCode1());
				codes.put(d.getCode2().getCode(), d.getCode2());
			}
		}
		return codes;
	}

	/**
	 * Convert to CSV output format
	 * @param metricArray
	 * @return
	 */
	public String convertToCsv(String[] metricArray) {
		
		String DELIMITER = ",";
		StringBuilder csv = new StringBuilder();
		String header = "PT_CODE_1,TERM_1,PT_CODE_2,TERM_2";
		for ( String metric : metricArray )
			header = header + "," + metric;
		
		// Build the header line
		csv.append(header);
		csv.append("\n");
		
		// Add distances
		for (Distance d : this.distances) {
			MeddraCode mc1 = d.getCode1();
			MeddraCode mc2 = d.getCode2();
			StringBuilder row = new StringBuilder();
			row.append(mc1.getCode());
			row.append(DELIMITER);
			row.append(mc1.getTerm());
			row.append(DELIMITER);

			row.append(mc2.getCode());
			row.append(DELIMITER);
			row.append(mc2.getTerm());

			// Add the scores
			for ( String metric : metricArray ) {
				double score = d.getDistanceMetrics().get(metric);
				row.append(DELIMITER);
				row.append(score);
			}
			
			csv.append(row.toString());
			csv.append("\n");
		}
		
		return csv.toString();
	}


}
