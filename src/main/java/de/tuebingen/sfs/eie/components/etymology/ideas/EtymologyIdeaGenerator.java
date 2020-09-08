package de.tuebingen.sfs.eie.components.etymology.ideas;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import de.jdellert.iwsa.corrmodel.CorrespondenceModel;
import de.jdellert.iwsa.sequence.PhoneticString;
import de.jdellert.iwsa.tokenize.IPATokenizer;
import de.tuebingen.sfs.cldfjava.data.CLDFWordlistDatabase;
import de.tuebingen.sfs.eie.components.etymology.problems.EtymologyProblem;
import de.tuebingen.sfs.eie.components.etymology.util.LevelBasedPhylogeny;
import de.tuebingen.sfs.eie.core.IndexedObjectStore;
import de.tuebingen.sfs.psl.engine.IdeaGenerator;
import de.tuebingen.sfs.psl.engine.PslProblem;
import de.tuebingen.sfs.psl.util.log.InferenceLogger;
import de.tuebingen.sfs.util.LoadUtils;
import de.tuebingen.sfs.util.PhoneticSimilarityHelper;
import de.tuebingen.sfs.util.SemanticNetwork;

public class EtymologyIdeaGenerator extends IdeaGenerator {
	private static final String DB_DIR = "src/test/resources/northeuralex-0.9";
	private static final String TEST_DB_DIR = "etinen-etymology/src/test/resources/testdb";
	private static final String NETWORK_EDGES_FILE = "src/test/resources/etymology/clics2-network-edges.txt";
	private static final String NETWORK_IDS_FILE = "src/test/resources/etymology/clics2-network-ids.txt";
	private static final String NELEX_CONCEPTS_FILE = "src/test/resources/northeuralex-0.9/parameters.csv";

	private Set<String> concepts;
	private SemanticNetwork semanticNet;
	private PhoneticSimilarityHelper phonSimHelper;
	private LevelBasedPhylogeny tree;
	private CLDFWordlistDatabase wordListDb;
	private IndexedObjectStore objectStore;
	private List<String> languages;
	private int treeDepth;
	private String treeFile;
	private boolean branchwiseBorrowing;

	private Set<Entry> entryPool;

	public static final String F_UFO_EX = PslProblem.existentialAtomName("Fufo");

	public EtymologyIdeaGenerator(EtymologyProblem problem, IndexedObjectStore objectStore, Set<String> concepts,
			List<String> languages, String treeFile, SemanticNetwork semanticNet,
			PhoneticSimilarityHelper phonSimHelper, CLDFWordlistDatabase wordListDb, int treeDepth,
			boolean branchwiseBorrowing) {
		// For proper serialization, the wordListDb and the phonSimHelper need
		// to be default versions of these objects,
		// e.g. wordListDb = LoadUtils.loadDatabase(DB_DIR, logger);
		// phonSimHelper = new PhoneticSimilarityHelper(new IPATokenizer(),
		// LoadUtils.loadCorrModel(DB_DIR, false, tokenizer, logger));
		super(problem);
		System.err.println("Creating EtymologyIdeaGenerator.");
		if (concepts == null) {
			System.err.println("...No concepts specified.");
			this.concepts = new HashSet<>();
		} else {
			this.concepts = concepts;
		}
		if (semanticNet == null) {
			System.err.println("...No semantic net given, using default network.");
			this.semanticNet = new SemanticNetwork(NETWORK_EDGES_FILE, NETWORK_IDS_FILE, NELEX_CONCEPTS_FILE, 2);
		} else {
			this.semanticNet = semanticNet;
		}
		InferenceLogger logger = new InferenceLogger();
		if (wordListDb == null) {
			System.err.println("...No CLDF Wordlist Database given, loading default version.");
			this.wordListDb = LoadUtils.loadDatabase(DB_DIR, logger);
		} else {
			this.wordListDb = wordListDb;
		}
		if (objectStore == null) {
			System.err.println("...No IndexedObjectStore given, loading the default version.");
			this.objectStore = new IndexedObjectStore(this.wordListDb, null);
		} else {
			this.objectStore = objectStore;
		}
		if (phonSimHelper == null) {
			System.err.println("...No Phonetic Similarity Helper given, using default version.");
			IPATokenizer tokenizer = new IPATokenizer();
			this.phonSimHelper = new PhoneticSimilarityHelper(tokenizer,
					LoadUtils.loadCorrModel(DB_DIR, false, tokenizer, logger), this.objectStore);
		} else {
			this.phonSimHelper = phonSimHelper;
		}
		this.branchwiseBorrowing = branchwiseBorrowing;
		if (treeDepth < 1) {
			this.treeDepth = 4;
			System.err
					.println("...No phylogenetic tree depth specified, using default value (" + this.treeDepth + ").");
		} else {
			this.treeDepth = treeDepth;
		}
		if (treeFile == null || treeFile.trim().isEmpty()) {
			this.treeFile = DB_DIR + "/tree.nwk";
			System.err.println("...No input file for the tree specified, using default: " + this.treeFile);
		} else {
			this.treeFile = treeFile;
		}
		if (languages == null) {
			System.err.println(
					"...No languages specified. Will only construct the phylogenetic tree once the languages are set via setLanguages().");
			this.languages = new ArrayList<>();
		} else {
			this.languages = languages;
			setTree();
		}
		entryPool = new HashSet<>();

		System.err.println("Finished setting up the Etymology Idea Generator.");
	}

	public static EtymologyIdeaGenerator initializeDefault(EtymologyProblem problem, IndexedObjectStore objectStore) {
		return new EtymologyIdeaGenerator(problem, objectStore, null, null, null, null, null, null, -1, true);
	}

	public static EtymologyIdeaGenerator fromJson(EtymologyProblem problem, IndexedObjectStore objectStore,
			ObjectMapper mapper, String path) {
		// return fromJson(problem, mapper,
		// EtymologyIdeaGenerator.class.getResourceAsStream(path));
		try {
			return fromJson(problem, objectStore, mapper, new FileInputStream(path));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	public static EtymologyIdeaGenerator fromJson(EtymologyProblem problem, IndexedObjectStore objectStore,
			ObjectMapper mapper, InputStream in) {
		Set<String> concepts = null;
		List<String> languages = null;
		Integer treeDepth = null;
		Boolean branchwiseBorrowing = null;
		String treeFile = null;
		SemanticNetwork semanticNet = null;
		String wordListDbDir = null;
		String correspondenceDbDir = null;
		try {
			JsonNode rootNode = mapper.readTree(in);
			concepts = mapper.treeToValue(rootNode.path("concepts"), HashSet.class);
			languages = mapper.treeToValue(rootNode.path("languages"), ArrayList.class);
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
		InferenceLogger logger = new InferenceLogger();
		IPATokenizer tokenizer = new IPATokenizer();
		CLDFWordlistDatabase wordListDb = LoadUtils.loadDatabase(wordListDbDir, logger);
		PhoneticSimilarityHelper phonSimHelper = new PhoneticSimilarityHelper(tokenizer,
				LoadUtils.loadCorrModel(correspondenceDbDir, false, tokenizer, logger), objectStore);

		return new EtymologyIdeaGenerator(problem, objectStore, concepts, languages, treeFile, semanticNet,
				phonSimHelper, wordListDb, treeDepth, branchwiseBorrowing);
	}

	public static EtymologyIdeaGenerator getIdeaGeneratorForTestingMountain(EtymologyProblem problem,
			boolean largeLanguageSet) {
		return getIdeaGeneratorForTestingMountain(problem, largeLanguageSet, false);
	}

	public static EtymologyIdeaGenerator getIdeaGeneratorForTestingMountain(EtymologyProblem problem,
			boolean largeLanguageSet, boolean branchwiseBorrowing) {
		Set<String> concepts = new HashSet<>();
		concepts.add("BergN");
		return getIdeaGeneratorForTesting(problem, concepts, largeLanguageSet, branchwiseBorrowing);
	}

	public static EtymologyIdeaGenerator getIdeaGeneratorForTestingHead(EtymologyProblem problem,
			boolean largeLanguageSet) {
		return getIdeaGeneratorForTestingHead(problem, largeLanguageSet, false);
	}

	public static EtymologyIdeaGenerator getIdeaGeneratorForTestingHead(EtymologyProblem problem,
			boolean largeLanguageSet, boolean branchwiseBorrowing) {
		Set<String> concepts = new HashSet<>();
		concepts.add("KopfN");
		return getIdeaGeneratorForTesting(problem, concepts, largeLanguageSet, branchwiseBorrowing);
	}

	public static EtymologyIdeaGenerator getIdeaGeneratorForTestingLanguage(EtymologyProblem problem,
			boolean largeConceptSet, boolean largeLanguageSet) {
		return getIdeaGeneratorForTestingLanguage(problem, largeConceptSet, largeLanguageSet, false);
	}

	public static EtymologyIdeaGenerator getIdeaGeneratorForTestingLanguage(EtymologyProblem problem,
			boolean largeConceptSet, boolean largeLanguageSet, boolean branchwiseBorrowing) {
		Set<String> concepts = new HashSet<>();
		concepts.add("SpracheN");
		if (largeConceptSet) {
			concepts.add("ZungeN");
		}
		return getIdeaGeneratorForTesting(problem, concepts, largeLanguageSet, branchwiseBorrowing);
	}

	private static EtymologyIdeaGenerator getIdeaGeneratorForTesting(EtymologyProblem problem, Set<String> concepts,
			boolean largeLanguageSet, boolean branchwiseBorrowing) {
		List<String> languages = new ArrayList<>();
		languages.add("eng");
		languages.add("deu");
		languages.add("swe");
		languages.add("nor");
		languages.add("dan");
		languages.add("fra");
		languages.add("spa");
		languages.add("ita");
		languages.add("cat");
		if (largeLanguageSet) {
			languages.add("isl");
			languages.add("nld");

			languages.add("por");
			languages.add("cat");
			languages.add("ron");
			languages.add("lat");

			languages.add("lit");
			languages.add("lav");
			languages.add("rus");
			languages.add("bel");
			languages.add("ukr");
			languages.add("pol");
			languages.add("ces");
			languages.add("slv");
			languages.add("slk");
			languages.add("hrv");
		}

		InferenceLogger logger = new InferenceLogger();
		CLDFWordlistDatabase wordListDb = LoadUtils.loadDatabase(DB_DIR, logger);
		IPATokenizer tokenizer = new IPATokenizer();
		IndexedObjectStore objectStore = new IndexedObjectStore(wordListDb, null);
		PhoneticSimilarityHelper phonSimHelper = new PhoneticSimilarityHelper(tokenizer,
				LoadUtils.loadCorrModel(DB_DIR, false, tokenizer, logger), objectStore);
		SemanticNetwork net = new SemanticNetwork(NETWORK_EDGES_FILE, NETWORK_IDS_FILE, NELEX_CONCEPTS_FILE, 2);
		int treeDepth = 4;

		EtymologyIdeaGenerator eig = new EtymologyIdeaGenerator(problem, objectStore, concepts, languages,
				DB_DIR + "/tree.nwk", net, phonSimHelper, wordListDb, treeDepth, branchwiseBorrowing);

		Map<String, String> ISO2LangID = new HashMap<>();
		for (String langID : objectStore.getLanguageIds()) {
			ISO2LangID.put(objectStore.getIsoForLang(langID), langID);
		}
		eig.languages = languages.stream().map(lang -> ISO2LangID.getOrDefault(lang, lang))
				.collect(Collectors.toList());

		return eig;
	}

	public static EtymologyIdeaGenerator getIdeaGeneratorWithFictionalData(EtymologyProblem problem, boolean synonyms,
			boolean moreLangsPerBranch, boolean moreBranches, boolean branchwiseBorrowing) {
		IPATokenizer tokenizer = new IPATokenizer();

		List<String> languages = new ArrayList<>();
		languages.add("a1");
		languages.add("a2");
		languages.add("a3");
		languages.add("b1");
		languages.add("b2");
		languages.add("b3");
		languages.add("c1");
		languages.add("c2");
		languages.add("c3");

		// Languages with several entries for one concept
		if (synonyms) {
			languages.add("a4");
		}

		if (moreLangsPerBranch) {
			languages.add("a5");
			languages.add("a6");
			languages.add("b4");
		}

		if (moreBranches) {
			languages.add("d1");
			languages.add("d2");
			languages.add("d3");
			languages.add("d4");
		}

		Set<String> concepts = new HashSet<>();
		concepts.add("SpracheN");

		InferenceLogger logger = new InferenceLogger();
		CLDFWordlistDatabase wordListDb = LoadUtils.loadDatabase(TEST_DB_DIR, logger);
		CorrespondenceModel corres = LoadUtils.loadCorrModel(DB_DIR, false, tokenizer, logger);
		IndexedObjectStore objectStore = new IndexedObjectStore(wordListDb, null);
		PhoneticSimilarityHelper phonSimHelper = new PhoneticSimilarityHelper(new IPATokenizer(), corres, objectStore);
		SemanticNetwork net = new SemanticNetwork(NETWORK_EDGES_FILE, NETWORK_IDS_FILE, NELEX_CONCEPTS_FILE, 2);
		int treeDepth = 2;

		return new EtymologyIdeaGenerator(problem, objectStore, concepts, languages, TEST_DB_DIR + "/tree.nwk", net,
				phonSimHelper, wordListDb, treeDepth, branchwiseBorrowing);
	}

	public void generateAtoms() {
		// 1. Determine and retrieve/generate the relevant F-atoms.

		// TODO
		// For the given concept and languages, retrieve Fufo, Flng, Fsem, Hmem
		// Get the Hmem siblings for these atoms and retrieve their F-atoms
		// (update `concepts`!)

		// Retrieving languages from the tree to get proto languages as well.
		for (String lang : tree.getAllLanguages()) {
			// TODO check if this includes only the languages in `languages' +
			// their ancestors (vbl)
			// System.out.println(lang + " : " + ISO2LangID.getOrDefault(lang,
			// lang));
			// ISO2LangID is useful for the NELex test, but *might* be
			// unnecessary otherwise.
//			Set<Integer> cldfForms = objectStore.getFormsForLanguage(ISO2LangID.getOrDefault(lang, lang));
			Set<Integer> cldfForms = objectStore.getFormsForLanguage(lang);
			if (cldfForms == null || cldfForms.isEmpty()) {
				// Proto language
				cldfForms = new HashSet<>();
				for (String concept : concepts) {
					// TODO get the most likely reconstructed form instead (if
					// available)
					// TODO note that is treated like a proper form, not like a
					// distribution over reconstructed ones
					int formId = objectStore.createFormId("");
					objectStore.addFormIdWithForm(formId, "");
					objectStore.addFormIdWithConcept(formId, concept);
					objectStore.addFormIdWithLanguage(formId, lang);
					cldfForms.add(formId);
				}
			}
			for (Integer cldfFormID : cldfForms) {
				if (concepts.contains(objectStore.getConceptForForm(cldfFormID))) {
					entryPool.add(new Entry(cldfFormID, lang,
							objectStore.getConceptForForm(cldfFormID)));
					// TODO only add these for proto languages that don't
					// have
					// these yet, retrieve existing F-atoms from db and pass
					// them on
					addFormAtoms(cldfFormID, lang, objectStore.getConceptForForm(cldfFormID),
							cldfFormID);
				}
			}
		}

		// 2. Generate semantic similarity atoms.

		for (String concept1 : concepts) {
			for (String concept2 : concepts) {
				pslProblem.addObservation("Ssim", semanticNet.getSimilarity(concept1, concept2), concept1, concept2);
			}
		}

		// 3. Generate language ancestry/contact atoms.

		for (String lang1 : tree.getAllLanguages()) {
			for (String lang2 : tree.getAllLanguages()) {
				if (lang1.equals(lang2)) {
					continue;
				}
				if (tree.distanceToAncestor(lang1, lang2) == 1) {
					pslProblem.addObservation("Tanc", 1.0, lang1, lang2);
				} else if ((!branchwiseBorrowing) && tree.getLevel(lang1) == tree.getLevel(lang2)) {
					// TODO: borrowing from e.g. Latin
					// TODO: geographical distance etc.
					// TODO: make this open instead? e.g.
					// pslProblem.addTarget
					pslProblem.addObservation("Tcnt", 1.0, lang1, lang2);
				} else if (branchwiseBorrowing && tree.getLevel(lang1) == tree.getLevel(lang2) + 1) {
					pslProblem.addObservation("Tcnt", 1.0, lang1, lang2);
				}
			}
		}

		// 4. Generate etymology atoms.

		for (Entry entry1 : entryPool) {
			pslProblem.addTarget("Eunk", entry1.formIdAsString);
			for (Entry entry2 : entryPool) {
				addAtomsForFormPair(entry1, entry2);
			}
		}
	}

	private void addAtomsForFormPair(Entry entry1, Entry entry2) {
		if (entry1.formId.equals(entry2.formId)) {
			return;
		}

		if (tree.distanceToAncestor(entry1.language, entry2.language) == 1) {
			pslProblem.addTarget("Einh", entry1.formIdAsString, entry2.formIdAsString);
			pslProblem.addObservation("Eloa", 0.0, entry1.formIdAsString, entry2.formIdAsString);
		} else if (tree.getLevel(entry1.language) == tree.getLevel(entry2.language)) {
			pslProblem.addObservation("Einh", 0.0, entry1.formIdAsString, entry2.formIdAsString);
			if (!branchwiseBorrowing) {
				pslProblem.addTarget("Eloa", entry1.formIdAsString, entry2.formIdAsString);
			}
		} else if (branchwiseBorrowing && tree.getLevel(entry1.language) == tree.getLevel(entry2.language) + 1) {
			pslProblem.addTarget("Eloa", entry1.formIdAsString, entry2.formIdAsString);
		} else {
			pslProblem.addObservation("Einh", 0.0, entry1.formIdAsString, entry2.formIdAsString);
			pslProblem.addObservation("Eloa", 0.0, entry1.formIdAsString, entry2.formIdAsString);
		}

		String ipa1 = "";
		String ipa2 = "";
		ipa1 = objectStore.getFormForFormId(entry1.formId);
		if (ipa1.isEmpty()) {
			return;
		}

		ipa2 = objectStore.getFormForFormId(entry2.formId);
		if (ipa2.isEmpty()) {
			return;
		}

		double sim = 0.0;
		PhoneticString form1 = null;
		PhoneticString form2 = null;
		form1 = phonSimHelper.extractSegments(entry1.formId);
		form2 = phonSimHelper.extractSegments(entry2.formId);
		sim = phonSimHelper.similarity(form1, form2);
		// pslProblem.addObservation("Fsimorig", sim, ipa1, ipa2);
		sim = logistic(sim);
		pslProblem.addObservation("Fsim", sim, ipa1, ipa2);
		System.out.println("Fsim(" + entry1.formId + "/" + ipa1 + "/" + form1 + "," + entry2.formId + "/" + ipa2 + "/" + form2
				+ ") " + sim);

	}

	private void addFormAtoms(int formId, String doculectId, String concept, int cldfForm) {
		pslProblem.addObservation("Flng", 1.0, formId + "", doculectId);
		pslProblem.addObservation("Fsem", 1.0, formId + "", concept);
		String ipa = objectStore.getFormForFormId(cldfForm);
		if (!ipa.isEmpty()) {
			// TODO add XFufo also for imported Fufo atoms!!
			pslProblem.addObservation(F_UFO_EX, 1.0, formId + "");
			pslProblem.addObservation("Fufo", 1.0, formId + "", ipa);
		}
	}

	private String getPrintForm(int form, String lang) {
		String ipa = objectStore.getFormForFormId(form);
		if (ipa.isEmpty()) {
			ipa = "N/A";
		}
		return lang + ":" + objectStore.getOrthoForForm(form) + ":" + ipa + ":" + objectStore.getConceptForForm(form);
	}

	private double logistic(double input) {
		double growthRate = 8.0;
		double midpoint = 0.42;
		return Math.pow(1 + Math.pow(Math.E, (-growthRate * (input - midpoint))), -1);
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
			rootNode.set("languages", (ArrayNode) mapper.readTree(mapper.writeValueAsString(languages)));
			rootNode.set("treeDepth", new IntNode(treeDepth));
			rootNode.set("branchwiseBorrowing",
					(BooleanNode) mapper.readTree(mapper.writeValueAsString(branchwiseBorrowing)));
			rootNode.set("treeFile", new TextNode(treeFile));
			rootNode.set("wordListDbDir", new TextNode(wordListDb.currentPath));
			rootNode.set("correspondenceDbDir", new TextNode(phonSimHelper.getCorrModel().getDbPath()));

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

	public void setConcepts(Set<String> concepts) {
		this.concepts = concepts;
	}

	// Costly because this involves renaming the leaf nodes in the tree.
	public void setLanguages(List<String> languages) {
		System.err.println("Adding languages to Etymology Idea Generator and updating the phylogenetic tree.");
		this.languages = languages;
		setTree();
	}
	
	private void setTree(){
		tree = new LevelBasedPhylogeny(treeDepth, treeFile, languages.stream().map(x -> objectStore.getIsoForLang(x)).collect(Collectors.toList()));
		tree.renameChildren(objectStore.getIsoToLanguageIdMap());
		tree.getTree().saveLayeredTreeToFile(System.out);
	}
	
	public LevelBasedPhylogeny getTree(){
		return tree;
	}
	
	private class Entry {

		Integer formId = null;
		String formIdAsString = null;
		String language;
		String concept;

		public Entry( int formId, String language, String concept) {
			this.formId = formId;
			this.formIdAsString = formId + "";
			this.language = language;
			this.concept = concept;
		}

		public String toString(){
			return formIdAsString + " (" + language + ", " + concept + ")";
		}

	}


}
