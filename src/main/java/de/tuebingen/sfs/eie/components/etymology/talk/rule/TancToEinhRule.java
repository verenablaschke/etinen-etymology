package de.tuebingen.sfs.eie.components.etymology.talk.rule;

import java.util.List;

import de.tuebingen.sfs.psl.engine.PslProblem;
import de.tuebingen.sfs.psl.engine.RuleAtomGraph;
import de.tuebingen.sfs.psl.talk.TalkingLogicalRule;
import de.tuebingen.sfs.psl.util.data.StringUtils;
import de.tuebingen.sfs.psl.util.data.Tuple;

public class TancToEinhRule extends TalkingLogicalRule {

	private static final String RULE = "Tanc(L1, L2) & Flng(X, L1) & Flng(Y, L2) -> Einh(X, Y)";
	private static final String VERBALIZATION = "A word can be inherited from its direct ancestor language";

	public TancToEinhRule(PslProblem pslProblem, double weight) {
		super("TancToEinh", weight + ": " + RULE, pslProblem, VERBALIZATION + ".");
	}

	@Override
	public String generateExplanation(String groundingName, String contextAtom, RuleAtomGraph rag,
			boolean whyExplanation) {
		String[] args = getArgs();
		List<Tuple> atomToStatus = rag.getLinkedAtomsForGroundingWithLinkStatusAsList(groundingName);
		
		String tanc = null;
		String ancestor = null;
		Double tancBelief = null;

		for (int i = 0; i < args.length; i++) {
			String groundAtom = atomToStatus.get(i).get(0);
			if (groundAtom.startsWith("Tanc")) {
				tanc = groundAtom;
				String[] predDetails = StringUtils.split(groundAtom, '(');
				ancestor = StringUtils.split(predDetails[1].substring(0, predDetails[1].length() - 1), ", ")[1];
				tancBelief = rag.getValue(groundAtom);
				break;
			}
		}

		StringBuilder sb = new StringBuilder();
		sb.append(VERBALIZATION).append(", in this case ");
		sb.append(ancestor).append(".");
		sb.append(atomToStatus);
		return sb.toString();
	}

}
