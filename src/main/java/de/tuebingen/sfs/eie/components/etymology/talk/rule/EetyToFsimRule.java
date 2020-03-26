package de.tuebingen.sfs.eie.components.etymology.talk.rule;

import de.tuebingen.sfs.psl.engine.PslProblem;
import de.tuebingen.sfs.psl.talk.TalkingLogicalRule;

public class EetyToFsimRule extends TalkingLogicalRule {

	// Only Eety and Fsim can have a value other than 0 or 1.
	private static final String RULE = "Eety(X, Z) & Eety(Y, Z) & (X != Y) & Fufo(X, F1) & Fufo(Y, F2) -> Fsim(F1, F2)";
	private static final String VERBALIZATION = "Words derived from the same source should be phonetically similar";

	public EetyToFsimRule(PslProblem pslProblem, double weight) {
		super("EetyToFsim", weight + ": " + RULE, pslProblem, VERBALIZATION + ".");
	}

}
