package de.tuebingen.sfs.eie.components.etymology.talk.rule;

import de.tuebingen.sfs.psl.engine.PslProblem;
import de.tuebingen.sfs.psl.talk.TalkingLogicalRule;

public class EinhAndEinhToFsimRule extends TalkingLogicalRule {

	// Only Eety and Fsim can have a value other than 0 or 1.
	private static final String RULE = "Einh(X, Z) & Einh(Y, Z) & (X != Y) & XFufo(X) & XFufo(Y) & Fufo(X, F1) & Fufo(Y, F2) -> Fsim(F1, F2)";
	private static final String VERBALIZATION = "Words derived from the same source should be phonetically similar (Einh & Einh)";

	public EinhAndEinhToFsimRule(PslProblem pslProblem, double weight) {
		super("EinhAndEinhToFsim", weight + ": " + RULE, pslProblem, VERBALIZATION + ".");
	}

}
