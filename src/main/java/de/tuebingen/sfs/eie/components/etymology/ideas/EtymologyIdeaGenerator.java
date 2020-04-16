package de.tuebingen.sfs.eie.components.etymology.ideas;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.jdellert.iwsa.corrmodel.CorrespondenceModel;
import de.jdellert.iwsa.sequence.PhoneticString;
import de.jdellert.iwsa.tokenize.IPATokenizer;
import de.jdellert.iwsa.util.phonsim.PhoneticSimilarityHelper;
import de.tuebingen.sfs.cldfjava.data.CLDFForm;
import de.tuebingen.sfs.cldfjava.data.CLDFLanguage;
import de.tuebingen.sfs.cldfjava.data.CLDFWordlistDatabase;
import de.tuebingen.sfs.eie.components.etymology.problems.EtymologyProblem;
import de.tuebingen.sfs.eie.components.etymology.util.LevelBasedPhylogeny;
import de.tuebingen.sfs.psl.engine.IdeaGenerator;
import de.tuebingen.sfs.psl.engine.PslProblem;
import de.tuebingen.sfs.util.InferenceLogger;
import de.tuebingen.sfs.util.LoadUtils;
import de.tuebingen.sfs.util.SemanticNetwork;

public class EtymologyIdeaGenerator extends IdeaGenerator {

	private Set<String> concepts;
	private SemanticNetwork semanticNet;
	private PhoneticSimilarityHelper phonSimHelper;
	private LevelBasedPhylogeny tree;
	private CLDFWordlistDatabase wordListDb;
	private Map<String, String> ISO2LangID;

	private Set<Entry> entryPool;

	public static final String F_UFO_EX = PslProblem.existentialAtomName("Fufo");

	public EtymologyIdeaGenerator(EtymologyProblem problem, Set<String> concepts, List<String> languages,
			String treeFile, SemanticNetwork semanticNet, PhoneticSimilarityHelper phonSimHelper,
			CLDFWordlistDatabase wordListDb) {
		super(problem);
		this.concepts = concepts;
		this.semanticNet = semanticNet;
		this.phonSimHelper = phonSimHelper;
		this.wordListDb = wordListDb;
		entryPool = new HashSet<>();
		tree = new LevelBasedPhylogeny(4, treeFile, languages);

		ISO2LangID = new HashMap<>();
		for (CLDFLanguage lang : wordListDb.getAllLanguages()) {
			ISO2LangID.put(lang.getIso639P3code(), lang.getLangID());
		}
	}

	public static EtymologyIdeaGenerator getIdeaGeneratorForTesting(EtymologyProblem problem, boolean largeConceptSet,
			boolean largeLanguageSet) {
		String dbDir = "src/test/resources/northeuralex-0.9";
		String networkEdgesFile = "src/test/resources/etymology/clics2-network-edges.txt";
		String networkIdsFile = "src/test/resources/etymology/clics2-network-ids.txt";
		String northeuralexConceptsFile = "src/test/resources/northeuralex-0.9/parameters.csv";
		String treeFile = "src/test/resources/northeuralex-0.9/tree.nwk";

		Set<String> concepts = new HashSet<>();
		concepts.add("SpracheN");
		if (largeConceptSet) {
			concepts.add("ZungeN");
			// concepts.add("BergN");
			// concepts.add("KopfN");

		}

		IPATokenizer tokenizer = new IPATokenizer();

		List<String> languages = new ArrayList<>();
		languages.add("eng");
		languages.add("deu");
		languages.add("swe");
		languages.add("nor");
		languages.add("dan");
		languages.add("fra");
		languages.add("spa");
		if (largeLanguageSet) {
			languages.add("isl");
			languages.add("nld");

			languages.add("por");
			languages.add("cat");
			languages.add("ita");
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
		CLDFWordlistDatabase wordListDb = LoadUtils.loadDatabase(dbDir, logger);
		CorrespondenceModel corres = LoadUtils.loadCorrModel(dbDir, false, tokenizer, logger);
		PhoneticSimilarityHelper phonSimHelper = new PhoneticSimilarityHelper(new IPATokenizer(), corres);
		SemanticNetwork net = new SemanticNetwork(networkEdgesFile, networkIdsFile, northeuralexConceptsFile, 2);

		return new EtymologyIdeaGenerator(problem, concepts, languages, treeFile, net, phonSimHelper, wordListDb);
	}

	public void generateAtoms() {
		// 1. Determine and retrieve/generate the relevant F-atoms.

		// TODO
		// For the given concept and languages, retrieve Fufo, Flng, Fsem, Hmem
		// Get the Hmem siblings for these atoms and retrieve their F-atoms
		// (update `concepts`!)
		
		// Retrieving languages from the tree to get proto languages as well.
		for (String lang : tree.getAllLanguages()) {
			List<CLDFForm> cldfForms = wordListDb.getFormsForLanguage(ISO2LangID.getOrDefault(lang, lang));
			if (cldfForms == null || cldfForms.isEmpty()) {
				// Proto language
				cldfForms = new ArrayList<CLDFForm>();
				for (String concept : concepts) {
					CLDFForm form = new CLDFForm();
					form.setParamID(concept);
					cldfForms.add(form);
				}
			}
			for (CLDFForm cldfForm : cldfForms) {
				if (concepts.contains(cldfForm.getParamID())) {
					entryPool.add(new Entry(getPrintForm(cldfForm, lang), cldfForm, lang, cldfForm.getParamID()));
					// TODO only add these for proto languages that don't have
					// these yet
					// retrieve existing F-atoms from db and pass them on
					addFormAtoms(getPrintForm(cldfForm, lang), lang, cldfForm.getParamID(), cldfForm);
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
				if (tree.distanceToAncestor(lang1, lang2) == 1) {
					pslProblem.addObservation("Tanc", 1.0, lang1, lang2);
				} else if (!lang1.equals(lang2) && tree.getLevel(lang1) == tree.getLevel(lang2)) {
					// TODO: borrowing from e.g. Latin
					// TODO: geographical distance etc.
					// TODO: make this open instead? e.g.
					// pslProblem.addTarget
					pslProblem.addObservation("Tcnt", 1.0, lang1, lang2);
				}
			}
		}

		// 4. Generate etymology atoms.

		for (Entry entry1 : entryPool) {
			pslProblem.addTarget("Eunk", entry1.id);
			for (Entry entry2 : entryPool) {
				addAtomsForFormPair(entry1, entry2);
			}
		}
	}

	private void addAtomsForFormPair(Entry entry1, Entry entry2) {
		if (entry1.id.equals(entry2.id) || (entry1.id.length() == 0 && entry2.id.length() == 0)) {
			return;
		}

		PhoneticString form1 = phonSimHelper.extractSegments(entry1.form);
		PhoneticString form2 = phonSimHelper.extractSegments(entry2.form);

		// TODO what if the language is identical? return;?
		if (tree.distanceToAncestor(entry1.language, entry2.language) == 1) {
			pslProblem.addTarget("Einh", entry1.id, entry2.id);
			pslProblem.addObservation("Eloa", 0.0, entry1.id, entry2.id);
		} else if (tree.getLevel(entry1.language) == tree.getLevel(entry2.language)) {
			pslProblem.addTarget("Eloa", entry1.id, entry2.id);
			pslProblem.addObservation("Einh", 0.0, entry1.id, entry2.id);
		} else {
			pslProblem.addObservation("Einh", 0.0, entry1.id, entry2.id);
			pslProblem.addObservation("Eloa", 0.0, entry1.id, entry2.id);
		}

		String ipa1 = getIpa(entry1.form);
		if (ipa1.isEmpty()) {
			return;
		}

		String ipa2 = getIpa(entry2.form);
		if (ipa2.isEmpty()) {
			return;
		}
		double sim = phonSimHelper.similarity(form1, form2);
		pslProblem.addObservation("Fsim", sim, ipa1, ipa2);
		System.out.println("Fsim(" + entry1.id + "/" + ipa1 + "/" + form1 + "," + entry2.id + "/" + ipa2 + "/" + form2
				+ ") " + sim);

	}

	private void addFormAtoms(String id, String doculect, String concept, CLDFForm cldfForm) {
		pslProblem.addObservation("Flng", 1.0, id, doculect);
		pslProblem.addObservation("Fsem", 1.0, id, concept);
		String ipa = getIpa(cldfForm);
		if (!ipa.isEmpty()) {
			// TODO add XFufo also for imported Fufo atoms!!
			pslProblem.addObservation(F_UFO_EX, 1.0, id);
			pslProblem.addObservation("Fufo", 1.0, id, ipa);
		}
	}

	private String getPrintForm(CLDFForm form, String lang) {
		String ipa = getIpa(form);
		if (ipa.isEmpty()) {
			ipa = "N/A";
		}
		return lang + ":" + form.getProperties().get("Orthography") + ":" + ipa + ":" + form.getParamID();
	}

	private String getIpa(CLDFForm form) {
		return String.join("", form.getSegments());
	}

}
