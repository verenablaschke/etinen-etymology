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
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import de.tuebingen.sfs.eie.shared.util.SemanticNetwork;
import de.tuebingen.sfs.psl.engine.DatabaseManager;
import de.tuebingen.sfs.psl.engine.PslProblemConfig;
import de.tuebingen.sfs.psl.util.log.InferenceLogger;

public class EtymologyConfig extends PslProblemConfig {

	public static final String DB_DIR = "src/test/resources/northeuralex-0.9";
	public static final String TEST_DB_DIR = "etinen-etymology/src/test/resources/testdb";
	public static final String NETWORK_EDGES_FILE = "src/test/resources/etymology/clics2-network-edges.txt";
	public static final String NETWORK_IDS_FILE = "src/test/resources/etymology/clics2-network-ids.txt";
	public static final String NELEX_CONCEPTS_FILE = "src/test/resources/northeuralex-0.9/parameters.csv";
	public static final int MAX_DIST = 2;

	private static final double DEFAULT_THRESHOLD = 0.05;
	private static final int DEFAULT_TREE_DEPTH = 4;
	private static final String DEFAULT_TREE_FILE = DB_DIR + "/tree.nwk";
	private static final String DEFAULT_LOGFILE_PATH = "src/test/resources/etym-inf-log.txt";

	private Integer treeDepth = null;
	private Boolean branchwiseBorrowing = null;
	private Boolean addSiblingLanguages = null;
	private String treeFile = null;
	private SemanticNetwork semanticNet = null;
	private String wordListDbDir = null;
	private String correspondenceDbDir = null;
	private List<String> concepts = null;
	private List<String> modernLanguages = null;

	private Map<String, Double> ruleWeights;
	private Set<String> ignoreRules;
	private double persistenceThreshold;
	private InferenceLogger logger;

	// --------------
	// Constructors
	// --------------

	public EtymologyConfig() {
		resetToDefaults();
	}

	public EtymologyConfig(String problemId, DatabaseManager dbManager) {
		this();
		setNonPersistableFeatures(problemId, dbManager);
	}

	public EtymologyConfig(List<String> concepts, List<String> modernLanguages, String treeFile,
			SemanticNetwork semanticNet, String wordListDbDir, int treeDepth, Boolean branchwiseBorrowing,
			Boolean addSiblingLanguages, String correspondenceDbDir, Map<String, Double> ruleWeights,
			Set<String> ignoreRules, Double persistenceThreshold, InferenceLogger logger) {
		resetToDefaults();
		if (logger != null) {
			this.logger = logger;
		}
		logger.displayln("Creating EtymologyIdeaGeneratorConfig.");
		if (concepts == null) {
			logger.displayln("...No concepts specified.");
		} else {
			this.concepts = concepts;
		}
		if (semanticNet == null) {
			logger.displayln("...No semantic net given, using default network.");
		} else {
			this.setSemanticNet(semanticNet);
		}
		if (wordListDbDir == null) {
			logger.displayln("...No CLDF Wordlist Database given, using default.");
		} else {
			this.wordListDbDir = wordListDbDir;
		}
		if (correspondenceDbDir == null || correspondenceDbDir.isEmpty()) {
			logger.displayln("...No Correspondence Database directory given, using default.");
		} else {
			this.correspondenceDbDir = correspondenceDbDir;
		}
		this.branchwiseBorrowing = branchwiseBorrowing;
		if (branchwiseBorrowing == null) {
			logger.displayln("...No value for branchwiseBorrowing given, using default (true).");
			this.branchwiseBorrowing = true;
		}
		this.addSiblingLanguages = addSiblingLanguages;
		if (addSiblingLanguages == null) {
			logger.displayln("...No value for addSiblingLanguages given, using default (true).");
			this.addSiblingLanguages = true;
		}
		if (treeDepth < 1) {
			System.err.println(
					"...No phylogenetic tree depth specified, using default value (" + DEFAULT_TREE_DEPTH + ").");
		} else {
			this.treeDepth = treeDepth;
		}
		if (treeFile == null || treeFile.trim().isEmpty()) {
			logger.displayln("...No input file for the tree specified, using default: " + DEFAULT_TREE_FILE);
		} else {
			this.treeFile = treeFile;
		}
		if (modernLanguages == null) {
			logger.displayln(
					"...No modernLanguages specified. Will only construct the phylogenetic tree once the modernLanguages are set via setLanguages().");
		} else {
			this.setModernLanguages(modernLanguages);
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
		if (persistenceThreshold == null) {
			logger.displayln(
					"...No confidence threshold for persistence specified, using default: " + DEFAULT_THRESHOLD);
		} else {
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

	public static EtymologyConfig fromJson(ObjectMapper mapper, JsonNode configJsonRoot) {
		EtymologyConfig config = new EtymologyConfig();
		config.setFromJson(mapper, configJsonRoot);
		return config;
	}

	public EtymologyConfig copy() {
		EtymologyConfig copy = new EtymologyConfig();
		super.copyFields(copy);

		copy.concepts = new ArrayList<>(concepts);
		copy.modernLanguages = new ArrayList<>(modernLanguages);

		copy.setSemanticNet(new SemanticNetwork(semanticNet.getNetworkEdgesFile(), semanticNet.getNetworkIdsFile(),
				semanticNet.getNelexConceptsFile(), semanticNet.getMaxDist()));

		copy.wordListDbDir = wordListDbDir;
		copy.correspondenceDbDir = correspondenceDbDir;

		copy.treeDepth = treeDepth;
		copy.treeFile = treeFile;

		copy.ruleWeights = new HashMap<>(ruleWeights);
		copy.ignoreRules = new HashSet<>(ignoreRules);

		copy.branchwiseBorrowing = branchwiseBorrowing;
		copy.addSiblingLanguages = addSiblingLanguages;

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

		concepts = new ArrayList<>();
		setModernLanguages(new ArrayList<>());

		setSemanticNet(new SemanticNetwork(NETWORK_EDGES_FILE, NETWORK_IDS_FILE, NELEX_CONCEPTS_FILE, MAX_DIST));

		wordListDbDir = DB_DIR;
		correspondenceDbDir = DB_DIR;

		treeDepth = DEFAULT_TREE_DEPTH;
		treeFile = DEFAULT_TREE_FILE;

		ruleWeights = new TreeMap<>();
		ignoreRules = new TreeSet<>();

		branchwiseBorrowing = true;
		addSiblingLanguages = true;

		logger = new InferenceLogger();
		setLogfile(DEFAULT_LOGFILE_PATH);
		persistenceThreshold = DEFAULT_THRESHOLD;
	}

	public int getTreeDepth() {
		return treeDepth;
	}

	public void setTreeDepth(int depth) {
		treeDepth = depth;
	}

	public String getTreeFile() {
		return treeFile;
	}

	public void setTreeFile(String treeFile) {
		this.treeFile = treeFile;
	}

	public boolean branchwiseBorrowing() {
		return branchwiseBorrowing;
	}

	public boolean addSiblingLanguages() {
		return addSiblingLanguages;
	}

	public String[] getSemanticNetworkConfig() {
		return new String[] { getSemanticNet().getNetworkEdgesFile(), getSemanticNet().getNetworkIdsFile(),
				getSemanticNet().getNelexConceptsFile(), getSemanticNet().getMaxDist() + "" };
	}

	public String getWordListDbDir() {
		return wordListDbDir;
	}

	public void setWordListDbDir(String wordListDbDir) {
		this.wordListDbDir = wordListDbDir;
	}

	public String getCorrespondenceDbDir() {
		return correspondenceDbDir;
	}

	public void setCorrespondenceDbDir(String correspondenceDbDir) {
		this.correspondenceDbDir = correspondenceDbDir;
	}

	public List<String> getConcepts() {
		return concepts;
	}

	public void setConcepts(List<String> concepts) {
		this.concepts = concepts;
	}

	public List<String> getLanguages() {
		return getModernLanguages();
	}

	public void setLanguages(List<String> modernLanguages) {
		this.setModernLanguages(modernLanguages);
	}

	public void setBranchwiseBorrowing(boolean branchwiseBorrowing) {
		this.branchwiseBorrowing = branchwiseBorrowing;
	}

	public void setAddSiblingLanguages(boolean addSiblingLanguages) {
		this.addSiblingLanguages = addSiblingLanguages;
	}

	public void setSemanticNetworkConfig(String networkEdgesFile, String networkIdsFile, String nelexConceptsFile,
			int maxDist) {
		this.setSemanticNet(new SemanticNetwork(networkEdgesFile, networkIdsFile, nelexConceptsFile, maxDist));
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

	public List<String> getModernLanguages() {
		return modernLanguages;
	}

	public void setModernLanguages(List<String> modernLanguages) {
		this.modernLanguages = modernLanguages;
	}

	public SemanticNetwork getSemanticNet() {
		return semanticNet;
	}

	public void setSemanticNet(SemanticNetwork semanticNet) {
		this.semanticNet = semanticNet;
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
		out.println("- branchwiseBorrowing: " + branchwiseBorrowing);
		out.println("- treeDepth: " + treeDepth);
		out.println("- treeFile: " + treeFile);
		out.println("- wordListDbDir: " + wordListDbDir);
		out.println("- correspondenceDbDir: " + correspondenceDbDir);
		out.println("- concepts: " + concepts);
		out.println("- modernLanguages: " + getModernLanguages());
		out.println("- networkEdgesFile: " + getSemanticNet().getNetworkEdgesFile());
		out.println("- networkIdsFile: " + getSemanticNet().getNetworkIdsFile());
		out.println("- nelexConceptsFile: " + getSemanticNet().getNelexConceptsFile());
		out.println("- maxDist: " + getSemanticNet().getMaxDist());
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
		logger.displayln("- branchwiseBorrowing: " + branchwiseBorrowing);
		logger.displayln("- treeDepth: " + treeDepth);
		logger.displayln("- treeFile: " + treeFile);
		logger.displayln("- wordListDbDir: " + wordListDbDir);
		logger.displayln("- correspondenceDbDir: " + correspondenceDbDir);
		logger.displayln("- concepts (in config!): " + concepts);
		logger.displayln("- modernLanguages (in config!): " + getModernLanguages());
		logger.displayln("- networkEdgesFile: " + getSemanticNet().getNetworkEdgesFile());
		logger.displayln("- networkIdsFile: " + getSemanticNet().getNetworkIdsFile());
		logger.displayln("- nelexConceptsFile: " + getSemanticNet().getNelexConceptsFile());
		logger.displayln("- maxDist: " + getSemanticNet().getMaxDist());
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
		super.setFromJson(mapper, rootNode);

		try {
			Object concepts = mapper.treeToValue(rootNode.path("concepts"), ArrayList.class);
			if (concepts != null)
				setConcepts((List<String>) concepts);
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
		try {
			Object modernLanguages = mapper.treeToValue(rootNode.path("modernLanguages"), ArrayList.class);
			if (modernLanguages != null)
				setModernLanguages((List<String>) modernLanguages);
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
		try {
			Object treeDepth = mapper.treeToValue(rootNode.path("treeDepth"), Integer.class);
			if (treeDepth != null)
				setTreeDepth((Integer) treeDepth);
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
		try {
			Object treeFile = mapper.treeToValue(rootNode.path("treeDepth"), String.class);
			if (treeFile != null)
				setTreeFile((String) treeFile);
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
		try {
			Object branchwiseBorrowing = mapper.treeToValue(rootNode.path("branchwiseBorrowing"), Boolean.class);
			if (branchwiseBorrowing != null)
				setBranchwiseBorrowing((Boolean) branchwiseBorrowing);
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
		try {
			Object addSiblingLanguages = mapper.treeToValue(rootNode.path("addSiblingLanguages"), Boolean.class);
			if (addSiblingLanguages != null)
				setAddSiblingLanguages((Boolean) addSiblingLanguages);
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
		try {
			HashMap<String, Object> semanticNetConfig = (HashMap<String, Object>) mapper
					.treeToValue(rootNode.path("semanticNet"), HashMap.class);
			if (semanticNetConfig != null) {
				setSemanticNetworkConfig((String) semanticNetConfig.get("edges"), (String) semanticNetConfig.get("ids"),
						(String) semanticNetConfig.get("nelexConcepts"), (Integer) semanticNetConfig.get("maxDist"));
			}
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
		try {
			Object wordListDbDir = mapper.treeToValue(rootNode.path("wordListDbDir"), String.class);
			if (wordListDbDir != null)
				setWordListDbDir((String) wordListDbDir);
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
		try {
			Object correspondenceDbDir = mapper.treeToValue(rootNode.path("correspondenceDbDir"), String.class);
			if (correspondenceDbDir != null)
				setCorrespondenceDbDir((String) correspondenceDbDir);
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
		try {
			Object ruleWeights = mapper.treeToValue(rootNode.path("ruleWeights"), HashMap.class);
			if (ruleWeights != null) {
				setRuleWeights((HashMap<String, Double>) ruleWeights);
			}
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
		try {
			Object ignoreRules = mapper.treeToValue(rootNode.path("ignoreRules"), HashSet.class);
			if (ignoreRules != null) {
				setIgnoreRules((HashSet<String>) ignoreRules);
			}
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
		try {
			Object persistenceThreshold = mapper.treeToValue(rootNode.path("persistenceThreshold"), Double.class);
			if (persistenceThreshold != null)
				setBeliefThreshold((Double) persistenceThreshold);
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}

	}

	public static EtymologyConfig fromJson(ObjectMapper mapper, InputStream in, InferenceLogger logger) {
		EtymologyConfig config = new EtymologyConfig();
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

	public void export(ObjectMapper mapper, String path) {
		try {
			export(mapper, new FileOutputStream(path));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	public void export(ObjectMapper mapper, OutputStream out) {
		try {
			ObjectNode rootNode = mapper.createObjectNode();
			rootNode.set("concepts", (ArrayNode) mapper.readTree(mapper.writeValueAsString(concepts)));
			rootNode.set("modernLanguages",
					(ArrayNode) mapper.readTree(mapper.writeValueAsString(getModernLanguages())));
			rootNode.set("treeDepth", new IntNode(treeDepth));
			rootNode.set("branchwiseBorrowing",
					(BooleanNode) mapper.readTree(mapper.writeValueAsString(branchwiseBorrowing)));
			rootNode.set("treeFile", new TextNode(treeFile));
			rootNode.set("wordListDbDir", new TextNode(wordListDbDir));
			rootNode.set("correspondenceDbDir", new TextNode(correspondenceDbDir));

			Map<String, Object> semanticNetConfig = new HashMap<>();
			semanticNetConfig.put("edges", getSemanticNet().getNetworkEdgesFile());
			semanticNetConfig.put("ids", getSemanticNet().getNetworkIdsFile());
			semanticNetConfig.put("nelexConcepts", getSemanticNet().getNelexConceptsFile());
			semanticNetConfig.put("maxDist", getSemanticNet().getMaxDist());
			rootNode.set("semanticNet", (ObjectNode) mapper.readTree(mapper.writeValueAsString(semanticNetConfig)));
			rootNode.set("ruleWeights", (ObjectNode) mapper.readTree(mapper.writeValueAsString(ruleWeights)));
			rootNode.set("ignoreRules", (ArrayNode) mapper.readTree(mapper.writeValueAsString(ignoreRules)));
			rootNode.set("persistenceThreshold",
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
