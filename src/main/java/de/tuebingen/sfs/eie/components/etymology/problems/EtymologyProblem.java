package de.tuebingen.sfs.eie.components.etymology.problems;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.linqs.psl.model.rule.GroundRule;

import de.tuebingen.sfs.eie.components.etymology.filter.EtymologyRagFilter;
import de.tuebingen.sfs.eie.components.etymology.talk.rule.EetyToFsimRule;
import de.tuebingen.sfs.eie.components.etymology.talk.rule.EinhOrEloaOrEunkRule;
import de.tuebingen.sfs.eie.components.etymology.talk.rule.EloaPriorRule;
import de.tuebingen.sfs.eie.components.etymology.talk.rule.FsimAndSsimToEetyRule;
import de.tuebingen.sfs.eie.components.etymology.talk.rule.TancToEinhRule;
import de.tuebingen.sfs.eie.components.etymology.talk.rule.TcntToEloaRule;
import de.tuebingen.sfs.psl.engine.AtomTemplate;
import de.tuebingen.sfs.psl.engine.DatabaseManager;
import de.tuebingen.sfs.psl.engine.InferenceResult;
import de.tuebingen.sfs.psl.engine.PslProblem;
import de.tuebingen.sfs.psl.engine.RagFilter;
import de.tuebingen.sfs.psl.engine.RuleAtomGraph;
import de.tuebingen.sfs.psl.talk.TalkingLogicalRule;

public class EtymologyProblem extends PslProblem {

	public EtymologyProblem(DatabaseManager dbManager, String name) {
		super(dbManager, name);
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
//		Map<String, Double> valueMap = extractResult();
		Map<String, Double> valueMap = extractResult(false);
		RuleAtomGraph rag = new RuleAtomGraph(this, new RagFilter(valueMap), groundRules);
		return new InferenceResult(rag, valueMap);
	}

}
