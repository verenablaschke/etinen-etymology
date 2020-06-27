package de.tuebingen.sfs.eie.components.etymology.problems;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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

	public static EtymologyConfig fromJson(ObjectMapper mapper, String path) {
//		if (! path.startsWith("/"))
//			path = "/" + path;
//		return fromJson(mapper, EtymologyConfig.class.getClass().getResourceAsStream(path));
		try {
			return fromJson(mapper, new FileInputStream(path));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	public static EtymologyConfig fromJson(ObjectMapper mapper, InputStream in) {
		Map<String, Double> ruleWeights = null;
		Set<String> ignoreRules = null;
		try {
			JsonNode rootNode = mapper.readTree(in);
			ruleWeights = mapper.treeToValue(rootNode.path("ruleWeights"), TreeMap.class);
			ignoreRules = mapper.treeToValue(rootNode.path("ignoreRules"), TreeSet.class);
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			in.close();
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

	public void export(ObjectMapper mapper, String path) {
		try {
			export(mapper, new FileOutputStream(path));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	public void export(ObjectMapper mapper, OutputStream out) {
		try {
			JsonNode rootNode = mapper.createObjectNode();
			((ObjectNode) rootNode).set("ruleWeights",
					(ObjectNode) mapper.readTree(mapper.writeValueAsString(ruleWeights)));
			((ObjectNode) rootNode).set("ignoreRules",
					(ArrayNode) mapper.readTree(mapper.writeValueAsString(ignoreRules)));
			mapper.writerWithDefaultPrettyPrinter().writeValue(out, rootNode);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
