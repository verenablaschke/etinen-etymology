package de.tuebingen.sfs.eie.components.etymology.talk.rule;

import de.tuebingen.sfs.eie.shared.talk.EtinenConstantRenderer;
import de.tuebingen.sfs.eie.shared.talk.pred.EinhPred;
import de.tuebingen.sfs.eie.shared.talk.pred.FsimPred;
import de.tuebingen.sfs.eie.shared.talk.rule.EtinenTalkingLogicalRule;
import de.tuebingen.sfs.psl.engine.PslProblem;
import de.tuebingen.sfs.psl.engine.RuleAtomGraph;
import de.tuebingen.sfs.psl.talk.BeliefScale;
import de.tuebingen.sfs.psl.util.data.StringUtils;
import de.tuebingen.sfs.psl.util.data.Tuple;

public class FsimToFsimRule extends EtinenTalkingLogicalRule {

    public static final String NAME = "FsimToFsim";
    private static final String RULE = "1: Fsim(X,Y) & Einh(X,W) & Einh(Y,Z) & (W != Z) -> Fsim(W,Z)";
    private static final String VERBALIZATION = "If two forms are similar and inherited from different sources, " +
            "those source words should be similar to one another too.";

    // For serialization.
    public FsimToFsimRule(String serializedParameters) {
        super(NAME, RULE, VERBALIZATION);
    }

    public FsimToFsimRule(PslProblem pslProblem) {
        super(NAME, RULE, pslProblem, VERBALIZATION);
    }


    @Override
    public String generateExplanation(EtinenConstantRenderer renderer, String groundingName, String contextAtom,
                                      RuleAtomGraph rag, boolean whyExplanation) {
        String einh1 = null;
        double einh1Belief = -1;
        String[] einh1Args = new String[2];
        String einh2 = null;
        double einh2Belief = -1;
        String[] einh2Args = new String[2];
        String fsimAnte = null;
        double fsimAnteBelief = -1;
        String[] fsimAnteArgs = new String[2];
        String fsimCons = null;
        double fsimConsBelief = -1;
        String[] fsimConsArgs = new String[2];

        for (Tuple atomToStatus : rag.getLinkedAtomsForGroundingWithLinkStatusAsList(groundingName)) {
            String atom = atomToStatus.get(0);
            String[] atomArgs = StringUtils.split(atom.substring(atom.indexOf('(') + 1, atom.length() - 1), ", ");
            if (atomToStatus.get(1).equals("+")) {
                fsimCons = atom;
                fsimConsBelief = rag.getValue(atom);
                fsimConsArgs = atomArgs;
            } else if (atom.toUpperCase().startsWith("FSIM")) {
                fsimAnte = atom;
                fsimAnteBelief = rag.getValue(atom);
                fsimAnteArgs = atomArgs;
            } else if (einh1 == null) {
                einh1 = atom;
                einh1Belief = rag.getValue(atom);
                einh1Args = atomArgs;
            } else {
                einh2 = atom;
                einh2Belief = rag.getValue(atom);
                einh2Args = atomArgs;
            }
        }

        // TODO this needs to depend on the context atom / on whether the scores are high/low
        StringBuilder sb = new StringBuilder();
        sb.append("\\url[");
        sb.append(escapeForURL(new FsimPred().verbalizeIdeaAsSentence(renderer, fsimAnteBelief, fsimAnteArgs)));
        sb.append("]{").append(fsimAnte).append("}, but ");
        sb.append("\\url[");
        sb.append(escapeForURL(new EinhPred().verbalizeIdeaAsSentence(renderer, einh1Belief, einh1Args)));
        sb.append("]{").append(fsimAnte).append("} and ");
        sb.append("\\url[");
        sb.append(escapeForURL(new EinhPred().verbalizeIdeaAsSentence(renderer, einh2Belief, einh2Args)));
        sb.append("]{").append(fsimAnte).append("}, implying that the parent forms ");
        fsimConsArgs = FsimPred.updateArgs(renderer, fsimConsArgs);
        sb.append(fsimConsArgs[0]).append(" and ").append(fsimConsArgs[1]).append(" should be at least ");
        double minSim = fsimAnteBelief + einh1Belief + einh2Belief - 2;
        if (minSim < 0) {
            minSim = 0;
            // TODO in that case, the rule should be phrased differently
        }
        sb.append(BeliefScale.verbalizeBeliefAsSimilarity(minSim));
        // TODO this comes off the wrong way when minSim is low ("should be at least dissimilar")
        sb.append(".");
        return sb.toString();
    }

    @Override
    public String getSerializedParameters() {
        return "";
    }
}
