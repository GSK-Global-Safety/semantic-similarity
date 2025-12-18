/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.ctakes.ytex.kernel.dao;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.ctakes.ytex.kernel.IntrinsicInfoContentEvaluator;
import org.apache.ctakes.ytex.kernel.model.ConcRel;
import org.apache.ctakes.ytex.kernel.model.ConceptGraph;
import org.hibernate.SessionFactory;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

public class ConceptDaoImpl implements ConceptDao {
	private static String CONCEPT_GRAPH_PATH = "org/apache/ctakes/ytex/conceptGraph/";
	/**
	 * the default concept id for the root. override with -Dytex.defaultRootId
	 */
	private static final String DEFAULT_ROOT_ID = "C0000000";
	/**
	 * ignore forbidden concepts. list Taken from umls-interface. f concept is one
	 * of the following just return #C1274012|Ambiguous concept (inactive concept)
	 * if($concept=~/C1274012/) { return 1; } #C1274013|Duplicate concept (inactive
	 * concept) if($concept=~/C1274013/) { return 1; } #C1276325|Reason not stated
	 * concept (inactive concept) if($concept=~/C1276325/) { return 1; }
	 * #C1274014|Outdated concept (inactive concept) if($concept=~/C1274014/) {
	 * return 1; } #C1274015|Erroneous concept (inactive concept)
	 * if($concept=~/C1274015/) { return 1; } #C1274021|Moved elsewhere (inactive
	 * concept) if($concept=~/C1274021/) { return 1; } #C1443286|unapproved
	 * attribute if($concept=~/C1443286/) { return 1; } #C1274012|non-current
	 * concept - ambiguous if($concept=~/C1274012/) { return 1; } #C2733115|limited
	 * status concept if($concept=~/C2733115/) { return 1; }
	 */
	private static final String defaultForbiddenConceptArr[] = new String[] { "C1274012", "C1274013", "C1276325",
			"C1274014", "C1274015", "C1274021", "C1443286", "C1274012", "C2733115" };
	private static Set<String> defaultForbiddenConcepts;
	private static final Log log = LogFactory.getLog(ConceptDaoImpl.class);

	static {
		defaultForbiddenConcepts = new HashSet<String>();
		defaultForbiddenConcepts.addAll(Arrays.asList(defaultForbiddenConceptArr));
	}

	public void setGraphPath(String conceptGraphPath) {
		this.CONCEPT_GRAPH_PATH = conceptGraphPath;
	}
	
	private IntrinsicInfoContentEvaluator intrinsicInfoContentEvaluator;

	private SessionFactory sessionFactory;

	private Properties ytexProperties;

	/**
	 * add the relationship to the concept map
	 * 
	 * @param conceptMap
	 * @param conceptIndexMap
	 * @param conceptList
	 * @param roots
	 * @param conceptPair
	 */
	private void addRelation(ConceptGraph cg, Set<String> roots, String childCUI, String parentCUI, boolean checkCycle,
			Set<String> forbiddenConcepts) {
		if (forbiddenConcepts.contains(childCUI) || forbiddenConcepts.contains(parentCUI)) {
			// ignore relationships to useless concepts
			if (log.isDebugEnabled())
				log.debug("skipping relation because of forbidden concept: par=" + parentCUI + " child=" + childCUI);
			return;
		}
		// ignore self relations
		if (!childCUI.equals(parentCUI)) {
			boolean parNull = false;
			// get parent from cui map
			ConcRel crPar = cg.getConceptMap().get(parentCUI);
			if (crPar == null) {
				parNull = true;
				// parent not in cui map - add it
				crPar = cg.addConcept(parentCUI);
				// this is a candidate root - add it to the set of roots
				roots.add(parentCUI);
			}
			// get the child cui
			ConcRel crChild = cg.getConceptMap().get(childCUI);
			// crPar already has crChild, return
			if (crChild != null && crPar.getChildren().contains(crChild))
				return;
			// avoid cycles - don't add child cui if it is an ancestor
			// of the parent. if the child is not yet in the map, then it can't
			// possibly induce a cycle.
			// if the parent is not yet in the map, it can't induce a cycle
			// else check for cycles
			// @TODO: this is very inefficient. implement feedback arc algo
			boolean bCycle = !parNull && crChild != null && checkCycle && checkCycle(crPar, crChild);
			if (bCycle) {
				log.warn("skipping relation that induces cycle: par=" + parentCUI + ", child=" + childCUI);
			} else {
				if (crChild == null) {
					// child not in cui map - add it
					crChild = cg.addConcept(childCUI);
				} else {
					// remove the cui from the list of candidate roots
					if (roots.contains(childCUI))
						roots.remove(childCUI);
				}
				// link child to parent and vice-versa
				crPar.getChildren().add(crChild);
				crChild.getParents().add(crPar);
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.ctakes.ytex.kernel.dao.ConceptDao#getConceptGraph(java.util
	 * .Set)
	 */
	public ConceptGraph getConceptGraph(String name) {
		ConceptGraph cg = this.readConceptGraph(name);
		if (cg != null) {
			this.initializeConceptGraph(cg);
			if (log.isInfoEnabled()) {
				log.info(String.format("concept graph %s, vertices: %s", name, cg.getConceptList().size()));
			}
		}
		return cg;
	}

	private File urlToFile(URL url) {
		if (url != null && "file".equals(url.getProtocol())) {
			File f;
			try {
				f = new File(url.toURI());
			} catch (URISyntaxException e) {
				f = new File(url.getPath());
			}
			return f;
		} else {
			return null;
		}

	}

	/**
	 * use value of org.apache.ctakes.ytex.conceptGraphDir if defined. else try to
	 * determine ytex.properties location and use the conceptGraph directory there.
	 * else return null
	 * 
	 * @return
	 */
	public String getDefaultConceptGraphDir() {
		return CONCEPT_GRAPH_PATH;
	}

	public IntrinsicInfoContentEvaluator getIntrinsicInfoContentEvaluator() {
		return intrinsicInfoContentEvaluator;
	}

	public SessionFactory getSessionFactory() {
		return sessionFactory;
	}

	public Properties getYtexProperties() {
		return ytexProperties;
	}

	private boolean checkCycle(ConcRel crPar, ConcRel crChild) {
		HashSet<Integer> visitedNodes = new HashSet<Integer>();
		return hasAncestor(crPar, crChild, visitedNodes);
	}

	/**
	 * check cycle.
	 * 
	 * @param crPar        parent
	 * @param crChild      child that should not be an ancestor of parent
	 * @param visitedNodes nodes we've visited in our search. keep track of this to
	 *                     avoid visiting the same node multiple times
	 * @return true if crChild is an ancestor of crPar
	 */
	private boolean hasAncestor(ConcRel crPar, ConcRel crChild, HashSet<Integer> visitedNodes) {
		// see if we've already visited this node - if yes then no need to redo
		// this
		if (visitedNodes.contains(crPar.getNodeIndex()))
			return false;
		// see if we're the same
		if (crPar.getNodeIndex() == crChild.getNodeIndex())
			return true;
		// recurse
		for (ConcRel c : crPar.getParents()) {
			if (hasAncestor(c, crChild, visitedNodes))
				return true;
		}
		// add ourselves to the set of visited nodes so we no not to revisit
		// this
		visitedNodes.add(crPar.getNodeIndex());
		return false;
	}

	/**
	 * replace cui strings in concrel with references to other nodes. initialize the
	 * concept list
	 * 
	 * @param cg
	 * @return
	 */
	private ConceptGraph initializeConceptGraph(ConceptGraph cg) {
		ImmutableMap.Builder<String, ConcRel> mb = new ImmutableMap.Builder<String, ConcRel>();
		for (ConcRel cr : cg.getConceptList()) {
			// use adjacency list representation for concept graphs that have
			// cycles
			if (cg.getDepthMax() > 0)
				cr.constructRel(cg.getConceptList());
			mb.put(cr.getConceptID(), cr);
		}
		cg.setConceptMap(mb.build());
		return cg;
	}

	private ConceptGraph readConceptGraph(String name) {
		ObjectInputStream is = null;
		try {
			// try loading from classpath
			InputStream resIs = this.getClass().getClassLoader().getResourceAsStream(CONCEPT_GRAPH_PATH + name + ".gz");
			if (resIs == null) {
				String cdir = this.getDefaultConceptGraphDir();
				if (cdir == null) {
					throw new IllegalArgumentException(
							"could not determine default concept graph directory; please set property org.apache.ctakes.ytex.conceptGraphDir");
				}
				File f = new File(cdir + "/" + name + ".gz");
				log.info("could not load conceptGraph from classpath, attempt to load from: " + f.getAbsolutePath());
				if (f.exists()) {
					resIs = new FileInputStream(f);
				} else {
					log.info(f.getAbsolutePath() + " not found, cannot load concept graph");
				}
			} else {
				log.info("loading concept graph from "
						+ this.getClass().getClassLoader().getResource(CONCEPT_GRAPH_PATH + name + ".gz"));
			}
			if (resIs != null) {
				is = new ObjectInputStream(new BufferedInputStream(new GZIPInputStream(resIs)));
				return (ConceptGraph) is.readObject();
			} else {
				log.info("could not load conceptGraph: " + name);
				return null;
			}
		} catch (IOException ioe) {
			throw new RuntimeException(ioe);
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		} finally {
			if (is != null)
				try {
					is.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
		}

	}

	public void setIntrinsicInfoContentEvaluator(IntrinsicInfoContentEvaluator intrinsicInfoContentEvaluator) {
		this.intrinsicInfoContentEvaluator = intrinsicInfoContentEvaluator;
	}

	// /**
	// * get maximum depth of graph.
	// *
	// * @param roots
	// * @param conceptMap
	// * @return
	// */
	// private int calculateDepthMax(String rootId, Map<String, ConcRel>
	// conceptMap) {
	// ConcRel crRoot = conceptMap.get(rootId);
	// return crRoot.depthMax();
	// }

	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	public void setYtexProperties(Properties ytexProperties) {
		this.ytexProperties = new Properties(ytexProperties);
		this.ytexProperties.putAll(System.getProperties());
	}

	// /**
	// * add parent to all descendants of crChild
	// *
	// * @param crPar
	// * @param crChild
	// * @param ancestorCache
	// */
	// private void updateDescendants(Set<Integer> ancestorsPar, ConcRel
	// crChild,
	// Map<Integer, Set<Integer>> ancestorCache, int depth) {
	// if (ancestorCache != null) {
	// Set<Integer> ancestors = ancestorCache.get(crChild.nodeIndex);
	// if (ancestors != null)
	// ancestors.addAll(ancestorsPar);
	// // recurse
	// for (ConcRel crD : crChild.getChildren()) {
	// updateDescendants(ancestorsPar, crD, ancestorCache, depth + 1);
	// }
	// }
	// }

	/**
	 * write the concept graph, create parent directories as required
	 * 
	 * @param name
	 * @param cg
	 */
	private void writeConceptGraph(String dir, String name, ConceptGraph cg) {
		ObjectOutputStream os = null;
		String outputDir = dir;
		File cgFile = new File(outputDir + "/" + name + ".gz");
		log.info("writing concept graph: " + cgFile.getAbsolutePath());
		if (!cgFile.getParentFile().exists())
			cgFile.getParentFile().mkdirs();
		try {
			os = new ObjectOutputStream(new BufferedOutputStream(new GZIPOutputStream(new FileOutputStream(cgFile))));
			// replace the writable list with an immutable list
			cg.setConceptList(ImmutableList.copyOf(cg.getConceptList()));
			os.writeObject(cg);
		} catch (IOException ioe) {
			throw new RuntimeException(ioe);
		} finally {
			if (os != null)
				try {
					os.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
		}
	}

}
