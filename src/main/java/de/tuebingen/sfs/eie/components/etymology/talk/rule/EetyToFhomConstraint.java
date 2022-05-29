package de.tuebingen.sfs.eie.components.etymology.talk.rule;

import de.tuebingen.sfs.eie.shared.talk.EtinenConstantRenderer;
import de.tuebingen.sfs.eie.shared.talk.pred.EinhPred;
import de.tuebingen.sfs.eie.shared.talk.pred.EloaPred;
import de.tuebingen.sfs.eie.shared.talk.pred.FhomPred;
import de.tuebingen.sfs.eie.shared.talk.rule.EtinenTalkingLogicalConstraint;
import de.tuebingen.sfs.psl.engine.PslProblem;
import de.tuebingen.sfs.psl.engine.RuleAtomGraph;
import de.tuebingen.sfs.psl.util.data.StringUtils;
import de.tuebingen.sfs.psl.util.data.Tuple;

public class EetyToFhomConstraint extends EtinenTalkingLogicalConstraint {

    public static final String NAME = "%sToFhom";
    private static final String RULE = "%s(X,Y) & Fhom(Y,H) -> Fhom(X,H) .";
    private static final String VERBALIZATION = "A%s relation implies that the donor and the recipient form must be from the same homologue set.";

    // For serialization.
    public EetyToFhomConstraint(String serializedParameters) {
        super(serializedParameters);
    }

    public EetyToFhomConstraint(String eetyType, PslProblem pslProblem) {
        super(NAME.formatted(eetyType), RULE.formatted(eetyType), pslProblem,
                VERBALIZATION.formatted(eetyType.equals("Eloa") ? " loanword" : "n inheritance"));
    }

    @Override
    public String generateExplanation(EtinenConstantRenderer renderer, String groundingName, String contextAtom,
                                      RuleAtomGraph rag, boolean whyExplanation) {
        String eety = null;
        double eetyBelief = -1;
        String[] eetyArgs = null;
        String fhomAnte = null;
        double fhomAnteBelief = -1;
        String[] fhomAnteArgs = null;
        String fhomCons = null;
        double fhomConsBelief = -1;
        String[] fhomConsArgs = null;

        for (Tuple atomToStatus : rag.getLinkedAtomsForGroundingWithLinkStatusAsList(groundingName)) {
            String atom = atomToStatus.get(0);
            String[] atomArgs = StringUtils.split(atom.substring(atom.indexOf('(') + 1, atom.length() - 1), ", ");
            if (atom.startsWith("E") || atom.startsWith("e")) {
                eety = atom;
                eetyBelief = rag.getValue(atom);
                eetyArgs = atomArgs;
            } else if (atomToStatus.get(1).equals("+")) {
                fhomCons = atom;
                fhomConsBelief = rag.getValue(atom);
                fhomConsArgs = atomArgs;
            } else {
                fhomAnte = atom;
                fhomAnteBelief = rag.getValue(atom);
                fhomAnteArgs = atomArgs;
            }
        }

        // TODO argument structure should depend on whether it's fhomAnte or fhomCons
        StringBuilder sb = new StringBuilder();
        sb.append(getVerbalization()).append("\n");
        sb.append("However, \\url[");
        if (contextAtom.equals(fhomCons) || contextAtom.equals(fhomAnte)) {
            if (eety.startsWith("Eloa")) {
                sb.append(escapeForURL(new EloaPred().verbalizeIdeaAsSentence(renderer, eetyBelief, eetyArgs)));
            } else {
                sb.append(escapeForURL(new EinhPred().verbalizeIdeaAsSentence(renderer, eetyBelief, eetyArgs)));
            }
            sb.append("]{").append(eety);
        } else {
            sb.append(escapeForURL(new FhomPred().verbalizeIdeaAsSentence(renderer, fhomAnteBelief, fhomAnteArgs)));
            sb.append("]{").append(fhomAnte);
        }
        sb.append("} and \\url[");
        if (contextAtom.equals(fhomCons)) {
            sb.append(escapeForURL(new FhomPred().verbalizeIdeaAsSentence(renderer, fhomAnteBelief, fhomAnteArgs)));
            sb.append("]{").append(fhomAnte);
        } else {
            sb.append(escapeForURL(new FhomPred().verbalizeIdeaAsSentence(renderer, fhomConsBelief, fhomConsArgs)));
            sb.append("]{").append(fhomCons);
        }
        sb.append("}. ");
        return sb.toString();
    }

    @Override
    public String getSerializedParameters() {
        return "";
    }


}
