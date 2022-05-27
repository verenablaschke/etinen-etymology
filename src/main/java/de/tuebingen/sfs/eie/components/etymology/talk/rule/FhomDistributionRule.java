package de.tuebingen.sfs.eie.components.etymology.talk.rule;

import de.tuebingen.sfs.eie.shared.talk.EtinenConstantRenderer;
import de.tuebingen.sfs.eie.shared.talk.pred.EloaPred;
import de.tuebingen.sfs.eie.shared.talk.pred.FhomPred;
import de.tuebingen.sfs.eie.shared.talk.rule.EtinenTalkingArithmeticRule;
import de.tuebingen.sfs.psl.engine.PslProblem;
import de.tuebingen.sfs.psl.engine.RuleAtomGraph;
import de.tuebingen.sfs.psl.talk.BeliefScale;
import de.tuebingen.sfs.psl.util.data.StringUtils;
import de.tuebingen.sfs.psl.util.data.Tuple;

public class FhomDistributionRule extends EtinenTalkingArithmeticRule {

    public static final String NAME = "FhomDistribution";
    private static final String RULE = "Fhom(X,+H) = 1.";
    private static final String VERBALIZATION = "Every word must belong to exactly one homologue set.";

    // For serialization.
    public FhomDistributionRule(String serializedParameters) {
        super(NAME, RULE, VERBALIZATION);
    }

    public FhomDistributionRule(PslProblem pslProblem) {
        super(NAME, RULE, pslProblem, VERBALIZATION);
    }

    @Override
    public String generateExplanation(String groundingName, String contextAtom, RuleAtomGraph rag,
                                      boolean whyExplanation) {
        return generateExplanation(null, groundingName, contextAtom, rag, whyExplanation);
    }

    @Override
    public String generateExplanation(EtinenConstantRenderer renderer, String groundingName, String contextAtom,
                                      RuleAtomGraph rag, boolean whyExplanation) {
        StringBuilder sb = new StringBuilder();
        sb.append(VERBALIZATION);
        boolean first = true;
        for (Tuple atomToStatus : rag.getLinkedAtomsForGroundingWithLinkStatusAsList(groundingName)) {
            String atom = atomToStatus.get(0);
            if (atom.equals(contextAtom)) {
                continue;
            }

            if (first) {
                sb.append("\nIt");
                first = false;
            } else {
                sb.append(", and it");
            }

            String[] args = StringUtils.split(StringUtils.split(atom.substring(0, atom.length() - 1), '(')[1], ", ");
            args = FhomPred.updateArgs(renderer, args);
            sb.append(" is ").append(BeliefScale.verbalizeBeliefAsAdjective(rag.getValue(atom)));
            sb.append(" that \\url[").append(escapeForURL(args[0])).append(" instead is a homologue of ");
            sb.append(escapeForURL(args[1])).append("]{").append(atom).append("}");
        }
        if (!first) {
            sb.append(".");
        }
        return sb.toString();
    }

    @Override
    public String getSerializedParameters() {
        return "";
    }


}
