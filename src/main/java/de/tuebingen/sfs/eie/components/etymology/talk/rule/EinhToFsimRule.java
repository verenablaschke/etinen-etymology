package de.tuebingen.sfs.eie.components.etymology.talk.rule;

import de.tuebingen.sfs.eie.shared.talk.EtinenConstantRenderer;
import de.tuebingen.sfs.eie.shared.talk.pred.EinhPred;
import de.tuebingen.sfs.eie.shared.talk.rule.EtinenTalkingLogicalRule;
import de.tuebingen.sfs.psl.engine.PslProblem;
import de.tuebingen.sfs.psl.engine.RuleAtomGraph;
import de.tuebingen.sfs.psl.talk.BeliefScale;
import de.tuebingen.sfs.psl.util.data.StringUtils;
import de.tuebingen.sfs.psl.util.data.Tuple;

public class EinhToFsimRule extends EtinenTalkingLogicalRule {

    public static final String NAME = "EinhToFsim";
    private static final String RULE = "Einh(X,Z) & Einh(Y,Z) & (X != Y) -> Fsim(X,Y)";
    private static final String VERBALIZATION = "If two forms are inherited from the same form, they should be similar";

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
        sb.append(VERBALIZATION).append("\n");

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

        if (fsimScore > 1 - RuleAtomGraph.DISSATISFACTION_PRECISION) {
            // Greyed out.
            sb.append("The judgments for the inheritance between ");
            if (contextAtom.equals(einh1)) {
                sb.append(x).append(" and ").append(z).append(" and between \\url[");
                sb.append(escapeForURL(y)).append(" and ").append(escapeForURL(z));
                sb.append("]{").append(einh2).append("} (");
                sb.append(BeliefScale.verbalizeBeliefAsAdjective(einh2Belief)).append(")");
            } else {
                sb.append("\\url[");
                sb.append(escapeForURL(x)).append(" and ").append(escapeForURL(z));
                sb.append("]{").append(einh1).append("} (");
                sb.append(BeliefScale.verbalizeBeliefAsAdjective(einh1Belief)).append(")");
                sb.append("and between ").append(y).append(" and ").append(z);
            }
            sb.append(" imply a minimum similarity for ").append(x).append(" and ").append(y);
            sb.append("However, since these two forms are ");
            sb.append(BeliefScale.verbalizeBeliefAsSimilarity(rag.getValue(fsim))); // 'extremely similar'
            sb.append(", changing either of the inheritance judgments wouldn't cause a rule violation.");
        }

        // antecedent -> 'why not higher?'

        sb.append("The judgments for the inheritance between ");
        sb.append(x).append(" and ").append(z).append(" and between");
        sb.append(y).append(" and ").append(z);
        sb.append(" imply a minimum similarity for ").append(x).append(" and ").append(y);
        sb.append("However, since these two forms are ");
        sb.append(BeliefScale.verbalizeBeliefAsSimilarityWithOnly(rag.getValue(fsim)));
        sb.append(", the maximum inheritance judgments are limited, and since ");
        if (contextAtom.equals(einh1)) {
            sb.append(new EinhPred().verbalizeIdeaAsSentence(renderer, einh2Belief, einh2Args));
            sb.append(", ").append(new EinhPred().verbalizeIdeaAsNP(renderer, einh1Args));
            sb.append(" should ").append(BeliefScale.verbalizeBeliefAsInfinitiveMaximumPredicate(einh1Belief));
        } else {
            sb.append(new EinhPred().verbalizeIdeaAsSentence(renderer, einh1Belief, einh1Args));
            sb.append(", ").append(new EinhPred().verbalizeIdeaAsNP(renderer, einh2Args));
            sb.append(" should ").append(BeliefScale.verbalizeBeliefAsInfinitiveMaximumPredicate(einh2Belief));
        }
        sb.append(".");
        return sb.toString();
    }

    @Override
    public String getSerializedParameters() {
        return "";
    }
}
