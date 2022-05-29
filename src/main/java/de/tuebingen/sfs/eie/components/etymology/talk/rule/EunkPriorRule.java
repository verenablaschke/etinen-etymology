package de.tuebingen.sfs.eie.components.etymology.talk.rule;

import de.tuebingen.sfs.eie.shared.talk.EtinenConstantRenderer;
import de.tuebingen.sfs.eie.shared.talk.rule.EtinenTalkingLogicalRule;
import de.tuebingen.sfs.psl.engine.PslProblem;
import de.tuebingen.sfs.psl.engine.RuleAtomGraph;

public class EunkPriorRule extends EtinenTalkingLogicalRule {

    public static final String NAME = "EunkPrior";
    public static final String RULE = "~Eunk(X)";
    public static final String VERBALIZATION = "By default, we prefer available explanations within the scope of the forms the inference is performed over.";

    // For serialization.
    public EunkPriorRule(String serializedParameters) {
        super(serializedParameters);
    }

    public EunkPriorRule(PslProblem pslProblem, double weight) {
        super(NAME, weight, RULE, pslProblem, VERBALIZATION);
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
