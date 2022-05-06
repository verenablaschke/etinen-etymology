package de.tuebingen.sfs.eie.components.etymology.talk.rule;

import de.tuebingen.sfs.eie.shared.talk.EtinenConstantRenderer;
import de.tuebingen.sfs.eie.shared.talk.pred.EinhPred;
import de.tuebingen.sfs.eie.shared.talk.pred.EloaPred;
import de.tuebingen.sfs.eie.shared.talk.pred.FsimPred;
import de.tuebingen.sfs.eie.shared.talk.rule.EtinenTalkingLogicalRule;
import de.tuebingen.sfs.psl.engine.PslProblem;
import de.tuebingen.sfs.psl.engine.RuleAtomGraph;
import de.tuebingen.sfs.psl.talk.BeliefScale;
import de.tuebingen.sfs.psl.util.data.StringUtils;
import de.tuebingen.sfs.psl.util.data.Tuple;

import java.util.List;
import java.util.Locale;

public class EetyToFsimRule extends EtinenTalkingLogicalRule {

    public static final String NAME = "EetyToFsim";
    // Only Eety and Fsim can have a value other than 0 or 1.
    // Eety is the only open predicate.
    private static final String RULE = "%.1f: %s(X, Z) & %s(Y, Z) & (X != Y) & XFufo(X) & XFufo(Y) -> Fsim(X, Y)";
    private static final String VERBALIZATION = "Words derived from the same source should be phonetically similar.";
    private String eetyType1;
    private String eetyType2;
    private double weight;

    // For serialization.
    public EetyToFsimRule(String serializedParameters) {
        super(NAME, RULE, VERBALIZATION);
        String[] params = serializedParameters.split("-");
        eetyType1 = params[0];
        eetyType2 = params[1];
        weight = Double.valueOf(params[2]);
        setName(String.format("%sAnd%sToFsim", eetyType1, eetyType2));
        setRuleString(String.format(Locale.US, RULE, weight, eetyType1, eetyType2));
    }

    public EetyToFsimRule(String eetyType1, String eetyType2, PslProblem pslProblem, double weight) {
        super(String.format("%sAnd%sToFsim", eetyType1, eetyType2),
                String.format(Locale.US, RULE, weight, eetyType1, eetyType2), pslProblem, VERBALIZATION);
        this.eetyType1 = eetyType1;
        this.eetyType2 = eetyType2;
        this.weight = weight;
    }

    @Override
    public String generateExplanation(String groundingName, String contextAtom, RuleAtomGraph rag,
                                      boolean whyExplanation) {
        return generateExplanation(null, groundingName, contextAtom, rag, whyExplanation);
    }

    @Override
    public String generateExplanation(EtinenConstantRenderer renderer, String groundingName, String contextAtom,
                                      RuleAtomGraph rag, boolean whyExplanation) {
        List<Tuple> atomsToStatuses = rag.getLinkedAtomsForGroundingWithLinkStatusAsList(groundingName);
        String[] eetyArgs = null;
        String eetyAtom = null;
        double eetyBelief = -1.0;
        String[] fsimArgs = null;
        double fsimBelief = -1.0;
        for (Tuple atomToStatus : atomsToStatuses) {
            String atom = atomToStatus.get(0);
            if (atom.equals(contextAtom)) {
                continue;
            }
            String[] predDetails = StringUtils.split(atom, '(');
            String predName = predDetails[0];
            String[] args = StringUtils.split(predDetails[1].substring(0, predDetails[1].length() - 1), ", ");
            double belief = rag.getValue(atom);
            if (predName.equals("Einh") || predName.equals("Eloa")) {
                eetyArgs = args;
                eetyAtom = atom;
                eetyBelief = belief;
            } else if (predName.equals("Fsim")) {
                fsimArgs = args;
                fsimBelief = belief;
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append(VERBALIZATION);
        sb.append(" It ").append(BeliefScale.verbalizeBeliefAsPredicate(eetyBelief));
        sb.append(" that ");
        sb.append("\\url[");
        String arg = eetyArgs[0];
        if (renderer != null) {
            arg = renderer.getFormRepresentation(arg);
        }
        sb.append(escapeForURL(arg)).append(" is also ");
        if (contextAtom.startsWith(EinhPred.NAME) && eetyAtom.startsWith(EinhPred.NAME)) {
            sb.append("inherited");
        } else if (contextAtom.startsWith(EloaPred.NAME) && eetyAtom.startsWith(EloaPred.NAME)) {
            sb.append("borrowed");
        } else {
            sb.append("derived");
        }
        arg = eetyArgs[1];
        if (renderer != null) {
            arg = renderer.getFormRepresentation(arg);
        }
        sb.append(" from ").append(escapeForURL(arg));
        sb.append("]{").append(eetyAtom).append("}");
        sb.append(", but ");
        sb.append(new FsimPred().verbalizeIdeaAsSentence(renderer, fsimBelief, fsimArgs));
        sb.append(" (" + (int) (100 * fsimBelief) + "%)");
        sb.append(". ");
        return sb.toString();
    }

    @Override
    public String getSerializedParameters() {
        return eetyType1 + "-" + eetyType2 + "-" + weight;
    }

}