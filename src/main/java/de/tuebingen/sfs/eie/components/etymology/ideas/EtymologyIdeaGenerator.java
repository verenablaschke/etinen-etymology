package de.tuebingen.sfs.eie.components.etymology.ideas;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;

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

public class EtymologyIdeaGenerator extends IdeaGenerator {

	private PhoneticSimilarityHelper phonSimHelper;
	private LevelBasedPhylogeny tree;
	private CLDFWordlistDatabase wordListDb;
	private IndexedObjectStore objectStore;
	private List<String> ancestors;
	private EtymologyIdeaGeneratorConfig ideaGenConfig;
	private Set<Entry> entryPool;

	// TODO use (vbl)
	private InferenceLogger logger;

	public static final String F_UFO_EX = PslProblem.existentialAtomName("Fufo");

	public EtymologyIdeaGenerator(EtymologyProblem problem, IndexedObjectStore objectStore,
			EtymologyIdeaGeneratorConfig ideaGenConfig, PhoneticSimilarityHelper phonSimHelper,
			CLDFWordlistDatabase wordListDb, InferenceLogger logger) {
		// For proper serialization, the wordListDb and the phonSimHelper need
		// to be default versions of these objects,
		// e.g. wordListDb = LoadUtils.loadDatabase(DB_DIR, logger);
		// phonSimHelper = new PhoneticSimilarityHelper(new IPATokenizer(),
		// LoadUtils.loadCorrModel(DB_DIR, false, tokenizer, logger));
		super(problem);
		System.err.println("...Creating EtymologyIdeaGenerator.");
		this.ideaGenConfig = ideaGenConfig;
		if (ideaGenConfig == null) {
			this.ideaGenConfig = new EtymologyIdeaGeneratorConfig();
		}
		System.err.println("...Working with the following idea generation configuration:");
		this.ideaGenConfig.printConfig();
		if (logger == null)
			this.logger = new InferenceLogger();
		else
			this.logger = logger;
		if (wordListDb == null) {
			System.err.println("...No CLDF Wordlist Database given, loading database.");
			this.wordListDb = LoadUtils.loadDatabase(ideaGenConfig.wordListDbDir, logger);
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
					LoadUtils.loadCorrModel(ideaGenConfig.correspondenceDbDir, false, tokenizer, this.logger),
					this.objectStore);
		} else {
			this.phonSimHelper = phonSimHelper;
		}
		if (ideaGenConfig.modernLanguages.isEmpty()) {
			System.err.println(
					"...No modernLanguages specified. Will only construct the phylogenetic tree once the modernLanguages are set via setLanguages().");
			// this.modernLanguages = new ArrayList<>();
		} else {
			// this.modernLanguages = languages;
			setTree();
		}
		entryPool = new HashSet<>();

		System.err.println("Finished setting up the Etymology Idea Generator.");
	}
	
	public static EtymologyIdeaGenerator initializeDefault(EtymologyProblem problem, IndexedObjectStore objectStore,
			InferenceLogger logger) {
		return new EtymologyIdeaGenerator(problem, objectStore, null, null, null, null);
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

	public static EtymologyIdeaGenerator fromJson(EtymologyProblem problem, IndexedObjectStore objectStore,
			ObjectMapper mapper, InputStream in) {
		EtymologyIdeaGeneratorConfig ideaGenConfig = new EtymologyIdeaGeneratorConfig();
		ideaGenConfig.initializeFromJson(mapper, in);
		InferenceLogger logger = new InferenceLogger();
		IPATokenizer tokenizer = new IPATokenizer();
		CLDFWordlistDatabase wordListDb = LoadUtils.loadDatabase(ideaGenConfig.wordListDbDir, logger);
		PhoneticSimilarityHelper phonSimHelper = null;
		if (objectStore != null && ideaGenConfig.correspondenceDbDir != null
				&& !ideaGenConfig.correspondenceDbDir.isEmpty())
			phonSimHelper = new PhoneticSimilarityHelper(tokenizer,
					LoadUtils.loadCorrModel(ideaGenConfig.correspondenceDbDir, false, tokenizer, logger), objectStore);

		return new EtymologyIdeaGenerator(problem, objectStore, ideaGenConfig, phonSimHelper, wordListDb, null);
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

		InferenceLogger logger = new InferenceLogger();
		CLDFWordlistDatabase wordListDb = LoadUtils.loadDatabase(EtymologyIdeaGeneratorConfig.DB_DIR, logger);
		IPATokenizer tokenizer = new IPATokenizer();
		IndexedObjectStore objectStore = new IndexedObjectStore(wordListDb, null);
		PhoneticSimilarityHelper phonSimHelper = new PhoneticSimilarityHelper(tokenizer,
				LoadUtils.loadCorrModel(EtymologyIdeaGeneratorConfig.DB_DIR, false, tokenizer, logger), objectStore);
		int treeDepth = 4;

		EtymologyIdeaGeneratorConfig conf = new EtymologyIdeaGeneratorConfig(concepts, languages,
				EtymologyIdeaGeneratorConfig.DB_DIR + "/tree.nwk", null, null, treeDepth,
				branchwiseBorrowing, null);
		return new EtymologyIdeaGenerator(problem, objectStore, conf, phonSimHelper, wordListDb, logger);
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

		InferenceLogger logger = new InferenceLogger();
		CLDFWordlistDatabase wordListDb = LoadUtils.loadDatabase(EtymologyIdeaGeneratorConfig.TEST_DB_DIR, logger);
		CorrespondenceModel corres = LoadUtils.loadCorrModel(EtymologyIdeaGeneratorConfig.DB_DIR, false, tokenizer,
				logger);
		IndexedObjectStore objectStore = new IndexedObjectStore(wordListDb, null);
		PhoneticSimilarityHelper phonSimHelper = new PhoneticSimilarityHelper(new IPATokenizer(), corres, objectStore);
		int treeDepth = 2;

		EtymologyIdeaGeneratorConfig conf = new EtymologyIdeaGeneratorConfig(concepts, languages,
				EtymologyIdeaGeneratorConfig.DB_DIR + "/tree.nwk", null, null, treeDepth,
				branchwiseBorrowing, null);
		return new EtymologyIdeaGenerator(problem, objectStore, conf, phonSimHelper, wordListDb, logger);
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
		for (String lang : tree.getAllLanguages()) {
			Set<Integer> cldfForms = objectStore.getFormsForLanguage(lang);
			if (cldfForms == null || cldfForms.isEmpty()) {
				// Proto language
				cldfForms = new HashSet<>();
				for (String concept : ideaGenConfig.concepts) {
					// TODO get the most likely reconstructed form instead (if
					// available) (vbl)
					int formId = objectStore.createFormId();
					objectStore.addFormIdWithConcept(formId, concept);
					objectStore.addFormIdWithLanguage(formId, lang);
					cldfForms.add(formId);
				}
			}
			for (Integer cldfFormID : cldfForms) {
				if (ideaGenConfig.concepts.contains(objectStore.getConceptForForm(cldfFormID))) {
					entryPool.add(new Entry(cldfFormID, lang, objectStore.getConceptForForm(cldfFormID)));
					// TODO only add these for proto modernLanguages that don't
					// have
					// these yet, retrieve existing F-atoms from db and pass
					// them on
					addFormAtoms(cldfFormID, lang, objectStore.getConceptForForm(cldfFormID), cldfFormID);
				}
			}
		}

		// 2. Generate semantic similarity atoms.

		for (String concept1 : ideaGenConfig.concepts) {
			for (String concept2 : ideaGenConfig.concepts) {
				pslProblem.addObservation("Ssim", ideaGenConfig.semanticNet.getSimilarity(concept1, concept2), concept1,
						concept2);
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
				} else if ((!ideaGenConfig.branchwiseBorrowing) && tree.getLevel(lang1) == tree.getLevel(lang2)) {
					// TODO: borrowing from e.g. Latin
					// TODO: geographical distance etc.
					// TODO: make this open instead? e.g.
					// pslProblem.addTarget
					pslProblem.addObservation("Tcnt", 1.0, lang1, lang2);
				} else if (ideaGenConfig.branchwiseBorrowing && tree.getLevel(lang1) == tree.getLevel(lang2) + 1) {
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
			if (!ideaGenConfig.branchwiseBorrowing) {
				pslProblem.addTarget("Eloa", entry1.formIdAsString, entry2.formIdAsString);
			}
		} else if (ideaGenConfig.branchwiseBorrowing && tree.getLevel(entry1.language) == tree.getLevel(entry2.language) + 1) {
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
		ideaGenConfig.export(mapper, out);
	}

	public void setConcepts(List<String> concepts) {
		ideaGenConfig.concepts = concepts;
	}

	// Costly because this involves renaming the leaf nodes in the tree.
	public void setLanguages(List<String> languages) {
		System.err.println("Adding modernLanguages to Etymology Idea Generator and updating the phylogenetic tree.");
		ideaGenConfig.modernLanguages = languages;
		setTree();
	}

	private void setTree() {
		tree = new LevelBasedPhylogeny(ideaGenConfig.treeDepth, ideaGenConfig.treeFile, ideaGenConfig.modernLanguages);
		ancestors = new ArrayList<>();
		for (String language : tree.getAllLanguages()) {
			if (ideaGenConfig.modernLanguages.contains(language)) {
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

	public void addSiblingLanguages() {
		logger.displayln("Adding sibling languages to the language set.");
		Set<String> parents = new HashSet<>();
		for (String lang : ideaGenConfig.modernLanguages) {
			parents.add(tree.getParent(lang));
		}
		LevelBasedPhylogeny fullTree = new LevelBasedPhylogeny(ideaGenConfig.treeFile);
		Set<String> targetLangs = new HashSet<>();
		for (String langId : objectStore.getLanguageIds()) {
			targetLangs.add(langId);
		}
		targetLangs.removeAll(ideaGenConfig.modernLanguages);
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
						tree.getTree().nodeToLayerPlacement.put(langId, ideaGenConfig.treeDepth);
						ideaGenConfig.modernLanguages.add(langId);
						addedAny = true;
						logger.displayln("- Added " + langId + ".");
						break;
					}
				}
			}
		}
		if (addedAny)
			logger.displayln("New language set: " + ideaGenConfig.modernLanguages);
		else
			logger.displayln("No sibling languages found.");
	}

	public Set<String> removeIsolates() {
		logger.displayln("Removing isolates from the language set.");
		// TODO check again (vbl). might be a bit overzealous and remove more nodes than
		// intended.
		// the parent==null check shouldn't be necessary
		// since the children map gets updated each loop, it might be necessary to
		// impose size==1
		// in the while loop only for the leaf nodes, and size==0 afterwards
		Set<String> toBeRemoved = new HashSet<>();
		tree.getTree().saveLayeredTreeToFile(System.err);
		boolean removedAny = false;
		for (String lang : ideaGenConfig.modernLanguages) {
			String parent = tree.getTree().parents.get(lang);
			// no siblings ...or...
			while (tree.getTree().children.get(parent).size() == 1
					// ...no children (as intermediary node)
					|| (tree.getLevel(lang) < ideaGenConfig.treeDepth && (tree.getTree().children.get(lang) == null
							|| tree.getTree().children.get(lang).isEmpty()))) {
				removedAny = true;
				tree.getTree().children.get(parent).remove(lang);
				tree.getTree().parents.remove(lang);
				toBeRemoved.add(lang);
				System.err.println("Removed " + lang + ".");
				logger.displayln("- Removed " + lang + ".");
				lang = parent;
				parent = tree.getTree().parents.get(parent);
				if (parent == null || lang.equals(tree.getTree().root)) {
					break;
				}
			}
		}
		ideaGenConfig.modernLanguages.removeAll(toBeRemoved);
		if (removedAny)
			logger.displayln("Remaining languages: " + ideaGenConfig.modernLanguages);
		else
			logger.displayln("No isolates found.");
		return toBeRemoved;
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
