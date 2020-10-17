package de.tuebingen.sfs.eie.components.etymology.talk.rule;

import java.util.List;

import de.tuebingen.sfs.eie.talk.EtinenConstantRenderer;
import de.tuebingen.sfs.eie.talk.rule.EtinenTalkingLogicalRule;
import de.tuebingen.sfs.psl.engine.PslProblem;
import de.tuebingen.sfs.psl.engine.RuleAtomGraph;
import de.tuebingen.sfs.psl.util.data.StringUtils;
import de.tuebingen.sfs.psl.util.data.Tuple;

public class TcntToEloaRule extends EtinenTalkingLogicalRule {

	public static final String NAME = "TcntToEloa";
	private static final String RULE = "Tcnt(L1, L2) & Flng(X, L1) & Flng(Y, L2) -> Eloa(X, Y)";
	private static final String VERBALIZATION = "A word can be loaned from a contact language";

	// For serialization.
	public TcntToEloaRule(String serializedParameters) {
		super(NAME, RULE, VERBALIZATION);
	}

	public TcntToEloaRule(PslProblem pslProblem, double weight) {
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
		String contactLang = null;

		for (int i = 0; i < args.length; i++) {
			String groundAtom = atomToStatus.get(i).get(0);
			if (groundAtom.startsWith("Tcnt")) {
				String[] predDetails = StringUtils.split(groundAtom, '(');
				contactLang = StringUtils.split(predDetails[1].substring(0, predDetails[1].length() - 1), ", ")[1];
				break;
			}
		}

		StringBuilder sb = new StringBuilder();
		sb.append(VERBALIZATION).append(", in this case ");
		if (renderer != null) {
			contactLang = renderer.getLanguageRepresentation(contactLang);
		}
		sb.append(contactLang).append(".");
		return sb.toString();
	}

}
