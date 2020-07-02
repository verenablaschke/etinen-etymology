package de.tuebingen.sfs.eie.components.etymology.talk.rule;

import de.tuebingen.sfs.psl.engine.PslProblem;
import de.tuebingen.sfs.psl.engine.RuleAtomGraph;
import de.tuebingen.sfs.psl.talk.TalkingLogicalRule;

public class EloaPriorRule extends TalkingLogicalRule {

	public static final String NAME = "EloaPrior";
	private static final String RULE = "~Eloa(X, Y)";
	private static final String VERBALIZATION = "By default, we do not assume that a word is a loanword.";

	public EloaPriorRule(){
		// For serialization.
	}
	
	public EloaPriorRule(PslProblem pslProblem, double weight) {
		super(NAME, weight + ": " + RULE, pslProblem, VERBALIZATION);
	}

	@Override
	public String generateExplanation(String groundingName, String contextAtom, RuleAtomGraph rag,
			boolean whyExplanation) {
		return VERBALIZATION;
	}

}
