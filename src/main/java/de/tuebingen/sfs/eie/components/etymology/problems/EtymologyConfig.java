package de.tuebingen.sfs.eie.components.etymology.problems;

import java.util.Map;
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
		if (ruleWeights == null) {
			this.ignoreRules = new TreeSet<>();
		} else {
			this.ignoreRules = ignoreRules;
		}
	}

	public void addRuleToIgnoreList(String rule) {
		ignoreRules.add(rule);
	}

	public void addRuleWeight(String rule, double weight) {
		ruleWeights.put(rule, weight);
	}

	public double getRuleWeightOrDefault(String rule, double defaultWeight) {
		return ruleWeights.getOrDefault(rule, defaultWeight);
	}

}
