package de.tuebingen.sfs.eie.components.etymology.talk.rule;

import de.tuebingen.sfs.eie.talk.pred.EloaPred;
import de.tuebingen.sfs.psl.engine.PslProblem;
import de.tuebingen.sfs.psl.engine.RuleAtomGraph;
import de.tuebingen.sfs.psl.talk.BeliefScale;
import de.tuebingen.sfs.psl.talk.TalkingArithmeticRule;
import de.tuebingen.sfs.psl.util.data.StringUtils;
import de.tuebingen.sfs.psl.util.data.Tuple;

public class EloaPlusEloaRule extends TalkingArithmeticRule {

	public static final String NAME = "EloaPlusEloa";
	private static final String RULE = "Eloa(X, Y) + Eloa(Y, X) <= 1 .";
	private static final String VERBALIZATION = "Borrowing cannot happen in a circular fashion.";

	public EloaPlusEloaRule(){
		// For serialization.
	}
	
	public EloaPlusEloaRule(PslProblem pslProblem) {
		super(NAME, RULE, pslProblem, VERBALIZATION);
	}

	@Override
	public String generateExplanation(String groundingName, String contextAtom, RuleAtomGraph rag,
			boolean whyExplanation) {
		String competitorAtom = null;
		String[] competitorArgs = null;
		double competitorBelief = -1.0;

		for (Tuple atomToStatus : rag.getLinkedAtomsForGroundingWithLinkStatusAsList(groundingName)) {
			String atom = atomToStatus.get(0);
			String[] predDetails = StringUtils.split(atom, '(');
			if (atom.equals(contextAtom)) {
				continue;
			}
			competitorAtom = atom;
			competitorArgs = StringUtils.split(predDetails[1].substring(0, predDetails[1].length() - 1), ", ");
			competitorBelief = rag.getValue(atom);
		}

		StringBuilder sb = new StringBuilder();
		sb.append("The inverse loanword relationship (");
		sb.append("\\url[");
		sb.append(escapeForURL(new EloaPred().verbalizeIdeaAsNP(competitorArgs)));
		sb.append("]{").append(competitorAtom).append("}");
		sb.append(") ").append(BeliefScale.verbalizeBeliefAsPredicate(competitorBelief));
		sb.append(".");
		return sb.toString();
	}

}
