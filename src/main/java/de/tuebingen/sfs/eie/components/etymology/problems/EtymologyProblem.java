package de.tuebingen.sfs.eie.components.etymology.problems;

import static de.tuebingen.sfs.psl.engine.AtomTemplate.ANY_CONST;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.linqs.psl.model.rule.GroundRule;

import de.tuebingen.sfs.eie.components.etymology.filter.EtymologyRagFilter;
import de.tuebingen.sfs.eie.components.etymology.talk.rule.DirectEetyToFsimRule;
import de.tuebingen.sfs.eie.components.etymology.talk.rule.EetyToFsimRule;
import de.tuebingen.sfs.eie.components.etymology.talk.rule.EetyToSsimRule;
import de.tuebingen.sfs.eie.components.etymology.talk.rule.EinhOrEloaOrEunkRule;
import de.tuebingen.sfs.eie.components.etymology.talk.rule.EloaPlusEloaRule;
import de.tuebingen.sfs.eie.components.etymology.talk.rule.EloaPriorRule;
import de.tuebingen.sfs.eie.components.etymology.talk.rule.EunkPriorRule;
import de.tuebingen.sfs.eie.components.etymology.talk.rule.TancToEinhRule;
import de.tuebingen.sfs.eie.components.etymology.talk.rule.TcntToEloaRule;
import de.tuebingen.sfs.eie.shared.talk.pred.EinhPred;
import de.tuebingen.sfs.eie.shared.talk.pred.EloaPred;
import de.tuebingen.sfs.eie.shared.talk.pred.EunkPred;
import de.tuebingen.sfs.eie.shared.talk.pred.FsimPred;
import de.tuebingen.sfs.psl.engine.AtomTemplate;
import de.tuebingen.sfs.psl.engine.InferenceResult;
import de.tuebingen.sfs.psl.engine.PslProblem;
import de.tuebingen.sfs.psl.engine.RuleAtomGraph;
import de.tuebingen.sfs.psl.util.log.InferenceLogger;

public class EtymologyProblem extends PslProblem {

	public static final String[] RULES = new String[] { EunkPriorRule.NAME, EloaPriorRule.NAME,
			EinhOrEloaOrEunkRule.NAME, EloaPlusEloaRule.NAME, TancToEinhRule.NAME, TcntToEloaRule.NAME,
			EetyToFsimRule.NAME, EetyToSsimRule.NAME, DirectEetyToFsimRule.NAME };

	// TODO make sure the config sets the dbmanager and problemId when it's
	// initialized
	// (old to-do)
	public EtymologyProblem(EtymologyProblemConfig config) {
		super(config);
		addInteractionRules();

		InferenceLogger logger = config.getLogger();
		logger.displayAndLogLn("==========");
		logger.displayAndLog("Initializing etymology model with the following configuration:");
		config.logSettings();
	}

	public EtymologyProblem fromConfig(EtymologyProblemConfig config) {
		return new EtymologyProblem(config);
	}

	@Override
	public void declarePredicates() {
		declareClosedPredicate("Xinh", 2);
		declareClosedPredicate("Xloa", 2);

		declareClosedPredicate("Xsth", 2);
		declareClosedPredicate("Xdst", 3);

		declareOpenPredicate("Fhom", 3);
		declareOpenPredicate(new FsimPred());
		declareOpenPredicate(new EinhPred());
		declareOpenPredicate(new EloaPred());
		declareOpenPredicate(new EunkPred());
	}

	@Override
	public void pregenerateAtoms() {
	}

	@Override
	public void addInteractionRules() {
		EtymologyProblemConfig config = (EtymologyProblemConfig) super.getConfig();
		// TODO add config checks for the new rules, like before:
		if (config.include(EunkPriorRule.NAME))
			addRule(new EunkPriorRule(this, config.getRuleWeightOrDefault(EunkPriorRule.NAME, 2.5)));
		if (config.include(EloaPriorRule.NAME))
			addRule(new EloaPriorRule(this, config.getRuleWeightOrDefault(EloaPriorRule.NAME, 2.0)));
		if (config.include(EinhOrEloaOrEunkRule.NAME))
			addRule(new EinhOrEloaOrEunkRule(this));
		if (config.include(EloaPlusEloaRule.NAME))
			addRule(new EloaPlusEloaRule(this));

		// Form similarity is symmetric:
		addRule("FsimSymmetry", "Fsim(X,Y) = Fsim(Y,X) .");
		// Form similarity is (partially) transitive (?):
		addRule("FsimTransitivity", "Fsim(X,Y) & Fsim(Y,Z) & (X != Y) -> Fsim(X,Z)");

		// If two forms are inherited from the same form, they should be similar:
		addRule("EinhToFsim", "Einh(X,Z) & Einh(Y,Z) & (X != Y) -> Fsim(X,Y)");
		// If two forms are similar and might be inherited from a common source, it's
		// likely that they really were.
		addRule("FsimToEinh", "Fsim(X,Y) & Xinh(X,Z) & Xinh(Y,Z) -> Einh(X,Z)");
		// If two forms are similar and inherited from different sources, those source
		// words should be similar to one another too.
		addRule("FsimToFsim", "Fsim(X,Y) & Einh(X,W) & Einh(Y,Z) & (W != Z) -> Fsim(W,Z)");

		// If a word is more similar to a word in a contact language than to its
		// reconstructed ancestor, that makes it more likely to be a loan:
		addRule("EloaAndFsim", "Xloa(X,W) + Eloa(X,W) >= Xinh(X,Z) + Fsim(X,W) - Fsim(X,Z).");

		// Sister forms should be less similar than either is to their common parent
		// form:
		addRule("FsimFamily", "(X != Y) + Xinh(X,Z) + Xinh(Y,Z) + Fsim(X,Y) <= 3 + Fsim(X,Z).");
		// The distance between two sister words must not exceed the sum of distances to
		// the common ancestor, reusing the grounding atoms to create the constant 2:
		addRule("FsimTriangle", "(X != Y) + Xinh(X,Z) + Xinh(Y,Z) - Fsim(X,Z) - Fsim(Y,Z) >= 2 - Fsim(X,Y).");
		// Smaller tree distances -> higher similarity
		addRule("XdstToFsim", "Xsth(D1,D2) & Xdst(X,Y,D1) & Xdst(X,Z,D2) & Fsim(X,Z) -> Fsim(X,Y)");

		// Every pair of sister languages in which a homologue set is reconstructed or
		// attested makes it more likely to have existed in the common parent language:
		addRule("FhomReconstruction", "Fhom(X,H,C) & Fhom(Y,H,C) & Xinh(X,Z) & Xinh(Y,Z) -> Fhom(Z,H,C)");
		// Also a reasonable assumption for unary branches
		// TODO give these two similar rules different weights
		addRule("FhomSingleReconstruction", "Fhom(X,H,C) & Xinh(X,Z) -> Fhom(Z,H,C)");
		// Also distribute evidence of the presence of homologue sets downwards?
		addRule("FhomChild", "Fhom(Z,H,C) & Xinh(X,Z) -> Fhom(X,H,C)");
		// Limit the number of homologues for each concept at each reconstructed
		// languages (express bias against synonyms)
		addRule("FhomSynonyms", "0.5: Fhom(Z,+H,C) <= 2");

		System.out.println("Rules added:");
		super.printRules(System.out);
	}

	// TODO this needs a general overhaul, on the full project level, based on
	// changed assumptions about the centrality of PSL
	@Override
	public Set<AtomTemplate> declareAtomsForCleanUp() {
		// TODO (outside this class)
		// - delete F-atoms for proto languages if they don't correspond to
		// high-belief E-atoms
		// - delete low-belief E-atoms
		Set<AtomTemplate> atomsToDelete = new HashSet<>();
		atomsToDelete.add(new AtomTemplate("Xinh", ANY_CONST, ANY_CONST));
		atomsToDelete.add(new AtomTemplate("Xloa", ANY_CONST, ANY_CONST));
		atomsToDelete.add(new AtomTemplate("Fhom", ANY_CONST, ANY_CONST));
		atomsToDelete.add(new AtomTemplate("Fsim", ANY_CONST, ANY_CONST));
		return atomsToDelete;
	}

	public EtymologyProblemConfig getEtymologyConfig() {
		return (EtymologyProblemConfig) super.getConfig();
	}

	public InferenceLogger getLogger() {
		return super.getConfig().getLogger();
	}

	@Override
	public InferenceResult call() throws Exception {
		addInteractionRules();
		List<List<GroundRule>> groundRules = runInference(true);
		RuleAtomGraph.GROUNDING_OUTPUT = true;
		RuleAtomGraph.ATOM_VALUE_OUTPUT = true;
		Map<String, Double> valueMap = extractResultsForAllPredicates(false);
		RuleAtomGraph rag = new RuleAtomGraph(this, new EtymologyRagFilter(valueMap), groundRules);
		return new InferenceResult(rag, valueMap, getEtymologyConfig().copy());
	}

}
