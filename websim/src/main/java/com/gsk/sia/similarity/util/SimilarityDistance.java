package com.gsk.sia.similarity.util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.apache.ctakes.ytex.kernel.dao.ConceptDao;
import org.apache.ctakes.ytex.kernel.dao.ConceptDaoImpl;
import org.apache.ctakes.ytex.kernel.metric.ConceptPair;
import org.apache.ctakes.ytex.kernel.metric.ConceptPairSimilarity;
import org.apache.ctakes.ytex.kernel.metric.ConceptSimilarityService.SimilarityMetricEnum;
import org.apache.ctakes.ytex.kernel.metric.ConceptSimilarityServiceImpl;
import org.apache.turbine.Turbine;

import com.gsk.sia.similarity.CuiPair;
import com.gsk.sia.similarity.json.om.ClusterEntry;
import com.gsk.sia.similarity.json.om.ClusterMetric;
import com.gsk.sia.similarity.json.om.ClusterResult;
import com.gsk.sia.similarity.json.om.Distance;
import com.gsk.sia.similarity.json.om.DistanceMatrix;
import com.gsk.sia.similarity.json.om.SMQCluster;
import com.gsk.sia.similarity.om.MeddraCode;
import com.gsk.sia.similarity.om.MeddraSmq;

public class SimilarityDistance {

	// Start this ONCE at the beginning
	private ConceptDao dao;
	private ConceptSimilarityServiceImpl simSvc;
	private String ONTOLOGY;

	// Cui sets for multiple ontologies
	private HashMap<String, Boolean> SNOMED_CUIS;
	private HashMap<String, Boolean> MESH_CUIS;
	private HashMap<String, Boolean> MEDRA_PT_CUIS;

	/**
	 * Initialize with an ontology view
	 * 
	 * @param onto
	 */
	public SimilarityDistance() {
		// Set the current ontology view for the similarity service
		this.ONTOLOGY = "";
	}

	/**
	 * Initialize with an ontology view
	 * 
	 * @param onto
	 */
	public SimilarityDistance(String onto) {
		// Set the current ontology view for the similarity service
		this.initOntology(onto);
	}

	/**
	 * Load a new ontology
	 * 
	 * @param ontology
	 */
	public void initOntology(String ontology) {

		// Set the current ontology view
		this.ONTOLOGY = ontology;

		this.MEDRA_PT_CUIS = this.getMeddraCuis();

		// Load SNOMED CUIs only if we are using the sct models
		if (this.ONTOLOGY.contains("sct")) {
			this.SNOMED_CUIS = this.getSnomedCuis();
		}

		// Load MeSH CUIs only if we are using the msh models
		if (this.ONTOLOGY.contains("msh")) {
			this.MESH_CUIS = this.getMeshCuis();
		}

		// Start this ONCE at the beginning
		this.dao = new ConceptDaoImpl();
		dao.setGraphPath(this.getConceptGraphPath());

		this.simSvc = new ConceptSimilarityServiceImpl();
		simSvc.setConceptDao(this.dao);
		simSvc.setConceptGraphName(ontology);
		simSvc.init();
	}

	/**
	 * Load the MeSH CUI set
	 * 
	 * @return
	 */
	private HashMap<String, Boolean> getMeshCuis() {
		HashMap<String, Boolean> cuis = new HashMap<>();
		try {
			// Get the real path
			String path = Turbine.getRealPath("/");
			String file = path + "/conceptGraphs/MESH_CUI.txt";

			BufferedReader reader;

			try {
				reader = new BufferedReader(new FileReader(file));
				String line = reader.readLine();

				while (line != null) {
					// read next line
					line = reader.readLine();
					String cui = line.trim();
					cuis.put(cui, true);
				}

				reader.close();
			} catch (IOException e) {
				e.printStackTrace();
			}

		} catch (Exception e) {
		}
		return cuis;

	}

	/**
	 * Load the MedDRA PT CUI set
	 * 
	 * @return
	 */
	private HashMap<String, Boolean> getMeddraCuis() {
		HashMap<String, Boolean> cuis = new HashMap<>();
		try {
			// Get the real path
			String path = Turbine.getRealPath("/");
			String file = path + "/conceptGraphs/MEDDRA_PT_CUIS.txt";

			BufferedReader reader;

			try {
				reader = new BufferedReader(new FileReader(file));
				String line = reader.readLine();

				while (line != null) {
					// read next line
					line = reader.readLine();
					String cui = line.trim();
					cuis.put(cui, true);
				}

				reader.close();
			} catch (IOException e) {
				e.printStackTrace();
			}

		} catch (Exception e) {
		}
		return cuis;

	}

	/**
	 * Load the SNOMED CUI set
	 * 
	 * @return
	 */
	private HashMap<String, Boolean> getSnomedCuis() {
		HashMap<String, Boolean> cuis = new HashMap<>();
		try {
			// Get the real path
			String path = Turbine.getRealPath("/");
			String file = path + "/conceptGraphs/SNOMED_CUI.txt";

			BufferedReader reader;

			try {
				reader = new BufferedReader(new FileReader(file));
				String line = reader.readLine();

				while (line != null) {
					// read next line
					line = reader.readLine();
					String cui = line.trim();
					cuis.put(cui, true);
				}

				reader.close();
			} catch (IOException e) {
				e.printStackTrace();
			}

		} catch (Exception e) {
		}
		return cuis;

	}

	/**
	 * Load the path to our concept graphs
	 * 
	 * @return
	 */
	private String getConceptGraphPath() {
		// Get the real path
		String path = Turbine.getRealPath("/");
		String file = path + "/conceptGraphs/";
		return file;
	}

	/**
	 * This is a transitive closure clustering algorithm matching Francois' original
	 * implementation
	 * 
	 * @param smq
	 * @param metrics
	 * @param ontologies
	 * @return
	 */
	public List<ClusterEntry> computeLevelClusters(com.gsk.sia.similarity.om.MeddraSmq smq, String[] metricArray,
			String[] ontologies, boolean ptOnly) {

		List<ClusterEntry> allLevelClusters = new ArrayList<>();
		try {
			List<String> listMetricArray = new ArrayList<>();
			for (String metric : metricArray) {
				listMetricArray.add(metric);
			}

			// Get all of the PT codes with this SMQ
			List<MeddraCode> codes = smq.getMeddraCodes(ptOnly);
			double[] distanceFilter = new double[1];
			distanceFilter[0] = -1;

			for (String ontology : ontologies) {

				// Set our ontology view
				this.initOntology(ontology);

				// Parallelise the job
				listMetricArray.parallelStream().forEach(metric -> computeSingleMetricLevelCluster(allLevelClusters,
						codes, distanceFilter, ontology, metric));

			}

			return allLevelClusters;

		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * Compute single metric cluster level inclusion
	 * 
	 * @param allLevelClusters
	 * @param codes
	 * @param distanceFilter
	 * @param ontology
	 * @param metric
	 */
	private void computeSingleMetricLevelCluster(List<ClusterEntry> allLevelClusters, List<MeddraCode> codes,
			double[] distanceFilter, String ontology, String metric) {
		// Get the inner matrix for this view/metric
		DistanceMatrix matrix = computeInnerDistanceMatrix(metric, codes, distanceFilter);
		HashMap<Double, Boolean> uniqueDistances = new HashMap<>();
		for (Distance d : matrix.getDistances()) {
			double distance = d.getDistanceMetrics().get(metric).doubleValue();
			uniqueDistances.put(distance, true);
		}

		// Let's track the change in clusters by level
		HashMap<Double, Integer> numClustersByLevel = new HashMap<>();
		List<ClusterEntry> filteredLevelClusters = new ArrayList<>();

		// Extract list of meddra codes in the inner matrix
		HashMap<String, com.gsk.sia.similarity.json.om.MeddraCode> jsonCodes = matrix.getJsonCodes();

		// Get unique distances in descending order
		Double[] distances = new Double[uniqueDistances.size()];
		int index = 0;
		for (double v : uniqueDistances.keySet()) {
			distances[index] = v;
			index++;
		}
		Arrays.sort(distances, Collections.reverseOrder());

		for (double currentSSMLevel : distances) {

			// Restart the cluster ID for each level/ontology/metric
			int clusterId = 1;

			// Create a set of subgroups which will be assigned to the cluster
			HashMap<Integer, HashMap<String, com.gsk.sia.similarity.json.om.MeddraCode>> subgroups = new HashMap<>();
			subgroups.put(clusterId, new HashMap<String, com.gsk.sia.similarity.json.om.MeddraCode>());

			// Track PT codes seen at this cluster level
			HashMap<String, Boolean> seenCodes = new HashMap<>();

			// Find entries where the metric distances are above this level
			List<Distance> matrixSubset = matrix.getSubmatrix(metric, currentSSMLevel);

			// Evaluate all codes in this matrix subset
			for (Distance mEntry : matrixSubset) {
				ClusterEntry clusterMember = new ClusterEntry(clusterId, metric, ontology, currentSSMLevel);

				// Get the first code
				com.gsk.sia.similarity.json.om.MeddraCode ptCode1 = mEntry.getCode1();
				String ptCode = ptCode1.getCode();

				// Has this code been added to a cluster?
				if (seenCodes.containsKey(ptCode1.getCode()) == false) {
					// Add this code to the cluster
					clusterMember.setCode(ptCode1);
					filteredLevelClusters.add(clusterMember);

					// Add the code to the current cluster
					subgroups.get(clusterId).put(ptCode1.getCode(), ptCode1);
					seenCodes.put(ptCode, true);

					//
					// For the first pass, we will evaluate the direct relation
					// to this PT code and find all codes which match at this level
					//
					List<Distance> matchedCodes = matrix.getAllDistancesWithCode(ptCode1, metric, currentSSMLevel);
					addRelatedCodesToCluster(filteredLevelClusters, ontology, metric, currentSSMLevel, clusterId,
							subgroups, seenCodes, matchedCodes);

					//
					// Now we should go through every code in the cluster and see if there
					// are any indirect associations at this same level
					//
					int testClusterIncrease = 0;
					int currentClusterSize = subgroups.get(clusterId).size();
					while (currentClusterSize > testClusterIncrease) {

						// Update with the current cluster size
						testClusterIncrease = subgroups.get(clusterId).size();
						for (String ptTestCode : jsonCodes.keySet()) {
							if (subgroups.get(clusterId).containsKey(ptTestCode) == true) {

								// Get the distances at this level from this code
								List<Distance> ptTestMatchedCodes = matrix
										.getAllDistancesWithCode(jsonCodes.get(ptTestCode), metric, currentSSMLevel);

								// Add any new codes discovered in this subset
								addRelatedCodesToCluster(filteredLevelClusters, ontology, metric, currentSSMLevel,
										clusterId, subgroups, seenCodes, ptTestMatchedCodes);
							}

						}

						//
						// Update to see if any new nodes were added,
						// if it increased, we will continue to loop
						//
						currentClusterSize = subgroups.get(clusterId).size();
					}

				}

				// Did we add any codes to this cluster?
				if (subgroups.get(clusterId).size() > 0) {
					clusterId++;
					// Create new subgroup entry
					subgroups.put(clusterId, new HashMap<String, com.gsk.sia.similarity.json.om.MeddraCode>());
				}
			}

			// Total number of clusters found at this level
			numClustersByLevel.put(currentSSMLevel, subgroups.size());
		}

		// Resort the distances in ascending order
		Arrays.sort(distances);

		//
		// Filter the results to only the cut points by level
		// Step through each level in ascending order
		//

		// Lowest distance set number of clusters
		int currentNumberClusters = numClustersByLevel.get(distances[0]);
		HashMap<Integer, Boolean> addedClusterSize = new HashMap<>();

		for (int i = 0; i < distances.length; i++) {
			// Test all distances until the end
			if (i + 1 < distances.length) {
				// How many clusters are at the next level
				int nextLevelClusters = numClustersByLevel.get(distances[i + 1]);

				// Test for any change in the number of clusters in the next level
				if (currentNumberClusters != nextLevelClusters) {
					// Add the current level to final result
					List<ClusterEntry> subset = getClusterMembers(filteredLevelClusters, distances[i]);
					for (ClusterEntry e : subset) {
						allLevelClusters.add(e);
					}

					addedClusterSize.put(currentNumberClusters, true);

					// update current number
					currentNumberClusters = nextLevelClusters;
				}
			} else if (i == distances.length) {
				// Test if we added the last entry
				if (addedClusterSize.containsKey(currentNumberClusters) == false) {
					// Add the current level to final result
					List<ClusterEntry> subset = getClusterMembers(filteredLevelClusters, distances[i]);
					for (ClusterEntry e : subset) {
						allLevelClusters.add(e);
					}
					addedClusterSize.put(currentNumberClusters, true);
				}
			}
		}

		//
		// WUPALMER has an interesting case where all clusters are the same size
		// If we never added any clusters, then add the last one if all the same
		boolean allSame = true;
		if (addedClusterSize.size() != 0) {
			allSame = false;
		}

		// Put the number of clusters here and add all members
		if (allSame) {
			addedClusterSize.put(currentNumberClusters, true);
			List<ClusterEntry> subset = getClusterMembers(filteredLevelClusters, distances[distances.length - 1]);
			for (ClusterEntry e : subset) {
				allLevelClusters.add(e);
			}
		}
	}

	/**
	 * Subset the cluster members to a specific level
	 * 
	 * @param filteredLevelClusters
	 * @param ssmLevel
	 * @return
	 */
	private List<ClusterEntry> getClusterMembers(List<ClusterEntry> filteredLevelClusters, Double ssmLevel) {
		List<ClusterEntry> subset = new ArrayList<>();
		for (ClusterEntry e : filteredLevelClusters) {
			if (e.getLevel() == ssmLevel) {
				subset.add(e);
			}
		}
		return subset;
	}

	/**
	 * Add related codes to the current cluster
	 * 
	 * @param levelClusters
	 * @param ontology
	 * @param metric
	 * @param currentSSMLevel
	 * @param clusterId
	 * @param subgroups
	 * @param seenCodes
	 * @param matchedCodes
	 */
	private void addRelatedCodesToCluster(List<ClusterEntry> levelClusters, String ontology, String metric,
			double currentSSMLevel, int clusterId,
			HashMap<Integer, HashMap<String, com.gsk.sia.similarity.json.om.MeddraCode>> subgroups,
			HashMap<String, Boolean> seenCodes, List<Distance> matchedCodes) {

		for (Distance dme : matchedCodes) {
			com.gsk.sia.similarity.json.om.MeddraCode dmCode1 = dme.getCode1();
			if (seenCodes.containsKey(dmCode1.getCode()) == false) {
				ClusterEntry clusterMember = new ClusterEntry(clusterId, metric, ontology, currentSSMLevel);
				clusterMember.setCode(dmCode1);
				levelClusters.add(clusterMember);

				subgroups.get(clusterId).put(dmCode1.getCode(), dmCode1);
				seenCodes.put(dmCode1.getCode(), true);
			}

			com.gsk.sia.similarity.json.om.MeddraCode dmCode2 = dme.getCode2();
			if (seenCodes.containsKey(dmCode1.getCode()) == false) {
				ClusterEntry clusterMember = new ClusterEntry(clusterId, metric, ontology, currentSSMLevel);
				clusterMember.setCode(dmCode2);
				levelClusters.add(clusterMember);

				subgroups.get(clusterId).put(dmCode2.getCode(), dmCode2);
				seenCodes.put(dmCode2.getCode(), true);
			}

		}
	}

	/**
	 * This enforces each code to belong to a single cluster
	 * 
	 * @param smq
	 * @param metrics
	 * @param ontologies
	 * @return
	 */
	public SMQCluster computeDirectCluster(com.gsk.sia.similarity.om.MeddraSmq smq, String[] metrics,
			String[] ontologies, boolean ptOnly) {

		try {
			SMQCluster cluster = new SMQCluster();
			cluster.setSmq(new com.gsk.sia.similarity.json.om.SMQ(smq, false));
			List<MeddraCode> codes = smq.getMeddraCodes(ptOnly);
			double[] distanceFilter = new double[1];
			distanceFilter[0] = -1;

			for (String ontology : ontologies) {

				// Set our ontology view
				this.initOntology(ontology);

				for (String metric : metrics) {

					// Get the inner matrix for this view/metric
					DistanceMatrix matrix = computeInnerDistanceMatrix(metric, codes, distanceFilter);
					HashMap<Double, Boolean> uniqueDistances = new HashMap<>();
					for (Distance d : matrix.getDistances()) {
						double distance = d.getDistanceMetrics().get(metric).doubleValue();
						uniqueDistances.put(distance, true);
					}

					// Get unique distances in descending order
					Double[] distances = new Double[uniqueDistances.size()];
					int index = 0;
					for (double v : uniqueDistances.keySet()) {
						distances[index] = v;
						index++;
					}
					Arrays.sort(distances, Collections.reverseOrder());

					//
					// Iterate at each level of distance for cluster inclusion
					//
					HashMap<String, Boolean> seenCodes = new HashMap<>();
					HashMap<Integer, List<Distance>> clusters = new HashMap<>();
					int clusterId = 1;
					for (double p : distances) {

						// Create new cluster for this level
						List<Distance> clusterMembers = new ArrayList<>();
						boolean addMember = false;

						// Find entries where the metric distances are above this level
						List<Distance> matrixSubset = matrix.getSubmatrix(metric, p);
						for (Distance mEntry : matrixSubset) {
							com.gsk.sia.similarity.json.om.MeddraCode ptCode1 = mEntry.getCode1();
							com.gsk.sia.similarity.json.om.MeddraCode ptCode2 = mEntry.getCode2();

							if (ptCode1.getCode().contentEquals(ptCode2.getCode()) == false) {
								// Test if we have put this code in a cluster
								if (seenCodes.containsKey(ptCode1.getCode()) == false) {
									clusterMembers.add(mEntry);
									addMember = true;
									seenCodes.put(ptCode1.getCode(), true);
								}

								// Test if we have put this code in a cluster
								if (seenCodes.containsKey(ptCode2.getCode()) == false) {
									clusterMembers.add(mEntry);
									addMember = true;
									seenCodes.put(ptCode2.getCode(), true);
								}
							}
						}

						clusters.put(clusterId, clusterMembers);

						// next cluster
						if (addMember == true)
							clusterId++;
					}

					// Record clustering results
					HashMap<String, Boolean> addedToResults = new HashMap<>();
					for (int cid : clusters.keySet()) {
						List<Distance> clusterMembers = clusters.get(cid);
						for (Distance dc : clusterMembers) {

							String code1 = dc.getCode1().getCode();
							String code2 = dc.getCode2().getCode();

							if (addedToResults.containsKey(code1) == false) {
								ClusterResult cr = new ClusterResult();
								cr.setCluster(cid);
								cr.setCode(dc.getCode1());
								cr.setValue(dc.getDistanceMetrics().get(metric));
								cr.setMetric(metric);
								cr.setOntology(ontology);
								cluster.getEntries().add(cr);
								addedToResults.put(code1, true);

							}

							if (addedToResults.containsKey(code2) == false) {
								ClusterResult cr = new ClusterResult();
								cr.setCluster(cid);
								cr.setCode(dc.getCode2());
								cr.setValue(dc.getDistanceMetrics().get(metric));
								cr.setMetric(metric);
								cr.setOntology(ontology);
								cluster.getEntries().add(cr);
								addedToResults.put(code2, true);

							}
						}
					}
				}
			}

			return cluster;

		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * Generate a set of codes with their distances from a given code
	 * 
	 * @param mCode    The code to compute from
	 * @param metric   The type of distance metric to be used
	 * @param ontoView The ontological view to impose
	 * @param minScore The minimum score to include a term in the match
	 * @param ptOnly   Include only PT terms
	 * @return
	 */
	public HashMap<MeddraCode, Double> getRelatedCodes(MeddraCode mCode, String myMetric, double minScore,
			boolean ptOnly, HashMap<String, com.gsk.sia.similarity.json.om.MeddraCode> faersCodes) {
		HashMap<String, MeddraCode> codesToKeep = new HashMap<>();
		HashMap<String, Double> results = new HashMap<String, Double>();
		try {

			// Step 1: Test if the code CUIs need to be updated
			if (mCode.getMeddraCodeType().getCodeType().contentEquals("LLT"))
				updateMeddraCui(mCode);

			HashMap<String, Double> matchedConcepts = new HashMap<>();

			// The metrics we want to test
			String[] metricArray = myMetric.split(",");
			List<SimilarityMetricEnum> metricList = parseMetrics(myMetric);

			// Load all UMLS Cuis for PT codes
			List<String> umlsCuis = new ArrayList<>();
			for (String cui : this.MEDRA_PT_CUIS.keySet())
				umlsCuis.add(cui);

			// Default parameters
			boolean lcs = false;

			// Parallelise the job
			umlsCuis.parallelStream().forEach(cui2 -> computeSingleConceptPairDistance(mCode, minScore, matchedConcepts,
					metricArray, metricList, lcs, cui2));

			// Now get the set of MedDRA codes that are in these matching CUIs
			// System.out.println("Matched CUIs: " + matchedConcepts.size());
			HashMap<String, Boolean> seenCode = new HashMap<>();
			for (String cui : matchedConcepts.keySet()) {
				double score = matchedConcepts.get(cui);
				List<MeddraCode> matchedCodes = MeddraCode.getCodesMatchedByCui(cui);
				for (MeddraCode mc : matchedCodes) {

					// Ignore if the same exact code
					if (mc.getRefId() != mCode.getRefId()) {

						// If we were passed in FAERS codes, then we are expected to filter on them
						boolean addCode = true;
						if (faersCodes != null) {
							if (faersCodes.size() > 0) {
								// If either Code 1 or 2 are not in FAERS, do not add this distance metric
								if (!faersCodes.containsKey(mc.getMeddraCode()))
									addCode = false;
							}
						}

						if (addCode == true) {

							// Apply filtering logic
							boolean isPt = false;
							if (mc.getMeddraCodeType().getCodeType().contentEquals("PT")) {
								isPt = true;
							}

							// Test for duplicate codes
							if (seenCode.containsKey(mc.getMeddraCode()) == false) {

								// Check if PT only filter in place
								if (ptOnly) {
									if (isPt == true) {
										results.put(mc.getMeddraCode(), score);
										codesToKeep.put(mc.getMeddraCode(), mc);
									}
								} else {
									results.put(mc.getMeddraCode(), score);
									codesToKeep.put(mc.getMeddraCode(), mc);
								}

								// record that we have seen the code
								seenCode.put(mc.getMeddraCode(), true);
							} else {
								if (isPt == true) {
									// over ride with the PT only version
									codesToKeep.put(mc.getMeddraCode(), mc);
								}
							}
						}
					}
				}
			}

		} catch (Exception e) {

		}

		// Consolidate final results
		HashMap<MeddraCode, Double> finalResults = new HashMap<>();
		for (String code : results.keySet()) {
			// Do not add the code to itself
			if (!code.contentEquals(mCode.getMeddraCode())) {
				Double score = results.get(code);
				MeddraCode mc = codesToKeep.get(code);
				finalResults.put(mc, score);
			}
		}
		return finalResults;
	}

	private void computeSingleConceptPairDistance(MeddraCode mCode, double minScore,
			HashMap<String, Double> matchedConcepts, String[] metricArray, List<SimilarityMetricEnum> metricList,
			boolean lcs, String cui2) {

		String cui1 = mCode.getUmlsCui();
		CuiPair cp = new CuiPair();
		cp.setCui1(cui1);
		cp.setCui2(cui2);

		List<ConceptPair> conceptPairs = cp.getConcept();
		List<ConceptPairSimilarity> conceptSimMap = simSvc.similarity(conceptPairs, metricList, null, lcs);

		// Add the labels
		List<String> metricLabels = new ArrayList<>();
		for (SimilarityMetricEnum metric : metricList) {
			metricLabels.add(metric.name());
		}

		int idx = 0;
		for (ConceptPairSimilarity csim : conceptSimMap) {
			for (Double sim : csim.getSimilarities()) {
				cp.addMetric(metricLabels.get(idx), sim);
				idx++;
			}
		}

		for (String m : metricArray) {

			try {
				double score = round(cp.getMetric(m), 6);
				if (score > minScore) {
					matchedConcepts.put(cui2, score);
				}
			} catch (Exception f1) {
			}

		}
	}

	/**
	 * Computes the entire distance matrix for a set of codes
	 * 
	 * @param onto
	 * @param metrics
	 * @param codes
	 * @return
	 */
	public DistanceMatrix computeInnerDistanceMatrix(String metrics, List<MeddraCode> codes, double[] distFilter) {
		DistanceMatrix matrix = new DistanceMatrix();
		matrix.setView(this.ONTOLOGY);

		try {
			// The metrics we want to compute
			String[] metricArray = metrics.split(",");

			// Are we filtering by distance?
			boolean useDistanceFilter = false;
			for (double v : distFilter) {
				if (v > -1)
					useDistanceFilter = true;
			}

			// Default parameters
			boolean lcs = false;

			// Get list of metrics to compute
			List<SimilarityMetricEnum> metricList = parseMetrics(metrics);

			// First pass - compute the inner PT related metrics
			List<Distance> allDistances = new ArrayList<>();

			// Create fully symetric matrix
			for (MeddraCode mc1 : codes) {

				// Test if the CUI requires updating
				updateMeddraCui(mc1);

				// Track the distances from a single code
				List<Distance> codeDistances = new ArrayList<>();
				boolean addDistance = true;
				for (MeddraCode mc2 : codes) {

					if (mc1.getRefId() != mc2.getRefId()) {
						// Test if the CUI requires updating
						updateMeddraCui(mc2);

						// Lookup the distance object or add to it
						Distance distance = new Distance();
						distance.setCode1(new com.gsk.sia.similarity.json.om.MeddraCode(mc1));
						distance.setCode2(new com.gsk.sia.similarity.json.om.MeddraCode(mc2));

						// From UMLS::Similarity test run
						HashMap<String, Double> results = getDistances(metrics, metricArray, lcs, simSvc, metricList,
								mc1.getUmlsCui(), mc2.getUmlsCui());
						for (String metric : results.keySet()) {
							try {
								double score = results.get(metric);
								distance.getDistanceMetrics().put(metric, score);
							} catch (Exception f1) {
								System.err.println("Error getting score from metric [1]");
							}
						}

						// Test if filter applies
						if (useDistanceFilter == true) {
							for (int i = 0; i < metricArray.length; i++) {
								String metric = metricArray[i];
								double minDist = distFilter[i];
								if (minDist > -1) {
									double testScore = results.get(metric);
									if (testScore < minDist) {
										addDistance = false;
									}
								}
							}

							// Apply the filter
							if (addDistance) {
								codeDistances.add(distance);
							}
						} else {
							// Add the distance
							codeDistances.add(distance);
						}
					}
				}

				// Compute max metric
				if (addDistance) {

					// Don't use self comparison for max calculation

					// Compute the max distance
					double maxCodeDistances[] = new double[metricList.size()];
					for (int i = 0; i < metricArray.length; i++) {
						maxCodeDistances[i] = -1d;
					}

					// Find max scores across all metrics
					for (Distance d : codeDistances) {

						// Don't use self comparison
						if (!d.getCode1().getCode().contentEquals(d.getCode2().getCode())) {
							HashMap<String, Double> scores = d.getDistanceMetrics();
							int i = 0;
							for (String metric : metricArray) {
								double tmpScore = scores.get(metric);
								if (tmpScore > maxCodeDistances[i])
									maxCodeDistances[i] = tmpScore;
								i++;
							}
						}
					}

					// Find entries matching max scores all metrics
					for (Distance d : codeDistances) {
						// Don't use self comparison
						if (!d.getCode1().getCode().contentEquals(d.getCode2().getCode())) {
							boolean isMax = false;
							HashMap<String, Double> scores = d.getDistanceMetrics();
							int i = 0;
							// max on any single metric will trigger flag
							for (String metric : metricArray) {

								try {
									double tmpScore = scores.get(metric);
									if (tmpScore >= maxCodeDistances[i])
										isMax = true;
								} catch (Exception f1) {
									System.err.println("Error getting score from metric [2]");
								}

								i++;
							}

							// Set max metric flag
							if (isMax)
								d.setMaxMetric(1);
						}
					}
				}

				// Add the distances computed for this code
				// System.out.println("Code distances compute: " + codeDistances.size());
				allDistances.addAll(codeDistances);
				// System.out.println("Total distances: " + allDistances.size());

			} // End computing distances from this MedDRA code

			// Compute max metric for each PT code
			matrix.setDistances(allDistances);

		} catch (Exception e) {
			System.err.println("Error computing inner matrix: " + e.toString());
		}

		return matrix;
	}

	/**
	 * Compute the distances between two codes
	 * 
	 * @param onto  Ontological View to compute distance on
	 * @param code1
	 * @param code2
	 * @return
	 */
	public HashMap<String, Double> computeDistance(String onto, String metric, MeddraCode mc1, MeddraCode mc2) {

		try {

			// Test if the CUI requires updating
			updateMeddraCui(mc1);

			// Test if the CUI requires updating
			updateMeddraCui(mc2);

			//
			// The metrics we want to compute
			//
			String metrics = metric;
			String[] metricArray = metrics.split(",");

			// Default parameters
			boolean lcs = false;

			// Get list of metrics to compute
			List<SimilarityMetricEnum> metricList = parseMetrics(metrics);

			// From UMLS::Similarity test run
			return getDistances(metrics, metricArray, lcs, simSvc, metricList, mc1.getUmlsCui(), mc2.getUmlsCui());
		} catch (Exception e) {
			return new HashMap<String, Double>();
		}
	}

	/**
	 * Compute the distances between two codes
	 * 
	 * @param onto  Ontological View to compute distance on
	 * @param code1
	 * @param code2
	 * @return
	 */

	public HashMap<String, Double> computeDistance(MeddraCode mc1, MeddraCode mc2) {

		try {

			// Step 1: Test if the code CUIs need to be updated
			if (mc1.getMeddraCodeType().getCodeType().contentEquals("LLT"))
				updateMeddraCui(mc1);

			if (mc2.getMeddraCodeType().getCodeType().contentEquals("LLT"))
				updateMeddraCui(mc2);

			//
			// The metrics we want to compute
			//
			String metrics = "LCH,INTRINSIC_LCH,WUPALMER_PERL,WUPALMER,INTRINSIC_LIN,JACCARD,SOKAL,INTRINSIC_RADA,DICE,SIMPSON,BRAUN_BLANQUET,OCHIAI,INTRINSIC_RESNIK";
			String[] metricArray = metrics.split(",");

			// Default parameters
			boolean lcs = false;

			// Get list of metrics to compute
			List<SimilarityMetricEnum> metricList = parseMetrics(metrics);

			// From UMLS::Similarity test run
			return getDistances(metrics, metricArray, lcs, simSvc, metricList, mc1.getUmlsCui(), mc2.getUmlsCui());
		} catch (Exception e) {
			return new HashMap<String, Double>();
		}
	}

	/**
	 * This should only be called on an LLT code
	 * 
	 * @param lltMeddraCode
	 */
	private void updateMeddraCui(MeddraCode lltMeddraCode) {

		try {
			if (lltMeddraCode.getMeddraCodeType().getCodeType().contentEquals("LLT")) {
				boolean hasCui = false;

				// Step 1: If the CUI is found in any PT term, then keep it
				if (this.MEDRA_PT_CUIS.containsKey(lltMeddraCode.getUmlsCui()) == true) {
					hasCui = true;
				}

				// Step 2: If the CUI is found in a target, then keep it
				if (this.ONTOLOGY.contains("sct")) {
					if (this.SNOMED_CUIS.containsKey(lltMeddraCode.getUmlsCui()) == true)
						hasCui = true;
				}

				if (this.ONTOLOGY.contains("msh")) {
					if (this.MESH_CUIS.containsKey(lltMeddraCode.getUmlsCui()) == true)
						hasCui = true;
				}

				// This is an LLT and has no CUI in either the target ontologies
				if (hasCui == false) {
					// Override the CUI for processing
					MeddraCode ptCode = lltMeddraCode.getMappedPTCode();
					if (ptCode != null) {
						lltMeddraCode.setUmlsCui(ptCode.getUmlsCui());
					}
				}
			}

		} catch (Exception e) {

		}

	}

	private static HashMap<String, Double> getDistances(String metrics, String[] metricArray, boolean lcs,
			ConceptSimilarityServiceImpl simSvc, List<SimilarityMetricEnum> metricList, String cui1, String cui2) {

		HashMap<String, Double> results = new HashMap<>();
		CuiPair cp = new CuiPair();
		cp.setCui1(cui1);
		cp.setCui2(cui2);

		List<ConceptPair> conceptPairs = cp.getConcept();
		List<ConceptPairSimilarity> conceptSimMap = simSvc.similarity(conceptPairs, metricList, null, lcs);

		// Add the labels
		List<String> metricLabels = new ArrayList<>();
		for (SimilarityMetricEnum metric : metricList) {
			metricLabels.add(metric.name());
		}

		int idx = 0;
		for (ConceptPairSimilarity csim : conceptSimMap) {
			for (Double sim : csim.getSimilarities()) {
				cp.addMetric(metricLabels.get(idx), sim);
				idx++;
			}
		}

		// Add scores to hashmap
		for (String m : metricArray) {
			try {
				double score = round(cp.getMetric(m), 6);
				results.put(m, score);
			} catch (Exception f1) {
				// We got NaN for PT 10070999 compared to itself ???
				// System.err.println("NaN found!");
				results.put(m, 0.0);
			}
		}

		return results;
	}

	/**
	 * Put error handling in to catch potential bad numbers
	 * 
	 * @param value
	 * @param places
	 * @return
	 */
	private static double round(double value, int places) {
		try {
			if (places < 0)
				throw new IllegalArgumentException();

			BigDecimal bd = new BigDecimal(Double.toString(value));
			bd = bd.setScale(places, RoundingMode.HALF_UP);
			return bd.doubleValue();
		} catch (Exception e) {
			return 0.0;
		}
	}

	private static List<SimilarityMetricEnum> parseMetrics(String metrics) {
		String ms[] = metrics.split(",");
		List<SimilarityMetricEnum> metricSet = new ArrayList<SimilarityMetricEnum>();
		for (String metric : ms) {
			SimilarityMetricEnum m = SimilarityMetricEnum.valueOf(metric);
			if (m == null)
				System.err.println("invalid metric: " + ms);
			else
				metricSet.add(m);
		}
		return metricSet;
	}

	/**
	 * Compute the outer distance matrix
	 * 
	 * @param onto
	 * @param metrics
	 * @param codes
	 * @param faers_codes
	 * @return
	 */
	public DistanceMatrix computeOuterDistanceMatrix(String metrics, List<MeddraCode> codes,
			HashMap<String, com.gsk.sia.similarity.json.om.MeddraCode> faers_codes, double[] distFilter,
			boolean ptOnly) {
		// Setup our distance matrix
		DistanceMatrix matrix = new DistanceMatrix();
		matrix.setView(this.ONTOLOGY);

		// Are we filtering by distance?
		boolean useDistanceFilter = false;
		for (double v : distFilter) {
			if (v > -1)
				useDistanceFilter = true;
		}

		try {
			// The metrics we want to compute
			String[] metricArray = metrics.split(",");

			// Default parameters
			boolean lcs = false;

			// Get list of metrics to compute
			List<SimilarityMetricEnum> metricList = parseMetrics(metrics);

			// First pass - compute the inner PT related metrics
			List<Distance> allDistances = new ArrayList<>();

			// Identify the codes in SMQ
			HashMap<String, Boolean> smqCodes = new HashMap<>();
			for (MeddraCode mc : codes) {

				boolean addCode = true;
				if (ptOnly == true) {
					if (!mc.getTty().contentEquals("PT"))
						addCode = false;
				}

				if (addCode == true) {
					// Test if the CUI requires updating before storing in our SMQ code set
					updateMeddraCui(mc);

					// Assign to the set of SMQ codes
					smqCodes.put(mc.getMeddraCode(), true);
				}
			}

			// Load FAERS codes
			List<com.gsk.sia.similarity.json.om.MeddraCode> faersCodes = new ArrayList<>();
			for (String ptCode : faers_codes.keySet()) {
				if (smqCodes.containsKey(ptCode) == false) {
					faersCodes.add(faers_codes.get(ptCode));
				}
			}

			// Code 1 will be PTs from the SMQ
			for (MeddraCode mc1 : codes) {

				// Code 2 will be PTs only from FAERS
				for (com.gsk.sia.similarity.json.om.MeddraCode mc2 : faersCodes) {

					boolean addCode = true;
					if (ptOnly == true) {
						if (!mc2.getTty().contentEquals("PT"))
							addCode = false;
					}

					if (addCode == true) {
						// Lookup the distance object or add to it
						Distance distance = new Distance();
						distance.setCode1(new com.gsk.sia.similarity.json.om.MeddraCode(mc1));
						distance.setCode2(mc2);

						// From UMLS::Similarity test run
						HashMap<String, Double> results = getDistances(metrics, metricArray, lcs, simSvc, metricList,
								mc1.getUmlsCui(), mc2.getCui());
						for (String metric : results.keySet()) {
							double score = results.get(metric);
							distance.getDistanceMetrics().put(metric, score);
						}

						// Test if filter applies
						boolean addDistance = true;
						if (useDistanceFilter == true) {
							for (int i = 0; i < metricArray.length; i++) {
								String metric = metricArray[i];
								double minDist = distFilter[i];
								if (minDist > -1) {
									double testScore = results.get(metric);
									if (testScore < minDist) {
										addDistance = false;
									}
								}
							}

							// Apply the filter
							if (addDistance)
								allDistances.add(distance);
						} else {
							// Add the distance
							allDistances.add(distance);
						}

					}
				}

			} // End computing distances from this MedDRA code

			// Compute max metric for each PT code
			matrix.setDistances(allDistances);

		} catch (

		Exception e) {
		}

		return matrix;
	}

	/**
	 * Compute the filter results
	 * 
	 * @param smq
	 * @param onto
	 * @param metrics
	 * @param distFilter
	 * @param faers_codes
	 * @param smqCodes
	 * @return
	 */
	public List<ClusterMetric> computeOuterMatrixFiltered(MeddraSmq smq, String metrics, double[] distFilter,
			List<MeddraCode> smqCodes, HashMap<String, com.gsk.sia.similarity.json.om.MeddraCode> faers_codes,
			boolean ptOnly) {
		List<ClusterMetric> results = new ArrayList<>();

		List<String> listMetricArray = new ArrayList<>();
		String[] metricArray = metrics.split(",");
		for (String metric : metricArray) {
			listMetricArray.add(metric);
		}

		// Parallelise the job
		listMetricArray.parallelStream().forEach(metric -> computeOuterMatrixSingleMetric(smq, distFilter, metricArray,
				smqCodes, faers_codes, results, metric, ptOnly));

		return results;
	}

	/**
	 * Compute a single PT matrix with a given metric
	 * 
	 * @param smq
	 * @param distFilter
	 * @param metricArray
	 * @param smqCodes
	 * @param faers_codes
	 * @param results
	 * @param metric
	 * @return
	 */
	private void computeOuterMatrixSingleMetric(MeddraSmq smq, double[] distFilter, String[] metricArray,
			List<MeddraCode> smqCodes, HashMap<String, com.gsk.sia.similarity.json.om.MeddraCode> faers_codes,
			List<ClusterMetric> results, String metric, boolean ptOnly) {

		// Debugging
		// System.out.println("Compute outer matrix: " + this.ONTOLOGY + " - " +
		// metric);

		// Set the ssm max for this metric
		double ssmMax = 0;
		for (int i = 0; i < metricArray.length; i++) {
			if (metricArray[i] == metric) {
				ssmMax = distFilter[i];
			}
		}

		// Create new metric counter
		ClusterMetric cm = new ClusterMetric();
		cm.setSmq(smq.getCode());
		cm.setOntology(this.ONTOLOGY);
		cm.setSsm(metric);
		cm.setSsmMax(ssmMax);

		// Filter for single metric
		double[] myFilter = new double[] { ssmMax };
		double[] noFilter = new double[] { -1.0 };
		DistanceMatrix unfilteredMatrix = computeOuterDistanceMatrix(metric, smqCodes, faers_codes, noFilter, ptOnly);
		DistanceMatrix filteredMatrix = computeOuterDistanceMatrix(metric, smqCodes, faers_codes, myFilter, ptOnly);

		// Count unique number of codes found
		cm.setOuterPtCount(unfilteredMatrix.getJsonCodes().size());
		cm.setFilteredOuterPtCount(filteredMatrix.getJsonCodes().size());
		results.add(cm);

		return;
	}

	/**
	 * Get related codes for a single view
	 * 
	 * @param mc
	 * @param metrics
	 * @param distanceFilter
	 * @param filter
	 * @return
	 */
	public DistanceMatrix getRelatedCodeMatrix(MeddraCode mc, String[] metricArray, double[] distanceFilter,
			boolean ptOnly, HashMap<String, com.gsk.sia.similarity.json.om.MeddraCode> faersCodes) {

		// Result matrix
		DistanceMatrix matrix = new DistanceMatrix();
		HashMap<String, Distance> allDistances = new HashMap<>();
		HashMap<String, MeddraCode> mdrCodeMap = new HashMap<>();
		try {
			// Iterate over all the metrics requested
			for (int index = 0; index < metricArray.length; index++) {
				String metric = metricArray[index];
				double minScore = distanceFilter[index];
				HashMap<MeddraCode, Double> codes = getRelatedCodes(mc, metric, minScore, ptOnly, faersCodes);

				for (MeddraCode mc2 : codes.keySet()) {

					// Store for below
					mdrCodeMap.put(mc2.getMeddraCode(), mc2);

					String key = mc.getRefId() + "-" + mc2.getRefId();
					double score = codes.get(mc2);
					Distance dObj = null;
					if (allDistances.containsKey(key) == false) {
						// Create the distance object
						dObj = new Distance();
						dObj.setCode1(new com.gsk.sia.similarity.json.om.MeddraCode(mc));
						dObj.setCode2(new com.gsk.sia.similarity.json.om.MeddraCode(mc2));
						allDistances.put(key, dObj);
					} else {
						dObj = allDistances.get(key);
					}

					// Update the metric value
					dObj.getDistanceMetrics().put(metric, score);
				}
			}

			// Convert to a list
			List<Distance> myDistances = new ArrayList<>();
			for (String key : allDistances.keySet()) {
				// Update any object that does not contain a distance metric
				Distance dObj = allDistances.get(key);
				for (String metric : metricArray) {

					if (!dObj.getDistanceMetrics().containsKey(metric)) {

						// Compute single distance
						if (mdrCodeMap.containsKey(dObj.getCode2().getCode()) == true) {
							MeddraCode mc2 = mdrCodeMap.get(dObj.getCode2().getCode());
							HashMap<String, Double> distance = computeDistance(this.ONTOLOGY, metric, mc, mc2);
							double score = distance.get(metric);
							dObj.getDistanceMetrics().put(metric, score);
						}
					}
				}
				myDistances.add(dObj);
			}

			// Set distances on the matrix object
			matrix.setDistances(myDistances);

		} catch (Exception e) {
			System.err.println("Error in getRelatedCodeMatrix() " + e.toString());
		}

		return matrix;
	}

	public DistanceMatrix getNormalizedRelatedCodeMatrix(MeddraCode mc, String[] metricArray, double[] distanceFilter,
			boolean ptOnly, HashMap<String, com.gsk.sia.similarity.json.om.MeddraCode> faersCodes, double minValue,
			double maxValue) {
		// Result matrix
		DistanceMatrix matrix = new DistanceMatrix();
		HashMap<String, Distance> allDistances = new HashMap<>();
		HashMap<String, MeddraCode> mdrCodeMap = new HashMap<>();
		try {
			// Iterate over all the metrics requested
			for (int index = 0; index < metricArray.length; index++) {
				String metric = metricArray[index];

				// Find the min score
				double minScore = distanceFilter[index];
				HashMap<MeddraCode, Double> codes = getRelatedCodes(mc, metric, minScore, ptOnly, faersCodes);

				for (MeddraCode mc2 : codes.keySet()) {

					// Store for below
					mdrCodeMap.put(mc2.getMeddraCode(), mc2);

					String key = mc.getRefId() + "-" + mc2.getRefId();
					double score = codes.get(mc2);
					score = normalizeScore( score, minValue, maxValue );
					
					Distance dObj = null;
					if (allDistances.containsKey(key) == false) {
						// Create the distance object
						dObj = new Distance();
						dObj.setCode1(new com.gsk.sia.similarity.json.om.MeddraCode(mc));
						dObj.setCode2(new com.gsk.sia.similarity.json.om.MeddraCode(mc2));
						allDistances.put(key, dObj);
					} else {
						dObj = allDistances.get(key);
					}

					// Update the metric value
					dObj.getDistanceMetrics().put(metric, score);
				}
			}

			// Convert to a list
			List<Distance> myDistances = new ArrayList<>();
			for (String key : allDistances.keySet()) {
				// Update any object that does not contain a distance metric
				Distance dObj = allDistances.get(key);
				for (String metric : metricArray) {

					if (!dObj.getDistanceMetrics().containsKey(metric)) {

						// Compute single distance
						if (mdrCodeMap.containsKey(dObj.getCode2().getCode()) == true) {
							MeddraCode mc2 = mdrCodeMap.get(dObj.getCode2().getCode());
							HashMap<String, Double> distance = computeDistance(this.ONTOLOGY, metric, mc, mc2);
							double score = distance.get(metric);
							score = normalizeScore(score, minValue, maxValue);
							dObj.getDistanceMetrics().put(metric, score);
						}
					}
				}
				myDistances.add(dObj);
			}

			// Set distances on the matrix object
			matrix.setDistances(myDistances);

		} catch (Exception e) {
			System.err.println("Error in getNormalizedRelatedCodeMatrix() " + e.toString());
		}

		return matrix;
	}

	/**
	 * Compute the normalized score
	 * 
	 * @param score
	 * @param min
	 * @param max
	 * @return
	 */
	private double normalizeScore(double score, double min, double max) {
		double norm = (score - min) / (max - min);
		return norm;
	}

}
