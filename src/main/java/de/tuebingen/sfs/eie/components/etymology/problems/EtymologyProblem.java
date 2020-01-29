package de.tuebingen.sfs.eie.components.etymology.problems;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.linqs.psl.model.rule.GroundRule;

import de.tuebingen.sfs.cldfjava.data.CLDFForm;
import de.tuebingen.sfs.cldfjava.data.CLDFLanguage;
import de.tuebingen.sfs.cldfjava.data.CLDFParameter;
import de.tuebingen.sfs.cldfjava.data.CLDFWordlistDatabase;
import de.tuebingen.sfs.eie.components.etymology.util.LevelBasedPhylogeny;
import de.tuebingen.sfs.eie.shared.util.SemanticNetwork;
import de.tuebingen.sfs.iwsa.corrmodel.CorrespondenceModel;
import de.tuebingen.sfs.iwsa.sequence.PhoneticString;
import de.tuebingen.sfs.iwsa.tokenize.IPATokenizer;
import de.tuebingen.sfs.iwsa.util.PhoneticSimilarityHelper;
import de.tuebingen.sfs.psl.engine.core.AtomTemplate;
import de.tuebingen.sfs.psl.engine.core.InferenceResult;
import de.tuebingen.sfs.psl.engine.core.PslProblem;
import de.tuebingen.sfs.psl.engine.core.RuleAtomGraph;
import de.tuebingen.sfs.psl.engine.talk.TalkingArithmeticRule;
import de.tuebingen.sfs.psl.engine.talk.TalkingLogicalRule;
import de.tuebingen.sfs.psl.engine.filter.RagFilter;

public class EtymologyProblem extends PslProblem {

	CLDFWordlistDatabase database;
	IPATokenizer ipaTokenizer;
	CorrespondenceModel corrModel;
	SemanticNetwork semanticNet;
	PhoneticSimilarityHelper phonSimHelper;
	LevelBasedPhylogeny phylogeny;
	Map<String, String> ISO2LangID;

	public EtymologyProblem(CLDFWordlistDatabase database, CorrespondenceModel corrModel, SemanticNetwork semanticNet,
			LevelBasedPhylogeny phylogeny) {
		this.database = database;
		this.corrModel = corrModel;
		this.ipaTokenizer = new IPATokenizer();
		this.semanticNet = semanticNet;
		this.phylogeny = phylogeny;
		phonSimHelper = new PhoneticSimilarityHelper(ipaTokenizer, corrModel);

		ISO2LangID = new HashMap<>();
		for (CLDFLanguage lang : database.getAllLanguages()) {
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

		// Similarity measures
		declareClosedPredicate("Fsim", 2);
		declareClosedPredicate("Ssim", 2);

		// Etymological information
		// Eety(ID1, ID2) -- ID1 comes from ID2
		declareOpenPredicate("Eety", 2);
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
		// Setting up Eety/Einh/Eloa/Eunk.
		this.rules.addRule(new TalkingArithmeticRule("EetyOrEunk", "Eety(X, +Y) + Eunk(X) = 1 .", this,
				"The possible explanations for a word's origin follow a probability distribution."));
		this.rules.addRule(new TalkingArithmeticRule("EinhOrEloa", "Eety(X, Y) = Einh(X, Y) + Eloa(X, Y) .", this,
				"A word is either inherited or loaned."));
		this.rules.addRule(new TalkingLogicalRule("EunkPrior", "6: ~Eunk(X)", this,
				"By default, we do not assume that words are of unknown origin."));
		this.rules.addRule(new TalkingLogicalRule("EloaPrior", "2: ~Eloa(X, Y)", this,
				"By default, we do not assume that word is a loanword."));

		this.rules.addRule(new TalkingLogicalRule("TancToEinh",
				"2: Tanc(L1, L2) & Flng(X, L1) & Flng(Y, L2) -> Einh(X, Y)", this));
		this.rules.addRule(new TalkingLogicalRule("TcntToEloa",
				"1: Tcnt(L1, L2) & Flng(X, L1) & Flng(Y, L2) -> Eloa(X, Y)", this));

		// Only the first two arguments of the antecedent can have a value other
		// than 0 or 1.
		this.rules.addRule(new TalkingLogicalRule("EetyToFsim",
				"8: Eety(X, Z) & Eety(Y, Z) & (X != Y) & Fufo(X, F1) & Fufo(Y, F2) -> Fsim(F1, F2)", this,
				"Words derived from the same source should be phonetically similar."));
		this.rules.addRule(new TalkingLogicalRule("EetyToSsim",
				"8: Eety(X, Z) & Eety(Y, Z) & (X != Y) & Fsem(X, C1) & Fsem(Y, C2) -> Ssim(C1, C2)", this,
				"Words derived from the same source should be semantically similar."));

		// TODO add restriction that ~Eety(X,Y), ~Eety(Y,X) ?
		// TODO somehow this rule disables the Eety=Einh+Eloa rule
//		this.rules.addRule(new TalkingLogicalRule("WsimAndSemsimToCety",
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

	// TODO remove arg and "in testConcepts" checks in the method
	public void generateDataAtoms(Set<String> testConcepts) {
		for (String concept1 : testConcepts) {
			for (String concept2 : testConcepts) {
				this.atoms.addObservation("Ssim", semanticNet.getSimilarity(concept1, concept2), concept1, concept2);
			}
		}

		// align all word pairs with the same meaning
		for (CLDFParameter concept : database.getAllConcepts()) {
			String paramID = concept.getParamID();
			if (!testConcepts.contains(paramID)) {
				continue;
			}

			List<String> allLangs = phylogeny.getAllLanguages();
			for (String lang1 : allLangs) {
				List<CLDFForm> lang1Forms = database.getForms(ISO2LangID.getOrDefault(lang1, lang1), paramID);

				// Proto language
				if (lang1Forms.isEmpty()) {
					lang1Forms = new ArrayList<CLDFForm>();
					CLDFForm form = new CLDFForm();
					form.setParamID(concept.getParamID());
					lang1Forms.add(form);
				}

				for (String lang2 : allLangs) {
					List<CLDFForm> lang2Forms = database.getFormsForLanguage(ISO2LangID.getOrDefault(lang2, lang2));

					if (phylogeny.distanceToAncestor(lang1, lang2) == 1) {
						this.atoms.addObservation("Tanc", 1.0, lang1, lang2);
					} else if (!lang1.equals(lang2) && phylogeny.getLevel(lang1) == phylogeny.getLevel(lang2)) {
						// TODO: borrowing from e.g. Latin
						// TODO: geographical distance etc.
						// TODO: make this open instead? e.g. addTarget
						this.atoms.addObservation("Tcnt", 1.0, lang1, lang2);
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
						this.addAtom("Eunk", id1);

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

		if (id1.equals(id2)) {
			return;
		}

		double sim = phonSimHelper.similarity(form1, form2);
		this.atoms.addObservation("Fsim", sim, id1, id2);
		System.out.println("Fsim(" + id1 + "," + id2 + ") " + sim);

		double ssim = semanticNet.getSimilarity(concept.getParamID(), lang2Form.getParamID());
		if (ssim < 0.01) {
			// TODO check rules. can we get rid of these atoms?
			// e.g. SsimToEinh
			this.atoms.addObservation("Einh", 0.0, id1, id2);
			this.atoms.addObservation("Eloa", 0.0, id1, id2);
			this.atoms.addObservation("Eety", 0.0, id1, id2);
			return;
		}

		if (phylogeny.distanceToAncestor(lang1, lang2) == 1) {
			this.atoms.addTarget("Einh", id1, id2);
			this.atoms.addTarget("Eety", id1, id2);
		} else if (phylogeny.getLevel(lang1) == phylogeny.getLevel(lang2)) {
			this.atoms.addTarget("Eloa", id1, id2);
			this.atoms.addTarget("Eety", id1, id2);
		}

	}

	private void addFormAtoms(String id, String doculect, String concept, String form) {
		this.atoms.addObservation("Flng", 1.0, id, doculect);
		this.atoms.addObservation("Fsem", 1.0, id, concept);
		this.atoms.addObservation("Fufo", 1.0, id, form);
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
		this.atoms.closeDatabase();
		RuleAtomGraph.GROUNDING_OUTPUT = true;
		RuleAtomGraph.ATOM_VALUE_OUTPUT = true;
		Map<String, Double> valueMap = extractResult();
		RuleAtomGraph rag = new RuleAtomGraph(this, new RagFilter(valueMap), groundRules);
		return new InferenceResult(rag, valueMap);
	}

	
}
