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
import de.tuebingen.sfs.eie.shared.talk.rule.EtinenTalkingArithmeticConstraint;
import de.tuebingen.sfs.psl.engine.PslProblem;
import de.tuebingen.sfs.psl.engine.RuleAtomGraph;
import de.tuebingen.sfs.psl.talk.BeliefScale;
import de.tuebingen.sfs.psl.util.data.StringUtils;
import de.tuebingen.sfs.psl.util.data.Tuple;

public class EloaPlusEloaConstraint extends EtinenTalkingArithmeticConstraint {

    public static final String NAME = "EloaPlusEloa";
    private static final String RULE = "Eloa(X, Y) + Eloa(Y, X) <= 1 .";
    private static final String VERBALIZATION = "Borrowing cannot happen in a circular fashion.";

    // For serialization.
    public EloaPlusEloaConstraint(String serializedParameters) {
        // No idiosyncrasies in this rule, just use default values:
        super(NAME, RULE, VERBALIZATION);
    }

    public EloaPlusEloaConstraint(PslProblem pslProblem) {
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
        String competitorAtom = null;
        String[] competitorArgs = null;
        double competitorBelief = -1.0;

        for (Tuple atomToStatus : rag.getLinkedAtomsForGroundingWithLinkStatusAsList(groundingName)) {
            String atom = atomToStatus.get(0);
            String[] predDetails = StringUtils.split(atom, '(');
            if (atom.equals(contextAtom)) {
                continue;
            }
            competitorAtom = atom;
            competitorArgs = StringUtils.split(predDetails[1].substring(0, predDetails[1].length() - 1), ", ");
            competitorBelief = rag.getValue(atom);
        }

        StringBuilder sb = new StringBuilder();
        sb.append(VERBALIZATION);
        sb.append(" The inverse loanword relationship (");
        sb.append("\\url[");
        sb.append(escapeForURL(new EloaPred().verbalizeIdeaAsNP(renderer, competitorArgs)));
        sb.append("]{").append(competitorAtom).append("}");
        sb.append(") ").append(BeliefScale.verbalizeBeliefAsPredicate(competitorBelief));
        sb.append(".");
        return sb.toString();
    }

    @Override
    public String getSerializedParameters() {
        return "";
    }


}
