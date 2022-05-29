package de.tuebingen.sfs.eie.components.etymology.talk.rule;

import de.tuebingen.sfs.eie.shared.talk.EtinenConstantRenderer;
import de.tuebingen.sfs.eie.shared.talk.pred.EinhPred;
import de.tuebingen.sfs.eie.shared.talk.pred.FsimPred;
import de.tuebingen.sfs.eie.shared.talk.rule.EtinenTalkingLogicalRule;
import de.tuebingen.sfs.psl.engine.PslProblem;
import de.tuebingen.sfs.psl.engine.RuleAtomGraph;
import de.tuebingen.sfs.psl.talk.Belief;
import de.tuebingen.sfs.psl.talk.BeliefScale;
import de.tuebingen.sfs.psl.util.data.StringUtils;
import de.tuebingen.sfs.psl.util.data.Tuple;

public class FsimToFsimRule extends EtinenTalkingLogicalRule {

    public static final String NAME = "FsimToFsim";
    private static final String RULE = "Fsim(X,Y) & Einh(X,W) & Einh(Y,Z) & (W != Z) -> Fsim(W,Z)";
    private static final String VERBALIZATION = "If two forms are similar and inherited from different sources, " +
            "those source words should be similar to one another too.";

    // For serialization.
    public FsimToFsimRule(String serializedParameters) {
        super(serializedParameters);
    }

    public FsimToFsimRule(PslProblem pslProblem, double weight) {
        super(NAME, weight, RULE, pslProblem, VERBALIZATION);
    }


    @Override
    public String generateExplanation(EtinenConstantRenderer renderer, String groundingName, String contextAtom,
                                      RuleAtomGraph rag, boolean whyExplanation) {
        String einh1 = null;
        double einh1Belief = -1;
        String[] einh1Args = null;
        String einh2 = null;
        double einh2Belief = -1;
        String[] einh2Args = null;
        String fsimAnte = null;
        double fsimAnteBelief = -1;
        String[] fsimAnteArgs = null;
        String fsimCons = null;
        double fsimConsBelief = -1;
        String[] fsimConsArgs = null;

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

        StringBuilder sb = new StringBuilder();
        sb.append(VERBALIZATION).append("\n");

        if (rag.getValue(contextAtom) > 0.999) {
            // Greyed out.
            sb.append("(\\url[");
            sb.append(escapeForURL(new FsimPred().verbalizeIdeaAsSentence(renderer, fsimAnteBelief, fsimAnteArgs)));
            sb.append("]{").append(fsimAnte).append("}, \\url[");
            sb.append(escapeForURL(new EinhPred().verbalizeIdeaAsSentence(renderer, einh1Belief, einh1Args)));
            sb.append("]{").append(einh1).append("}, \\url[");
            sb.append(escapeForURL(new EinhPred().verbalizeIdeaAsSentence(renderer, einh2Belief, einh2Args)));
            sb.append("]{").append(einh2).append("}, and \\url[");
            sb.append(escapeForURL(new FsimPred().verbalizeIdeaAsSentence(renderer, fsimConsBelief, fsimConsArgs)));
            sb.append("]{").append(fsimCons).append("}.)");
            return sb.toString();
        }

        String w = renderer == null ? fsimConsArgs[0] : renderer.getFormRepresentation(fsimConsArgs[0]);
        String z = renderer == null ? fsimConsArgs[1] : renderer.getFormRepresentation(fsimConsArgs[1]);

        if (contextAtom.equals(fsimCons)) {
            // consequent, 'why not lower?'
            sb.append("Since \\url[");
            sb.append(escapeForURL(new EinhPred().verbalizeIdeaAsSentence(renderer, einh1Belief, einh1Args)));
            sb.append("]{").append(einh1).append("}, \\url[");
            sb.append(escapeForURL(new EinhPred().verbalizeIdeaAsSentence(renderer, einh2Belief, einh2Args)));
            sb.append("]{").append(einh2).append("} and \\url[");
            sb.append(escapeForURL(new FsimPred().verbalizeIdeaAsSentence(renderer, fsimAnteBelief, fsimAnteArgs)));
            sb.append("]{").append(fsimAnte).append("}, ").append(w).append(" and ").append(z).append(" should ");
            double minSim = einh1Belief + einh2Belief + fsimAnteBelief - 2;
            if (minSim < 0) {
                minSim = 0;
            }
            sb.append(BeliefScale.verbalizeBeliefAsMinimumSimilarityInfinitive(minSim)).append(".");
            return sb.toString();
        }

        String x = renderer == null ? fsimAnteArgs[0] : renderer.getFormRepresentation(fsimAnteArgs[0]);
        String y = renderer == null ? fsimAnteArgs[1] : renderer.getFormRepresentation(fsimAnteArgs[1]);
        String xLang = renderer == null ? fsimAnteArgs[0] : renderer.getLanguageRepresentationForForm(fsimAnteArgs[0]);
        String yLang = renderer == null ? fsimAnteArgs[1] : renderer.getLanguageRepresentationForForm(fsimAnteArgs[1]);
        String wLang = renderer == null ? fsimConsArgs[0] : renderer.getLanguageRepresentationForForm(fsimConsArgs[0]);
        String zLang = renderer == null ? fsimConsArgs[1] : renderer.getLanguageRepresentationForForm(fsimConsArgs[1]);

        if (fsimConsBelief > 0.999) {
            // Greyed out.
            sb.append("The (").append(BeliefScale.verbalizeBeliefAsAdjectiveHigh(fsimAnteBelief)).append(")");
            sb.append(" \\url[");
            sb.append(escapeForURL(new FsimPred().verbalizeIdeaAsNP(renderer, false, fsimAnteArgs)));
            sb.append("]{").append(fsimAnte).append(" and the plausibility judgments for the inheritance relations ");
            sb.append("\\url[between ").append(escapeForURL(xLang)).append(" and ").append(wLang).append("]{");
            sb.append(einh1).append("} (").append(BeliefScale.verbalizeBeliefAsAdjectiveHigh(einh1Belief));
            sb.append(") and \\url[between ").append(escapeForURL(yLang)).append(" and ").append(escapeForURL(zLang));
            sb.append("]{").append(einh2).append("} (");
            sb.append(BeliefScale.verbalizeBeliefAsAdjectiveHigh(einh2Belief));
            sb.append(") imply a minimum similarity between the potential source words ");
            sb.append(w).append(" and ").append(z).append(". However, since these source words are already");
            sb.append(BeliefScale.verbalizeBeliefAsSimilarity(fsimConsBelief)); // 'extremely similar'
            sb.append(", changing the ");
            if (contextAtom.equals(fsimAnte)) {
                sb.append("similarity judgment for ").append(x).append(" and ").append(y);
            } else {
                sb.append("plausibility judgment for ");
                sb.append(
                        new EinhPred().verbalizeIdeaAsNP(renderer, contextAtom.equals(einh1) ? einh1Args : einh2Args));
            }
            sb.append("wouldn't cause a rule violation.");
            return sb.toString();
        }

        // antecedent, 'why not higher?'

        if (contextAtom.equals(fsimAnte)) {
            // "Since <X [adverb] is inherited from W> and <Y [adverb] is inherited from Z>
            // but <W and Z are only [similar]>, X and Y should not be more than [max_sim]"
            sb.append("Since \\url[");
            sb.append(escapeForURL(new EinhPred().verbalizeIdeaAsSentence(renderer, einh1Belief, einh1Args)));
            sb.append("]{").append(einh1).append("} and \\url[");
            sb.append(escapeForURL(new EinhPred().verbalizeIdeaAsSentence(renderer, einh2Belief, einh2Args)));
            sb.append("]{").append(einh2).append("} and \\url[").append(escapeForURL(w)).append(", but ");
            sb.append(escapeForURL(z)).append(" are ");
            sb.append(BeliefScale.verbalizeBeliefAsSimilarityWithOnly(fsimConsBelief)).append("]{").append(fsimCons);
            sb.append("}, ");
            double maxSim = fsimConsBelief - (einh1Belief + einh2Belief - 2);
            if (maxSim > 0.999) {
                sb.append("there are actually no restraints for the similarity between ");
                sb.append(x).append(" and ").append(y);
            } else {
//                sb.append(x).append(" and ").append(y).append(" should ");
//                sb.append(BeliefScale.verbalizeBeliefAsMaximumSimilarityInfinitive(maxSim));
                sb.append("this reduces the expected similarity between ").append(x).append(" and ").append(y);
            }
            return sb.append(".").toString();
        }

        if (contextAtom.equals(einh1)) {
            // "Since <X and Y are [similar]> and <Y [adverb] is inherited from Z>, but <W and Z are only [similar]>,
            // it should not be more than [likely] that X is inherited from W."
            sb.append("Since \\url[");
            sb.append(escapeForURL(new FsimPred().verbalizeIdeaAsSentence(renderer, fsimAnteBelief, fsimAnteArgs)));
            sb.append("]{").append(fsimAnte).append("} and \\url[");
            sb.append(escapeForURL(new EinhPred().verbalizeIdeaAsSentence(renderer, einh2Belief, einh2Args)));
            sb.append("]{").append(einh2).append("}, but \\url[").append(escapeForURL(w)).append(" and ");
            sb.append(escapeForURL(z)).append(" are ");
            sb.append(BeliefScale.verbalizeBeliefAsSimilarityWithOnly(fsimConsBelief)).append("]{").append(fsimCons);
            sb.append("}, ");
            double maxInh = fsimConsBelief - (fsimAnteBelief + einh2Belief - 2);
            if (maxInh > 0.999) {
                sb.append("there are actually no restraints for the inheritance relation between ");
                sb.append(x).append(" and ").append(w);
            } else {
                sb.append("this makes an inheritance relation between ");
                sb.append(x).append(" and ").append(w).append(" less likely");
            }
            return sb.append(".").toString();
        }

        sb.append("Since \\url[");
        sb.append(escapeForURL(new FsimPred().verbalizeIdeaAsSentence(renderer, fsimAnteBelief, fsimAnteArgs)));
        sb.append("]{").append(fsimAnte).append("} and \\url[");
        sb.append(escapeForURL(new EinhPred().verbalizeIdeaAsSentence(renderer, einh1Belief, einh1Args)));
        sb.append("]{").append(einh1).append("}, but \\url[").append(escapeForURL(w)).append(" and ");
        sb.append(escapeForURL(z)).append(" are ");
        sb.append(BeliefScale.verbalizeBeliefAsSimilarityWithOnly(fsimConsBelief)).append("]{").append(fsimCons);
        sb.append("}, ");
        double maxInh = fsimConsBelief - (fsimAnteBelief + einh1Belief - 2);
        if (maxInh > 0.999) {
            sb.append("there are actually no restraints for the inheritance relation between ");
            sb.append(y).append(" and ").append(z);
        } else {
            sb.append("this makes an inheritance relation between ");
            sb.append(y).append(" and ").append(z).append(" less likely");
        }
        return sb.append(".").toString();
    }

    @Override
    public String getSerializedParameters() {
        return "";
    }
}
