package de.tuebingen.sfs.eie.components.etymology.talk.rule;

import de.tuebingen.sfs.psl.engine.PslProblem;
import de.tuebingen.sfs.psl.talk.TalkingLogicalRule;

public class EloaAndEinhToFsimRule extends TalkingLogicalRule {
	private static final String RULE = "Eloa(X, Z) & Einh(Y, Z) & (X != Y) & XFufo(X) & XFufo(Y) & Fufo(X, F1) & Fufo(Y, F2) -> Fsim(F1, F2)";
	private static final String VERBALIZATION = "Words derived from the same source should be phonetically similar (Eloa & Einh)";

	public EloaAndEinhToFsimRule(PslProblem pslProblem, double weight) {
		super("EloaAndEinhToFsim", weight + ": " + RULE, pslProblem, VERBALIZATION + ".");
	}
}
