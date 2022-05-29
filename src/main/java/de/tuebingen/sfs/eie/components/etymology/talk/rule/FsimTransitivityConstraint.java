package de.tuebingen.sfs.eie.components.etymology.talk.rule;

import de.tuebingen.sfs.eie.shared.talk.EtinenConstantRenderer;
import de.tuebingen.sfs.eie.shared.talk.pred.FsimPred;
import de.tuebingen.sfs.eie.shared.talk.rule.EtinenTalkingArithmeticConstraint;
import de.tuebingen.sfs.psl.engine.PslProblem;
import de.tuebingen.sfs.psl.engine.RuleAtomGraph;
import de.tuebingen.sfs.psl.talk.BeliefScale;
import de.tuebingen.sfs.psl.util.data.StringUtils;
import de.tuebingen.sfs.psl.util.data.Tuple;

public class FsimTransitivityConstraint extends EtinenTalkingArithmeticConstraint {

    public static final String NAME = "FsimTransitivity";
    private static final String RULE = "Fsim(X,Y) & Fsim(Y,Z) & (X != Y) & (X != Z) & (Y != Z) -> Fsim(X,Z) .";
    private static final String VERBALIZATION = "Form similarity is transitive: " +
            "if a form is similar to two other forms, those should also be similar to one another.";

    // For serialization.
    public FsimTransitivityConstraint(String serializedParameters) {
        // No idiosyncrasies in this rule, just use default values:
        super(NAME, RULE, VERBALIZATION);
    }

    public FsimTransitivityConstraint(PslProblem pslProblem) {
        super(NAME, RULE, pslProblem, VERBALIZATION);
    }


    @Override
    public String generateExplanation(EtinenConstantRenderer renderer, String groundingName, String contextAtom,
                                      RuleAtomGraph rag, boolean whyExplanation) {
        String antecedent0 = null;
        String antecedent1 = null;
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
            } else if (antecedent0 == null) {
                antecedent0 = atom;
                beliefVals[0] = rag.getValue(atom);
                args[0] = atomArgs;
            } else {
                antecedent1 = atom;
                beliefVals[1] = rag.getValue(atom);
                args[1] = atomArgs;
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append(VERBALIZATION);
        sb.append("\n");

        if (rag.getValue(contextAtom) > 0.999) {
            sb.append("(");
            String ante1 = new FsimPred().verbalizeIdeaAsSentence(renderer, beliefVals[0], args[0]);
            if (ante1.startsWith("the")) {
                ante1 = "T" + ante1.substring(1);
            }
            if (contextAtom.equals(antecedent0)) {
                sb.append(ante1);
            } else {
                sb.append("\\url[").append(escapeForURL(ante1)).append("]{").append(antecedent0).append("}");
            }
            sb.append(", ");
            if (contextAtom.equals(antecedent1)) {
                sb.append(new FsimPred().verbalizeIdeaAsSentence(renderer, beliefVals[1], args[1]));
            } else {
                sb.append("\\url[");
                sb.append(escapeForURL(new FsimPred().verbalizeIdeaAsSentence(renderer, beliefVals[1], args[1])));
                sb.append("]{").append(antecedent1).append("}");
            }
            sb.append(", and ");
            if (contextAtom.equals(consequent)) {
                sb.append(new FsimPred().verbalizeIdeaAsSentence(renderer, beliefVals[2], args[2]));
            } else {
                sb.append("\\url[");
                sb.append(escapeForURL(new FsimPred().verbalizeIdeaAsSentence(renderer, beliefVals[2], args[2])));
                sb.append("]{").append(consequent).append("}");
            }
            sb.append(").");
            return sb.toString();
        }

        if (contextAtom.equals(consequent)) {
            // Why not lower?
            sb.append("Since \\url[");
            sb.append(escapeForURL(new FsimPred().verbalizeIdeaAsSentence(renderer, beliefVals[0], args[0])));
            sb.append("]{").append(antecedent0).append("} and the ");
            sb.append("\\url[");
            sb.append(escapeForURL(new FsimPred().verbalizeIdeaAsSentence(renderer, beliefVals[1], args[1])));
            sb.append("]{").append(antecedent1).append("}, ");
            args[2] = FsimPred.updateArgs(renderer, args[2]);
            sb.append(args[2][0]).append(" and ").append(args[2][1]).append(" should ");
            double minSim = beliefVals[0] + beliefVals[1] - 1;
            if (minSim < 0) {
                minSim = 0;
            }
            sb.append(BeliefScale.verbalizeBeliefAsMinimumSimilarityInfinitive(minSim)).append(".");
            return sb.toString();
        }

        String x = renderer == null ? args[0][0] : renderer.getFormRepresentation(args[0][0]);
        String y = renderer == null ? args[0][1] : renderer.getFormRepresentation(args[0][1]);
        String z = renderer == null ? args[1][1] : renderer.getFormRepresentation(args[1][1]);
        sb.append("The similarities ");
        if (contextAtom.equals(antecedent0)) {
            sb.append(" between ").append(x).append(" and ").append(y);
        } else {
            sb.append(" between \\url[").append(escapeForURL(x)).append(" and ").append(escapeForURL(y));
            sb.append("]{").append(antecedent0).append("} (");
            sb.append(BeliefScale.verbalizeBeliefAsSimilarity(beliefVals[0])).append(")");
        }
        sb.append(" and ");
        if (contextAtom.equals(antecedent1)) {
            sb.append(" between ").append(y).append(" and ").append(z);
        } else {
            sb.append(" between \\url[").append(escapeForURL(y)).append(" and ").append(escapeForURL(z));
            sb.append("]{").append(antecedent1).append("} (");
            sb.append(BeliefScale.verbalizeBeliefAsSimilarity(beliefVals[1])).append(")");
        }
        sb.append(" determine a minimum similarity \\url[between").append(escapeForURL(x));
        sb.append(" and ").append(escapeForURL(z)).append("]{").append(consequent).append("}. ");

        if (beliefVals[2] > 0.999) {
            // Entire rule is greyed out.
            sb.append("However, since ").append(x).append(" and ").append(z).append(" are already ");
            sb.append(BeliefScale.verbalizeBeliefAsSimilarity(beliefVals[2])); // 'extremely similar'
            sb.append(", changing the similarity of ").append(x).append(" and ").append(y);
            sb.append(" wouldn't cause a rule violation.");
            return sb.toString();
        }

        sb.append("Since ").append(x).append(" and ").append(z).append(" are ");
        sb.append(BeliefScale.verbalizeBeliefAsSimilarityWithOnly(beliefVals[2]));
        sb.append(", the similarity between ").append(x).append(" and ").append(y).append(" cannot be ");
        double maxSim = beliefVals[2] + 1 - beliefVals[contextAtom.equals(antecedent0) ? 1 : 0];
        if (maxSim > 1) {
            maxSim = 1;
        }
        if (maxSim - beliefVals[contextAtom.equals(antecedent0) ? 0 : 1] > 0.01) {
            sb.append("much");
        } else {
            sb.append("any");
        }
        sb.append(" higher.");
        return sb.toString();
    }

    @Override
    public String getSerializedParameters() {
        return "";
    }
}
