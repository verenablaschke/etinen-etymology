package de.tuebingen.sfs.eie.components.etymology.problems;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.tuebingen.sfs.psl.engine.PslProblemConfig;
import de.tuebingen.sfs.psl.util.log.InferenceLogger;

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

public class EtymologyConfig extends PslProblemConfig {

    private static final double DEFAULT_THRESHOLD = 0.05;
    private Map<String, Double> ruleWeights;
    private Set<String> ignoreRules;
    private double persistenceThreshold;
    private String logfilePath;
    private InferenceLogger logger;

    public EtymologyConfig(InferenceLogger logger) {
        this(null, null, null, logger);
    }

    public EtymologyConfig(Map<String, Double> ruleWeights, InferenceLogger logger) {
        this(ruleWeights, null, null, logger);
    }

    public EtymologyConfig(Map<String, Double> ruleWeights, Set<String> ignoreRules, Double persistenceThreshold,
                           InferenceLogger logger) {
        this.logger = logger;
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

    public static EtymologyConfig fromJson(ObjectMapper mapper, String path, InferenceLogger logger) {
        // if (! path.startsWith("/"))
        // path = "/" + path;
        // return fromJson(mapper, EtymologyConfig.class.getClass().getResourceAsStream(path));
        try {
            return fromJson(mapper, new FileInputStream(path), logger);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public static EtymologyConfig fromJson(ObjectMapper mapper, InputStream in, InferenceLogger logger) {
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
        return new EtymologyConfig(ruleWeights, ignoreRules, persistenceThreshold, logger);
    }

    private void defaultValues() {
        this.ruleWeights = new TreeMap<>();
        this.ignoreRules = new TreeSet<>();
        setLogfile("src/test/resources/etym-inf-log.txt");
        persistenceThreshold = DEFAULT_THRESHOLD;
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

    public double getBeliefThreshold() {
        return persistenceThreshold;
    }

    public void setBeliefThreshold(Double threshold) {
        if (threshold != null)
            this.persistenceThreshold = threshold;
    }

    public void print(PrintStream out) {
        out.println("Etymology config");
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
        if (ignoreRules == null || ignoreRules.isEmpty()) {
            logger.displayln("No rules to ignore.");
        } else {
            logger.displayln("Ignoring:");
            for (String rule : ignoreRules) {
                logger.displayln("- " + rule);
            }
        }
        if (ruleWeights == null || ruleWeights.isEmpty()) {
            logger.displayln("No rule weights changed.");
        } else {
            logger.displayln("Updated rule weights:");
            for (Entry<String, Double> entry : ruleWeights.entrySet()) {
                logger.displayln("- " + entry.getKey() + " : " + entry.getValue());
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
            rootNode.set("ruleWeights", mapper.readTree(mapper.writeValueAsString(ruleWeights)));
            rootNode.set("ignoreRules", mapper.readTree(mapper.writeValueAsString(ignoreRules)));
            rootNode.set("persistenceThreshold", mapper.readTree(mapper.writeValueAsString(persistenceThreshold)));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return rootNode;
    }
}
