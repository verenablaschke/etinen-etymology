package de.tuebingen.sfs.eie.components.etymology.talk.rule;

import de.tuebingen.sfs.eie.shared.talk.EtinenConstantRenderer;
import de.tuebingen.sfs.eie.shared.talk.pred.FsimPred;
import de.tuebingen.sfs.eie.shared.talk.rule.EtinenTalkingArithmeticRule;
import de.tuebingen.sfs.psl.engine.PslProblem;
import de.tuebingen.sfs.psl.engine.RuleAtomGraph;
import de.tuebingen.sfs.psl.talk.BeliefScale;
import de.tuebingen.sfs.psl.util.data.StringUtils;
import de.tuebingen.sfs.psl.util.data.Tuple;

public class FsimTransitivityRule extends EtinenTalkingArithmeticRule {

    public static final String NAME = "FsimTransitivity";
    private static final String RULE = "Fsim(X,Y) & Fsim(Y,Z) & (X != Y) & (X != Z) & (Y != Z) -> Fsim(X,Z) .";
    private static final String VERBALIZATION = "Form similarity is transitive.";

    // For serialization.
    public FsimTransitivityRule(String serializedParameters) {
        super(NAME, RULE, VERBALIZATION);
    }

    public FsimTransitivityRule(PslProblem pslProblem) {
        super(NAME, RULE, pslProblem, VERBALIZATION);
    }


    @Override
    public String generateExplanation(EtinenConstantRenderer renderer, String groundingName, String contextAtom,
                                      RuleAtomGraph rag, boolean whyExplanation) {
        String antecedent1 = null;
        String antecedent2 = null;
        String consequent = null;
        double[] beliefVals = new double[3];
        String[][] args = new String[3][];

        for (Tuple atomToStatus : rag.getLinkedAtomsForGroundingWithLinkStatusAsList(groundingName)) {
            String atom = atomToStatus.get(0);
            String[] atomArgs = StringUtils.split(atom.substring(atom.indexOf('(') + 1, atom.length() - 1), ", ");
            if (atomToStatus.get(1).equals("+")) {
                consequent = atom;
                beliefVals[2] = rag.getValue(atom);
                args[2] = atomArgs;
            } else if (antecedent1 == null) {
                antecedent1 = atom;
                beliefVals[0] = rag.getValue(atom);
                args[0] = atomArgs;
            } else {
                antecedent2 = atom;
                beliefVals[1] = rag.getValue(atom);
                args[1] = atomArgs;
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append(VERBALIZATION);
        sb.append("\n");

        if (contextAtom.equals(consequent)) {
            sb.append("The \\url[");
            sb.append(escapeForURL(new FsimPred().verbalizeIdeaAsNP(renderer, beliefVals[0], args[0])));
            sb.append("]{").append(antecedent1).append("} and the ");
            sb.append("\\url[");
            sb.append(escapeForURL(new FsimPred().verbalizeIdeaAsNP(renderer, beliefVals[1], args[1])));
            sb.append("]{").append(antecedent2).append("} imply that ");
            args[2] = FsimPred.updateArgs(renderer, args[2]);
            sb.append(args[2][0]).append(" and ").append(args[2][1]).append(" should be at least ");
            double minSim = beliefVals[0] + beliefVals[1] - 1;
            if (minSim < 0) {
                minSim = 0;
                // TODO in that case, the rule should be phrased differently.
            }
            // TODO this comes off the wrong way when minSim is low ("should be at least dissimilar")
            sb.append(BeliefScale.verbalizeBeliefAsSimilarity(minSim)).append(".");
            return sb.toString();
        }

        // TODO rephrase?
        sb.append(" Since \\url[");
        int otherAnteIdx = contextAtom.equals(antecedent1) ? 1 : 0;
        sb.append(escapeForURL(
                new FsimPred().verbalizeIdeaAsSentence(renderer, beliefVals[otherAnteIdx], args[otherAnteIdx])));
        sb.append("]{").append(otherAnteIdx == 0 ? antecedent1 : antecedent2).append("} and  \\url[");
        sb.append(escapeForURL(
                new FsimPred().verbalizeIdeaAsSentence(renderer, beliefVals[2], args[2])));
        sb.append("]{").append(consequent).append("}, the similarity between ");
        int contextIdx = otherAnteIdx == 0 ? 1 : 0;
        args[contextIdx] = FsimPred.updateArgs(renderer, args[contextIdx]);
        sb.append(args[contextIdx][0]).append(" and ").append(args[contextIdx][1]).append(" should not exceed ");
        double maxSim = beliefVals[2] + 1 - beliefVals[otherAnteIdx];
        if (maxSim > 1) {
            maxSim = 1;
            // TODO in that case, the rule should be phrased differently.
        }
        sb.append("%.2f".formatted(maxSim));
        return sb.toString();
    }

    @Override
    public String getSerializedParameters() {
        return "";
    }
}
