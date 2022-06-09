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
package de.tuebingen.sfs.eie.components.etymology.problems;

import de.tuebingen.sfs.eie.components.etymology.filter.EtymologyRagFilter;
import de.tuebingen.sfs.eie.components.etymology.talk.rule.*;
import de.tuebingen.sfs.eie.shared.talk.pred.*;
import de.tuebingen.sfs.psl.engine.AtomTemplate;
import de.tuebingen.sfs.psl.engine.InferenceResult;
import de.tuebingen.sfs.psl.engine.PslProblem;
import de.tuebingen.sfs.psl.engine.RuleAtomGraph;
import de.tuebingen.sfs.psl.talk.TalkingRuleOrConstraint;
import de.tuebingen.sfs.psl.util.log.InferenceLogger;
import org.linqs.psl.model.rule.GroundRule;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static de.tuebingen.sfs.psl.engine.AtomTemplate.ANY_CONST;

public class EtymologyProblem extends PslProblem {

    public static boolean verbose = true;

    Set<String> fixedAtoms = new HashSet<>();
    Set<String> hiddenAtoms = new HashSet<>();

    // Used by the config GUI:
    public static Set<String> RULES = new HashSet<>() {{
        add(EinhOrEloaOrEunkConstraint.NAME);
        add(EloaPlusEloaConstraint.NAME);
        add(FsimSymmetryConstraint.NAME);
        add(FsimTransitivityConstraint.NAME);
        add(FhomDistributionConstraint.NAME);
        add(EetyToFhomConstraint.NAME.formatted("Einh"));
        add(EetyToFhomConstraint.NAME.formatted("Eloa"));
        add(EunkPriorRule.NAME);
        add(EloaPriorRule.NAME);
        add(EinhToFsimRule.NAME);
        add(FsimToFsimRule.NAME);
        add("EloaToFsim"); // TODO
        add(FhomChildToParentRule.NAME);
        add(FhomParentToChildRule.NAME);
        add(FhomToEinhRule.NAME);
        add(FhomToEloaRule.NAME);
    }};

    // TODO make sure the config sets the dbmanager and problemId when it's initialized
    // (old to-do)
    public EtymologyProblem(EtymologyProblemConfig config) {
        super(config);
        addInteractionRules();

        InferenceLogger logger = config.getLogger();
        logger.displayAndLogLn("==========");
        logger.displayAndLog("Initializing etymology model with the following configuration:");
        config.logSettings();
    }

    public EtymologyProblem fromConfig(EtymologyProblemConfig config) {
        return new EtymologyProblem(config);
    }

    @Override
    public void declarePredicates() {
        declareClosedPredicate(new XinhPred());
        declareClosedPredicate(new XloaPred());

        declareClosedPredicate(new XsthPred());
        declareClosedPredicate(new XdstPred());

        declareOpenPredicate(new FhomPred());
        declareOpenPredicate(new FsimPred());
        declareOpenPredicate(new EinhPred());
        declareOpenPredicate(new EloaPred());
        declareOpenPredicate(new EunkPred());
    }

    @Override
    public void pregenerateAtoms() {
    }

    @Override
    public void addInteractionRules() {
        EtymologyProblemConfig config = (EtymologyProblemConfig) super.getConfig();

        // -------------------
        // CONSTRAINTS
        for (TalkingRuleOrConstraint constraint : new TalkingRuleOrConstraint[]{new EinhOrEloaOrEunkConstraint(this),
                new EloaPlusEloaConstraint(this), new FsimSymmetryConstraint(this),
                new FsimTransitivityConstraint(this), new FhomDistributionConstraint(this)}) {

            if (config.include(constraint.getName())) {
                addRule(constraint);
            }
        }
        //A loanword relation implies that the donor and the recipient form must be from the same homologue set.
        addRule(new EetyToFhomConstraint("Eloa", this));
        //An inheritance relation implies that the two forms must be from the same homologue set.
        addRule(new EetyToFhomConstraint("Einh", this));

        // -------------------
        // PRIORS

        // Biases against borrowing and against unknown etymologies
        if (config.include(EunkPriorRule.NAME))
            addRule(new EunkPriorRule(this, config.getRuleWeightOrDefault(EunkPriorRule.NAME, 2.5)));
        if (config.include(EloaPriorRule.NAME))
            addRule(new EloaPriorRule(this, config.getRuleWeightOrDefault(EloaPriorRule.NAME, 0.5)));

        // -------------------
        // "REGULAR" WEIGHTED RULES

        // If two forms are inherited from the same form, they should be similar:
        if (config.include(EinhToFsimRule.NAME))
            addRule(new EinhToFsimRule(this, config.getRuleWeightOrDefault(EinhToFsimRule.NAME, 2.0)));
        // If two forms are similar and inherited from different sources, those source
        // words should be similar to one another too.
        if (config.include(FsimToFsimRule.NAME))
            addRule(new FsimToFsimRule(this, config.getRuleWeightOrDefault(FsimToFsimRule.NAME, 1.0)));

        //A borrowed form should be more similar to its donor than to any other word.
        // TODO proper class w/ verbalization
        if (config.include("EloaToFsim"))
            addRule("EloaToFsimRelation", "1: Eloa(X,Y) & Fsim(X, Z) & Y != Z & X != Z -> Fsim(X,Y)");

        // Propagating evidence along unary branches, with negative evidence being weaker
        if (config.include(FhomChildToParentRule.NAME))
            addRule(new FhomChildToParentRule(this, config.getRuleWeightOrDefault(FhomChildToParentRule.NAME, 0.6)));
        if (config.include(FhomParentToChildRule.NAME))
            addRule(new FhomParentToChildRule(this, config.getRuleWeightOrDefault(FhomParentToChildRule.NAME, 0.2)));

        // If both parent and child share the same homologue set, that provides some evidence of inheritance
        if (config.include(FhomToEinhRule.NAME))
            addRule(new FhomToEinhRule(this, config.getRuleWeightOrDefault(FhomToEinhRule.NAME, 0.4)));
        // If there is a doubt about the reconstructability of a homologue set in the parent, an available
        // loanword etymology becomes much more likely
        if (config.include(FhomToEloaRule.NAME))
            addRule(new FhomToEloaRule(this, config.getRuleWeightOrDefault(FhomToEloaRule.NAME, 1.0)));

        // -------------------
        // Experimental rules:

        // If two forms are similar and might be inherited from a common source, it's
        // likely that they really were.
        //addRule("FsimToEinh", "1: Fsim(X,Y) & Xinh(X,Z) & Xinh(Y,Z) & (X != Y) -> Einh(X,Z)");
        //If a borrowing relation between two forms is possible and they are quite similar, this makes a borrowing likely.
        //addRule("FsimToEloa", "2: Fsim(X,Y) & Xloa(X,Y) -> Eloa(X,Y)");

        // If a word is more similar to a word in a contact language than to its
        // reconstructed ancestor, that makes it more likely to be a loan:
        //addRule("EloaAndFsim", "1: Xloa(X,W) + Eloa(X,W) >= Xinh(X,Z) + Fsim(X,W) - Fsim(X,Z)");

        //An inherited form should be more similar to its immediate ancestor than to any other word.
        //addRule("EinhToFsimRelation", "1: Einh(X,Y) & Fsim(X, Z) & X != Z & Y != Z -> Fsim(X,Y)");

        // Sister forms should be less similar than either is to their common parent
        // form:
        //addRule("FsimFamily", "1: (X != Y) + Xinh(X,Z) + Xinh(Y,Z) + Fsim(X,Y) <= 3 + Fsim(X,Z)");
        // The distance between two sister words must not exceed the sum of distances to
        // the common ancestor, reusing the grounding atoms to create the constant 2:
        //addRule("FsimTriangle", "1: (X != Y) + Xinh(X,Z) + Xinh(Y,Z) - Fsim(X,Z) - Fsim(Y,Z) >= 2 - Fsim(X,Y)");
        // Smaller tree distances -> higher similarity
        //addRule("XdstToFsim", "1: Xsth(D1,D2) & Xdst(X,Y,D1) & Xdst(X,Z,D2) & Fhom(X,H) & Fhom(Y,H) & Fhom(Z,H) & Fsim(X,Z) -> Fsim(X,Y)");
        // We need to further push up low-tree-distance similarities:
        //addRule("XdstOneToFsim", "0.3: Xdst(X,Y,'1') & Fhom(X,H,C) & Fhom(Y,H,C) -> Fsim(X,Y)");

        // Every pair of sister languages in which a homologue set is reconstructed or
        // attested makes it more likely to have existed in the common parent language:
        //addRule("FhomReconstruction", "1: Fhom(X,H) & Fhom(Y,H) & Xinh(X,Z) & Xinh(Y,Z) & (X != Y) -> Fhom(Z,H)");

        // -------------------

        System.out.println("Rules added:");
        super.printRules(System.out);
    }

    // TODO this needs a general overhaul, on the full project level, based on
    // changed assumptions about the centrality of PSL
    @Override
    public Set<AtomTemplate> declareAtomsForCleanUp() {
        // TODO (outside this class)
        // - delete F-atoms for proto languages if they don't correspond to
        // high-belief E-atoms
        // - delete low-belief E-atoms
        Set<AtomTemplate> atomsToDelete = new HashSet<>();
        atomsToDelete.add(new AtomTemplate("Xinh", ANY_CONST, ANY_CONST));
        atomsToDelete.add(new AtomTemplate("Xloa", ANY_CONST, ANY_CONST));
        atomsToDelete.add(new AtomTemplate("Fhom", ANY_CONST, ANY_CONST));
        atomsToDelete.add(new AtomTemplate("Fsim", ANY_CONST, ANY_CONST));
        return atomsToDelete;
    }

    public EtymologyProblemConfig getEtymologyConfig() {
        return (EtymologyProblemConfig) super.getConfig();
    }

    public InferenceLogger getLogger() {
        return super.getConfig().getLogger();
    }

    public void addFixedAtom(String atom) {
        fixedAtoms.add(atom);
    }

    public void addFixedAtom(String pred, String... args) {
        // TODO do this with AtomTemplates instead
        fixedAtoms.add(pred + "(" + String.join(", ", args) + ")");
    }

    public void addHiddenAtom(String atom) {
        hiddenAtoms.add(atom);
    }

    public void addHiddenAtom(String pred, String... args) {
        // TODO do this with AtomTemplates instead
        hiddenAtoms.add(pred + "(" + String.join(", ", args) + ")");
    }

    @Override
    public InferenceResult call() throws Exception {
        addInteractionRules();
        List<List<GroundRule>> groundRules = runInference(true);
        RuleAtomGraph.GROUNDING_OUTPUT = true;
        RuleAtomGraph.ATOM_VALUE_OUTPUT = true;
        Map<String, Double> valueMap = extractResultsForAllPredicates(false);
        if (verbose) System.err.println("FIXED: " + fixedAtoms);
        if (verbose) System.err.println("HIDDEN: " + hiddenAtoms);
        RuleAtomGraph rag = new RuleAtomGraph(this, new EtymologyRagFilter(valueMap, fixedAtoms, hiddenAtoms),
                groundRules);
        return new InferenceResult(rag, valueMap, getEtymologyConfig().copy());
    }

}
