package de.tuebingen.sfs.eie.components.etymology.talk.rule;

import de.tuebingen.sfs.psl.engine.PslProblem;
import de.tuebingen.sfs.psl.talk.TalkingLogicalRule;

public class EloaPriorRule extends TalkingLogicalRule {

	private static final String RULE = "~Eloa(X, Y)";
	private static final String VERBALIZATION = "By default, we do not assume that a word is a loanword.";

	public EloaPriorRule(PslProblem pslProblem, double weight) {
		super("EloaPrior", weight + ": " + RULE, pslProblem, VERBALIZATION);
	}

}
