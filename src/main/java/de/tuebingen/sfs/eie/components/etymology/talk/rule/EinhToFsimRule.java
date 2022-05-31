package de.tuebingen.sfs.eie.components.etymology.talk.rule;

import de.tuebingen.sfs.eie.shared.talk.EtinenConstantRenderer;
import de.tuebingen.sfs.eie.shared.talk.pred.EinhPred;
import de.tuebingen.sfs.eie.shared.talk.rule.EtinenTalkingLogicalRule;
import de.tuebingen.sfs.psl.engine.PslProblem;
import de.tuebingen.sfs.psl.engine.RuleAtomGraph;
import de.tuebingen.sfs.psl.talk.Belief;
import de.tuebingen.sfs.psl.talk.BeliefScale;
import de.tuebingen.sfs.psl.util.data.StringUtils;
import de.tuebingen.sfs.psl.util.data.Tuple;

public class EinhToFsimRule extends EtinenTalkingLogicalRule {

    public static final String NAME = "EinhToFsim";
    private static final String RULE = "Einh(X,Z) & Einh(Y,Z) & (X != Y) -> Fsim(X,Y)";
    private static final String VERBALIZATION = "If two forms are inherited from the same form, they should be similar.";

    // For serialization.
    public EinhToFsimRule(String serializedParameters) {
        super(serializedParameters);
    }

    public EinhToFsimRule(PslProblem pslProblem, double weight) {
        super(NAME, weight, RULE, pslProblem, VERBALIZATION);
    }


    @Override
    public String generateExplanation(EtinenConstantRenderer renderer, String groundingName, String contextAtom,
                                      RuleAtomGraph rag, boolean whyExplanation) {
        String einh1 = null;
        String[] einh1Args = null;
        String einh2 = null;
        String[] einh2Args = null;
        String fsim = null;
        String[] fsimArgs = null;

        for (Tuple atomToStatus : rag.getLinkedAtomsForGroundingWithLinkStatusAsList(groundingName)) {
            String atom = atomToStatus.get(0);
            if (atom.startsWith("X")) {
                continue;
            }
            String[] atomArgs = StringUtils.split(atom.substring(atom.indexOf('(') + 1, atom.length() - 1), ", ");
            if (atom.startsWith("F")) {
                fsim = atom;
                fsimArgs = atomArgs;
            } else if (einh1 != null) {
                einh2 = atom;
                einh2Args = atomArgs;
            } else {
                einh1 = atom;
                einh1Args = atomArgs;
            }
        }

        double einh1Belief = rag.getValue(einh1);
        double einh2Belief = rag.getValue(einh2);
        double fsimScore = rag.getValue(fsim);
        String x = renderer == null ? einh1Args[0] : renderer.getFormRepresentation(einh1Args[0]);
        String y = renderer == null ? einh2Args[0] : renderer.getFormRepresentation(einh2Args[0]);
        String z = renderer == null ? einh1Args[1] : renderer.getFormRepresentation(einh1Args[1]);

        StringBuilder sb = new StringBuilder();
        sb.append(VERBALIZATION).append(" ");

        if (contextAtom.equals(fsim)) {
            // consequent: 'why not lower?'
            sb.append("Since \\url[");
            sb.append(escapeForURL(new EinhPred().verbalizeIdeaAsSentence(renderer, einh1Belief, einh1Args)));
            sb.append("]{").append(einh1).append("} and \\url[");
            if (einh1Belief <= 0.5 && einh2Belief <= 0.5) {
                // <x is probably not derived from z> and <y is also probably not derived from z>
                sb.append(escapeForURL(y)).append(" is also ");
                sb.append(BeliefScale.verbalizeBeliefAsAdverb(einh2Belief)).append(" inherited from ");
                sb.append(escapeForURL(z));
            } else if (einh1Belief > 0.5 && einh2Belief > 0.5) {
                // <x is probably derived from z> and <y is probably also derived from z>
                sb.append(
                        escapeForURL(new EinhPred().verbalizeIdeaAsSentenceWithAlso(renderer, einh2Belief, einh2Args)));
            } else {
                sb.append(escapeForURL(new EinhPred().verbalizeIdeaAsSentence(renderer, einh2Belief, einh2Args)));
            }
            sb.append("]{").append(einh2).append("}, ");
            double minSim = einh1Belief + einh2Belief - 1;
            if (minSim < RuleAtomGraph.DISSATISFACTION_PRECISION) {
                sb.append(" changing the similarity estimate would actually not cause a rule violation");
            } else {
                sb.append(x).append(" and ").append(y).append(" should ");
                sb.append(BeliefScale.verbalizeBeliefAsMinimumSimilarityInfinitive(minSim));
            }
            return sb.append(".").toString();
        }

        // antecedent -> 'why not higher?'

        if (fsimScore > 1 - RuleAtomGraph.DISSATISFACTION_PRECISION) {
            // Greyed out.
            sb.append("However, since ").append(x).append(" and ").append(y).append(" are ");
            sb.append(BeliefScale.verbalizeBeliefAsSimilarity(rag.getValue(fsim))); // 'extremely similar'
            sb.append(", changing either of the inheritance judgments wouldn't cause a rule violation. (\\url[");
            sb.append(new EinhPred().verbalizeIdeaAsSentence(renderer, einh1Belief, einh1Args));
            sb.append("]{").append(einh1).append("} and \\url[");
            sb.append(new EinhPred().verbalizeIdeaAsSentence(renderer, einh2Belief, einh2Args));
            sb.append("]{").append(einh2).append("}.)");
            return sb.toString();
        }

        sb.append("Since \\url[");
        if (contextAtom.equals(einh1)) {
            sb.append(new EinhPred().verbalizeIdeaAsSentence(renderer, einh2Belief, einh2Args));
            sb.append("]{").append(einh2);
        } else {
            sb.append(new EinhPred().verbalizeIdeaAsSentence(renderer, einh1Belief, einh1Args));
            sb.append("]{").append(einh1);
        }
        sb.append("} but is \\url[").append(BeliefScale.verbalizeBeliefAsSimilarityWithOnly(fsimScore)).append(" to ");
        sb.append(escapeForURL(contextAtom.equals(einh1) ? y : x)).append("]{").append(fsim).append("}, ");
        double maxVal = fsimScore + 1 - (contextAtom.equals(einh1) ? einh2Belief : einh1Belief);
        if (maxVal > 1 - RuleAtomGraph.DISSATISFACTION_PRECISION) {
            sb.append("the inheritance judgment for ").append(contextAtom.equals(einh1) ? x : y);
            sb.append(" doesn't actually have an influence here.");
        } else {
            sb.append(" it should ");
//                    sb.append(BeliefScale.verbalizeBeliefAsInfinitiveMaximumPredicate(maxVal));
            sb.append(" be unlikely");
            sb.append(" that ").append(contextAtom.equals(einh1) ? x : y).append(" is derived from the same source.");
        }
        return sb.toString();
    }

    @Override
    public String getSerializedParameters() {
        return "";
    }
}
