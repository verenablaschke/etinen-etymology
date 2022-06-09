/*
 * Copyright 2018–2022 University of Tübingen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tuebingen.sfs.eie.components.etymology.problems;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.DoubleNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import de.tuebingen.sfs.psl.engine.DatabaseManager;
import de.tuebingen.sfs.psl.engine.PslProblemConfig;
import de.tuebingen.sfs.psl.util.log.InferenceLogger;

public class EtymologyProblemConfig extends PslProblemConfig {

	private static final double DEFAULT_THRESHOLD = 0.05;
	private static final String DEFAULT_LOGFILE_PATH = "src/test/resources/etym-inf-log.txt";
	private static final int DEFAULT_MAX_SEM_EDGE_DIST = 2;

	private List<Integer> formIds = null;

	private Map<String, Double> ruleWeights;
	private Set<String> ignoreRules;
	// The maximum number of edges that can be between a pair of concepts for them
	// to get a Ssim score higher than 0.
	private int maxSemEdgeDist;

	private double persistenceThreshold;

	private InferenceLogger logger;

	public EtymologyProblemConfig() {
		resetToDefaults();
	}

	public EtymologyProblemConfig(String problemId, DatabaseManager dbManager) {
		this();
		setNonPersistableFeatures(problemId, dbManager);
	}

	public EtymologyProblemConfig(List<Integer> formIds, Map<String, Double> ruleWeights, Set<String> ignoreRules,
			int maxSemEdgeDist, Double persistenceThreshold, InferenceLogger logger) {
		resetToDefaults();
		this.logger = logger;
		logger.displayln("Creating EtymologyIdeaGeneratorConfig.");
		if (formIds == null) {
			logger.displayln("...No forms specified.");
		} else {
			this.formIds = formIds;
		}
		if (ruleWeights == null) {
			logger.displayln("...No rule weights specified.");
		} else {
			this.ruleWeights = ruleWeights;
		}
		if (ignoreRules == null) {
			logger.displayln("...No blacklist of rules specified.");
		} else {
			this.ignoreRules = ignoreRules;
		}
		if (maxSemEdgeDist < 0) {
			logger.displayln("...No maximum concept graph distance specified.");
		} else {
			this.maxSemEdgeDist = maxSemEdgeDist;
		}
		if (persistenceThreshold == null) {
			logger.displayln(
					"...No confidence threshold for persistence specified, using default: " + DEFAULT_THRESHOLD);
		} else {
			this.persistenceThreshold = persistenceThreshold;
		}
	}

	public static EtymologyProblemConfig fromStream(ObjectMapper mapper, InputStream in, InferenceLogger logger) {
		EtymologyProblemConfig config = new EtymologyProblemConfig();
		try {
			JsonNode rootNode = mapper.readTree(in);
			config.setFromJson(mapper, rootNode);
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			in.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return config;
	}

	public static EtymologyProblemConfig fromJson(ObjectMapper mapper, String path, InferenceLogger logger) {
		try {
			return fromStream(mapper, new FileInputStream(path), logger);
		} catch (FileNotFoundException e) {
			System.err.println("File not found, using default configuration file (" + path + ")");
			return new EtymologyProblemConfig();
		}
	}

	public static EtymologyProblemConfig fromJson(ObjectMapper mapper, JsonNode configJsonRoot) {
		EtymologyProblemConfig config = new EtymologyProblemConfig();
		config.setFromJson(mapper, configJsonRoot);
		return config;
	}

	public EtymologyProblemConfig copy() {
		EtymologyProblemConfig copy = new EtymologyProblemConfig();
		super.copyFields(copy);

		copy.formIds = new ArrayList<>(formIds);

		copy.ruleWeights = new HashMap<>(ruleWeights);
		copy.ignoreRules = new HashSet<>(ignoreRules);
		copy.maxSemEdgeDist = maxSemEdgeDist;

		// copy.logger = logger;
		copy.setLogfile(super.getLogfilePath());
		copy.persistenceThreshold = persistenceThreshold;

		return copy;
	}

	// -------------
	// Get/set/add
	// -------------

	public void resetToDefaults() {
		super.resetToDefaults();
		super.setName("etymology");
		super.setDeclareUserPrior(false); // TODO

		formIds = new ArrayList<>();
		ruleWeights = new TreeMap<>();
		ignoreRules = new TreeSet<>();
		maxSemEdgeDist = DEFAULT_MAX_SEM_EDGE_DIST;

		logger = new InferenceLogger();
		setLogfile(DEFAULT_LOGFILE_PATH);
		persistenceThreshold = DEFAULT_THRESHOLD;
	}

	public List<Integer> getFormIds() {
		return formIds;
	}

	public void setFormIds(List<Integer> formIds) {
		this.formIds = formIds;
	}

	public void setIgnoreRules(Set<String> ignoreRules) {
		this.ignoreRules = ignoreRules;
	}

	public void addRuleToIgnoreList(String rule) {
		ignoreRules.add(rule);
	}

	public boolean include(String rule) {
		return !ignoreRules.contains(rule);
	}

	public void setRuleWeights(Map<String, Double> ruleWeights) {
		this.ruleWeights = ruleWeights;
	}

	public void addRuleWeight(String rule, double weight) {
		ruleWeights.put(rule, weight);
	}

	public double getRuleWeightOrDefault(String rule, double defaultWeight) {
		return ruleWeights.getOrDefault(rule, defaultWeight);
	}

	public boolean hasRuleWeight(String rule) {
		return ruleWeights.containsKey(rule);
	}

	public double getRuleWeight(String rule) {
		return ruleWeights.get(rule);
	}

	public double getBeliefThreshold() {
		return persistenceThreshold;
	}

	public void setBeliefThreshold(Double threshold) {
		if (threshold != null)
			this.persistenceThreshold = threshold;
	}

	public int getMaxSemEdgeDist() {
		return maxSemEdgeDist;
	}

	public void setMaxSemEdgeDist(int maxSemEdgeDist) {
		if (maxSemEdgeDist >= 0)
			this.maxSemEdgeDist = maxSemEdgeDist;
	}

	public void setNonPersistableFeatures(String problemId, DatabaseManager dbManager) {
		setName(problemId);
		setDbManager(dbManager);
	}

	// ------------
	// Print/log
	// ------------

	public void print(PrintStream out) {
		out.println("Etymology config");
		out.println("- Forms: " + formIds);
		out.println("- Maximum concept distance: " + maxSemEdgeDist);
		if (ignoreRules == null || ignoreRules.isEmpty()) {
			out.println("- No rules to ignore.");
		} else {
			out.println("- Ignoring:");
			for (String rule : ignoreRules) {
				out.println("  - " + rule);
			}
		}
		if (ruleWeights == null || ruleWeights.isEmpty()) {
			out.println("- No rule weights changed.");
		} else {
			out.println("- Updated rule weights:");
			for (Entry<String, Double> entry : ruleWeights.entrySet()) {
				out.println("  - " + entry.getKey() + " : " + entry.getValue());
			}
		}
	}

	public void logSettings() {
		logger.displayln("Etymology config");
		logger.displayln("- Forms (in config!): " + formIds);
		logger.displayln("- Maximum concept distance: " + maxSemEdgeDist);
		if (ignoreRules == null || ignoreRules.isEmpty()) {
			logger.displayln("- No rules to ignore.");
		} else {
			logger.displayln("- Ignoring:");
			for (String rule : ignoreRules) {
				logger.displayln("  - " + rule);
			}
		}
		if (ruleWeights == null || ruleWeights.isEmpty()) {
			logger.displayln("- No rule weights changed.");
		} else {
			logger.displayln("- Updated rule weights:");
			for (Entry<String, Double> entry : ruleWeights.entrySet()) {
				logger.displayln("  - " + entry.getKey() + " : " + entry.getValue());
			}
		}
	}

	// ---------------
	// Import/export
	// ---------------

	@SuppressWarnings("unchecked")
	public void setFromJson(ObjectMapper mapper, JsonNode rootNode) {
		System.err.println("Setting EtymologyProblemConfig from JSON");
		super.setFromJson(mapper, rootNode);

		try {
			String name = mapper.treeToValue(rootNode.path(PslProblemConfig.NAME_FIELD), String.class);
			if (name != null)
				super.setName(name);
		} catch (JsonProcessingException e) {
			System.err.println("No name given. (Using default.)");
		}
		try {
			Boolean declareUserPrior = mapper.treeToValue(rootNode.path(PslProblemConfig.USER_PRIOR_FIELD),
					Boolean.class);
			if (declareUserPrior != null)
				super.setDeclareUserPrior(declareUserPrior);
		} catch (JsonProcessingException e) {
			System.err.println("No value for declareUserPrior given. (Using default.)");
		}
		try {
			// TODO check integer conversion
			List<Integer> formIds = mapper.treeToValue(rootNode.path("formIds"), ArrayList.class);
			if (formIds != null)
				setFormIds(formIds);
		} catch (JsonProcessingException e) {
			System.err.println("No form IDs given. (Using empty list.)");
		}
		try {
			Object ruleWeights = mapper.treeToValue(rootNode.path("ruleWeights"), HashMap.class);
			if (ruleWeights != null) {
				setRuleWeights((HashMap<String, Double>) ruleWeights);
			}
		} catch (JsonProcessingException e) {
			System.err.println("No ruleWeights given. (Using default.)");
		}
		try {
			Object ignoreRules = mapper.treeToValue(rootNode.path("ignoreRules"), HashSet.class);
			if (ignoreRules != null) {
				setIgnoreRules((HashSet<String>) ignoreRules);
			}
		} catch (JsonProcessingException e) {
			System.err.println("No rule blacklist given. (Using default.)");
		}
		try {
			Object maxSemEdgeDist = mapper.treeToValue(rootNode.path("maxSemEdgeDist"), Integer.class);
			if (maxSemEdgeDist != null)
				setMaxSemEdgeDist(Integer.parseInt((String) maxSemEdgeDist));
		} catch (JsonProcessingException e) {
			System.err.println("No maxSemEdgeDist given. (Using default.)");
		} catch (NumberFormatException e) {
			System.err.println("Could not read the maxSemEdgeDist. (Using default.)");
		}
		try {
			Object persistenceThreshold = mapper.treeToValue(rootNode.path("persistenceThreshold"), Double.class);
			if (persistenceThreshold != null)
				setBeliefThreshold((Double) persistenceThreshold);
		} catch (JsonProcessingException e) {
			System.err.println("No persistenceThreshold given. (Using default.)");
		}

	}

	public void export(ObjectMapper mapper, String path) {
		try {
			export(mapper, new FileOutputStream(path));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	public void export(ObjectMapper mapper, OutputStream out) {
		try {
			mapper.writerWithDefaultPrettyPrinter().writeValue(out, toJson(mapper));
		} catch (NullPointerException | IOException e) {
			System.err.println("Could not save the EtymologyProblemConfig.");
			e.printStackTrace();
		}
	}

	@Override
	public ObjectNode toJson(ObjectMapper mapper) {
		ObjectNode rootNode = null;
		try {
			rootNode = mapper.createObjectNode();
			rootNode.set(PslProblemConfig.NAME_FIELD, mapper.readTree(mapper.writeValueAsString(super.getName())));
			rootNode.set(PslProblemConfig.USER_PRIOR_FIELD,
					mapper.readTree(mapper.writeValueAsString(super.isDeclareUserPrior())));
			rootNode.set("formIds", (ArrayNode) mapper.readTree(mapper.writeValueAsString(formIds)));
			rootNode.set("ruleWeights", (ObjectNode) mapper.readTree(mapper.writeValueAsString(ruleWeights)));
			rootNode.set("ignoreRules", (ArrayNode) mapper.readTree(mapper.writeValueAsString(ignoreRules)));
			rootNode.set("persistenceThreshold",
					(DoubleNode) mapper.readTree(mapper.writeValueAsString(persistenceThreshold)));
			rootNode.set("maxSemEdgeDist", mapper.readTree(mapper.writeValueAsString(maxSemEdgeDist)));
		} catch (IOException e) {
			e.printStackTrace();
		}
		return rootNode;
	}

}
