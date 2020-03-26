package de.tuebingen.sfs.eie.components.etymology.talk.rule;

import de.tuebingen.sfs.psl.engine.PslProblem;
import de.tuebingen.sfs.psl.engine.RuleAtomGraph;
import de.tuebingen.sfs.psl.talk.TalkingArithmeticRule;

public class EetyOrEunkRule extends TalkingArithmeticRule {

	private static String name = "EetyOrEunk";
	private static String rule = "Eety(X, +Y) + Eunk(X) = 1 .";
	private static String verbalization = "The possible explanations for a word's origin follow a probability distribution.";
	private boolean debuggingMode = false;

	public EetyOrEunkRule(PslProblem pslProblem, boolean debuggingMode) {
		super(name, rule, pslProblem, verbalization);
		this.debuggingMode = debuggingMode;
	}

	public EetyOrEunkRule(PslProblem pslProblem) {
		super(name, rule, pslProblem, verbalization);
	}

	@Override
	public String generateExplanation(String groundingName, String contextAtom, RuleAtomGraph rag,
			boolean whyExplanation) {
		if (!debuggingMode) {
			return super.getDefaultExplanation(groundingName, contextAtom, rag, whyExplanation);
		}
		String expl = super.getDefaultExplanation(groundingName, contextAtom, rag, whyExplanation);
		// TODO
		return expl;
	}

}
