package de.tuebingen.sfs.eie.components.etymology.talk.rule;

import de.tuebingen.sfs.eie.shared.talk.EtinenConstantRenderer;
import de.tuebingen.sfs.eie.shared.talk.pred.EinhPred;
import de.tuebingen.sfs.eie.shared.talk.pred.EloaPred;
import de.tuebingen.sfs.eie.shared.talk.pred.FhomPred;
import de.tuebingen.sfs.eie.shared.talk.rule.EtinenTalkingLogicalRule;
import de.tuebingen.sfs.psl.engine.PslProblem;
import de.tuebingen.sfs.psl.engine.RuleAtomGraph;
import de.tuebingen.sfs.psl.talk.BeliefScale;
import de.tuebingen.sfs.psl.util.data.StringUtils;
import de.tuebingen.sfs.psl.util.data.Tuple;

public class FhomToEinhRule extends EtinenTalkingLogicalRule {

    public static final String NAME = "FhomToEinh";
    private static final String RULE = "Fhom(X,H) & Fhom(Y,H) & Xinh(X,Y) -> Einh(X,Y)";
    private static final String VERBALIZATION = "If both parent and child share the same homologue set, that provides some evidence of inheritance.";

    // For serialization.
    public FhomToEinhRule(String serializedParameters) {
        super(serializedParameters);
    }

    public FhomToEinhRule(PslProblem pslProblem, double weight) {
        super(NAME, weight, RULE, pslProblem, VERBALIZATION);
    }


    @Override
    public String generateExplanation(EtinenConstantRenderer renderer, String groundingName, String contextAtom,
                                      RuleAtomGraph rag, boolean whyExplanation) {
        String fhomChild = null;
        String[] fhomChildArgs = null;
        String fhomParent = null;
        String[] fhomParentArgs = null;
        String einh = null;
        String[] einhArgs = null;

        for (Tuple atomToStatus : rag.getLinkedAtomsForGroundingWithLinkStatusAsList(groundingName)) {
            String atom = atomToStatus.get(0);
            if (atom.startsWith("X")) {
                continue;
            }
            String[] atomArgs = StringUtils.split(atom.substring(atom.indexOf('(') + 1, atom.length() - 1), ", ");
            if (atom.startsWith("E")) {
                einh = atom;
                einhArgs = atomArgs;
            } else if (fhomChild != null) {
                fhomParent = atom;
                fhomParentArgs = atomArgs;
            } else {
                fhomChild = atom;
                fhomChildArgs = atomArgs;
            }
        }

        double fhomChildBelief = rag.getValue(fhomChild);
        double fhomParentBelief = rag.getValue(fhomParent);
        double einhBelief = rag.getValue(einh);
        String x = renderer == null ? fhomChildArgs[0] : renderer.getFormRepresentation(fhomChildArgs[0]);
        String y = renderer == null ? fhomParentArgs[0] : renderer.getFormRepresentation(fhomParentArgs[0]);

        StringBuilder sb = new StringBuilder();
        sb.append(VERBALIZATION).append("\n");

        if (contextAtom.equals(einh)) {
            // consequent: 'why not lower?'
            sb.append("Since \\url[");
            sb.append(escapeForURL(new FhomPred().verbalizeIdeaAsSentence(renderer, fhomChildBelief, fhomChildArgs)));
            sb.append("]{").append(fhomChild).append("} ");
            if ((fhomChildBelief <= 0.5 && fhomParentBelief <= 0.5) ||
                    (fhomChildBelief > 0.5 && fhomParentBelief > 0.5)) {
                sb.append("and \\url[");
                sb.append(escapeForURL(
                        new FhomPred().verbalizeIdeaAsSentenceWithAlso(renderer, fhomParentBelief, fhomParentArgs)));
            } else {
                sb.append("but \\url[");
                sb.append(escapeForURL(
                        new FhomPred().verbalizeIdeaAsSentence(renderer, fhomParentBelief, fhomParentArgs)));
            }
            sb.append("]{").append(fhomParent).append("}, ");
            double minLoa = fhomChildBelief + fhomParentBelief - 1;
            if (minLoa < RuleAtomGraph.DISSATISFACTION_PRECISION) {
                sb.append(" changing the inheritance judgment would actually not cause a rule violation");
            } else {
                sb.append("an inheritance relationship should ")
                        .append(BeliefScale.verbalizeBeliefAsInfinitiveMinimumPredicate(minLoa));
            }
            return sb.append(".").toString();
        }

        // antecedent -> 'why not higher?'

        String h = renderer == null ? fhomParentArgs[1] : renderer.getFormRepresentation(fhomParentArgs[1]);
        sb.append("The homology judgments for ");
        if (contextAtom.equals(fhomChild)) {
            sb.append(x).append(" and ").append(h).append(" and for \\url[");
            sb.append(escapeForURL(y)).append(" and ").append(escapeForURL(h));
            sb.append("]{").append(fhomParent).append("} (");
            sb.append(BeliefScale.verbalizeBeliefAsAdjective(fhomParentBelief)).append(")");
        } else {
            sb.append(y).append(" and ").append(h).append(" and for \\url[");
            sb.append(escapeForURL(x)).append(" and ").append(escapeForURL(h));
            sb.append("]{").append(fhomChild).append("} (");
            sb.append(BeliefScale.verbalizeBeliefAsAdjective(fhomChildBelief)).append(")");
        }
        sb.append(" imply a minimum certainty for ").append(x).append(" being a loanword. ");

        if (einhBelief > 1 - RuleAtomGraph.DISSATISFACTION_PRECISION) {
            // Greyed out.
            sb.append("However, since it is already ");
            sb.append(BeliefScale.verbalizeBeliefAsAdjective(rag.getValue(einh))); // 'extremely likely'
            sb.append(" that ").append(x).append(" is borrowed, changing either of the homology judgments ");
            sb.append("wouldn't cause a rule violation.");
        }

        sb.append("Since it ").append(BeliefScale.verbalizeBeliefAsPredicateWithOnly(einhBelief));
        sb.append(" that ").append(new EinhPred().verbalizeIdeaAsSentence(renderer, einhArgs));
        sb.append(", the possibility of ");
        if (contextAtom.equals(fhomChild)) {
            sb.append(x);
        } else {
            sb.append(y);
        }
        sb.append(" is limited.");
        return sb.toString();
    }

    @Override
    public String getSerializedParameters() {
        return "";
    }
}
