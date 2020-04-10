package de.tuebingen.sfs.eie.components.etymology.talk.rule;

import de.tuebingen.sfs.psl.engine.PslProblem;
import de.tuebingen.sfs.psl.talk.TalkingLogicalRule;

public class EloaAndEloaToFsimRule extends TalkingLogicalRule {
	private static final String RULE = "Eloa(X, Z) & Eloa(Y, Z) & (X != Y) & XFufo(X) & XFufo(Y) & Fufo(X, F1) & Fufo(Y, F2) -> Fsim(F1, F2)";
	private static final String VERBALIZATION = "Words derived from the same source should be phonetically similar";

	public EloaAndEloaToFsimRule(PslProblem pslProblem, double weight) {
		super("EloaAndEloaToFsim", weight + ": " + RULE, pslProblem, VERBALIZATION + ".");
	}
}
