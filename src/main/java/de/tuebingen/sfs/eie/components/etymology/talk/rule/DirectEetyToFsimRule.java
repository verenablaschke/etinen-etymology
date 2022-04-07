package de.tuebingen.sfs.eie.components.etymology.talk.rule;

import de.tuebingen.sfs.eie.shared.talk.EtinenConstantRenderer;
import de.tuebingen.sfs.eie.shared.talk.pred.FsimPred;
import de.tuebingen.sfs.eie.shared.talk.rule.EtinenTalkingLogicalRule;
import de.tuebingen.sfs.psl.engine.PslProblem;
import de.tuebingen.sfs.psl.engine.RuleAtomGraph;
import de.tuebingen.sfs.psl.util.data.StringUtils;
import de.tuebingen.sfs.psl.util.data.Tuple;

import java.util.List;
import java.util.Locale;

public class DirectEetyToFsimRule extends EtinenTalkingLogicalRule {

    public static final String NAME = "DirectEetyToFsim";
    private static final String RULE = "%.1f: %s(X, Y) & XFufo(X) & XFufo(Y) & -> Fsim(X, Y)";
    private static final String VERBALIZATION = "A word should be phonetically similar to its source form.";

    // For serialization.
    public DirectEetyToFsimRule(String serializedParameters) {
        super(NAME, RULE, VERBALIZATION);
    }

    public DirectEetyToFsimRule(String eetyType1, PslProblem pslProblem, double weight) {
        super(String.format("%sToFsim", eetyType1), String.format(Locale.US, RULE, weight, eetyType1), pslProblem,
                VERBALIZATION);
    }

    @Override
    public String generateExplanation(EtinenConstantRenderer renderer, String groundingName, String contextAtom,
                                      RuleAtomGraph rag, boolean whyExplanation) {
        List<Tuple> atomsToStatuses = rag.getLinkedAtomsForGroundingWithLinkStatusAsList(groundingName);
        String[] fsimArgs = null;
        double fsimBelief = -1.0;
        for (Tuple atomToStatus : atomsToStatuses) {
            String atom = atomToStatus.get(0);
            if (atom.equals(contextAtom)) {
                continue;
            }
            String[] predDetails = StringUtils.split(atom, '(');
            String predName = predDetails[0];
            String[] args = StringUtils.split(predDetails[1].substring(0, predDetails[1].length() - 1), ", ");
            double belief = rag.getValue(atom);
            if (predName.equals("Fsim")) {
                fsimArgs = args;
                fsimBelief = belief;
                break;
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append(VERBALIZATION);
        sb.append(new FsimPred().verbalizeIdeaAsSentence(renderer, fsimBelief, fsimArgs));
        sb.append(" (" + (int) (100 * fsimBelief) + "%)");
        sb.append(". ");
        return sb.toString();
    }

    @Override
    public String getSerializedParameters() {
        return "";
    }


}