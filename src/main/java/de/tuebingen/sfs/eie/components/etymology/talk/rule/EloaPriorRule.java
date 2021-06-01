package de.tuebingen.sfs.eie.components.etymology.talk.rule;

import de.tuebingen.sfs.eie.shared.talk.EtinenConstantRenderer;
import de.tuebingen.sfs.eie.shared.talk.rule.EtinenTalkingLogicalRule;
import de.tuebingen.sfs.psl.engine.PslProblem;
import de.tuebingen.sfs.psl.engine.RuleAtomGraph;

public class EloaPriorRule extends EtinenTalkingLogicalRule {

    public static final String NAME = "EloaPrior";
    private static final String RULE = "~Eloa(X, Y)";
    private static final String VERBALIZATION = "By default, we do not assume that a word is a loanword.";

    // For serialization.
    public EloaPriorRule(String serializedParameters) {
        super(NAME, RULE, VERBALIZATION);
    }

    public EloaPriorRule(PslProblem pslProblem, double weight) {
        super(NAME, weight + ": " + RULE, pslProblem, VERBALIZATION);
    }

    @Override
    public String generateExplanation(String groundingName, String contextAtom, RuleAtomGraph rag,
                                      boolean whyExplanation) {
        return VERBALIZATION;
    }

    @Override
    public String generateExplanation(EtinenConstantRenderer renderer, String groundingName, String contextAtom,
                                      RuleAtomGraph rag, boolean whyExplanation) {
        return VERBALIZATION;
    }

    @Override
    public String getSerializedParameters() {
        return "";
    }


}
