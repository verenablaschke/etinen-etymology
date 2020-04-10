package de.tuebingen.sfs.eie.components.etymology.talk.rule;

import de.tuebingen.sfs.psl.engine.PslProblem;
import de.tuebingen.sfs.psl.engine.RuleAtomGraph;
import de.tuebingen.sfs.psl.talk.TalkingArithmeticRule;

public class EinhOrEloaOrEunkRule extends TalkingArithmeticRule {

	private static final String NAME = "EinhOrEloaOrEunk";
	private static final String RULE = "Einh(X, +Y) + Eloa(X, +Z) + Eunk(X) = 1 .";
	private static final String VERBALIZATION = "The possible explanations for a word's origin follow a probability distribution.";
	private boolean debuggingMode = false;

	public EinhOrEloaOrEunkRule(PslProblem pslProblem, boolean debuggingMode) {
		super(NAME, RULE, pslProblem, VERBALIZATION);
		this.debuggingMode = debuggingMode;
	}

	public EinhOrEloaOrEunkRule(PslProblem pslProblem) {
		super(NAME, RULE, pslProblem, VERBALIZATION);
	}

	@Override
	public String generateExplanation(String groundingName, String contextAtom, RuleAtomGraph rag,
			boolean whyExplanation) {
		if (!debuggingMode) {
			return getDefaultExplanation(groundingName, contextAtom, rag, whyExplanation);
		}
		StringBuilder sb = new StringBuilder();
		sb.append(getDefaultExplanation(groundingName, contextAtom, rag, whyExplanation));
		
		
		
		return sb.toString();
	}

}
