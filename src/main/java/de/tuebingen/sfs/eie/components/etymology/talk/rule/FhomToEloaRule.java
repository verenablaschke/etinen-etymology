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

public class FhomToEloaRule extends EtinenTalkingLogicalRule {

    public static final String NAME = "FhomToEloa";
    private static final String RULE = "1.0: Fhom(X,H) & ~Fhom(Y,H) & Xinh(X,Y) & Xloa(X,Z) -> Eloa(X,Z)";
    private static final String VERBALIZATION =
            "If there is any doubt about the reconstructability of a homologue set in the parent, " +
                    "an available loanword etymology becomes much more likely.";

    // For serialization.
    public FhomToEloaRule(String serializedParameters) {
        super(NAME, RULE, VERBALIZATION);
    }

    public FhomToEloaRule(PslProblem pslProblem) {
        super(NAME, RULE, pslProblem, VERBALIZATION);
    }


    @Override
    public String generateExplanation(EtinenConstantRenderer renderer, String groundingName, String contextAtom,
                                      RuleAtomGraph rag, boolean whyExplanation) {
        String fhomCur = null;
        String[] fhomCurArgs = null;
        String fhomParent = null;
        String[] fhomParentArgs = null;
        String eloa = null;
        String[] eloaArgs = null;

        for (Tuple atomToStatus : rag.getLinkedAtomsForGroundingWithLinkStatusAsList(groundingName)) {
            String atom = atomToStatus.get(0);
            if (atom.startsWith("X")) {
                continue;
            }
            String[] atomArgs = StringUtils.split(atom.substring(atom.indexOf('(') + 1, atom.length() - 1), ", ");
            if (atom.startsWith("Eloa")) {
                eloa = atom;
                eloaArgs = atomArgs;
            } else if (atomToStatus.get(1).equals("+")) {
                fhomParent = atom;
                fhomParentArgs = atomArgs;
            } else {
                fhomCur = atom;
                fhomCurArgs = atomArgs;
            }
        }

        StringBuilder sb = new StringBuilder();

        if (contextAtom.equals(eloa)) {
            sb.append("It is ").append(BeliefScale.verbalizeBeliefAsAdjective(rag.getValue(fhomCur)));
            sb.append(" that \\url[")
                    .append(escapeForURL(new FhomPred().verbalizeIdeaAsSentence(renderer, fhomCurArgs)));
            sb.append("]{").append(fhomCur).append("}, but the \\url[");
            sb.append("reconstructability of this homologue set in the parent ");
            sb.append(FhomPred.updateArgs(renderer, fhomParentArgs)[0]).append("]{").append(fhomParent).append("} ");
            sb.append(BeliefScale.verbalizeBeliefAsPredicate(rag.getValue(fhomParent)));
            sb.append(", which makes a loanword etymology more likely.");
            // TODO
            return sb.toString();
        }
        
        //TODO
        sb.append(VERBALIZATION);
        return sb.toString();
    }

    @Override
    public String getSerializedParameters() {
        return "";
    }
}
