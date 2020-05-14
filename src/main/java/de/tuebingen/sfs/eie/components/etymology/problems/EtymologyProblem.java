package de.tuebingen.sfs.eie.components.etymology.problems;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.linqs.psl.model.rule.GroundRule;

import de.tuebingen.sfs.eie.components.etymology.filter.EtymologyRagFilter;
import de.tuebingen.sfs.eie.components.etymology.talk.rule.DirectEetyToFsimRule;
import de.tuebingen.sfs.eie.components.etymology.talk.rule.EetyToFsimRule;
import de.tuebingen.sfs.eie.components.etymology.talk.rule.EetyToSsimRule;
import de.tuebingen.sfs.eie.components.etymology.talk.rule.EinhOrEloaOrEunkRule;
import de.tuebingen.sfs.eie.components.etymology.talk.rule.EloaAndEetyToFsimRule;
import de.tuebingen.sfs.eie.components.etymology.talk.rule.EloaPlusEloaRule;
import de.tuebingen.sfs.eie.components.etymology.talk.rule.EloaPriorRule;
import de.tuebingen.sfs.eie.components.etymology.talk.rule.EunkPriorRule;
import de.tuebingen.sfs.eie.components.etymology.talk.rule.FsimAndSsimToEetyRule;
import de.tuebingen.sfs.eie.components.etymology.talk.rule.TancToEinhRule;
import de.tuebingen.sfs.eie.components.etymology.talk.rule.TcntToEloaRule;
import de.tuebingen.sfs.psl.engine.AtomTemplate;
import de.tuebingen.sfs.psl.engine.DatabaseManager;
import de.tuebingen.sfs.psl.engine.InferenceResult;
import de.tuebingen.sfs.psl.engine.PslProblem;
import de.tuebingen.sfs.psl.engine.RuleAtomGraph;

public class EtymologyProblem extends PslProblem {
	
	private Map<String, Double> ruleWeights;

	public EtymologyProblem(DatabaseManager dbManager, String name) {
		super(dbManager, name);
		this.ruleWeights = new TreeMap<>();
	}
	
	public EtymologyProblem(DatabaseManager dbManager, String name, Map<String, Double> ruleWeights) {
		super(dbManager, name);
		if (ruleWeights == null){
			ruleWeights = new TreeMap<>();
		}
		this.ruleWeights = ruleWeights;
	}

	@Override
	public void declarePredicates() {
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
		addRule(new EloaPlusEloaRule(this));
		
		addRule(new EunkPriorRule(this, ruleWeights.getOrDefault(EunkPriorRule.NAME, 2.5)));
		addRule(new EloaPriorRule(this, ruleWeights.getOrDefault(EloaPriorRule.NAME, 2.0)));
		
		addRule(new TancToEinhRule(this, ruleWeights.getOrDefault(TancToEinhRule.NAME, 1.0)));
		addRule(new TcntToEloaRule(this, ruleWeights.getOrDefault(TcntToEloaRule.NAME, 1.0)));

		addRule(new EetyToFsimRule("Einh", "Einh", this, ruleWeights.getOrDefault(EetyToFsimRule.NAME, 5.0)));
		addRule(new EetyToFsimRule("Einh", "Eloa", this, ruleWeights.getOrDefault(EetyToFsimRule.NAME, 5.0)));
		addRule(new EetyToFsimRule("Eloa", "Einh", this, ruleWeights.getOrDefault(EetyToFsimRule.NAME, 5.0)));
		addRule(new EetyToFsimRule("Eloa", "Eloa", this, ruleWeights.getOrDefault(EetyToFsimRule.NAME, 5.0)));

		addRule(new EetyToSsimRule("Einh", "Einh", this, ruleWeights.getOrDefault(EetyToSsimRule.NAME, 5.0)));
		addRule(new EetyToSsimRule("Einh", "Eloa", this, ruleWeights.getOrDefault(EetyToSsimRule.NAME, 5.0)));
		addRule(new EetyToSsimRule("Eloa", "Einh", this, ruleWeights.getOrDefault(EetyToSsimRule.NAME, 5.0)));
		addRule(new EetyToSsimRule("Eloa", "Eloa", this, ruleWeights.getOrDefault(EetyToSsimRule.NAME, 5.0)));

		addRule(new FsimAndSsimToEetyRule("Einh", this, ruleWeights.getOrDefault(FsimAndSsimToEetyRule.NAME, 8.0)));
		
//		addRule(new DirectEetyToFsimRule("Eloa", this, 5.0));
		addRule(new EloaAndEetyToFsimRule("Einh", "Einh", this, 5.0));
		addRule(new EloaAndEetyToFsimRule("Einh", "Eloa", this, 5.0));
		addRule(new EloaAndEetyToFsimRule("Eloa", "Einh", this, 5.0));
		addRule(new EloaAndEetyToFsimRule("Eloa", "Eloa", this, 5.0));
		
		// TODO Is this rule necessary? If yes: how to prevent this rule from being essentially grounded twice?
		// e.g. A,B + B,A <= 1 and B,A + A,B <= 1
//		addRule(new FsimAndSsimToEetyRule("Eloa", this, ruleWeights.getOrDefault(FsimAndSsimToEetyRule.NAME, 8.0)));
	}	

	@Override
	public Set<AtomTemplate> declareAtomsForCleanUp() {
		// TODO (outside this class)
		// - delete F-atoms for proto languages if they don't correspond to high-belief E-atoms
		// - delete low-belief E-atoms
		Set<AtomTemplate> atomsToDelete = new HashSet<>();
		atomsToDelete.add(new AtomTemplate("Tanc", "?", "?"));
		atomsToDelete.add(new AtomTemplate("Tcnt", "?", "?"));
		atomsToDelete.add(new AtomTemplate("XFufo", "?", "?"));
		atomsToDelete.add(new AtomTemplate("Fsim", "?", "?"));
		return atomsToDelete;
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
