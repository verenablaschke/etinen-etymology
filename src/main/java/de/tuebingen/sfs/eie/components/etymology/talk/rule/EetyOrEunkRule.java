package de.tuebingen.sfs.eie.components.etymology.talk.rule;

import de.tuebingen.sfs.psl.engine.PslProblem;
import de.tuebingen.sfs.psl.engine.RuleAtomGraph;
import de.tuebingen.sfs.psl.talk.TalkingArithmeticRule;

public class EetyOrEunkRule extends TalkingArithmeticRule {

	private static final String NAME = "EetyOrEunk";
	private static final String RULE = "Eety(X, +Y) + Eunk(X) = 1 .";
	private static final String VERBALIZATION = "The possible explanations for a word's origin follow a probability distribution.";
	private boolean debuggingMode = false;

	public EetyOrEunkRule(PslProblem pslProblem, boolean debuggingMode) {
		super(NAME, RULE, pslProblem, VERBALIZATION);
		this.debuggingMode = debuggingMode;
	}

	public EetyOrEunkRule(PslProblem pslProblem) {
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
