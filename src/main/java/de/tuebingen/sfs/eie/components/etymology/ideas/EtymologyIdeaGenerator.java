package de.tuebingen.sfs.eie.components.etymology.ideas;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import de.tuebingen.sfs.eie.components.etymology.problems.EtymologyProblem;
import de.tuebingen.sfs.eie.components.etymology.problems.EtymologyProblemConfig;
import de.tuebingen.sfs.eie.shared.core.EtymologicalTheory;
import de.tuebingen.sfs.eie.shared.core.IndexedObjectStore;
import de.tuebingen.sfs.eie.shared.core.LanguagePhylogeny;
import de.tuebingen.sfs.eie.shared.util.Pair;
import de.tuebingen.sfs.eie.shared.util.PhoneticSimilarityHelper;
import de.tuebingen.sfs.psl.engine.IdeaGenerator;
import de.tuebingen.sfs.psl.util.data.Multimap;
import de.tuebingen.sfs.psl.util.data.Multimap.CollectionType;
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
		IndexedObjectStore objectStore = theory.getIndexedObjectStore();
		LanguagePhylogeny phylo = theory.getLanguagePhylogeny();

		Stack<String> langStack = new Stack<>();
		Set<String> conceptsAdded = new HashSet<>();

		Multimap<String, Integer> langsToForms = new Multimap<>(CollectionType.SET);

		for (int formId : config.getFormIds()) {
			String lang = objectStore.getLangForForm(formId);
			langStack.add(lang);
			langsToForms.put(lang, formId);
			List<String> concepts = objectStore.getConceptsForForm(formId);
			conceptsAdded.addAll(concepts);
		}
		String concept = conceptsAdded.iterator().next(); // TODO

		// Language atoms
		Set<String> langsAdded = new HashSet<>();
		List<Integer> allForms = new ArrayList<>();
		String anc = phylo.lowestCommonAncestor(langStack);
		if (anc.equals(LanguagePhylogeny.root)) {
			// TODO if there are contact links: go up to oldest contact
			// if not: treat as multiple intra-family inferences? warn user?
		} else {
			// TODO message about skipped family members?
			while (!langStack.isEmpty()) {
				String lang = langStack.pop();
				if (langsAdded.contains(lang)) {
					continue;
				}
				langsAdded.add(lang);
				String parent = phylo.parents.get(lang);
				pslProblem.addObservation("Tanc", 1.0, lang, parent);
				if (langsToForms.containsKey(lang)) {
					allForms.addAll(langsToForms.get(lang));
				} else {
					Set<Integer> forms = objectStore.getFormsForLangAndConcepts(lang, concept);
					if (forms == null) {
						int formId = objectStore.createFormId();
						objectStore.addFormIdWithConcept(formId, concept);
						objectStore.addFormIdWithLanguage(formId, lang);
						langsToForms.put(lang, formId);
					} else {
						langsToForms.putAll(lang, forms);
						allForms.addAll(forms);
					}
				}
				if (!parent.equals(anc) && !langsAdded.contains(parent)) {
					langStack.push(parent);
				}
			}
		}
		for (String lang : langsAdded) {
			if (phylo.hasIncomingInfluences(lang)) {
				for (String contact : phylo.getIncomingInfluences(lang)) {
					if (langsAdded.contains(contact)) {
						pslProblem.addObservation("Tcnt", 1.0, lang, contact);
					}
				}
			}
		}

		PhoneticSimilarityHelper phonSim = new PhoneticSimilarityHelper(objectStore.getCorrModel(), theory);

		// Form atoms
		for (String lang : langsToForms.keySet()) {
			for (int formId : langsToForms.get(lang)) {
				pslProblem.addObservation("Flng", 1.0, formId + "", lang);
				for (String conc : objectStore.getConceptsForForm(formId)) {
					pslProblem.addObservation("Fsem", 1.0, formId + "", conc);
				}

				boolean xfufo = objectStore.hasUnderlyingForm(formId);
				if (xfufo) {
					pslProblem.addObservation("XFufo", 1.0, formId + "");
				}

				pslProblem.addTarget("Eunk", formId + "");
				if (phylo.hasIncomingInfluences(lang)) {
					for (String contact : phylo.getIncomingInfluences(lang)) {
						if (langsToForms.containsKey(contact)) {
							for (int contactFormId : langsToForms.get(contact)) {
								pslProblem.addTarget("Eloa", formId + "", contactFormId + "");
							}
						}
					}
				}
				String parent = phylo.parents.get(lang);
				if (langsToForms.containsKey(parent)) {
					for (int parentFormId : langsToForms.get(parent)) {
						pslProblem.addTarget("Einh", formId + "", parentFormId + "");
					}
				}
			}
		}

		for (int i = 0; i < allForms.size() - 1; i++) {
			int formIdI = allForms.get(i);
			for (int j = i + 1; j < allForms.size(); j++) {
				int formIdJ = allForms.get(j);
				double fSim = phonSim.similarity(formIdI, formIdJ);
				pslProblem.addObservation("Fsim", fSim, formIdI + "", formIdJ + "");
				pslProblem.addObservation("Fsim", fSim, formIdJ + "", formIdI + "");
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

	private class EtymologyEntry {
		int formId;
		List<String> concepts;
		String language;

		EtymologyEntry(int formId, List<String> concepts, String language) {
			this.formId = formId;
			this.concepts = concepts;
			this.language = language;
		}
	}

}
