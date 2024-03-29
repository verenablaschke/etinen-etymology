/*
 * Copyright 2018–2022 University of Tübingen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tuebingen.sfs.eie.components.etymology.talk.rule;

import de.tuebingen.sfs.eie.shared.talk.EtinenConstantRenderer;
import de.tuebingen.sfs.eie.shared.talk.pred.EinhPred;
import de.tuebingen.sfs.eie.shared.talk.pred.EloaPred;
import de.tuebingen.sfs.eie.shared.talk.pred.FhomPred;
import de.tuebingen.sfs.eie.shared.talk.rule.EtinenTalkingLogicalConstraint;
import de.tuebingen.sfs.psl.engine.PslProblem;
import de.tuebingen.sfs.psl.engine.RuleAtomGraph;
import de.tuebingen.sfs.psl.talk.BeliefScale;
import de.tuebingen.sfs.psl.util.data.StringUtils;
import de.tuebingen.sfs.psl.util.data.Tuple;

public class EetyToFhomConstraint extends EtinenTalkingLogicalConstraint {

    public static final String NAME = "%sToFhom";
    private static final String RULE = "%s(X,Y) & Fhom(Y,H) -> Fhom(X,H) .";
    private static final String VERBALIZATION =
            "When we reconstruct a%s and assign some belief to the homologue status of the %s, " +
                    "we must assign at least as much belief to the %s's inclusion in the same homologue set.";

    // For serialization.
    public EetyToFhomConstraint(String serializedParameters) {
        super(serializedParameters);
    }

    public EetyToFhomConstraint(String eetyType, PslProblem pslProblem) {
        super(NAME.formatted(eetyType), RULE.formatted(eetyType), pslProblem,
                eetyType.equals("Eloa") ? VERBALIZATION.formatted(" borrowing", "donor", "recipient") :
                        VERBALIZATION.formatted("n inheritance", "parent", "child"));
    }

    @Override
    public String generateExplanation(EtinenConstantRenderer renderer, String groundingName, String contextAtom,
                                      RuleAtomGraph rag, boolean whyExplanation) {
        String eety = null;
        double eetyBelief = -1;
        String[] eetyArgs = null;
        String fhomParent = null;
        double fhomParentBelief = -1;
        String[] fhomParentArgs = null;
        String fhomChild = null;
        double fhomChildBelief = -1;
        String[] fhomChildArgs = null;
        boolean eetyIsInheritance = false;

        for (Tuple atomToStatus : rag.getLinkedAtomsForGroundingWithLinkStatusAsList(groundingName)) {
            String atom = atomToStatus.get(0);
            String[] atomArgs = StringUtils.split(atom.substring(atom.indexOf('(') + 1, atom.length() - 1), ", ");
            if (atom.startsWith("E")) {
                eety = atom;
                eetyBelief = rag.getValue(atom);
                eetyArgs = atomArgs;
                eetyIsInheritance = atom.startsWith("Einh");
            } else if (atomToStatus.get(1).equals("+")) {
                fhomChild = atom;
                fhomChildBelief = rag.getValue(atom);
                fhomChildArgs = atomArgs;
            } else {
                fhomParent = atom;
                fhomParentBelief = rag.getValue(atom);
                fhomParentArgs = atomArgs;
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append(getVerbalization()).append(" ");

        String childForm = renderer == null ? fhomChildArgs[0] : renderer.getFormRepresentation(fhomChildArgs[0]);
        String parentForm = renderer == null ? fhomParentArgs[0] : renderer.getFormRepresentation(fhomParentArgs[0]);
        String hom = renderer == null ? fhomParentArgs[1] : renderer.getFormRepresentation(fhomParentArgs[1]);

        if (contextAtom.equals(fhomChild)) {
            // consequent, 'why not lower'
            sb.append("Since \\url[");
            if (eetyIsInheritance) {
                sb.append(new EinhPred().verbalizeIdeaAsSentence(renderer, eetyBelief, eetyArgs));
            } else {
                sb.append(new EloaPred().verbalizeIdeaAsSentence(renderer, eetyBelief, eetyArgs));
            }
            sb.append("]{").append(eety).append("} and \\url[");
            sb.append(new FhomPred().verbalizeIdeaAsSentence(renderer, fhomParentBelief, fhomParentArgs));
            sb.append("]{").append(fhomParent).append("}, the homology judgment for ").append(childForm);
            double minVal = eetyBelief + fhomParentBelief - 1;
            if (minVal < RuleAtomGraph.DISSATISFACTION_PRECISION) {
                sb.append(" actually isn't restricted.");
            } else {
                sb.append(" should ").append(BeliefScale.verbalizeBeliefAsInfinitiveMinimumPredicate(minVal));
                sb.append(".");
            }
            return sb.toString();
        }

        // antecedent, 'why not higher'

        sb.append("Applying this logic to the homologue set for ").append(hom).append(", ");

        if (fhomChildBelief > 1 - RuleAtomGraph.DISSATISFACTION_PRECISION) {
            // trivially true case
            sb.append("we are already certain that ").append(childForm);
            sb.append(" \\url[belongs to this set]{");
            sb.append(fhomChild).append("}. (Therefore, the ");
            if (contextAtom.equals(eety)) {
                if (eetyIsInheritance) {
                    sb.append("inheritance");
                } else {
                    sb.append("borrowing");
                }
                sb.append(" relationship and \\url[the homologue set]{").append(fhomParent);
                sb.append("} of ").append(parentForm).append(" are actually not relevant.)");
            } else {
                sb.append("\\url[");
                if (eetyIsInheritance) {
                    sb.append("inheritance");
                } else {
                    sb.append("borrowing");
                }
                sb.append(" relationship]{").append(eety).append("} and the homologue set of ");
                sb.append(fhomParent).append(" are actually not relevant.)");
            }
            return sb.toString();
        }

        if (contextAtom.equals(eety)) {
            sb.append(parentForm).append(" \\url[");
            sb.append(BeliefScale.verbalizeBeliefAsAdverb(fhomParentBelief)).append(" belongs to this set");
            sb.append("]{").append(fhomParent).append("} ");
            if (!BeliefScale.sameBeliefInterval(fhomChildBelief, fhomParentBelief) &&
                    fhomParentBelief > fhomChildBelief) {
                sb.append("but ");
                if (fhomChildBelief < RuleAtomGraph.DISSATISFACTION_PRECISION) {
                    sb.append(childForm).append(" \\url[certainly does not]{").append(fhomChild).append("}");
                } else {
                    sb.append("the homologue membership of ").append(childForm).append("\\url[");
                    sb.append(BeliefScale.verbalizeBeliefAsPredicateWithOnly(fhomChildBelief));
                    sb.append("]{").append(fhomChild).append("} ");
                }
                sb.append(", so reconstructing a");
                if (eetyIsInheritance) {
                    sb.append("n inheritance");
                } else {
                    sb.append(" borrowing");
                }
                sb.append(" becomes problematic.");
                return sb.toString();
            }
            sb.append("and \\url[");
            sb.append(new FhomPred().verbalizeIdeaAsSentence(renderer, fhomChildBelief, fhomChildArgs));
            sb.append("]{").append(fhomChild).append("}, which limits our belief in a possibly reconstructed ");
            if (eetyIsInheritance) {
                sb.append("inheritance.");
            } else {
                sb.append("borrowing.");
            }
            return sb.toString();
        }

        // context atom is fhomParent
        sb.append("\\url[");
        if (eetyIsInheritance) {
            sb.append(new EinhPred().verbalizeIdeaAsSentence(renderer, eetyBelief, eetyArgs));
        } else {
            sb.append(new EloaPred().verbalizeIdeaAsSentence(renderer, eetyBelief, eetyArgs));
        }
        sb.append("]{").append(eety).append("} and \\url[");
        sb.append(new FhomPred().verbalizeIdeaAsNP(renderer, fhomChildArgs));
        sb.append("]{").append(fhomChild).append("} ");
        sb.append(BeliefScale.verbalizeBeliefAsPredicateWithOnly(fhomChildBelief));

        return sb.toString();
    }

    @Override
    public String getSerializedParameters() {
        return "";
    }


}
