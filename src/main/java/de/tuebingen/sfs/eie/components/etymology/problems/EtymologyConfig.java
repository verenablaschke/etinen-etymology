package de.tuebingen.sfs.eie.components.etymology.problems;

import java.io.PrintStream;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

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

}
