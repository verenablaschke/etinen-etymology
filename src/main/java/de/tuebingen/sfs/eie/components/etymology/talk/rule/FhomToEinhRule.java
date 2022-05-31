package de.tuebingen.sfs.eie.components.etymology.talk.rule;

import de.tuebingen.sfs.eie.shared.talk.EtinenConstantRenderer;
import de.tuebingen.sfs.eie.shared.talk.pred.EinhPred;
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
    private static final String VERBALIZATION =
            "If the forms in a language and its parent are assigned to the same homologue set, " +
                    "this suggests that the form in the child language was inherited.";

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
        String childForm = renderer == null ? fhomChildArgs[0] : renderer.getFormRepresentation(fhomChildArgs[0]);
        String parentForm = renderer == null ? fhomParentArgs[0] : renderer.getFormRepresentation(fhomParentArgs[0]);
        String hom = renderer == null ? fhomParentArgs[1] : renderer.getFormRepresentation(fhomParentArgs[1]);

        StringBuilder sb = new StringBuilder();
        sb.append(VERBALIZATION).append(" ");

        if (contextAtom.equals(einh)) {
            // consequent: 'why not lower?'
            sb.append("Applying this logic to the homologue set for ").append(hom).append(", ");

            String lowerFhom;
            double lowerFhomBelief;
            String[] lowerFhomArgs;
            String lowerForm;
            String higherFhom;
            double higherFhomBelief;
            String[] higherFhomArgs;
            String higherForm;
            if (fhomParentBelief > fhomChildBelief) {
                lowerFhom = fhomChild;
                lowerFhomBelief = fhomChildBelief;
                lowerFhomArgs = fhomChildArgs;
                lowerForm = childForm;
                higherFhomBelief = fhomParentBelief;
                higherFhom = fhomParent;
                higherFhomArgs = fhomParentArgs;
                higherForm = parentForm;
            } else {
                lowerFhom = fhomParent;
                lowerFhomBelief = fhomParentBelief;
                lowerFhomArgs = fhomParentArgs;
                lowerForm = parentForm;
                higherFhom = fhomChild;
                higherFhomBelief = fhomChildBelief;
                higherFhomArgs = fhomChildArgs;
                higherForm = childForm;
            }

            if (lowerFhomBelief < RuleAtomGraph.DISSATISFACTION_PRECISION) {
                // The rule is trivially satisfied because at least one of the forms doesn't belong to the grounding's homologue set.
                sb.append("since \\url[");
                if (renderer == null) {
                    sb.append(escapeForURL(lowerFhom));
                } else {
                    sb.append("the ").append(escapeForURL(renderer.getLanguageRepresentationForForm(lowerFhomArgs[0])));
                    sb.append(" form");
                }
                sb.append("]{").append(lowerFhom).append("} almost certainly does not belong to this set (");
                if (fhomChildBelief < 0.5) {
                    sb.append("and");
                    sb.append(new FhomPred().verbalizeIdeaAsSentence(renderer, higherFhomBelief, higherFhomArgs));
                } else {
                    sb.append("although \\url[");
                    if (renderer == null) {
                        sb.append(escapeForURL(higherForm));
                    } else {
                        sb.append("the ");
                        sb.append(escapeForURL(renderer.getLanguageRepresentationForForm(higherFhomArgs[0])));
                        sb.append(" one");
                    }
                    sb.append("]{").append(higherFhom).append("} ");
                    sb.append(BeliefScale.verbalizeBeliefAsAdverb(higherFhomBelief));
                    sb.append(" does");
                }
                sb.append("), the inheritance judgment is not influenced.");
                return sb.toString();
            }

            sb.append("since neither homology judgment is entirely unlikely (\\url[");
            sb.append(escapeForURL(new FhomPred().verbalizeIdeaAsSentence(renderer, higherFhomBelief, higherFhomArgs)));
            sb.append("]{").append(higherFhom).append("} ");
            if ((lowerFhomBelief <= 0.5 && higherFhomBelief <= 0.5) ||
                    (lowerFhomBelief > 0.5 && higherFhomBelief > 0.5)) {
                sb.append("and \\url[");
                sb.append(escapeForURL(
                        new FhomPred().verbalizeIdeaAsSentenceWithAlso(renderer, lowerFhomBelief, lowerFhomArgs)));
            } else {
                sb.append("although \\url[");
                sb.append(escapeForURL(
                        new FhomPred().verbalizeIdeaAsSentence(renderer, lowerFhomBelief, lowerFhomArgs)));
            }
            sb.append("]{").append(lowerFhom).append("}), ");
            sb.append("we cannot disregard the possibility that ").append(childForm).append(" is inherited from ");
            sb.append(parentForm).append(".");
            return sb.toString();

//            double minLoa = lowerFhomBelief + higherFhomBelief - 1;
//            sb.append("an inheritance relationship should ");
////            sb.append("not be impossible."); // TODO
//            sb.append(BeliefScale.verbalizeBeliefAsInfinitiveMinimumPredicate(minLoa));
//            return sb.append(".").toString();
        }

        // antecedent -> 'why not higher?'

        sb.append("The homology judgments for ");
        if (contextAtom.equals(fhomChild)) {
            sb.append(childForm).append(" and ").append(hom).append(" and for \\url[");
            sb.append(escapeForURL(parentForm)).append(" and ").append(escapeForURL(hom));
            sb.append("]{").append(fhomParent).append("} (");
            sb.append(BeliefScale.verbalizeBeliefAsAdjective(fhomParentBelief)).append(")");
        } else {
            sb.append(parentForm).append(" and ").append(hom).append(" and for \\url[");
            sb.append(escapeForURL(childForm)).append(" and ").append(escapeForURL(hom));
            sb.append("]{").append(fhomChild).append("} (");
            sb.append(BeliefScale.verbalizeBeliefAsAdjective(fhomChildBelief)).append(")");
        }
        sb.append(" imply a minimum certainty for ").append(childForm).append(" being a loanword. ");

        if (einhBelief > 1 - RuleAtomGraph.DISSATISFACTION_PRECISION) {
            // Greyed out.
            sb.append("However, since it is already ");
            sb.append(BeliefScale.verbalizeBeliefAsAdjective(rag.getValue(einh))); // 'extremely likely'
            sb.append(" that ").append(childForm).append(" is borrowed, changing either of the homology judgments ");
            sb.append("wouldn't cause a rule violation.");
        }

        sb.append("Since it ").append(BeliefScale.verbalizeBeliefAsPredicateWithOnly(einhBelief));
        sb.append(" that ").append(new EinhPred().verbalizeIdeaAsSentence(renderer, einhArgs));
        sb.append(", the possibility of ");
        if (contextAtom.equals(fhomChild)) {
            sb.append(childForm);
        } else {
            sb.append(parentForm);
        }
        sb.append(" is limited.");
        return sb.toString();
    }

    @Override
    public String getSerializedParameters() {
        return "";
    }
}
