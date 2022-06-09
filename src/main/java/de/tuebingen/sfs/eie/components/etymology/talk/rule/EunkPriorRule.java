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

public class EunkPriorRule extends EtinenTalkingLogicalRule {

    public static final String NAME = "EunkPrior";
    public static final String RULE = "~Eunk(X)";
    public static final String VERBALIZATION = "By default, we prefer available explanations within the scope of the forms the inference is performed over.";

    // For serialization.
    public EunkPriorRule(String serializedParameters) {
        super(serializedParameters);
    }

    public EunkPriorRule(PslProblem pslProblem, double weight) {
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
