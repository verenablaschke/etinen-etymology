package de.tuebingen.sfs.eie.components.etymology.problems;

import static de.tuebingen.sfs.psl.engine.AtomTemplate.ANY_CONST;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.tuebingen.sfs.eie.components.etymology.talk.rule.*;
import de.tuebingen.sfs.eie.shared.talk.pred.*;
import de.tuebingen.sfs.psl.talk.TalkingRule;
import org.linqs.psl.model.rule.GroundRule;

import de.tuebingen.sfs.eie.components.etymology.filter.EtymologyRagFilter;
import de.tuebingen.sfs.psl.engine.AtomTemplate;
import de.tuebingen.sfs.psl.engine.InferenceResult;
import de.tuebingen.sfs.psl.engine.PslProblem;
import de.tuebingen.sfs.psl.engine.RuleAtomGraph;
import de.tuebingen.sfs.psl.util.log.InferenceLogger;

public class EtymologyProblem extends PslProblem {

    public static boolean verbose = true;

    Set<String> fixedAtoms = new HashSet<>();

    // TODO make sure the config sets the dbmanager and problemId when it's
    // initialized
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
        // TODO add config checks for the new rules, like before:

        // --- CONSTRAINTS ---
        for (TalkingRule rule : new TalkingRule[]{new EinhOrEloaOrEunkRule(this),
                new EloaPlusEloaRule(this), new FsimSymmetryRule(this), new FsimTransitivityRule(this),
                new FhomDistributionRule(this)}) {
            if (config.include(rule.getName())) {
                addRule(rule);
            }
        }
        // -------------------

        // WEIGHTED RULES

        // Biases against borrowing and against unknown etymologies
        if (config.include(EunkPriorRule.NAME))
            addRule(new EunkPriorRule(this, config.getRuleWeightOrDefault(EunkPriorRule.NAME, 2.5)));
        if (config.include(EloaPriorRule.NAME))
            addRule(new EloaPriorRule(this, config.getRuleWeightOrDefault(EloaPriorRule.NAME, 0.5)));

        // If two forms are inherited from the same form, they should be similar:
        addRule("EinhToFsim", "2: Einh(X,Z) & Einh(Y,Z) & (X != Y) -> Fsim(X,Y)");
        // If two forms are similar and might be inherited from a common source, it's
        // likely that they really were.
        //addRule("FsimToEinh", "1: Fsim(X,Y) & Xinh(X,Z) & Xinh(Y,Z) & (X != Y) -> Einh(X,Z)");
        //If a borrowing relation between two forms is possible and they are quite similar, this makes a borrowing likely.
        //addRule("FsimToEloa", "2: Fsim(X,Y) & Xloa(X,Y) -> Eloa(X,Y)");
        // If two forms are similar and inherited from different sources, those source
        // words should be similar to one another too.
        addRule(new FsimToFsimRule(this));

        // If a word is more similar to a word in a contact language than to its
        // reconstructed ancestor, that makes it more likely to be a loan:
        //addRule("EloaAndFsim", "1: Xloa(X,W) + Eloa(X,W) >= Xinh(X,Z) + Fsim(X,W) - Fsim(X,Z)");

        //A loanword relation implies that the donor and the recipient form must be from the same homologue set.
        addRule(new EetyToFhomRule("Eloa", this));
        //An inheritance relation implies that the two forms must be from the same homologue set.
        addRule(new EetyToFhomRule("Einh", this));

        //An inherited form should be more similar to its immediate ancestor than to any other word.
        addRule("EinhToFsimRelation", "1: Einh(X,Y) & Fsim(X, Z) & X != Z & Y != Z -> Fsim(X,Y)");

        //A borrowed form should be more similar to its donor than to any other word.
        addRule("EloaToFsimRelation", "1: Eloa(X,Y) & Fsim(X, Z) & Y != Z & X != Z -> Fsim(X,Y)");

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
        // Propagating evidence along unary branches, with negative evidence being weaker
        addRule(new FhomParentPositiveRule(this));
        addRule(new FhomParentNegativeRule(this));
        // If both parent and child share the same homologue set, that provides some evidence of inheritance
        addRule("FhomToEinh", "0.4: Fhom(X,H) & Fhom(Y,H) & Xinh(X,Y) -> Einh(X,Y)");
        // If there is a doubt about the reconstructability of a homologue set in the parent, an available
        // loanword etymology becomes much more likely
        addRule(new FhomToEloaRule(this));

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
        fixedAtoms.add(pred + "(" + String.join(",", args) + ")");
    }

    @Override
    public InferenceResult call() throws Exception {
        addInteractionRules();
        List<List<GroundRule>> groundRules = runInference(true);
        RuleAtomGraph.GROUNDING_OUTPUT = true;
        RuleAtomGraph.ATOM_VALUE_OUTPUT = true;
        Map<String, Double> valueMap = extractResultsForAllPredicates(false);
        if (verbose) System.err.println("FIXED: " + fixedAtoms);
        RuleAtomGraph rag = new RuleAtomGraph(this, new EtymologyRagFilter(valueMap, fixedAtoms), groundRules);
        return new InferenceResult(rag, valueMap, getEtymologyConfig().copy());
    }

}
