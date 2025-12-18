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
package org.apache.ctakes.ytex.kernel;

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * parse classifier evaluation results. expect input data files to classifier in
 * working directory. expect output in dir option or subdirectories thereof.
 * expect an options.properties in each directory that contains classifier
 * output. See {@link #ClassifierEvaluationImporter()} for a list of options in
 * options.properties. You can override options via system properties (java -D
 * options).
 * 
 * @author vijay
 */
public class ClassifierEvaluationImporter {
	private static final Log log = LogFactory
			.getLog(ClassifierEvaluationImporter.class);

	private Map<String, ClassifierEvaluationParser> nameToParserMap;

	public Map<String, ClassifierEvaluationParser> getNameToParserMap() {
		return nameToParserMap;
	}

	public void setNameToParserMap(
			Map<String, ClassifierEvaluationParser> nameToParserMap) {
		this.nameToParserMap = nameToParserMap;
	}


}
