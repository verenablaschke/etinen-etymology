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
import de.tuebingen.sfs.eie.shared.talk.rule.EtinenTalkingLogicalRule;
import de.tuebingen.sfs.psl.engine.PslProblem;
import de.tuebingen.sfs.psl.engine.RuleAtomGraph;

public class EloaPriorRule extends EtinenTalkingLogicalRule {

    public static final String NAME = "EloaPrior";
    private static final String RULE = "~Eloa(X, Y)";
    private static final String VERBALIZATION = "By default, we do not assume that a word is a loanword.";

    // For serialization.
    public EloaPriorRule(String serializedParameters) {
        super(serializedParameters);
    }

    public EloaPriorRule(PslProblem pslProblem, double weight) {
        super(NAME, weight, RULE, pslProblem, VERBALIZATION);
    }

    @Override
    public String generateExplanation(String groundingName, String contextAtom, RuleAtomGraph rag,
                                      boolean whyExplanation) {
        return VERBALIZATION;
    }

    @Override
    public String generateExplanation(EtinenConstantRenderer renderer, String groundingName, String contextAtom,
                                      RuleAtomGraph rag, boolean whyExplanation) {
        return VERBALIZATION;
    }

    @Override
    public String getSerializedParameters() {
        return "";
    }


}
