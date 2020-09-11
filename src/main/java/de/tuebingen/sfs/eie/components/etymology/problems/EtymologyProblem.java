package de.tuebingen.sfs.eie.components.etymology.problems;

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
import de.tuebingen.sfs.eie.components.etymology.talk.rule.FsimAndSsimToEetyRule;
import de.tuebingen.sfs.eie.components.etymology.talk.rule.TancToEinhRule;
import de.tuebingen.sfs.eie.components.etymology.talk.rule.TcntToEloaRule;
import de.tuebingen.sfs.eie.talk.pred.EinhPred;
import de.tuebingen.sfs.eie.talk.pred.EloaPred;
import de.tuebingen.sfs.eie.talk.pred.EunkPred;
import de.tuebingen.sfs.psl.engine.AtomTemplate;
import de.tuebingen.sfs.psl.engine.DatabaseManager;
import de.tuebingen.sfs.psl.engine.InferenceResult;
import de.tuebingen.sfs.psl.engine.PslProblem;
import de.tuebingen.sfs.psl.engine.RuleAtomGraph;
import de.tuebingen.sfs.psl.talk.TalkingArithmeticRule;
import de.tuebingen.sfs.psl.talk.TalkingLogicalRule;

import static de.tuebingen.sfs.psl.engine.AtomTemplate.ANY_CONST;

public class EtymologyProblem extends PslProblem {

	private EtymologyConfig config;

	public EtymologyProblem(DatabaseManager dbManager, String name) {
		this(dbManager, name, null);
	}

	public EtymologyProblem(DatabaseManager dbManager, String name, EtymologyConfig config) {
		super(dbManager, name);
		if (config == null) {
			config = new EtymologyConfig();
		}
		this.config = config;
		System.out.println("Using the following configuration:");
		config.print(System.out);
	}

	@Override
	public void declarePredicates() {
		// TODO use predicate classes here

		// Information about the forms
		// Flng(ID, language)
		declareClosedPredicate("Flng", 2);
		declareClosedPredicate("Fufo", 2);
		declareClosedPredicate("Fsem", 2);
		declareClosedPredicate("XFufo", 1);

		// Similarity measures
		declareClosedPredicate("Fsim", 2);
		declareClosedPredicate("Ssim", 2);

		// Etymological information
		// Eety(ID1, ID2) -- ID1 comes from ID2
		declareOpenPredicate(new EinhPred());
		declareOpenPredicate(new EloaPred());
		declareOpenPredicate(new EunkPred());

		// Phylogenetic information.
		declareClosedPredicate("Tanc", 2);
		declareClosedPredicate("Tcnt", 2);

		// declareClosedPredicate("Fsimorig", 2);
	}

	@Override
	public void pregenerateAtoms() {
	}

	@Override
	public void addInteractionRules() {
		if (config.include(EunkPriorRule.NAME))
			addRule(new EunkPriorRule(this, config.getRuleWeightOrDefault(EunkPriorRule.NAME, 2.5)));
		if (config.include(EloaPriorRule.NAME))
			addRule(new EloaPriorRule(this, config.getRuleWeightOrDefault(EloaPriorRule.NAME, 2.0)));

		if (config.include(EinhOrEloaOrEunkRule.NAME))
			addRule(new EinhOrEloaOrEunkRule(this));
		if (config.include(EloaPlusEloaRule.NAME))
			addRule(new EloaPlusEloaRule(this));

		if (config.include(TancToEinhRule.NAME))
			addRule(new TancToEinhRule(this, config.getRuleWeightOrDefault(TancToEinhRule.NAME, 1.0)));
		if (config.include(TcntToEloaRule.NAME))
			addRule(new TcntToEloaRule(this, config.getRuleWeightOrDefault(TcntToEloaRule.NAME, 1.0)));

		if (config.include(EetyToFsimRule.NAME)) {
			addRule(new EetyToFsimRule("Einh", "Einh", this, config.getRuleWeightOrDefault(EetyToFsimRule.NAME, 5.0)));
			addRule(new EetyToFsimRule("Einh", "Eloa", this, config.getRuleWeightOrDefault(EetyToFsimRule.NAME, 5.0)));
			addRule(new EetyToFsimRule("Eloa", "Einh", this, config.getRuleWeightOrDefault(EetyToFsimRule.NAME, 5.0)));
			addRule(new EetyToFsimRule("Eloa", "Eloa", this, config.getRuleWeightOrDefault(EetyToFsimRule.NAME, 5.0)));

			// addRule(new TalkingArithmeticRule("EetyFsimArith",
			// "Fsim(F1, +F2) >= Einh(X, Z) + Einh(+Y, Z)"
			// + "{Y: XFufo(Y) & XFufo(X) & Fufo(X, F1)} & (X != Y)", //
			//// + "{F2: Fufo(Y, F2)}",
			// this));
		}

		if (config.include(EetyToSsimRule.NAME)) {
			addRule(new EetyToSsimRule("Einh", "Einh", this, config.getRuleWeightOrDefault(EetyToSsimRule.NAME, 5.0)));
			addRule(new EetyToSsimRule("Einh", "Eloa", this, config.getRuleWeightOrDefault(EetyToSsimRule.NAME, 5.0)));
			addRule(new EetyToSsimRule("Eloa", "Einh", this, config.getRuleWeightOrDefault(EetyToSsimRule.NAME, 5.0)));
			addRule(new EetyToSsimRule("Eloa", "Eloa", this, config.getRuleWeightOrDefault(EetyToSsimRule.NAME, 5.0)));
		}

		if (config.include(DirectEetyToFsimRule.NAME)) {
			addRule(new DirectEetyToFsimRule("Eloa", this,
					config.getRuleWeightOrDefault(DirectEetyToFsimRule.NAME, 8.0)));
			addRule(new DirectEetyToFsimRule("Einh", this,
					config.getRuleWeightOrDefault(DirectEetyToFsimRule.NAME, 8.0)));
		}

		// addRule(new EloaAndEetyToFsimRule("Einh", "Einh", this, 3.0));
		// addRule(new EloaAndEetyToFsimRule("Einh", "Eloa", this, 3.0));
		// addRule(new EloaAndEetyToFsimRule("Eloa", "Einh", this, 3.0));
		// addRule(new EloaAndEetyToFsimRule("Eloa", "Eloa", this, 3.0));

		// TODO Is this rule necessary? If yes: how to prevent this rule from
		// being essentially grounded twice?
		// e.g. A,B + B,A <= 1 and B,A + A,B <= 1
		// addRule(new FsimAndSsimToEetyRule("Eloa", this,
		// config.getRuleWeightOrDefault(FsimAndSsimToEetyRule.NAME, 8.0)));
		// addRule(new FsimAndSsimToEetyRule("Einh", this,
		// config.getRuleWeightOrDefault(FsimAndSsimToEetyRule.NAME, 8.0)));
		// addRule(new TalkingLogicalRule("FsimAndSsimToEety1",
		// "Fufo(X, F1) & Fufo(Y, F2) & Fsim(F1, F2) &" + "Fsem(X, C1) & Fsem(Y,
		// C2) & Ssim(C1, C2) &"
		// + "Einh(X, Z) & (X != Y) & (Y != Z) &" + "Einh(Z, W) & (Y != W)"
		// + "-> Einh(Y, Z) | Eloa(Y, Z) | Einh(Y, W) | Eloa(Y, W)",
		// this));
		// addRule(new TalkingLogicalRule("FsimAndSsimToEety2",
		// "Fufo(X, F1) & Fufo(Y, F2) & Fsim(F1, F2) &" + "Fsem(X, C1) & Fsem(Y,
		// C2) & Ssim(C1, C2) &"
		// + "Eloa(X, Z) & (X != Y) & (Y != Z) &" + "Einh(Z, W) & (Y != W)"
		// + "-> Einh(Y, Z) | Eloa(Y, Z) | Einh(Y, W) | Eloa(Y, W)",
		// this));
		System.out.println("Rules added:");
		super.printRules(System.out);
	}

	@Override
	public Set<AtomTemplate> declareAtomsForCleanUp() {
		// TODO (outside this class)
		// - delete F-atoms for proto languages if they don't correspond to
		// high-belief E-atoms
		// - delete low-belief E-atoms
		Set<AtomTemplate> atomsToDelete = new HashSet<>();
		atomsToDelete.add(new AtomTemplate("Tanc", ANY_CONST, ANY_CONST));
		atomsToDelete.add(new AtomTemplate("Tcnt", ANY_CONST, ANY_CONST));
		atomsToDelete.add(new AtomTemplate("XFufo", ANY_CONST, ANY_CONST));
		atomsToDelete.add(new AtomTemplate("Fsim", ANY_CONST, ANY_CONST));
		return atomsToDelete;
	}

	public EtymologyConfig getConfig() {
		return config;
	}

	@Override
	public InferenceResult call() throws Exception {
		addInteractionRules();
		List<List<GroundRule>> groundRules = runInference(true);
		RuleAtomGraph.GROUNDING_OUTPUT = true;
		RuleAtomGraph.ATOM_VALUE_OUTPUT = true;
		Map<String, Double> valueMap = extractResult(false);
		RuleAtomGraph rag = new RuleAtomGraph(this, new EtymologyRagFilter(valueMap), groundRules);
		return new InferenceResult(rag, valueMap);
	}

}
