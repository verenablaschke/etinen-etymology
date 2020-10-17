package de.tuebingen.sfs.eie.components.etymology.talk.rule;

import java.util.List;

import de.tuebingen.sfs.eie.talk.EtinenConstantRenderer;
import de.tuebingen.sfs.eie.talk.rule.EtinenTalkingLogicalRule;
import de.tuebingen.sfs.psl.engine.PslProblem;
import de.tuebingen.sfs.psl.engine.RuleAtomGraph;
import de.tuebingen.sfs.psl.util.data.StringUtils;
import de.tuebingen.sfs.psl.util.data.Tuple;

public class TancToEinhRule extends EtinenTalkingLogicalRule {

	public static final String NAME = "TancToEinh";
	private static final String RULE = "Tanc(L1, L2) & Flng(X, L1) & Flng(Y, L2) -> Einh(X, Y)";
	private static final String VERBALIZATION = "A word can be inherited from its direct ancestor language";

	// For serialization.
	public TancToEinhRule(String serializedParameters) {
		super(NAME, RULE, VERBALIZATION);
	}

	public TancToEinhRule(PslProblem pslProblem, double weight) {
		super(NAME, weight + ": " + RULE, pslProblem, VERBALIZATION + ".");
	}

	@Override
	public String generateExplanation(String groundingName, String contextAtom, RuleAtomGraph rag,
			boolean whyExplanation) {
		return generateExplanation(null, groundingName, contextAtom, rag, whyExplanation);
	}

	@Override
	public String generateExplanation(EtinenConstantRenderer renderer, String groundingName, String contextAtom,
			RuleAtomGraph rag, boolean whyExplanation) {
		String[] args = getArgs();
		List<Tuple> atomToStatus = rag.getLinkedAtomsForGroundingWithLinkStatusAsList(groundingName);

		String ancestor = null;
		for (int i = 0; i < args.length; i++) {
			String groundAtom = atomToStatus.get(i).get(0);
			if (groundAtom.startsWith("Tanc")) {
				String[] predDetails = StringUtils.split(groundAtom, '(');
				ancestor = StringUtils.split(predDetails[1].substring(0, predDetails[1].length() - 1), ", ")[1];
				break;
			}
		}

		StringBuilder sb = new StringBuilder();
		sb.append(VERBALIZATION).append(", in this case ");
		if (renderer != null) {
			ancestor = renderer.getLanguageRepresentation(ancestor);
		}
		sb.append(ancestor).append(".");
		return sb.toString();
	}

}
