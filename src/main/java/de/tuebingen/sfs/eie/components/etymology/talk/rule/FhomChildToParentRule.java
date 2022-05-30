package de.tuebingen.sfs.eie.components.etymology.talk.rule;

import de.tuebingen.sfs.eie.shared.talk.EtinenConstantRenderer;
import de.tuebingen.sfs.eie.shared.talk.pred.FhomPred;
import de.tuebingen.sfs.eie.shared.talk.rule.EtinenTalkingLogicalRule;
import de.tuebingen.sfs.psl.engine.PslProblem;
import de.tuebingen.sfs.psl.engine.RuleAtomGraph;
import de.tuebingen.sfs.psl.talk.BeliefScale;
import de.tuebingen.sfs.psl.util.data.StringUtils;
import de.tuebingen.sfs.psl.util.data.Tuple;

public class FhomChildToParentRule extends EtinenTalkingLogicalRule {

    public static final String NAME = "FhomChildToParent";
    private static final String RULE = "Fhom(X,H) & Xinh(X,Z) -> Fhom(Z,H)";
    private static final String VERBALIZATION = "If a homologue of H in unlikely to exist in a parent language, " +
            "that makes it less likely for a homologue to exist in the child language.";

    // For serialization.
    public FhomChildToParentRule(String serializedParameters) {
        super(serializedParameters);
    }

    public FhomChildToParentRule(PslProblem pslProblem, double weight) {
        super(NAME, weight, RULE, pslProblem, VERBALIZATION);
    }


    @Override
    public String generateExplanation(EtinenConstantRenderer renderer, String groundingName, String contextAtom,
                                      RuleAtomGraph rag, boolean whyExplanation) {
        String child = null;
        String[] childArgs = null;
        String parent = null;
        String[] parentArgs = null;

        for (Tuple atomToStatus : rag.getLinkedAtomsForGroundingWithLinkStatusAsList(groundingName)) {
            String atom = atomToStatus.get(0);
            if (atom.startsWith("X")) {
                continue;
            }
            String[] atomArgs = StringUtils.split(atom.substring(atom.indexOf('(') + 1, atom.length() - 1), ", ");
            if (atomToStatus.get(1).equals("+")) {
                parent = atom;
                parentArgs = atomArgs;
            } else {
                child = atom;
                childArgs = atomArgs;
            }
        }

        StringBuilder sb = new StringBuilder();
        String parentLang = renderer == null ? parentArgs[0] : renderer.getLanguageRepresentationForForm(parentArgs[0]);
        String childLang = renderer == null ? childArgs[0] : renderer.getLanguageRepresentationForForm(childArgs[0]);
        String h = renderer == null ? parentArgs[1] : renderer.getFormRepresentation(parentArgs[1]);

        if (contextAtom.equals(parent)) {
            // 'parent perspective', consequent, 'why not lower?'
            sb.append("A homologue of ").append(h).append(" in a parent language (").append(parentLang);
            sb.append(") becomes more likely if there is evidence for a homologue in the child language (");
            sb.append(childLang).append(").\n");
            sb.append("Since \\url[").append(childLang).append(" ");
            sb.append(BeliefScale.verbalizeBeliefAsAdverb(rag.getValue(child))).append(" has a homologue of ");
            sb.append(h).append("]{").append(child).append("}, it should be at least as likely that ");
            sb.append(parentLang).append(" does too.");
            return sb.toString();
        }

        // 'child perspective', antecedent, 'why not higher?'
        sb.append("If a homologue of ").append(h).append(" is unlikely to exist in a parent language (");
        sb.append(parentLang).append("), that makes it less likely for one to exist in the child language (");
        sb.append(childLang).append(").\n");

        double childVal = rag.getValue(parent);
        if (childVal > 1 - RuleAtomGraph.DISSATISFACTION_PRECISION) {
            // Rule is greyed out.
            sb.append("However, since it is in fact ");
            sb.append(BeliefScale.verbalizeBeliefAsAdjective(childVal)); // 'extremely likely'
            sb.append(" that \\url[").append(escapeForURL(parentLang)).append(" has a homologue of ");
            sb.append(escapeForURL(h)).append("]{").append(parent).append("}, changing the homologue judgement of ");
            sb.append(renderer == null ? childArgs[0] : renderer.getFormRepresentation(childArgs[0]));
            sb.append(" wouldn't cause a rule violation.");
            return sb.toString();
        }

        sb.append("Since it ").append(BeliefScale.verbalizeBeliefAsPredicateWithOnly(childVal)).append(" that \\url[");
        sb.append(escapeForURL(parentLang)).append(" has a homologue of ").append(escapeForURL(h)).append("]{");
        sb.append(parent).append("}, it shouldn't be any more likely that ").append(childLang).append(" does.");
        return sb.toString();
    }

    @Override
    public String getSerializedParameters() {
        return "";
    }
}
