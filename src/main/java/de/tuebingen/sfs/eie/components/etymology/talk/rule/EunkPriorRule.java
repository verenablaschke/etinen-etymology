package de.tuebingen.sfs.eie.components.etymology.talk.rule;

import de.tuebingen.sfs.psl.engine.PslProblem;
import de.tuebingen.sfs.psl.talk.TalkingLogicalRule;

public class EunkPriorRule extends TalkingLogicalRule {
	
	public static final String NAME = "EunkPrior";
	public static final String RULE = "~Eunk(X)";

	// For serialization.
	public EunkPriorRule(){
		super(RULE);
	}
	
	public EunkPriorRule(PslProblem pslProblem, double weight) {
		super(NAME, weight + ": " + RULE, pslProblem, "By default, we do not assume that words are of unknown origin.");
	}

}
