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
import de.tuebingen.sfs.eie.shared.talk.pred.FhomPred;
import de.tuebingen.sfs.eie.shared.talk.rule.EtinenTalkingLogicalRule;
import de.tuebingen.sfs.psl.engine.PslProblem;
import de.tuebingen.sfs.psl.engine.RuleAtomGraph;
import de.tuebingen.sfs.psl.talk.Belief;
import de.tuebingen.sfs.psl.talk.BeliefScale;
import de.tuebingen.sfs.psl.util.data.StringUtils;
import de.tuebingen.sfs.psl.util.data.Tuple;

public class FhomParentToChildRule extends EtinenTalkingLogicalRule {

    public static final String NAME = "FhomParentToChild";
    private static final String RULE = "Fhom(Z,H) & Xinh(X,Z) -> Fhom(X,H)";
    private static final String VERBALIZATION = "If a homologue of H in unlikely to exist in a child language, " +
            "that makes it less likely for a homologue to exist in the parent language.";

    // For serialization.
    public FhomParentToChildRule(String serializedParameters) {
        super(serializedParameters);
    }

    public FhomParentToChildRule(PslProblem pslProblem, double weight) {
        super(NAME, weight, RULE, pslProblem, VERBALIZATION);
    }


    @Override
    public String generateExplanation(EtinenConstantRenderer renderer, String groundingName, String contextAtom,
                                      RuleAtomGraph rag, boolean whyExplanation) {
        String parent = null;
        String[] parentArgs = null;
        String child = null;
        String[] childArgs = null;

        for (Tuple atomToStatus : rag.getLinkedAtomsForGroundingWithLinkStatusAsList(groundingName)) {
            String atom = atomToStatus.get(0);
            if (atom.startsWith("X")) {
                continue;
            }
            String[] atomArgs = StringUtils.split(atom.substring(atom.indexOf('(') + 1, atom.length() - 1), ", ");
            if (atomToStatus.get(1).equals("+")) {
                child = atom;
                childArgs = atomArgs;
            } else {
                parent = atom;
                parentArgs = atomArgs;
            }
        }

        StringBuilder sb = new StringBuilder();
        String childLang = renderer == null ? childArgs[0] : renderer.getLanguageRepresentationForForm(childArgs[0]);
        String parentLang = renderer == null ? parentArgs[0] : renderer.getLanguageRepresentationForForm(parentArgs[0]);
        String h = renderer == null ? childArgs[1] : renderer.getFormRepresentation(childArgs[1]);

        if (contextAtom.equals(child)) {
            // 'child perspective', consequent, 'why not lower?'
            sb.append("A homologue of ").append(h).append(" in a child language (").append(childLang);
            sb.append(") becomes more likely if there is evidence for a homologue in the parent language (");
            sb.append(parentLang).append("). ");
            sb.append("Since \\url[").append(parentLang).append(" ");
            sb.append(BeliefScale.verbalizeBeliefAsAdverb(rag.getValue(parent))).append(" has a homologue of ");
            sb.append(h).append("]{").append(parent).append("}, it should be at least as likely that ");
            sb.append(childLang).append(" does too.");
            return sb.toString();
        }

        // 'parent perspective', antecedent, 'why not higher?'
        sb.append("If a homologue of ").append(h).append(" is unlikely to exist in a child language (");
        sb.append(childLang).append("), that makes it less likely for one to exist in the parent language (");
        sb.append(parentLang).append("). ");

        double childVal = rag.getValue(child);
        if (childVal > 1 - RuleAtomGraph.DISSATISFACTION_PRECISION) {
            // Rule is greyed out.
            sb.append("However, since it is in fact ");
            sb.append(BeliefScale.verbalizeBeliefAsAdjective(childVal)); // 'extremely likely'
            sb.append(" that \\url[").append(escapeForURL(childLang)).append(" has a homologue of ");
            sb.append(escapeForURL(h)).append("]{").append(child).append("}, changing the homologue judgement of ");
            sb.append(renderer == null ? parentArgs[0] : renderer.getFormRepresentation(parentArgs[0]));
            sb.append(" wouldn't cause a rule violation.");
            return sb.toString();
        }

        sb.append("Since it ").append(BeliefScale.verbalizeBeliefAsPredicateWithOnly(childVal)).append(" that \\url[");
        sb.append(escapeForURL(childLang)).append(" has a homologue of ").append(escapeForURL(h)).append("]{");
        sb.append(child).append("}, it shouldn't be any more likely that ").append(parentLang).append(" does.");
        return sb.toString();
    }

    @Override
    public String getSerializedParameters() {
        return "";
    }
}
