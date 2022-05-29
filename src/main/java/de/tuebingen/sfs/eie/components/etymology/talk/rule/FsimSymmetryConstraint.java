package de.tuebingen.sfs.eie.components.etymology.talk.rule;

import de.tuebingen.sfs.eie.shared.talk.EtinenConstantRenderer;
import de.tuebingen.sfs.eie.shared.talk.rule.EtinenTalkingArithmeticConstraint;
import de.tuebingen.sfs.psl.engine.PslProblem;
import de.tuebingen.sfs.psl.engine.RuleAtomGraph;
import de.tuebingen.sfs.psl.talk.BeliefScale;
import de.tuebingen.sfs.psl.util.data.Tuple;

public class FsimSymmetryConstraint extends EtinenTalkingArithmeticConstraint {

    public static final String NAME = "FsimSymmetry";
    private static final String RULE = "Fsim(X,Y) = Fsim(Y,X) .";
    private static final String VERBALIZATION = "Form similarity is symmetric.";

    // For serialization.
    public FsimSymmetryConstraint(String serializedParameters) {
        // No idiosyncrasies in this rule, just use default values:
        super(NAME, RULE, VERBALIZATION);
    }

    public FsimSymmetryConstraint(PslProblem pslProblem) {
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

        if (inverseBelief < 0) {
            // Stayed -1 because the contextAtom and the inverse atom are identical.
            return "Form similarity is symmetric.";
        }

        boolean similar = Math.abs(inverseBelief - rag.getValue(contextAtom)) < 0.01;

        StringBuilder sb = new StringBuilder();
        sb.append("Form similarity is symmetric, ").append(similar ? "and" : "but");
        sb.append(" the \\url[").append(escapeForURL("inverse similarity")).append("]{").append(inverseAtom);
        sb.append("} is ");
        if (similar) {
            sb.append("also ");
        }
        sb.append(BeliefScale.verbalizeBeliefAsAdjectiveHigh(inverseBelief));
        if (!similar) {
            sb.append(" (%.2f)".formatted(inverseBelief));
        }
        sb.append(".");
        return sb.toString();
    }

    @Override
    public String getSerializedParameters() {
        return "";
    }
}
