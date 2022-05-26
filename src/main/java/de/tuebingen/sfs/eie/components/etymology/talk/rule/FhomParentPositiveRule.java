package de.tuebingen.sfs.eie.components.etymology.talk.rule;

import de.tuebingen.sfs.eie.shared.talk.EtinenConstantRenderer;
import de.tuebingen.sfs.eie.shared.talk.pred.FhomPred;
import de.tuebingen.sfs.eie.shared.talk.pred.XinhPred;
import de.tuebingen.sfs.eie.shared.talk.rule.EtinenTalkingLogicalRule;
import de.tuebingen.sfs.psl.engine.PslProblem;
import de.tuebingen.sfs.psl.engine.RuleAtomGraph;
import de.tuebingen.sfs.psl.talk.BeliefScale;
import de.tuebingen.sfs.psl.util.data.StringUtils;
import de.tuebingen.sfs.psl.util.data.Tuple;

public class FhomParentPositiveRule extends EtinenTalkingLogicalRule {

    public static final String NAME = "FhomParentPositive";
    private static final String RULE = "0.6: Fhom(X,H) & Xinh(X,Z) -> Fhom(Z,H)";
    // TODO rephrase 'parent form'?
    private static final String VERBALIZATION = "It is likely that a form belongs to the same homologue set as its parent form.";

    // For serialization.
    public FhomParentPositiveRule(String serializedParameters) {
        super(NAME, RULE, VERBALIZATION);
    }

    public FhomParentPositiveRule(PslProblem pslProblem) {
        super(NAME, RULE, pslProblem, VERBALIZATION);
    }


    @Override
    public String generateExplanation(EtinenConstantRenderer renderer, String groundingName, String contextAtom,
                                      RuleAtomGraph rag, boolean whyExplanation) {
        String fhomAnte = null;
        String[] fhomAnteArgs = null;
        String fhomCons = null;
        String[] fhomConsArgs = null;
        String homPeg = null;

        for (Tuple atomToStatus : rag.getLinkedAtomsForGroundingWithLinkStatusAsList(groundingName)) {
            String atom = atomToStatus.get(0);
            if (atom.startsWith("X") || atom.startsWith("x")) {
                homPeg = atom.split(",")[1].strip().replaceAll("\\)", "");
                continue;
            }
            String[] atomArgs = StringUtils.split(atom.substring(atom.indexOf('(') + 1, atom.length() - 1), ", ");
            if (atomToStatus.get(1).equals("+")) {
                fhomCons = atom;
                fhomConsArgs = atomArgs;
            } else {
                fhomAnte = atom;
                fhomAnteArgs = atomArgs;
            }
        }

        StringBuilder sb = new StringBuilder();
        String[] xinhArgs = XinhPred.updateArgs(renderer, new String[]{fhomAnteArgs[0], fhomConsArgs[0]});
        sb.append("It is likely that a form (").append(xinhArgs[0]);
        sb.append(") belongs to the same homologue set as its parent form ("); // TODO see above
        sb.append(xinhArgs[1]).append("), ");

        if (contextAtom.equals(fhomCons)) {
            // 'why not lower?'
            sb.append(", and it is ").append(BeliefScale.verbalizeBeliefAsAdjective(rag.getValue(fhomAnte)));
            sb.append("that \\url[");
            sb.append(escapeForURL(xinhArgs[0]));
            sb.append(" is also a homologue of ")
                    .append(renderer == null ? homPeg : renderer.getFormRepresentation(homPeg));
            sb.append("]{").append(fhomAnte).append("}.");
        } else {
            // 'why not higher?'
            sb.append(", but it is only ");
            sb.append(BeliefScale.verbalizeBeliefAsAdjective(rag.getValue(fhomCons)));
            sb.append(" that \\url[");
            sb.append(escapeForURL(xinhArgs[1]));
            sb.append(" is also a homologue of ")
                    .append(renderer == null ? homPeg : renderer.getFormRepresentation(homPeg));
            sb.append("]{").append(fhomCons).append("}.");
        }
        return sb.toString();
    }

    @Override
    public String getSerializedParameters() {
        return "";
    }
}
