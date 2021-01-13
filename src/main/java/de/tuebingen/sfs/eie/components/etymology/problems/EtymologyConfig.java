package de.tuebingen.sfs.eie.components.etymology.problems;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import de.tuebingen.sfs.psl.engine.PslProblemConfig;
import de.tuebingen.sfs.psl.util.log.InferenceLogger;

public class EtymologyConfig extends PslProblemConfig {

	// TODO (vbl) make sure logger is used

	private Map<String, Double> ruleWeights;
	private Set<String> ignoreRules;
	private double persistenceThreshold;

	private static final double DEFAULT_THRESHOLD = 0.05;

	private String logfilePath;
	private InferenceLogger logger;

	public EtymologyConfig() {
		this(null, null, null);
	}

	public EtymologyConfig(Map<String, Double> ruleWeights) {
		this(ruleWeights, null, null);
	}

	public EtymologyConfig(Map<String, Double> ruleWeights, Set<String> ignoreRules, Double persistenceThreshold) {
		defaultValues();
		if (ruleWeights != null) {
			this.ruleWeights = ruleWeights;
		}
		if (ignoreRules != null) {
			this.ignoreRules = ignoreRules;
		}
		if (persistenceThreshold != null) {
			this.persistenceThreshold = persistenceThreshold;
		}
	}

	private void defaultValues() {
		this.ruleWeights = new TreeMap<>();
		this.ignoreRules = new TreeSet<>();
		logger = new InferenceLogger();
		setLogfile("src/test/resources/etym-inf-log.txt");
		persistenceThreshold = DEFAULT_THRESHOLD;
	}

	public static EtymologyConfig fromJson(ObjectMapper mapper, String path) {
		// if (! path.startsWith("/"))
		// path = "/" + path;
		// return fromJson(mapper, EtymologyConfig.class.getClass().getResourceAsStream(path));
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
		double persistenceThreshold = -1;
		try {
			JsonNode rootNode = mapper.readTree(in);
			try {
				ruleWeights = mapper.treeToValue(rootNode.path("ruleWeights"), TreeMap.class);
			} catch (JsonProcessingException e) {
				e.printStackTrace();
			}
			try {
				ignoreRules = mapper.treeToValue(rootNode.path("ignoreRules"), TreeSet.class);
			} catch (JsonProcessingException e) {
				e.printStackTrace();
			}
			try {
				persistenceThreshold = mapper.treeToValue(rootNode.path("persistenceThreshold"), Double.class);
			} catch (JsonProcessingException e) {
				e.printStackTrace();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			in.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return new EtymologyConfig(ruleWeights, ignoreRules, persistenceThreshold);
	}

	@Override
	@SuppressWarnings("unchecked")
	public void setFromJson(ObjectMapper mapper, JsonNode rootNode) {
		super.setFromJson(mapper, rootNode);
		try {
			ruleWeights = mapper.treeToValue(rootNode.path("ruleWeights"), TreeMap.class);
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
		try {
			ignoreRules = mapper.treeToValue(rootNode.path("ignoreRules"), TreeSet.class);
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
		try {
			persistenceThreshold = mapper.treeToValue(rootNode.path("persistenceThreshold"), Double.class);
		} catch (JsonProcessingException e) {
			e.printStackTrace();
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

	public boolean hasRuleWeight(String rule) {
		return ruleWeights.containsKey(rule);
	}

	public double getRuleWeight(String rule) {
		return ruleWeights.get(rule);
	}

	public void setBeliefThreshold(Double threshold) {
		if (threshold != null)
			this.persistenceThreshold = threshold;
	}

	public double getBeliefThreshold() {
		return persistenceThreshold;
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
			((ObjectNode) rootNode).set("persistenceThreshold",
					(ArrayNode) mapper.readTree(mapper.writeValueAsString(persistenceThreshold)));
			mapper.writerWithDefaultPrettyPrinter().writeValue(out, rootNode);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public ObjectNode toJson(ObjectMapper mapper) {
		ObjectNode rootNode = super.toJson(mapper);
		try {
			rootNode.set("ruleWeights",
					mapper.readTree(mapper.writeValueAsString(ruleWeights)));
			rootNode.set("ignoreRules",
					mapper.readTree(mapper.writeValueAsString(ignoreRules)));
			rootNode.set("persistenceThreshold",
					mapper.readTree(mapper.writeValueAsString(persistenceThreshold)));
		} catch (IOException e) {
			e.printStackTrace();
		}
		return rootNode;
	}

	public void setGuiMessager(Consumer<String> messager) {
		logger.setGuiStream(messager);
	}

	public Consumer<String> getGuiMessager() {
		return logger.getGuiStream();
	}

	public boolean setLogfile(String logfilePath) {
		this.logfilePath = logfilePath;
		if (logfilePath.isEmpty())
			logger.setLogStream(System.err);
		else {
			try {
				PrintStream logStream = new PrintStream(logfilePath, "UTF-8");
				logger.setLogStream(logStream);
			} catch (FileNotFoundException | UnsupportedEncodingException e) {
				e.printStackTrace();
				return false;
			}
		}
		return true;
	}

	public String getLogfilePath() {
		return logfilePath;
	}

	public InferenceLogger getLogger() {
		return logger;
	}

}
