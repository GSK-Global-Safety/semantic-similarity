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
package org.apache.ctakes.ytex.kernel.tree;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.sql.DataSource;
import java.io.*;
import java.util.*;

public class InstanceTreeBuilderImpl implements InstanceTreeBuilder {
	static final Log log = LogFactory.getLog(InstanceTreeBuilderImpl.class);	
	private DataSource dataSource;

	public DataSource getDataSource() {
		return dataSource;
	}

	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	Node nodeFromRow(NodeMappingInfo nodeInfo, Map<String, Object> nodeValues) {
		Node n = null;
		Map<String, Serializable> values = new HashMap<String, Serializable>(
				nodeInfo.getValues().size());
		for (String valueName : nodeInfo.getValues()) {
			if (nodeValues.containsKey(valueName)
					&& nodeValues.get(valueName) != null) {
				values.put(valueName, (Serializable) nodeValues.get(valueName));
			}
		}
		// make sure there is something to put in
		if (!values.isEmpty()) {
			n = new Node();
			n.setType(nodeInfo.getNodeType());
			n.setValue(values);
		}
		return n;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Map<Long, Node> loadInstanceTrees(String filename)
			throws IOException, ClassNotFoundException {
		ObjectInputStream os = null;
		try {
			os = new ObjectInputStream(new BufferedInputStream(
					new FileInputStream(filename)));
			return (Map<Long, Node>) os.readObject();
		} finally {
			if (os != null)
				os.close();
		}
	}






}
