package de.tuebingen.sfs.eie.components.etymology.ideas;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.jdellert.iwsa.corrmodel.CorrespondenceModel;
import de.jdellert.iwsa.sequence.PhoneticString;
import de.jdellert.iwsa.tokenize.IPATokenizer;
import de.tuebingen.sfs.cldfjava.data.CLDFWordlistDatabase;
import de.tuebingen.sfs.eie.components.etymology.problems.EtymologyProblemConfig;
import de.tuebingen.sfs.eie.components.etymology.problems.EtymologyProblem;
import de.tuebingen.sfs.eie.components.etymology.util.LevelBasedPhylogeny;
import de.tuebingen.sfs.eie.shared.core.IndexedObjectStore;
import de.tuebingen.sfs.eie.shared.core.TreeLayer;
import de.tuebingen.sfs.eie.shared.io.LanguageTreeStorage;
import de.tuebingen.sfs.eie.shared.util.LoadUtils;
import de.tuebingen.sfs.eie.shared.util.PhoneticSimilarityHelper;
import de.tuebingen.sfs.eie.shared.util.SemanticNetwork;
import de.tuebingen.sfs.psl.engine.IdeaGenerator;
import de.tuebingen.sfs.psl.engine.PslProblem;
import de.tuebingen.sfs.psl.util.log.InferenceLogger;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class EtymologyIdeaGenerator extends IdeaGenerator {

	public static final String F_UFO_EX = PslProblem.existentialAtomName("Fufo");
	private PhoneticSimilarityHelper phonSimHelper;
	private LevelBasedPhylogeny tree;
	private CLDFWordlistDatabase wordListDb;
	private IndexedObjectStore objectStore;
	private List<String> ancestors;
	private Set<Entry> entryPool;
	private Set<String> removedIsolates;
	private InferenceLogger logger;
	private EtymologyProblemConfig config;

	public EtymologyIdeaGenerator(EtymologyProblem problem, IndexedObjectStore objectStore,
			PhoneticSimilarityHelper phonSimHelper, CLDFWordlistDatabase wordListDb) {
		// For proper serialization, the wordListDb and the phonSimHelper need
		// to be default versions of these objects,
		// e.g. wordListDb = LoadUtils.loadDatabase(DB_DIR, logger);
		// phonSimHelper = new PhoneticSimilarityHelper(new IPATokenizer(),
		// LoadUtils.loadCorrModel(DB_DIR, false, tokenizer, logger));
		super(problem);
		this.logger = problem.getLogger();
		logger.displayln("...Creating EtymologyIdeaGenerator.");
		config = problem.getEtymologyConfig();
		config.print(System.out);
		logger.displayln("...Working with the following idea generation configuration:");
		config.logSettings();
		if (wordListDb == null) {
			logger.displayln("...No CLDF Wordlist Database given, loading database.");
			this.wordListDb = LoadUtils.loadDatabase(config.getWordListDbDir(), logger);
		} else {
			this.wordListDb = wordListDb;
		}

		if (objectStore == null) {
			logger.displayln("...No IndexedObjectStore given, loading the default version.");
			this.objectStore = new IndexedObjectStore(this.wordListDb, null);
		} else {
			this.objectStore = objectStore;
		}
		if (phonSimHelper == null) {
			logger.displayln("...No Phonetic Similarity Helper given, using default version.");
			IPATokenizer tokenizer = new IPATokenizer();
			this.phonSimHelper = new PhoneticSimilarityHelper(tokenizer,
					LoadUtils.loadCorrModel(config.getCorrespondenceDbDir(), false, tokenizer, this.logger),
					this.objectStore);
		} else {
			this.phonSimHelper = phonSimHelper;
		}
		if (config.getModernLanguages().isEmpty()) {
			logger.displayln(
					"...No modernLanguages specified. Will only construct the phylogenetic tree once the modernLanguages are set via setLanguages().");
		} else {
			setTree();
		}
		entryPool = new HashSet<>();

		updateLanguagesAndTree();

		logger.displayln("Finished setting up the Etymology Idea Generator.");
	}

	public static EtymologyIdeaGenerator initializeDefault(EtymologyProblem problem, IndexedObjectStore objectStore) {
		return new EtymologyIdeaGenerator(problem, objectStore, null, null);
	}

	public static EtymologyIdeaGenerator fromJson(EtymologyProblem problem, IndexedObjectStore objectStore,
			ObjectMapper mapper, String path, InferenceLogger logger) {
		// return fromJson(problem, mapper,
		// EtymologyIdeaGenerator.class.getResourceAsStream(path));
		try {
			return fromJson(problem, objectStore, mapper, new FileInputStream(path), logger);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return null;
		}
	}

	public static EtymologyIdeaGenerator fromJson(EtymologyProblem problem, IndexedObjectStore objectStore,
			ObjectMapper mapper, InputStream in, InferenceLogger logger) {
		IPATokenizer tokenizer = new IPATokenizer();
		CLDFWordlistDatabase wordListDb = LoadUtils.loadDatabase(problem.getEtymologyConfig().getWordListDbDir(),
				logger);
		PhoneticSimilarityHelper phonSimHelper = null;
		if (objectStore != null && problem.getEtymologyConfig().getCorrespondenceDbDir() != null
				&& !problem.getEtymologyConfig().getCorrespondenceDbDir().isEmpty())
			phonSimHelper = new PhoneticSimilarityHelper(tokenizer, LoadUtils.loadCorrModel(
					problem.getEtymologyConfig().getCorrespondenceDbDir(), false, tokenizer, logger), objectStore);

		return new EtymologyIdeaGenerator(problem, objectStore, phonSimHelper, wordListDb);
	}

	public static EtymologyIdeaGenerator getIdeaGeneratorForTestingMountain(EtymologyProblem problem,
			boolean largeLanguageSet) {
		return getIdeaGeneratorForTestingMountain(problem, largeLanguageSet, false);
	}

	public static EtymologyIdeaGenerator getIdeaGeneratorForTestingMountain(EtymologyProblem problem,
			boolean largeLanguageSet, boolean branchwiseBorrowing) {
		List<String> concepts = new ArrayList<>();
		concepts.add("BergN");
		return getIdeaGeneratorForTesting(problem, concepts, largeLanguageSet, branchwiseBorrowing);
	}

	public static EtymologyIdeaGenerator getIdeaGeneratorForTestingHead(EtymologyProblem problem,
			boolean largeLanguageSet) {
		return getIdeaGeneratorForTestingHead(problem, largeLanguageSet, false);
	}

	public static EtymologyIdeaGenerator getIdeaGeneratorForTestingHead(EtymologyProblem problem,
			boolean largeLanguageSet, boolean branchwiseBorrowing) {
		List<String> concepts = new ArrayList<>();
		concepts.add("KopfN");
		return getIdeaGeneratorForTesting(problem, concepts, largeLanguageSet, branchwiseBorrowing);
	}

	public static EtymologyIdeaGenerator getIdeaGeneratorForTestingLanguage(EtymologyProblem problem,
			boolean largeConceptSet, boolean largeLanguageSet) {
		return getIdeaGeneratorForTestingLanguage(problem, largeConceptSet, largeLanguageSet, false);
	}

	public static EtymologyIdeaGenerator getIdeaGeneratorForTestingLanguage(EtymologyProblem problem,
			boolean largeConceptSet, boolean largeLanguageSet, boolean branchwiseBorrowing) {
		List<String> concepts = new ArrayList<>();
		concepts.add("SpracheN");
		if (largeConceptSet) {
			concepts.add("ZungeN");
		}
		return getIdeaGeneratorForTesting(problem, concepts, largeLanguageSet, branchwiseBorrowing);
	}

	private static EtymologyIdeaGenerator getIdeaGeneratorForTesting(EtymologyProblem problem, List<String> concepts,
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

		InferenceLogger logger = problem.getConfig().getLogger();
		CLDFWordlistDatabase wordListDb = LoadUtils.loadDatabase(EtymologyProblemConfig.DB_DIR, logger);
		IPATokenizer tokenizer = new IPATokenizer();
		IndexedObjectStore objectStore = new IndexedObjectStore(wordListDb, null);
		PhoneticSimilarityHelper phonSimHelper = new PhoneticSimilarityHelper(tokenizer,
				LoadUtils.loadCorrModel(EtymologyProblemConfig.DB_DIR, false, tokenizer, logger), objectStore);
		int treeDepth = 4;

		return new EtymologyIdeaGenerator(problem, objectStore, phonSimHelper, wordListDb);
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

		List<String> concepts = new ArrayList<>();
		concepts.add("SpracheN");

		InferenceLogger logger = problem.getConfig().getLogger();
		CLDFWordlistDatabase wordListDb = LoadUtils.loadDatabase(EtymologyProblemConfig.TEST_DB_DIR, logger);
		CorrespondenceModel corres = LoadUtils.loadCorrModel(EtymologyProblemConfig.DB_DIR, false, tokenizer, logger);
		IndexedObjectStore objectStore = new IndexedObjectStore(wordListDb, null);
		PhoneticSimilarityHelper phonSimHelper = new PhoneticSimilarityHelper(new IPATokenizer(), corres, objectStore);
		int treeDepth = 2;

		return new EtymologyIdeaGenerator(problem, objectStore, phonSimHelper, wordListDb);
	}

	public void generateAtoms() {
		// 1. Determine and retrieve/generate the relevant F-atoms.

		// TODO
		// For the given concept and modernLanguages, retrieve Fufo, Flng, Fsem,
		// Hmem
		// Get the Hmem siblings for these atoms and retrieve their F-atoms
		// (update `concepts`!)

		// Retrieving modernLanguages from the tree to get proto modernLanguages
		// as well.
		List<String> concepts = config.getConcepts();
		for (String lang : tree.getAllLanguages()) {
			Set<Integer> cldfForms = objectStore.getFormsForLanguage(lang);
			if (cldfForms == null || cldfForms.isEmpty()) {
				// Proto language
				cldfForms = new HashSet<>();
				for (String concept : concepts) {
					// TODO get the most likely reconstructed form instead (if
					// available) (vbl)
					int formId = objectStore.createFormId();
					objectStore.addFormIdWithConcept(formId, concept);
					objectStore.addFormIdWithLanguage(formId, lang);
					cldfForms.add(formId);
				}
			}
			for (Integer cldfFormID : cldfForms) {
				List<String> conceptsForForm = objectStore.getConceptsForForm(cldfFormID);
				for (String concept : conceptsForForm) {
					if (concepts.contains(concept)) {
						entryPool.add(new Entry(cldfFormID, lang, concept));
						// TODO only add these for proto modernLanguages that don't
						// have
						// these yet, retrieve existing F-atoms from db and pass
						// them on
						addFormAtoms(cldfFormID, lang, concept, cldfFormID);
					}
				}
			}
		}

		// 2. Generate semantic similarity atoms.

		SemanticNetwork semanticNet = config.getSemanticNet();
		for (String concept1 : concepts) {
			for (String concept2 : concepts) {
				pslProblem.addObservation("Ssim", semanticNet.getSimilarity(concept1, concept2), concept1, concept2);
			}
		}

		// 3. Generate language ancestry/contact atoms.

		boolean branchwiseBorrowing = config.branchwiseBorrowing();
		for (String lang1 : tree.getAllLanguages()) {
			for (String lang2 : tree.getAllLanguages()) {
				if (lang1.equals(lang2)) {
					continue;
				}
				if (tree.distanceToAncestor(lang1, lang2) == 1) {
					pslProblem.addObservation("Tanc", 1.0, lang1, lang2);
				} else if ((!branchwiseBorrowing) && tree.getLevel(lang1).equals(tree.getLevel(lang2))) {
					// TODO: borrowing from e.g. Latin
					// TODO: geographical distance etc.
					// TODO: make this open instead? e.g.
					// pslProblem.addTarget
					pslProblem.addObservation("Tcnt", 1.0, lang1, lang2);
				} else if (branchwiseBorrowing && tree.getLevel(lang1).getFocusNode().equals(tree.getLevel(lang1).getFocusNode())
						&& tree.getLevel(lang1).getIndex().equals(tree.getLevel(lang2).getIndex() + 1)) {
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
			if (!config.branchwiseBorrowing()) {
				pslProblem.addTarget("Eloa", entry1.formIdAsString, entry2.formIdAsString);
			}
		} else if (config.branchwiseBorrowing()
				&& tree.getLevel(entry1.language).getFocusNode().equals(tree.getLevel(entry2.language).getFocusNode())
				&& tree.getLevel(entry1.language).getIndex().equals(tree.getLevel(entry2.language).getIndex() + 1)) {
			pslProblem.addTarget("Eloa", entry1.formIdAsString, entry2.formIdAsString);
		} else {
			pslProblem.addObservation("Einh", 0.0, entry1.formIdAsString, entry2.formIdAsString);
			pslProblem.addObservation("Eloa", 0.0, entry1.formIdAsString, entry2.formIdAsString);
		}

		String ipa1 = "";
		String ipa2 = "";
		ipa1 = objectStore.getFormForFormId(entry1.formId);
		if (ipa1 == null || ipa1.isEmpty()) {
			return;
		}

		ipa2 = objectStore.getFormForFormId(entry2.formId);
		if (ipa2 == null || ipa2.isEmpty()) {
			return;
		}

		double sim = 0.0;
		PhoneticString form1 = null;
		PhoneticString form2 = null;
		form1 = phonSimHelper.extractSegments(entry1.formId);
		form2 = phonSimHelper.extractSegments(entry2.formId);
		sim = phonSimHelper.similarity(form1, form2);
		sim = logistic(sim);
		pslProblem.addObservation("Fsim", sim, entry1.formIdAsString, entry2.formIdAsString);
		System.out.println("Fsim(" + entry1.formId + "/" + ipa1 + "/" + form1 + "," + entry2.formId + "/" + ipa2 + "/"
				+ form2 + ") " + sim);

	}

	private void addFormAtoms(int formId, String doculectId, String concept, int cldfForm) {
		pslProblem.addObservation("Flng", 1.0, formId + "", doculectId);
		pslProblem.addObservation("Fsem", 1.0, formId + "", concept);
		String ipa = objectStore.getFormForFormId(cldfForm);
		if (ipa != null && !ipa.isEmpty()) {
			// TODO add XFufo also for imported Fufo atoms!!
			pslProblem.addObservation(F_UFO_EX, 1.0, formId + "");
			pslProblem.addObservation("Fufo", 1.0, formId + "", formId + "");
		}
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
		config.export(mapper, out);
	}

	// Costly because this involves renaming the leaf nodes in the tree.
	private void updateLanguagesAndTree() {
		setTree();
		System.err.println("Initial tree:");
		LanguageTreeStorage.saveLayeredTree(tree.getTree(), System.err);
		if (config.addSiblingLanguages())
			addSiblingLanguages();
		removeIsolates();
		logger.displayln("");
	}

	// Costly because this involves renaming the leaf nodes in the tree.
	public void setLanguages(List<String> languages) {
		config.setLanguages(languages);
		updateLanguagesAndTree();
	}

	public void setConcepts(List<String> concepts) {
		config.setConcepts(concepts);
	}

	private void setTree() {
		tree = new LevelBasedPhylogeny(config.getTreeDepth(), config.getTreeFile(), config.getModernLanguages());
		ancestors = new ArrayList<>();
		for (String language : tree.getAllLanguages()) {
			if (config.getModernLanguages().contains(language)) {
				continue;
			}
			String languageId = language;
			if (objectStore.getNameForLang(language) == null) {
				languageId = objectStore.addNewLanguage(language);
				if (!languageId.equals(language))
					tree.getTree().renameNode(language, languageId);
			}
			ancestors.add(languageId);
		}
	}

	public LevelBasedPhylogeny getTree() {
		return tree;
	}

	private void addSiblingLanguages() {
		logger.displayln("Adding sibling languages to the language set.");
		System.err.println("Adding sibling languages to the language set.");
		Set<String> parents = new HashSet<>();
		for (String lang : config.getModernLanguages()) {
			parents.add(tree.getParent(lang));
		}
		LevelBasedPhylogeny fullTree = new LevelBasedPhylogeny(config.getTreeFile());
		Set<String> targetLangs = new HashSet<>();
		for (String langId : objectStore.getLanguageIds()) {
			targetLangs.add(langId);
		}
		targetLangs.removeAll(config.getModernLanguages());
		targetLangs.removeAll(ancestors);
		String anc;
		boolean addedAny = false;
		for (String langId : targetLangs) {
			for (String ancestor : fullTree.getTree().pathToRoot(langId)) {
				anc = objectStore.getNameForLang(ancestor);
				if (anc != null) {
					ancestor = anc;
				}
				for (String parent : parents) {
					if (ancestor.equals(parent)) {
						tree.getTree().children.get(parent).add(langId);
						tree.getTree().parents.put(langId, parent);
						TreeLayer layer = tree.getTree().getLayer("ROOT", config.getTreeDepth());
						tree.getTree().nodesToLayers.put(langId, layer);
						config.getModernLanguages().add(langId);
						addedAny = true;
						logger.displayln("- Added " + langId + ".");
						System.err.println("- Added " + langId + ".");
						break;
					}
				}
			}
		}
		if (addedAny) {
			logger.displayln("New language set: " + config.getModernLanguages());
			LanguageTreeStorage.saveLayeredTree(tree.getTree(), logger.getGuiStream());
			LanguageTreeStorage.saveLayeredTree(tree.getTree(), System.err);
		} else {
			logger.displayln("No missing sibling languages found. Tree unchanged.");
			System.err.println("No missing sibling languages found. Tree unchanged.");
		}
	}

	private void removeIsolates() {
		logger.displayln("Removing isolates from the language set.");
		System.err.println("Removing isolates from the language set.");
		Set<String> toBeRemoved = new HashSet<>();
		boolean removedAny = false;
		for (String lang : config.getModernLanguages()) {
			String parent = tree.getTree().parents.get(lang);
			//System.err.println("lang: " + lang + ", level: " + tree.getLevel(lang) + ", parent: " + parent + ", siblings: " + tree.getTree().children.get(parent));
			while (// Leaf node with no siblings
			(tree.getLevel(lang).equals(TreeLayer.leaves()) && tree.getTree().children.get(parent).size() == 1)
					// Intermediate node with no children
					|| (!tree.getLevel(lang).equals(TreeLayer.leaves()) && (tree.getTree().children.get(lang) == null
							|| tree.getTree().children.get(lang).isEmpty()))) {

				removedAny = true;
				tree.getTree().children.get(parent).remove(lang);
				tree.getTree().parents.remove(lang);
				toBeRemoved.add(lang);
				logger.displayln("- Removing " + lang + ".");
				System.err.println("- Removing " + lang + ".");
				lang = parent;
				parent = tree.getTree().parents.get(parent);
				if (parent == null || lang.equals(tree.getTree().root)) {
					break;
				}
			}
		}
		config.getModernLanguages().removeAll(toBeRemoved);
		if (removedAny) {
			logger.displayln("Remaining languages: " + config.getModernLanguages());
			if (logger.getGuiStream() != null) {
				// GUI stream is null when running offline inferences
				LanguageTreeStorage.saveLayeredTree(tree.getTree(), logger.getGuiStream());
			}
			LanguageTreeStorage.saveLayeredTree(tree.getTree(), System.err);
		} else {
			logger.displayln("No isolates found. Tree unchanged.");
			System.err.println("No isolates found. Tree unchanged.");
		}
		this.removedIsolates = toBeRemoved;
	}

	public Set<String> getRemovedIsolates() {
		return removedIsolates;
	}

	private class Entry {

		Integer formId = null;
		String formIdAsString = null;
		String language;
		String concept;

		public Entry(int formId, String language, String concept) {
			this.formId = formId;
			this.formIdAsString = formId + "";
			this.language = language;
			this.concept = concept;
		}

		public String toString() {
			return formIdAsString + " (" + language + ", " + concept + ")";
		}

	}

}
