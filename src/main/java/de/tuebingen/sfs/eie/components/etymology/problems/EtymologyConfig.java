package de.tuebingen.sfs.eie.components.etymology.problems;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class EtymologyConfig {

	Map<String, Double> ruleWeights;
	Set<String> ignoreRules;

	public EtymologyConfig() {
		this(null, null);
	}

	public EtymologyConfig(Map<String, Double> ruleWeights) {
		this(ruleWeights, null);
	}

	public EtymologyConfig(Map<String, Double> ruleWeights, Set<String> ignoreRules) {
		if (ruleWeights == null) {
			this.ruleWeights = new TreeMap<>();
		} else {
			this.ruleWeights = ruleWeights;
		}
		if (ignoreRules == null) {
			this.ignoreRules = new TreeSet<>();
		} else {
			this.ignoreRules = ignoreRules;
		}
	}

	@SuppressWarnings("unchecked")
	public static EtymologyConfig fromJson(ObjectMapper mapper, File path) {
		Map<String, Double> ruleWeights = null;
		Set<String> ignoreRules = null;
		try {
			JsonNode rootNode = mapper.readTree(path);
			ruleWeights = mapper.treeToValue(rootNode.path("ruleWeights"), TreeMap.class);
			ignoreRules = mapper.treeToValue(rootNode.path("ignoreRules"), TreeSet.class);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return new EtymologyConfig(ruleWeights, ignoreRules);
	}

	public void addRuleToIgnoreList(String rule) {
		ignoreRules.add(rule);
	}

	public boolean include(String rule) {
		return !ignoreRules.contains(rule);
	}

	public void addRuleWeight(String rule, double weight) {
		ruleWeights.put(rule, weight);
	}

	public double getRuleWeightOrDefault(String rule, double defaultWeight) {
		return ruleWeights.getOrDefault(rule, defaultWeight);
	}

	public void print(PrintStream out) {
		out.println("Etymology config");
		if (ignoreRules == null || ignoreRules.isEmpty()) {
			out.println("No rules to ignore.");
		} else {
			out.println("Ignoring:");
			for (String rule : ignoreRules) {
				out.println("- " + rule);
			}
		}
		if (ruleWeights == null || ruleWeights.isEmpty()) {
			out.println("No rule weights changed.");
		} else {
			out.println("Updated rule weights:");
			for (Entry<String, Double> entry : ruleWeights.entrySet()) {
				out.println("- " + entry.getKey() + " : " + entry.getValue());
			}
		}

	}

	public void export(ObjectMapper mapper, File path) {
		try {
			JsonNode rootNode = mapper.createObjectNode();
			((ObjectNode) rootNode).set("ruleWeights",
					(ObjectNode) mapper.readTree(mapper.writeValueAsString(ruleWeights)));
			((ObjectNode) rootNode).set("ignoreRules",
					(ArrayNode) mapper.readTree(mapper.writeValueAsString(ignoreRules)));
			mapper.writerWithDefaultPrettyPrinter().writeValue(path, rootNode);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
