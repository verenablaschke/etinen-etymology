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
import de.tuebingen.sfs.eie.shared.talk.pred.EloaPred;
import de.tuebingen.sfs.eie.shared.talk.pred.FhomPred;
import de.tuebingen.sfs.eie.shared.talk.pred.XinhPred;
import de.tuebingen.sfs.eie.shared.talk.rule.EtinenTalkingLogicalRule;
import de.tuebingen.sfs.psl.engine.PslProblem;
import de.tuebingen.sfs.psl.engine.RuleAtomGraph;
import de.tuebingen.sfs.psl.talk.Belief;
import de.tuebingen.sfs.psl.talk.BeliefScale;
import de.tuebingen.sfs.psl.util.data.StringUtils;
import de.tuebingen.sfs.psl.util.data.Tuple;

public class FhomToEloaRule extends EtinenTalkingLogicalRule {

    public static final String NAME = "FhomToEloa";
    private static final String RULE = "Fhom(X,H) & ~Fhom(Y,H) & Xinh(X,Y) & Xloa(X,Z) -> Eloa(X,Z)";
    private static final String VERBALIZATION =
            "If there is any doubt about the reconstructability of a homologue set in the parent, " +
                    "an available loanword etymology becomes much more likely.";

    // For serialization.
    public FhomToEloaRule(String serializedParameters) {
        super(serializedParameters);
    }

    public FhomToEloaRule(PslProblem pslProblem, double weight) {
        super(NAME, weight, RULE, pslProblem, VERBALIZATION);
    }


    @Override
    public String generateExplanation(EtinenConstantRenderer renderer, String groundingName, String contextAtom,
                                      RuleAtomGraph rag, boolean whyExplanation) {
        String fhomCur = null;
        String[] fhomCurArgs = null;
        String fhomParent = null;
        String[] fhomParentArgs = null;
        String eloa = null;
        String[] eloaArgs = null;

        for (Tuple atomToStatus : rag.getLinkedAtomsForGroundingWithLinkStatusAsList(groundingName)) {
            String atom = atomToStatus.get(0);
            if (atom.startsWith("X")) {
                continue;
            }
            String[] atomArgs = StringUtils.split(atom.substring(atom.indexOf('(') + 1, atom.length() - 1), ", ");
            if (atom.startsWith("Eloa")) {
                eloa = atom;
                eloaArgs = atomArgs;
            } else if (atomToStatus.get(1).equals("+")) {
                fhomParent = atom;
                fhomParentArgs = atomArgs;
            } else {
                fhomCur = atom;
                fhomCurArgs = atomArgs;
            }
        }

        double fhomCurBelief = rag.getValue(fhomCur);
        double fhomParentBelief = rag.getValue(fhomParent);
        double eloaBelief = rag.getValue(eloa);
        String x = renderer == null ? fhomCurArgs[0] : renderer.getFormRepresentation(fhomCurArgs[0]);
        String y = renderer == null ? fhomParentArgs[0] : renderer.getFormRepresentation(fhomParentArgs[0]);

        StringBuilder sb = new StringBuilder();
        sb.append(VERBALIZATION).append(" ");

        if (contextAtom.equals(eloa)) {
            // consequent: 'why not lower?'
            sb.append("Since \\url[");
            sb.append(escapeForURL(new FhomPred().verbalizeIdeaAsSentence(renderer, fhomCurBelief, fhomCurArgs)));
            sb.append("]{").append(fhomCur).append("} and \\url[");
            sb.append(escapeForURL(new FhomPred().verbalizeIdeaAsSentence(renderer, fhomParentBelief, fhomParentArgs)));
            sb.append("]{").append(fhomParent).append("}, ");
            double minLoa = fhomCurBelief - fhomParentBelief;
            if (minLoa < RuleAtomGraph.DISSATISFACTION_PRECISION) {
                sb.append(" changing the loanword judgment would actually not cause a rule violation");
            } else {
                sb.append("a loanword relationship, such as between ").append(x).append(" and ").append(y);
                sb.append(" should ").append(BeliefScale.verbalizeBeliefAsInfinitiveMinimumPredicate(minLoa));
            }
            return sb.append(".").toString();
        }

        String h = renderer == null ? fhomParentArgs[1] : renderer.getFormRepresentation(fhomParentArgs[1]);

        if (eloaBelief > 1 - RuleAtomGraph.DISSATISFACTION_PRECISION) {
            // Greyed out.
            sb.append("The homology judgments for ");
            if (contextAtom.equals(fhomCur)) {
                sb.append(x).append(" and ").append(h).append(" and for \\url[");
                sb.append(escapeForURL(y)).append(" and ").append(escapeForURL(h));
                sb.append("]{").append(fhomParent).append("} (");
                sb.append(BeliefScale.verbalizeBeliefAsAdjective(fhomParentBelief)).append(")");
            } else {
                sb.append(y).append(" and ").append(h).append(" and for \\url[");
                sb.append(escapeForURL(y)).append(" and ").append(escapeForURL(h));
                sb.append("]{").append(fhomParent).append("} (");
                sb.append(BeliefScale.verbalizeBeliefAsAdjective(fhomParentBelief)).append(")");


                sb.append("\\url[");
                sb.append(escapeForURL(x)).append(" and ").append(escapeForURL(h));
                sb.append("]{").append(fhomCur).append("} (");
                sb.append(BeliefScale.verbalizeBeliefAsAdjective(fhomCurBelief)).append(")");
                sb.append(" and for ").append(y).append(" and ").append(h);
            }
            sb.append(" influence how likely it should \\textit{at least} be that ").append(x);
            sb.append(" is a loanword. ");
            sb.append("However, since it is ");
            sb.append(BeliefScale.verbalizeBeliefAsAdjective(rag.getValue(eloa))); // 'extremely likely'
            sb.append(" that ").append(x).append(" is borrowed, changing either of the homology judgments ");
            sb.append("wouldn't cause a rule violation.");
        }

        if (contextAtom.equals(fhomCur)) {
            // antecedent -> 'why not higher?'
            sb.append("Since ").append(new EloaPred().verbalizeIdeaAsSentence(renderer, eloaBelief, eloaArgs));
            // TODO
        }

        // negated antecedent -> 'why not lower?'

        //TODO
        return sb.toString();
    }

    @Override
    public String getSerializedParameters() {
        return "";
    }
}
