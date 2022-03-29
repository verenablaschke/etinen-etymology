package de.tuebingen.sfs.eie.components.etymology.ideas;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import de.tuebingen.sfs.eie.components.etymology.problems.EtymologyProblem;
import de.tuebingen.sfs.eie.components.etymology.problems.EtymologyProblemConfig;
import de.tuebingen.sfs.eie.shared.core.EtymologicalTheory;
import de.tuebingen.sfs.eie.shared.core.IndexedObjectStore;
import de.tuebingen.sfs.psl.engine.IdeaGenerator;
import de.tuebingen.sfs.psl.util.log.InferenceLogger;

public class EtymologyIdeaGenerator extends IdeaGenerator {

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
//		config = problem.getConfig();
//		config.logSettings();

		logger.displayln("Finished setting up the Etymology Idea Generator.");
	}

	public void generateAtoms() {
		IndexedObjectStore objectStore = theory.getIndexedObjectStore();
		Set<String> concepts = new HashSet<>();
		Stack<String> langs = new Stack<>();

		// 1. Atoms about the word forms.
		for (int formId : config.getFormIds()) {
			String lang = objectStore.getLangForForm(formId);
			pslProblem.addObservation("Flng", 1.0, formId + "", lang);
			langs.push(lang);
			for (String concept : objectStore.getConceptsForForm(formId)) {
				pslProblem.addObservation("Fsem", 1.0, formId + "", concept);
				concepts.add(concept);
			}
			pslProblem.addObservation("XFufo", 1.0, formId + "");
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
		while (!langs.isEmpty()) {
			String lang = langs.pop();
			for (String contact : theory.getLanguagePhylogeny().getIncomingInfluences(lang)) {
				// TODO only add contact languages with forms in the input data!
				pslProblem.addObservation("Tcnt", 1.0, lang, contact);
			}
			String parent = theory.getLanguagePhylogeny().parents.get(lang);
			langs.push(parent); // TODO determine when to stop
			pslProblem.addObservation("Tanc", 1.0, lang, parent);
			// TODO:
//			for (String formId : theory.getIndexedObjectStore().getFormsForLangAndConcepts(parent, concept)) {
//				pslProblem.addObservation("Flng", 1.0, formId + "", parent);
//				pslProblem.addObservation("Fsem", 1.0, formId + "", concept);
//				pslProblem.addObservation("XFufo", 1.0, formId + "");
//			}
		}

		// TODO:
		// 4. Generate etymology atoms.
//		for (Entry entry1 : entryPool) {
//			pslProblem.addTarget("Eunk", entry1.formIdAsString);
//			for (Entry entry2 : entryPool) {
//				addAtomsForFormPair(entry1, entry2);
//			}
//		}
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
