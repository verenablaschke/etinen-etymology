package de.tuebingen.sfs.eie.components.etymology.problems;

//TODO currently, this only infers 1.0 everywhere (vbl)
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.linqs.psl.model.rule.GroundRule;

import de.tuebingen.sfs.eie.components.cognacy.PhoneticSimilarityHelper;
import de.tuebingen.sfs.eie.components.etymology.util.LevelBasedPhylogeny;
import de.tuebingen.sfs.eie.components.lexdata.CLDFForm;
import de.tuebingen.sfs.eie.components.lexdata.CLDFLanguage;
import de.tuebingen.sfs.eie.components.lexdata.CLDFParameter;
import de.tuebingen.sfs.eie.components.lexdata.CLDFWordlistDatabase;
import de.tuebingen.sfs.iwsa.sequence.PhoneticString;
import de.tuebingen.sfs.psl.engine.AtomTemplate;
import de.tuebingen.sfs.psl.engine.DatabaseManager;
import de.tuebingen.sfs.psl.engine.InferenceResult;
import de.tuebingen.sfs.psl.engine.PslProblem;
import de.tuebingen.sfs.psl.engine.RagFilter;
import de.tuebingen.sfs.psl.engine.RuleAtomGraph;
import de.tuebingen.sfs.psl.talk.TalkingArithmeticRule;
import de.tuebingen.sfs.psl.talk.TalkingLogicalRule;
import de.tuebingen.sfs.util.SemanticNetwork;

public class EtymologyProblem extends PslProblem {

	CLDFWordlistDatabase wordListDb;
	SemanticNetwork semanticNet;
	PhoneticSimilarityHelper phonSimHelper;
	LevelBasedPhylogeny phylogeny;
	Map<String, String> ISO2LangID;

	public EtymologyProblem(DatabaseManager dbManager, String name, CLDFWordlistDatabase wordListDb, 
			PhoneticSimilarityHelper phonSimHelper, SemanticNetwork semanticNet,
			LevelBasedPhylogeny phylogeny) {
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
		declarePredicate("Flng", 2);
		declarePredicate("Fufo", 2);
		declarePredicate("Fsem", 2);

		// Similarity measures
		declarePredicate("Fsim", 2);
		declarePredicate("Ssim", 2);

		// Etymological information
		// Eety(ID1, ID2) -- ID1 comes from ID2
		declarePredicate("Eety", 2);
		declarePredicate("Einh", 2);
		declarePredicate("Eloa", 2);
		declarePredicate("Eunk", 1);

		// Phylogenetic information.
		declarePredicate("Tanc", 2);
		declarePredicate("Tcnt", 2);
	}

	@Override
	public void pregenerateAtoms() {
	}

	@Override
	public void addInteractionRules() {
		// Setting up Eety/Einh/Eloa/Eunk.
		addRule(new TalkingArithmeticRule("EetyOrEunk", "Eety(X, +Y) + Eunk(X) = 1 .", this,
				"The possible explanations for a word's origin follow a probability distribution."));
		addRule(new TalkingArithmeticRule("EinhOrEloa", "Eety(X, Y) = Einh(X, Y) + Eloa(X, Y) .", this,
				"A word is either inherited or loaned."));
		addRule(new TalkingLogicalRule("EunkPrior", "6: ~Eunk(X)", this,
				"By default, we do not assume that words are of unknown origin."));
		addRule(new TalkingLogicalRule("EloaPrior", "2: ~Eloa(X, Y)", this,
				"By default, we do not assume that word is a loanword."));

		addRule(new TalkingLogicalRule("TancToEinh",
				"2: Tanc(L1, L2) & Flng(X, L1) & Flng(Y, L2) -> Einh(X, Y)", this));
		addRule(new TalkingLogicalRule("TcntToEloa",
				"1: Tcnt(L1, L2) & Flng(X, L1) & Flng(Y, L2) -> Eloa(X, Y)", this));

		// Only the first two arguments of the antecedent can have a value other
		// than 0 or 1.
		// TODO had to temporarily change this rule to make the grounding work again. investigate in detail (vbl)
//		addRule(new TalkingLogicalRule("EetyToFsim",
//				"8: Eety(X, Z) & Eety(Y, Z) & (X != Y) & Fufo(X, F1) & Fufo(Y, F2) -> Fsim(F1, F2)", this,
//				"Words derived from the same source should be phonetically similar."));
		addRule(new TalkingLogicalRule("EetyToFsim",
				"8: Eety(X, Z) & Eety(Y, Z) & (X != Y) & Fufo(X, F1) & Fufo(Y, F2) -> Fsim(X, Y)", this,
				"Words derived from the same source should be phonetically similar."));
		addRule(new TalkingLogicalRule("EetyToSsim",
				"8: Eety(X, Z) & Eety(Y, Z) & (X != Y) & Fsem(X, C1) & Fsem(Y, C2) -> Ssim(C1, C2)", this,
				"Words derived from the same source should be semantically similar."));

		// TODO add restriction that ~Eety(X,Y), ~Eety(Y,X) ?
		// TODO somehow this rule disables the Eety=Einh+Eloa rule
//		addRule(new TalkingLogicalRule("WsimAndSemsimToCety",
//				// phonetic similarity
//				"Fufo(X, F1) & Fufo(Y, F2) & Fsim(F1, F2) &"
//						// semantic similarity
//						+ "Fsem(X, C1) & Fsem(Y, C2) & Ssim(C1, C2) &"
//						// -> same source
//						+ "Eety(X, Z) & (Y != Z)" + "-> Eety(Y, Z)",
//				// + "-> Einh(Y, Z) | Eloa(Y, Z)", // doesn't make a difference?
//				this, "If two words are phonetically and semantically similar, "
//						+ "they are probably derived from the same source."));
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
						addFormAtoms(id1, lang1, paramID, form1.toString());
						addTarget("Eunk", id1);

						for (CLDFForm lang2Form : lang2Forms) {
							if (!testConcepts.contains(lang2Form.getParamID())) {
								continue;
							}
							addAtoms(form1, id1, lang1, lang2, concept, lang2Form);
						}

					}
				}
			}
		}
	}

	private void addAtoms(PhoneticString form1, String id1, String lang1, String lang2, CLDFParameter concept,
			CLDFForm lang2Form) {
		PhoneticString form2 = phonSimHelper.extractSegments(lang2Form);
		String id2 = getPrintForm(lang2Form, lang2);

		if (id1.equals(id2) || (id1.length() == 0 && id2.length() == 0)) {
			return;
		}

		double sim = phonSimHelper.similarity(form1, form2);
		addObservation("Fsim", sim, id1, id2);
		System.out.println("Fsim(" + id1 + "/" + form1 + "," + id2 + "/" + form2 + ") " + sim);

		double ssim = semanticNet.getSimilarity(concept.getParamID(), lang2Form.getParamID());
		if (ssim < 0.01) {
			// TODO check rules. can we get rid of these atoms?
			// e.g. SsimToEinh
			addObservation("Einh", 0.0, id1, id2);
			addObservation("Eloa", 0.0, id1, id2);
			addObservation("Eety", 0.0, id1, id2);
			return;
		}

		if (phylogeny.distanceToAncestor(lang1, lang2) == 1) {
			addTarget("Einh", id1, id2);
			addTarget("Eety", id1, id2);
		} else if (phylogeny.getLevel(lang1) == phylogeny.getLevel(lang2)) {
			addTarget("Eloa", id1, id2);
			addTarget("Eety", id1, id2);
		}

	}

	private void addFormAtoms(String id, String doculect, String concept, String form) {
		addObservation("Flng", 1.0, id, doculect);
		addObservation("Fsem", 1.0, id, concept);
		addObservation("Fufo", 1.0, id, form);
	}

	private String getPrintForm(CLDFForm form, String lang) {
		return lang + ":" + form.getProperties().get("Orthography") + ":" + form.getParamID();
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
		RuleAtomGraph rag = new RuleAtomGraph(this, new RagFilter(valueMap), groundRules);
		return new InferenceResult(rag, valueMap);
	}

	
}
