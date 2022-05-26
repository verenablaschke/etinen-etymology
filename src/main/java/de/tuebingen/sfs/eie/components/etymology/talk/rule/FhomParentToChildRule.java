package de.tuebingen.sfs.eie.components.etymology.talk.rule;

import de.tuebingen.sfs.eie.shared.talk.EtinenConstantRenderer;
import de.tuebingen.sfs.eie.shared.talk.pred.FhomPred;
import de.tuebingen.sfs.eie.shared.talk.rule.EtinenTalkingLogicalRule;
import de.tuebingen.sfs.psl.engine.PslProblem;
import de.tuebingen.sfs.psl.engine.RuleAtomGraph;
import de.tuebingen.sfs.psl.util.data.StringUtils;
import de.tuebingen.sfs.psl.util.data.Tuple;

public class FhomParentToChildRule extends EtinenTalkingLogicalRule {

    public static final String NAME = "FhomParentToChild";
    private static final String RULE = "0.4: Fhom(Z,H) & Xinh(X,Z) -> Fhom(X,H)";
    // TODO rephrase 'parent form'?
    private static final String VERBALIZATION = "If a homologue of H in unlikely to exist in a child language, " +
            "that makes it less likely for a homologue to exist in the parent language.";

    // For serialization.
    public FhomParentToChildRule(String serializedParameters) {
        super(NAME, RULE, VERBALIZATION);
    }

    public FhomParentToChildRule(PslProblem pslProblem) {
        super(NAME, RULE, pslProblem, VERBALIZATION);
    }


    @Override
    public String generateExplanation(EtinenConstantRenderer renderer, String groundingName, String contextAtom,
                                      RuleAtomGraph rag, boolean whyExplanation) {
        String parent = null;
        String[] parentArgs = null;
        String child = null;
        String[] childArgs = null;

        for (Tuple atomToStatus : rag.getLinkedAtomsForGroundingWithLinkStatusAsList(groundingName)) {
            String atom = atomToStatus.get(0);
            if (atom.startsWith("X")) {
                continue;
            }
            String[] atomArgs = StringUtils.split(atom.substring(atom.indexOf('(') + 1, atom.length() - 1), ", ");
            if (atomToStatus.get(1).equals("-")) {
                child = atom;
                childArgs = atomArgs;
            } else {
                parent = atom;
                parentArgs = atomArgs;
            }
        }

        StringBuilder sb = new StringBuilder();
        parentArgs = FhomPred.updateArgs(renderer, parentArgs);
        childArgs = FhomPred.updateArgs(renderer, childArgs);
        if (contextAtom.equals(parent)) {
            // 'parent perspective'
            sb.append("If a homologue of ").append(parentArgs[1]);
            sb.append(" is unlikely to exist in a child language (");
            sb.append(renderer == null ? childArgs[0] : renderer.getLanguageRepresentation(childArgs[0]));
            sb.append("), that makes it less likely for a homologue to exist in the parent language (");
            sb.append(renderer == null ? parentArgs[0] : renderer.getLanguageRepresentation(parentArgs[0]));
            sb.append(").");
        } else {
            // 'child perspective'
            sb.append("A homologue of ").append(parentArgs[1]);
            sb.append(" in a child language (");
            sb.append(renderer == null ? childArgs[0] : renderer.getLanguageRepresentation(childArgs[0]));
            sb.append("), becomes more likely if there is evidence for a homologue in the parent language (");
            sb.append(renderer == null ? parentArgs[0] : renderer.getLanguageRepresentation(parentArgs[0]));
            sb.append(").");
        }
        sb.append("\n");

        // TODO

//        String[] xinhArgs = XinhPred.updateArgs(renderer, parentArgs[0], childArgs[0]);
//        sb.append("It is unlikely that a form (").append(xinhArgs[0]);
//        sb.append(") belongs to a different homologue set than its parent form ("); // TODO see above
//        sb.append(xinhArgs[1]).append("), ");
//
//        if (contextAtom.equals(child)) {
//            // 'why not higher?'
//            sb.append("but it is only ");
//            sb.append(BeliefScale.verbalizeBeliefAsAdjective(rag.getValue(parent)));
//            sb.append(" that \\url[");
//            sb.append(escapeForURL(xinhArgs[0]));
//            sb.append(" is also a homologue of ")
//                    .append(renderer == null ? homPeg : renderer.getFormRepresentation(homPeg));
//            sb.append("]{").append(parent).append("}.");
//        } else {
//            // 'why not lower?'
//            sb.append("and it is ").append(BeliefScale.verbalizeBeliefAsAdjective(rag.getValue(child)));
//            sb.append("that \\url[");
//            sb.append(escapeForURL(xinhArgs[1]));
//            sb.append(" is also a homologue of ")
//                    .append(renderer == null ? homPeg : renderer.getFormRepresentation(homPeg));
//            sb.append("]{").append(child).append("}.");
//        }
        return sb.toString();
    }

    @Override
    public String getSerializedParameters() {
        return "";
    }
}
