package de.tuebingen.sfs.eie.components.etymology.problems;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.linqs.psl.model.rule.GroundRule;

import de.jdellert.iwsa.sequence.PhoneticString;
import de.jdellert.iwsa.util.phonsim.PhoneticSimilarityHelper;
import de.tuebingen.sfs.cldfjava.data.CLDFForm;
import de.tuebingen.sfs.cldfjava.data.CLDFLanguage;
import de.tuebingen.sfs.cldfjava.data.CLDFParameter;
import de.tuebingen.sfs.cldfjava.data.CLDFWordlistDatabase;
import de.tuebingen.sfs.eie.components.etymology.filter.EtymologyRagFilter;
import de.tuebingen.sfs.eie.components.etymology.talk.rule.EetyToFsimRule;
import de.tuebingen.sfs.eie.components.etymology.talk.rule.EinhOrEloaOrEunkRule;
import de.tuebingen.sfs.eie.components.etymology.talk.rule.EloaPriorRule;
import de.tuebingen.sfs.eie.components.etymology.talk.rule.FsimAndSsimToEetyRule;
import de.tuebingen.sfs.eie.components.etymology.talk.rule.TancToEinhRule;
import de.tuebingen.sfs.eie.components.etymology.talk.rule.TcntToEloaRule;
import de.tuebingen.sfs.eie.components.etymology.util.LevelBasedPhylogeny;
import de.tuebingen.sfs.psl.engine.AtomTemplate;
import de.tuebingen.sfs.psl.engine.DatabaseManager;
import de.tuebingen.sfs.psl.engine.InferenceResult;
import de.tuebingen.sfs.psl.engine.PslProblem;
import de.tuebingen.sfs.psl.engine.RagFilter;
import de.tuebingen.sfs.psl.engine.RuleAtomGraph;
import de.tuebingen.sfs.psl.talk.TalkingLogicalRule;
import de.tuebingen.sfs.util.SemanticNetwork;

public class EtymologyProblem extends PslProblem {

	CLDFWordlistDatabase wordListDb;
	SemanticNetwork semanticNet;
	PhoneticSimilarityHelper phonSimHelper;
	LevelBasedPhylogeny phylogeny;
	Map<String, String> ISO2LangID;

	private static final String F_UFO_EX = PslProblem.existentialAtomName("Fufo");

	public EtymologyProblem(DatabaseManager dbManager, String name, CLDFWordlistDatabase wordListDb,
			PhoneticSimilarityHelper phonSimHelper, SemanticNetwork semanticNet, LevelBasedPhylogeny phylogeny) {
		super(dbManager, name);
		this.wordListDb = wordListDb;
		this.phonSimHelper = phonSimHelper;
		this.semanticNet = semanticNet;
		this.phylogeny = phylogeny;

		ISO2LangID = new HashMap<>();
		for (CLDFLanguage lang : wordListDb.getAllLanguages()) {
			ISO2LangID.put(lang.getIso639P3code(), lang.getLangID());
		}
	}

	@Override
	public void declarePredicates() {
		// Information about the forms
		// Flng(ID, language)
		declareClosedPredicate("Flng", 2);
		declareClosedPredicate("Fufo", 2);
		declareClosedPredicate("Fsem", 2);
		declareClosedPredicate(F_UFO_EX, 1);

		// Similarity measures
		declareClosedPredicate("Fsim", 2);
		declareClosedPredicate("Ssim", 2);

		// Etymological information
		// Eety(ID1, ID2) -- ID1 comes from ID2
		declareOpenPredicate("Einh", 2);
		declareOpenPredicate("Eloa", 2);
		declareOpenPredicate("Eunk", 1);

		// Phylogenetic information.
		declareClosedPredicate("Tanc", 2);
		declareClosedPredicate("Tcnt", 2);
	}

	@Override
	public void pregenerateAtoms() {
	}

	@Override
	public void addInteractionRules() {
		addRule(new EinhOrEloaOrEunkRule(this));
		addRule(new TalkingLogicalRule("EunkPrior", "6: ~Eunk(X)", this,
				"By default, we do not assume that words are of unknown origin."));
		addRule(new EloaPriorRule(this, 2.0));

		addRule(new TancToEinhRule(this, 1.0));
		addRule(new TcntToEloaRule(this, 1.0));

		addRule(new EetyToFsimRule("Einh", "Einh", this, 5.0));
		addRule(new EetyToFsimRule("Einh", "Eloa", this, 5.0));
		addRule(new EetyToFsimRule("Eloa", "Einh", this, 5.0));
		addRule(new EetyToFsimRule("Eloa", "Eloa", this, 5.0));

		addRule(new FsimAndSsimToEetyRule("Einh", this, 8.0));
		addRule(new FsimAndSsimToEetyRule("Eloa", this, 8.0));

		// addRule(new TalkingLogicalRule("NotFsimToNotEety",
		// "3: Fufo(X, F1) & Fufo(Y, F2) & ~Fsim(F1, F2) -> ~Eety(X, Y)", this,
		// "Dissimilar forms are probably derived from different sources."));
	}

	// TODO make concepts a constructor arg (vbl)
	public void generateDataAtoms(Set<String> testConcepts) {
		for (String concept1 : testConcepts) {
			for (String concept2 : testConcepts) {
				addObservation("Ssim", semanticNet.getSimilarity(concept1, concept2), concept1, concept2);
			}
		}

		// align all word pairs with the same meaning
		for (CLDFParameter concept : wordListDb.getAllConcepts()) {
			String paramID = concept.getParamID();
			if (!testConcepts.contains(paramID)) {
				continue;
			}
			System.err.println("Generating atoms for concept " + concept.getName());

			List<String> allLangs = phylogeny.getAllLanguages();
			for (String lang1 : allLangs) {
				List<CLDFForm> lang1Forms = wordListDb.getForms(ISO2LangID.getOrDefault(lang1, lang1), paramID);

				// Proto language
				if (lang1Forms.isEmpty()) {
					lang1Forms = new ArrayList<CLDFForm>();
					CLDFForm form = new CLDFForm();
					form.setParamID(concept.getParamID());
					lang1Forms.add(form);
				}

				for (String lang2 : allLangs) {
					List<CLDFForm> lang2Forms = wordListDb.getFormsForLanguage(ISO2LangID.getOrDefault(lang2, lang2));

					if (phylogeny.distanceToAncestor(lang1, lang2) == 1) {
						addObservation("Tanc", 1.0, lang1, lang2);
					} else if (!lang1.equals(lang2) && phylogeny.getLevel(lang1) == phylogeny.getLevel(lang2)) {
						// TODO: borrowing from e.g. Latin
						// TODO: geographical distance etc.
						// TODO: make this open instead? e.g. addTarget
						addObservation("Tcnt", 1.0, lang1, lang2);
					}

					// Proto language
					if (lang2Forms.isEmpty()) {
						lang2Forms = new ArrayList<CLDFForm>();
						for (String testConcept : testConcepts) {
							CLDFForm form = new CLDFForm();
							form.setParamID(testConcept);
							lang2Forms.add(form);
						}
					}

					for (CLDFForm lang1Form : lang1Forms) {
						PhoneticString form1 = phonSimHelper.extractSegments(lang1Form);
						String id1 = getPrintForm(lang1Form, lang1);
						addFormAtoms(id1, lang1, paramID, lang1Form);
						addTarget("Eunk", id1);

						for (CLDFForm lang2Form : lang2Forms) {
							if (!testConcepts.contains(lang2Form.getParamID())) {
								continue;
							}
							addAtoms(form1, id1, lang1, lang2, concept, lang1Form, lang2Form);
						}

					}
				}
			}
		}
	}

	private void addAtoms(PhoneticString form1, String id1, String lang1, String lang2, CLDFParameter concept,
			CLDFForm lang1Form, CLDFForm lang2Form) {
		PhoneticString form2 = phonSimHelper.extractSegments(lang2Form);
		String id2 = getPrintForm(lang2Form, lang2);

		if (id1.equals(id2) || (id1.length() == 0 && id2.length() == 0)) {
			return;
		}

		double ssim = semanticNet.getSimilarity(concept.getParamID(), lang2Form.getParamID());
		if (ssim < 0.01) {
			// TODO check rules. can we get rid of these atoms?
			// e.g. SsimToEinh
			addObservation("Einh", 0.0, id1, id2);
			addObservation("Eloa", 0.0, id1, id2);
			return;
		}

		if (phylogeny.distanceToAncestor(lang1, lang2) == 1) {
			addTarget("Einh", id1, id2);
			addObservation("Eloa", 0.0, id1, id2);
		} else if (phylogeny.getLevel(lang1) == phylogeny.getLevel(lang2)) {
			addTarget("Eloa", id1, id2);
			addObservation("Einh", 0.0, id1, id2);
		} else {
			addObservation("Einh", 0.0, id1, id2);
			addObservation("Eloa", 0.0, id1, id2);
		}

		String ipa1 = getIpa(lang1Form);
		if (ipa1.isEmpty()) {
			return;
		}

		String ipa2 = getIpa(lang2Form);
		if (ipa2.isEmpty()) {
			return;
		}
		double sim = phonSimHelper.similarity(form1, form2);
		addObservation("Fsim", sim, ipa1, ipa2);
		System.out
				.println("Fsim(" + id1 + "/" + ipa1 + "/" + form1 + "," + id2 + "/" + ipa2 + "/" + form2 + ") " + sim);

	}

	private void addFormAtoms(String id, String doculect, String concept, CLDFForm cldfForm) {
		addObservation("Flng", 1.0, id, doculect);
		addObservation("Fsem", 1.0, id, concept);
		String ipa = getIpa(cldfForm);
		if (!ipa.isEmpty()) {
			addObservation(F_UFO_EX, 1.0, id);
			addObservation("Fufo", 1.0, id, ipa);
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

	@Override
	public Set<AtomTemplate> declareAtomsForCleanUp() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public InferenceResult call() throws Exception {
		addInteractionRules();
		List<List<GroundRule>> groundRules = runInference(true);
		RuleAtomGraph.GROUNDING_OUTPUT = true;
		RuleAtomGraph.ATOM_VALUE_OUTPUT = true;
//		Map<String, Double> valueMap = extractResult();
		Map<String, Double> valueMap = extractResult(false);
		RuleAtomGraph rag = new RuleAtomGraph(this, new EtymologyRagFilter(valueMap), groundRules);
		return new InferenceResult(rag, valueMap);
	}

}
