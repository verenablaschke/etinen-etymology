package de.tuebingen.sfs.eie.components.etymology.talk.rule;

import de.tuebingen.sfs.eie.shared.talk.EtinenConstantRenderer;
import de.tuebingen.sfs.eie.shared.talk.pred.EloaPred;
import de.tuebingen.sfs.eie.shared.talk.pred.FsimPred;
import de.tuebingen.sfs.eie.shared.talk.rule.EtinenTalkingArithmeticRule;
import de.tuebingen.sfs.psl.engine.PslProblem;
import de.tuebingen.sfs.psl.engine.RuleAtomGraph;
import de.tuebingen.sfs.psl.talk.BeliefScale;
import de.tuebingen.sfs.psl.util.data.StringUtils;
import de.tuebingen.sfs.psl.util.data.Tuple;

public class FsimSymmetryRule extends EtinenTalkingArithmeticRule {

    public static final String NAME = "FsimSymmetry";
    private static final String RULE = "Fsim(X,Y) = Fsim(Y,X) .";
    private static final String VERBALIZATION = "Form similarity is symmetric.";

    // For serialization.
    public FsimSymmetryRule(String serializedParameters) {
        super(NAME, RULE, VERBALIZATION);
    }

    public FsimSymmetryRule(PslProblem pslProblem) {
        super(NAME, RULE, pslProblem, VERBALIZATION);
    }


    @Override
    public String generateExplanation(EtinenConstantRenderer renderer, String groundingName, String contextAtom,
                                      RuleAtomGraph rag, boolean whyExplanation) {
        String inverseAtom = null;
        double inverseBelief = -1.0;

        for (Tuple atomToStatus : rag.getLinkedAtomsForGroundingWithLinkStatusAsList(groundingName)) {
            String atom = atomToStatus.get(0);
            if (atom.equals(contextAtom)) {
                continue;
            }
            inverseAtom = atom;
            inverseBelief = rag.getValue(atom);
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Form similarity is symmetric, and the ");
        sb.append("\\url[").append(escapeForURL("inverse similarity")).append("]{").append(inverseAtom).append("}");
        sb.append(" is ");
//        sb.append(BeliefScale.verbalizeBeliefAsAdjectiveHigh(inverseBelief));
        sb.append("%.2f".formatted(inverseBelief));
        sb.append(".");
        return sb.toString();
    }

    @Override
    public String getSerializedParameters() {
        return "";
    }
}
