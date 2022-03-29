package de.tuebingen.sfs.eie.components.etymology.ideas;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.tuebingen.sfs.eie.components.etymology.problems.EtymologyProblem;
import de.tuebingen.sfs.eie.components.etymology.problems.EtymologyProblemConfig;
import de.tuebingen.sfs.eie.shared.core.EtymologicalTheory;
import de.tuebingen.sfs.eie.shared.core.IndexedObjectStore;
import de.tuebingen.sfs.eie.shared.util.Pair;
import de.tuebingen.sfs.eie.shared.util.PhoneticSimilarityHelper;
import de.tuebingen.sfs.psl.engine.IdeaGenerator;
import de.tuebingen.sfs.psl.util.log.InferenceLogger;

public class EtymologyIdeaGenerator extends IdeaGenerator {

	public static boolean PRINT_LOG = true;

	private EtymologicalTheory theory;
	private EtymologyProblemConfig config;
	private InferenceLogger logger;

	public EtymologyIdeaGenerator(EtymologyProblem problem, EtymologicalTheory theory) {
		super(problem);
		this.theory = theory;
		this.logger = problem.getLogger();
		logger.displayln("...Creating EtymologyIdeaGenerator.");
		logger.displayln("...Working with the following idea generation configuration:");
		// TODO:
		config = (EtymologyProblemConfig) problem.getConfig();
		if (PRINT_LOG) {
			config.logSettings();
		}

		logger.displayln("Finished setting up the Etymology Idea Generator.");
	}

	public void generateAtoms() {
		// Current assumption:
		// - all relevant forms are selected, including protoforms!
		// - this also includes all relevant protolanguages

		IndexedObjectStore objectStore = theory.getIndexedObjectStore();
		Set<String> concepts = new HashSet<>();
		Set<String> langsAdded = new HashSet<>();
		Set<Pair<Integer, String>> formsAndLangs = new HashSet<>();

		// 1. Atoms about the word forms.
		for (int formId : config.getFormIds()) {
			String lang = objectStore.getLangForForm(formId);
			pslProblem.addObservation("Flng", 1.0, formId + "", lang);
			formsAndLangs.add(new Pair<Integer, String>(formId, lang));
			langsAdded.add(lang);
			for (String concept : objectStore.getConceptsForForm(formId)) {
				pslProblem.addObservation("Fsem", 1.0, formId + "", concept);
				concepts.add(concept);
			}
			pslProblem.addObservation("XFufo", 1.0, formId + "");
			pslProblem.addTarget("Eunk", formId + "");
		}
		// TODO relevant (non-selected) forms of proto languages

		// 2. Semantic similarity atoms.
		List<String> conceptList = new ArrayList<>(concepts);
		for (int i = 0; i < conceptList.size() - 1; i++) {
			String concept1 = conceptList.get(i);
			for (int j = i; j < conceptList.size(); j++) {
				String concept2 = conceptList.get(j);
				pslProblem.addObservation("Ssim", getSemanticSimilarity(concept1, concept2), concept1, concept2);
			}
		}

		// 3. Language ancestry/contact atoms.
		for (String lang : langsAdded) {
			for (String contact : theory.getLanguagePhylogeny().getIncomingInfluences(lang)) {
				if (langsAdded.contains(contact)) {
					pslProblem.addObservation("Tcnt", 1.0, lang, contact);
				}
			}
			String parent = theory.getLanguagePhylogeny().parents.get(lang);
			if (langsAdded.contains(parent)) {
				pslProblem.addObservation("Tanc", 1.0, lang, parent);
			}
		}

		PhoneticSimilarityHelper phonSim = new PhoneticSimilarityHelper(theory.getIndexedObjectStore().getCorrModel(),
				theory);

		for (Pair<Integer, String> formAndLang1 : formsAndLangs) {
			for (Pair<Integer, String> formAndLang2 : formsAndLangs) {
				if (formAndLang1.equals(formAndLang2)) {
					continue;
				}

				double fSim = phonSim.similarity(formAndLang1.first, formAndLang2.first);
				pslProblem.addObservation("Fsim", fSim, formAndLang1.first + "", formAndLang2.first + "");
				pslProblem.addObservation("Fsim", fSim, formAndLang2.first + "", formAndLang1.first + "");

				if (theory.getLanguagePhylogeny().parents.get(formAndLang1.second).equals(formAndLang2.second)) {
					pslProblem.addTarget("Einh", formAndLang1.first + "", formAndLang2.first + "");
				} else if (theory.getLanguagePhylogeny().parents.get(formAndLang2.second).equals(formAndLang1.second)) {
					pslProblem.addTarget("Einh", formAndLang2.first + "", formAndLang1.first + "");
				}
				// allow cross-temporal borrowing (?)
				if (theory.getLanguagePhylogeny().getIncomingInfluences(formAndLang1.second)
						.contains(formAndLang2.second)) {
					pslProblem.addTarget("Eloa", formAndLang1.first + "", formAndLang2.first + "");
				}
				if (theory.getLanguagePhylogeny().getIncomingInfluences(formAndLang2.second)
						.contains(formAndLang1.second)) {
					pslProblem.addTarget("Eloa", formAndLang2.first + "", formAndLang1.first + "");
				}
			}
		}

		if (PRINT_LOG) {
			super.pslProblem.printAtomsToConsole();
		}
	}

	private double getSemanticSimilarity(String concept1, String concept2) {
		if (concept1.equals(concept2)) {
			return 1.0;
		}
		if (config.getMaxSemEdgeDist() == 0) {
			return 0.0;
		}
		int nestingLvl = 0;
		boolean found = false;
		List<String> compareNow = new ArrayList<>();
		List<String> compareNext = new ArrayList<>();
		compareNext.add(concept1);
		while (nestingLvl < config.getMaxSemEdgeDist()) {
			compareNow.addAll(compareNext);
			compareNext.clear();
			nestingLvl++;

			for (String c1 : compareNow) {
				for (String c2 : theory.getIndexedObjectStore().getConceptConnections(c1)) {
					if (c2.equals(concept2)) {
						found = true;
						break;
					}
					compareNext.add(c2);
				}
			}
		}

		if (!found) {
			return 0.0;
		}

		// TODO rethink the dist --> sim conversion
		return 1.0 / nestingLvl;
	}

}
