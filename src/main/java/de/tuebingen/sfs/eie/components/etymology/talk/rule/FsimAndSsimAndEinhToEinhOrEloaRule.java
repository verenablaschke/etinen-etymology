package de.tuebingen.sfs.eie.components.etymology.talk.rule;

import de.tuebingen.sfs.psl.engine.PslProblem;
import de.tuebingen.sfs.psl.engine.RuleAtomGraph;
import de.tuebingen.sfs.psl.talk.TalkingLogicalRule;

public class FsimAndSsimAndEinhToEinhOrEloaRule extends TalkingLogicalRule {
	// TODO add restriction that ~Eety(X,Y), ~Eety(Y,X) ?
	
	private static final String RULE = "Fufo(X, F1) & Fufo(Y, F2) & Fsim(F1, F2) &" // phonetic similarity
//			+ "Fsem(X, C1) & Fsem(Y, C2) & Ssim(C1, C2) &" // semantic similarity TODO uncomment
			+ "Einh(X, Z) & (X != Y) & (Y != Z)" + "-> Einh(Y, Z) | Eloa(Y, Z)"; // -> same source
	private static final String VERBALIZATION = "If two words are phonetically and semantically similar, "
			+ "they are probably derived from the same source.";

	public FsimAndSsimAndEinhToEinhOrEloaRule(PslProblem pslProblem, double weight) {
		super("FsimAndSsimAndEinhToEety", weight + ": " + RULE, pslProblem, VERBALIZATION);
	}
	
	@Override
	public String generateExplanation(String groundingName, String contextAtom, RuleAtomGraph rag,
			boolean whyExplanation) {
		// TODO
		return getDefaultExplanation(groundingName, contextAtom, rag, whyExplanation);
	}

}
