package de.tuebingen.sfs.eie.components.etymology.ideas;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import de.tuebingen.sfs.eie.components.etymology.problems.EtymologyProblem;
import de.tuebingen.sfs.eie.components.etymology.problems.EtymologyProblemConfig;
import de.tuebingen.sfs.eie.shared.core.EtymologicalTheory;
import de.tuebingen.sfs.eie.shared.core.IndexedObjectStore;
import de.tuebingen.sfs.eie.shared.core.LanguagePhylogeny;
import de.tuebingen.sfs.eie.shared.talk.EtinenConstantRenderer;
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
		config = (EtymologyProblemConfig) problem.getConfig();
		if (PRINT_LOG) {
			config.logSettings();
		}

		logger.displayln("Finished setting up the Etymology Idea Generator.");
	}

	public void generateAtoms(EtinenConstantRenderer renderer) {
		// TODO warn user about missing homologue members if applicable?

		IndexedObjectStore objectStore = theory.getIndexedObjectStore();
		LanguagePhylogeny phylo = theory.getLanguagePhylogeny();

		Stack<String> langStack = new Stack<>();
		Multimap<String, Integer> langsToForms = new Multimap<>(CollectionType.SET);
		Set<Integer> homPegs = new HashSet<>();
		for (int formId : config.getFormIds()) {
			String lang = objectStore.getLangForForm(formId);
			langStack.add(lang);
			langsToForms.put(lang, formId);
			homPegs.add(objectStore.getPegOrSingletonForFormId(formId));
		}

		// Language atoms
		Set<String> langsAdded = new HashSet<>();
		String anc = phylo.lowestCommonAncestor(langStack);
		if (anc.equals(LanguagePhylogeny.root)) {
			// If the selection contains languages from multiple different families,
			// add languages + word forms from up to the earliest established contact.
			// TODO warn user if there are no relevant contacts
			Multimap<String, String> familyAncestorToLangs = new Multimap<>(CollectionType.SET);
			for (String lang : langStack) {
				familyAncestorToLangs.put(phylo.getPathFor(lang).get(0), lang);
			}
			for (Collection<String> relatedLangs : familyAncestorToLangs.values()) {
				langStack.clear();
				langStack.addAll(relatedLangs);
				anc = phylo.lowestCommonAncestor(langStack);
				addLanguageFamily(phylo, objectStore, renderer, homPegs, anc, langStack, langsAdded, langsToForms);
			}
		} else {
			// If there is a (non-root) common ancestor,
			// add languages + word forms up to the lowest common ancestor.
			addLanguageFamily(phylo, objectStore, renderer, homPegs, anc, langStack, langsAdded, langsToForms);
		}
		for (String lang : langsAdded) {
			if (phylo.hasIncomingInfluences(lang)) {
				for (String contact : phylo.getIncomingInfluences(lang)) {
					if (langsAdded.contains(contact)) {
						pslProblem.addObservation("Xloa", 1.0, lang, contact);
					}
				}
			}
		}

		// Form atoms
		// TODO check EtymologicalTheory to see if confirm Einh/Eloa/Eety belief values
		// from previous inferences can be used here
		PhoneticSimilarityHelper phonSim = new PhoneticSimilarityHelper(objectStore.getCorrModel(), theory);
		for (String lang : langsToForms.keySet()) {
			for (int formId : langsToForms.get(lang)) {
				pslProblem.addTarget("Eunk", formId + "");

				if (phylo.hasIncomingInfluences(lang)) {
					for (String contact : phylo.getIncomingInfluences(lang)) {
						if (langsToForms.containsKey(contact)) {
							for (int contactFormId : langsToForms.get(contact)) {
								pslProblem.addObservation("Xloa", 1.0, formId + "", contactFormId + "");
								pslProblem.addTarget("Eloa", formId + "", contactFormId + "");
							}
						}
					}
				}
				String parent = phylo.parents.get(lang);
				if (langsToForms.containsKey(parent)) {
					for (int parentFormId : langsToForms.get(parent)) {
						pslProblem.addObservation("Xinh", 1.0, formId + "", parentFormId + "");
						pslProblem.addTarget("Einh", formId + "", parentFormId + "");
					}
				}
			}
		}

		List<Integer> allForms = new ArrayList<>();
		langsToForms.values().forEach(allForms::addAll);
		for (int i = 0; i < allForms.size() - 1; i++) {
			int formIdI = allForms.get(i);
			addHomsetInfo(objectStore, formIdI, homPegs);

			// Compare phonetic forms.
			boolean hasUnderlyingForm1 = objectStore.hasUnderlyingForm(formIdI);
			for (int j = i + 1; j < allForms.size(); j++) {
				int formIdJ = allForms.get(j);
				if (!hasUnderlyingForm1 || !objectStore.hasUnderlyingForm(formIdJ)) {
					pslProblem.addTarget("Fsim", formIdI + "", formIdJ + "");
					pslProblem.addTarget("Fsim", formIdJ + "", formIdI + "");
				} else {
					double fSim = phonSim.similarity(formIdI, formIdJ);
					pslProblem.addObservation("Fsim", fSim, formIdI + "", formIdJ + "");
					pslProblem.addObservation("Fsim", fSim, formIdJ + "", formIdI + "");
				}
			}
		}
		addHomsetInfo(objectStore, allForms.get(allForms.size() - 1), homPegs);

		for (int form : allForms) {
			System.err.println("Form: " + form + " " + objectStore.getLangForForm(form) + " "
					+ objectStore.getRawFormForFormId(form));
		}
		for (int form : homPegs) {
			System.err.println("Peg: " + form + " " + objectStore.getLangForForm(form) + " "
					+ objectStore.getRawFormForFormId(form));
		}

		if (PRINT_LOG) {
			super.pslProblem.printAtomsToConsole();
		}
	}

	private void addLanguageFamily(LanguagePhylogeny phylo, IndexedObjectStore objectStore,
			EtinenConstantRenderer renderer, Set<Integer> homPegs, String lowestCommonAnc, Stack<String> langStack,
			Set<String> langsAdded, Multimap<String, Integer> langsToForms) {
		logger.displayln(
				"Adding languages descended from " + renderer.getLanguageRepresentation(lowestCommonAnc) + ":");
		while (!langStack.isEmpty()) {
			String lang = langStack.pop();
			if (langsAdded.contains(lang)) {
				continue;
			}
			logger.displayln("- " + renderer.getLanguageRepresentation(lang));
			langsAdded.add(lang);
			String parent = phylo.parents.get(lang);
			pslProblem.addObservation("Xinh", 1.0, lang, parent);
			Collection<Integer> forms = langsToForms.get(lang);
			if (forms == null) {
				// Deal with (not-yet-reconstructed) protoforms
				boolean foundForm = false;
				for (int homPeg : homPegs) {
					// Any confirmed reconstructions we can work with?
					int recForm = objectStore.getReconstructionIdForPegAndLang(homPeg, lang);
					if (recForm != -1) {
						langsToForms.put(lang, recForm);
						foundForm = true;
					}
				}
				if (!foundForm) {
					// Create a dummy form
					int formId = objectStore.createFormId();
					objectStore.addFormIdWithLanguage(formId, lang);
					langsToForms.put(lang, formId);
				}
			}
			if (!parent.equals(lowestCommonAnc) && !langsAdded.contains(parent)) {
				langStack.push(parent);
			}
		}
	}

	private void addHomsetInfo(IndexedObjectStore objectStore, int formId, Set<Integer> homPegs) {
		int pegForForm = objectStore.getPegForFormIdIfRegistered(formId);
		System.err.println("PEG: " + formId + " " + objectStore.getLangForForm(formId) + " " + pegForForm);
		if (pegForForm < 0) {
			for (int homPeg : homPegs) {
				pslProblem.addTarget("Fhom", formId + "", homPeg + "");
				System.err.println("Fhom(" + objectStore.getLangForForm(formId) + ", "
						+ objectStore.getRawFormForFormId(homPeg) + ")");
			}
		} else {
			for (Integer homPeg : homPegs) {
				if (homPeg.equals(pegForForm)) {
					pslProblem.addObservation("Fhom", 1.0, formId + "", homPeg + "");
					System.err.println("Fhom(" + objectStore.getLangForForm(formId) + ", "
							+ objectStore.getRawFormForFormId(homPeg) + ") 1.0");
				} else {
					pslProblem.addObservation("Fhom", 0.0, formId + "", homPeg + "");
					System.err.println("Fhom(" + objectStore.getLangForForm(formId) + ", "
							+ objectStore.getRawFormForFormId(homPeg) + ") 0.0");
				}
			}
		}
	}

}
