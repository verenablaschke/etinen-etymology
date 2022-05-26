package de.tuebingen.sfs.eie.components.etymology.talk.rule.deprecated;

import de.tuebingen.sfs.eie.shared.talk.EtinenConstantRenderer;
import de.tuebingen.sfs.eie.shared.talk.pred.EinhPred;
import de.tuebingen.sfs.eie.shared.talk.pred.EloaPred;
import de.tuebingen.sfs.eie.shared.talk.pred.EtinenTalkingPredicate;
import de.tuebingen.sfs.eie.shared.talk.pred.FsimPred;
import de.tuebingen.sfs.eie.shared.talk.rule.EtinenTalkingLogicalRule;
import de.tuebingen.sfs.psl.engine.PslProblem;
import de.tuebingen.sfs.psl.engine.RuleAtomGraph;
import de.tuebingen.sfs.psl.util.data.StringUtils;
import de.tuebingen.sfs.psl.util.data.Tuple;

import java.util.List;
import java.util.Locale;

public class FsimAndSsimToEetyRule extends EtinenTalkingLogicalRule {

    public static final String NAME = "FsimAndSsimToEety";
    private static final String RULE = // phonetic similarity
            "%.1f: Fufo(X, F1) & Fufo(Y, F2) & Fsim(F1, F2) &"
                    // semantic similarity
                    + "Fsem(X, C1) & Fsem(Y, C2) & Ssim(C1, C2) &"
                    // -> same source
                    + "%s(X, Z) & (X != Y) & (Y != Z)" + "-> Einh(Y, Z) | Eloa(Y, Z)";
    private static final String VERBALIZATION = "If two words are phonetically and semantically similar, "
            + "they are probably derived from the same source.";
    private String eetyType = null;

    // For serialization.
    public FsimAndSsimToEetyRule(String serializedParameters) {
        super(NAME, RULE, VERBALIZATION);
    }

    public FsimAndSsimToEetyRule(String eetyType, PslProblem pslProblem, double weight) {
        super(String.format("FsimAndSsimAnd%sToEety", eetyType), String.format(Locale.US, RULE, weight, eetyType),
                pslProblem, VERBALIZATION);
        this.eetyType = eetyType;
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
        String[] fsimArgs = null;
        double fsimBelief = -2.0;
        String[] eetyArgs = null;
        String eetyAtom = null;
        double eetyBelief = -2.0;
        // Assuming (for now) that Fufo is a closed predicate.
        for (Tuple atomToStatus : atomsToStatuses) {
            String[] predDetails = StringUtils.split(atomToStatus.get(0), '(');
            String predName = predDetails[0];
            String[] args = StringUtils.split(predDetails[1].substring(0, predDetails[1].length() - 1), ", ");
            double belief = rag.getValue(atomToStatus.get(0));
            if (predName.equals("Fsim")) {
                fsimArgs = args;
                fsimBelief = belief;
            } else if (predName.equals(eetyType) && !atomToStatus.get(0).equals(contextAtom)) {
                eetyArgs = args;
                eetyAtom = atomToStatus.get(0);
                eetyBelief = belief;
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append(VERBALIZATION).append(" ");
        sb.append(escapeForURL(new FsimPred().verbalizeIdeaAsSentence(renderer, fsimBelief, fsimArgs)));
        sb.append(" (" + (int) (100 * fsimBelief) + "%)");
        sb.append(", and \\url[");
        EtinenTalkingPredicate pred = eetyType.equals("Einh") ? new EinhPred() : new EloaPred();
        sb.append(escapeForURL(pred.verbalizeIdeaAsSentence(renderer, eetyBelief, eetyArgs)));
        sb.append("]{").append(eetyAtom).append("}");
        sb.append(". ");

        return sb.toString();
    }

    @Override
    public String getSerializedParameters() {
        return "";
    }

}