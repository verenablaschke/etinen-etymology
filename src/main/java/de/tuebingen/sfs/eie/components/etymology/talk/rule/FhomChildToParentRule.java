package de.tuebingen.sfs.eie.components.etymology.talk.rule;

import de.tuebingen.sfs.eie.shared.talk.EtinenConstantRenderer;
import de.tuebingen.sfs.eie.shared.talk.pred.FhomPred;
import de.tuebingen.sfs.eie.shared.talk.rule.EtinenTalkingLogicalRule;
import de.tuebingen.sfs.psl.engine.PslProblem;
import de.tuebingen.sfs.psl.engine.RuleAtomGraph;
import de.tuebingen.sfs.psl.util.data.StringUtils;
import de.tuebingen.sfs.psl.util.data.Tuple;

public class FhomChildToParentRule extends EtinenTalkingLogicalRule {

    public static final String NAME = "FhomChildToParent";
    private static final String RULE = "0.6: Fhom(X,H) & Xinh(X,Z) -> Fhom(Z,H)";
    private static final String VERBALIZATION = "If a homologue of H in unlikely to exist in a parent language, " +
            "that makes it less likely for a homologue to exist in the child language.";

    // For serialization.
    public FhomChildToParentRule(String serializedParameters) {
        super(NAME, RULE, VERBALIZATION);
    }

    public FhomChildToParentRule(PslProblem pslProblem) {
        super(NAME, RULE, pslProblem, VERBALIZATION);
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
            if (atom.startsWith("X") || atom.startsWith("x")) {
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
        parentArgs = FhomPred.updateArgs(renderer, parentArgs);
        childArgs = FhomPred.updateArgs(renderer, childArgs);
        if (contextAtom.equals(child)) {
            // 'child perspective'
            sb.append("If a homologue of ").append(childArgs[1]);
            sb.append(" is unlikely to exist in a parent language (");
            sb.append(renderer == null ? parentArgs[0] : renderer.getLanguageRepresentation(parentArgs[0]));
            sb.append("), that makes it less likely for a homologue to exist in the child language (");
            sb.append(renderer == null ? childArgs[0] : renderer.getLanguageRepresentation(childArgs[0]));
            sb.append(").");
        } else {
            // 'parent perspective'
            sb.append("A homologue of ").append(childArgs[1]);
            sb.append(" in a parent language (");
            sb.append(renderer == null ? parentArgs[0] : renderer.getLanguageRepresentation(parentArgs[0]));
            sb.append("), becomes more likely if there is evidence for a homologue in the child language (");
            sb.append(renderer == null ? childArgs[0] : renderer.getLanguageRepresentation(childArgs[0]));
            sb.append(").");
        }
        sb.append("\n");
        return sb.toString();

        // TODO atom-specific explanation / links

//        StringBuilder sb = new StringBuilder();
//        String[] xinhArgs = XinhPred.updateArgs(renderer, new String[]{childArgs[0], parentArgs[0]});
//        sb.append("It is likely that a form (").append(xinhArgs[0]);
//        sb.append(") belongs to the same homologue set as its parent form ("); // TODO see above
//        sb.append(xinhArgs[1]).append(")");
//
//        if (contextAtom.equals(parent)) {
//            // 'why not lower?'
//            sb.append(", and it is ").append(BeliefScale.verbalizeBeliefAsAdjective(rag.getValue(child)));
//            sb.append("that \\url[");
//            sb.append(escapeForURL(xinhArgs[0]));
//            sb.append(" is also a homologue of ")
//                    .append(renderer == null ? homPeg : renderer.getFormRepresentation(homPeg));
//            sb.append("]{").append(child).append("}.");
//        } else {
//            // 'why not higher?'
//            sb.append(", but it is only ");
//            sb.append(BeliefScale.verbalizeBeliefAsAdjective(rag.getValue(parent)));
//            sb.append(" that \\url[");
//            sb.append(escapeForURL(xinhArgs[1]));
//            sb.append(" is also a homologue of ")
//                    .append(renderer == null ? homPeg : renderer.getFormRepresentation(homPeg));
//            sb.append("]{").append(parent).append("}.");
//        }
//        return sb.toString();
    }

    @Override
    public String getSerializedParameters() {
        return "";
    }
}
