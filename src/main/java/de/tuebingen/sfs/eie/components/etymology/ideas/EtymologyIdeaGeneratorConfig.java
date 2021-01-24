package de.tuebingen.sfs.eie.components.etymology.ideas;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import de.tuebingen.sfs.psl.util.log.InferenceLogger;
import de.tuebingen.sfs.util.SemanticNetwork;

public class EtymologyIdeaGeneratorConfig {

	static final String DB_DIR = "src/test/resources/northeuralex-0.9";
	static final String TEST_DB_DIR = "etinen-etymology/src/test/resources/testdb";
	static final String NETWORK_EDGES_FILE = "src/test/resources/etymology/clics2-network-edges.txt";
	static final String NETWORK_IDS_FILE = "src/test/resources/etymology/clics2-network-ids.txt";
	static final String NELEX_CONCEPTS_FILE = "src/test/resources/northeuralex-0.9/parameters.csv";
	static final int MAX_DIST = 2;

	Integer treeDepth = null;
	Boolean branchwiseBorrowing = null;
	Boolean addSiblingLanguages = null;
	String treeFile = null;
	SemanticNetwork semanticNet = null;
	String wordListDbDir = null;
	String correspondenceDbDir = null;
	List<String> concepts = null;
	List<String> modernLanguages = null;
	InferenceLogger logger = null;

	public EtymologyIdeaGeneratorConfig(List<String> concepts, List<String> modernLanguages, String treeFile,
			SemanticNetwork semanticNet, String wordListDbDir, int treeDepth, Boolean branchwiseBorrowing,
			Boolean addSiblingLanguages, String correspondenceDbDir, InferenceLogger logger) {
		this.logger = logger;
		logger.displayln("Creating EtymologyIdeaGeneratorConfig.");
		if (concepts == null) {
			logger.displayln("...No concepts specified.");
			this.concepts = new ArrayList<>();
		} else {
			this.concepts = concepts;
		}
		if (semanticNet == null) {
			logger.displayln("...No semantic net given, using default network.");
			this.semanticNet = new SemanticNetwork(NETWORK_EDGES_FILE, NETWORK_IDS_FILE, NELEX_CONCEPTS_FILE, MAX_DIST);
		} else {
			this.semanticNet = semanticNet;
		}
		if (wordListDbDir == null) {
			logger.displayln("...No CLDF Wordlist Database given, using default.");
			this.wordListDbDir = DB_DIR;
		} else {
			this.wordListDbDir = wordListDbDir;
		}
		if (correspondenceDbDir == null || correspondenceDbDir.isEmpty()) {
			logger.displayln("...No Correspondence Database directory given, using default.");
			this.correspondenceDbDir = DB_DIR;
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
			this.treeDepth = 4;
			System.err
					.println("...No phylogenetic tree depth specified, using default value (" + this.treeDepth + ").");
		} else {
			this.treeDepth = treeDepth;
		}
		if (treeFile == null || treeFile.trim().isEmpty()) {
			this.treeFile = DB_DIR + "/tree.nwk";
			logger.displayln("...No input file for the tree specified, using default: " + this.treeFile);
		} else {
			this.treeFile = treeFile;
		}
		if (modernLanguages == null) {
			logger.displayln(
					"...No modernLanguages specified. Will only construct the phylogenetic tree once the modernLanguages are set via setLanguages().");
			this.modernLanguages = new ArrayList<>();
		} else {
			this.modernLanguages = modernLanguages;
		}
	}

	public EtymologyIdeaGeneratorConfig(InferenceLogger logger) {
		this(null, null, null, null, null, -1, null, null, null, logger);
	}

	@SuppressWarnings("unchecked")
	public void initializeFromJson(ObjectMapper mapper, InputStream in, InferenceLogger logger) {
		this.logger = logger;
		try {
			JsonNode rootNode = mapper.readTree(in);
			concepts = mapper.treeToValue(rootNode.path("concepts"), ArrayList.class);
			modernLanguages = mapper.treeToValue(rootNode.path("modernLanguages"), ArrayList.class);
			treeDepth = mapper.treeToValue(rootNode.path("treeDepth"), Integer.class);
			branchwiseBorrowing = mapper.treeToValue(rootNode.path("branchwiseBorrowing"), Boolean.class);
			treeFile = mapper.treeToValue(rootNode.path("treeFile"), String.class);
			Map<String, Object> semanticNetConfig = mapper.treeToValue(rootNode.path("semanticNet"), HashMap.class);
			semanticNet = new SemanticNetwork((String) semanticNetConfig.get("edges"),
					(String) semanticNetConfig.get("ids"), (String) semanticNetConfig.get("nelexConcepts"),
					(Integer) semanticNetConfig.get("maxDist"));
			wordListDbDir = mapper.treeToValue(rootNode.path("wordListDbDir"), String.class);
			correspondenceDbDir = mapper.treeToValue(rootNode.path("correspondenceDbDir"), String.class);
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			in.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void export(ObjectMapper mapper, OutputStream out) {
		try {
			ObjectNode rootNode = mapper.createObjectNode();
			rootNode.set("concepts", (ArrayNode) mapper.readTree(mapper.writeValueAsString(concepts)));
			rootNode.set("modernLanguages", (ArrayNode) mapper.readTree(mapper.writeValueAsString(modernLanguages)));
			rootNode.set("treeDepth", new IntNode(treeDepth));
			rootNode.set("branchwiseBorrowing",
					(BooleanNode) mapper.readTree(mapper.writeValueAsString(branchwiseBorrowing)));
			rootNode.set("treeFile", new TextNode(treeFile));
			rootNode.set("wordListDbDir", new TextNode(wordListDbDir));
			rootNode.set("correspondenceDbDir", new TextNode(correspondenceDbDir));

			Map<String, Object> semanticNetConfig = new HashMap<>();
			semanticNetConfig.put("edges", semanticNet.getNetworkEdgesFile());
			semanticNetConfig.put("ids", semanticNet.getNetworkIdsFile());
			semanticNetConfig.put("nelexConcepts", semanticNet.getNelexConceptsFile());
			semanticNetConfig.put("maxDist", semanticNet.getMaxDist());
			rootNode.set("semanticNet", (ObjectNode) mapper.readTree(mapper.writeValueAsString(semanticNetConfig)));

			mapper.writerWithDefaultPrettyPrinter().writeValue(out, rootNode);
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public int getTreeDepth() {
		return treeDepth;
	}

	public String getTreeFile() {
		return treeFile;
	}

	public boolean branchwiseBorrowing() {
		return branchwiseBorrowing;
	}

	public boolean addSiblingLanguages() {
		return addSiblingLanguages;
	}

	public String[] getSemanticNetworkConfig() {
		return new String[] { semanticNet.getNetworkEdgesFile(), semanticNet.getNetworkIdsFile(),
				semanticNet.getNelexConceptsFile(), semanticNet.getMaxDist() + "" };
	}

	public String getWordListDbDir() {
		return wordListDbDir;
	}

	public String getCorrespondenceDbDir() {
		return correspondenceDbDir;
	}

	public List<String> getConcepts() {
		return concepts;
	}

	public List<String> getLanguages() {
		return modernLanguages;
	}

	public void setTreeDepth(int depth) {
		treeDepth = depth;
	}

	public void setTreeFile(String treeFile) {
		this.treeFile = treeFile;
	}

	public void setBranchwiseBorrowing(boolean branchwiseBorrowing) {
		this.branchwiseBorrowing = branchwiseBorrowing;
	}

	public void setAddSiblingLanguages(boolean addSiblingLanguages) {
		this.addSiblingLanguages = addSiblingLanguages;
	}

	public void setSemanticNetworkConfig(String networkEdgesFile, String networkIdsFile, String nelexConceptsFile,
			int maxDist) {
		this.semanticNet = new SemanticNetwork(networkEdgesFile, networkIdsFile, nelexConceptsFile, maxDist);
	}

	public void setWordListDbDir(String wordListDbDir) {
		this.wordListDbDir = wordListDbDir;
	}

	public void setCorrespondenceDbDir(String correspondenceDbDir) {
		this.correspondenceDbDir = correspondenceDbDir;
	}

	public void setConcepts(List<String> concepts) {
		this.concepts = concepts;
	}

	public void setLanguages(List<String> modernLanguages) {
		this.modernLanguages = modernLanguages;
	}
	
	public void printConfig() {
		System.out.println("- branchwiseBorrowing: " + branchwiseBorrowing);
		System.out.println("- treeDepth: " + treeDepth);
		System.out.println("- treeFile: " + treeFile);
		System.out.println("- wordListDbDir: " + wordListDbDir);
		System.out.println("- correspondenceDbDir: " + correspondenceDbDir);
		System.out.println("- concepts (in config!): " + concepts);
		System.out.println("- modernLanguages (in config!): " + modernLanguages);
		System.out.println("- networkEdgesFile: " + semanticNet.getNetworkEdgesFile());
		System.out.println("- networkIdsFile: " + semanticNet.getNetworkIdsFile());
		System.out.println("- nelexConceptsFile: " + semanticNet.getNelexConceptsFile());
		System.out.println("- maxDist: " + semanticNet.getMaxDist());
	}

	public void logConfig() {
		logger.displayln("- branchwiseBorrowing: " + branchwiseBorrowing);
		logger.displayln("- treeDepth: " + treeDepth);
		logger.displayln("- treeFile: " + treeFile);
		logger.displayln("- wordListDbDir: " + wordListDbDir);
		logger.displayln("- correspondenceDbDir: " + correspondenceDbDir);
		logger.displayln("- concepts (in config!): " + concepts);
		logger.displayln("- modernLanguages (in config!): " + modernLanguages);
		logger.displayln("- networkEdgesFile: " + semanticNet.getNetworkEdgesFile());
		logger.displayln("- networkIdsFile: " + semanticNet.getNetworkIdsFile());
		logger.displayln("- nelexConceptsFile: " + semanticNet.getNelexConceptsFile());
		logger.displayln("- maxDist: " + semanticNet.getMaxDist());
	}
}
