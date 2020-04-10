package de.tuebingen.sfs.eie.components.etymology.talk.rule;

import java.util.List;

import de.tuebingen.sfs.eie.components.etymology.talk.pred.EetyPred;
import de.tuebingen.sfs.psl.engine.PslProblem;
import de.tuebingen.sfs.psl.engine.RuleAtomGraph;
import de.tuebingen.sfs.psl.talk.TalkingLogicalRule;
import de.tuebingen.sfs.psl.util.data.StringUtils;
import de.tuebingen.sfs.psl.util.data.Tuple;

public class EetyToFsimRule extends TalkingLogicalRule {

	// Only Eety and Fsim can have a value other than 0 or 1.
	private static final String RULE = "Eety(X, Z) & Eety(Y, Z) & (X != Y) & XFufo(X) & XFufo(Y) & Fufo(X, F1) & Fufo(Y, F2) -> Fsim(F1, F2)";
	private static final String VERBALIZATION = "Words derived from the same source should be phonetically similar.";

	public EetyToFsimRule(PslProblem pslProblem, double weight) {
		super("EetyToFsim", weight + ": " + RULE, pslProblem, VERBALIZATION);
	}

	@Override
	public String generateExplanation(String groundingName, String contextAtom, RuleAtomGraph rag,
			boolean whyExplanation) {
		List<Tuple> atomsToStatuses = rag.getLinkedAtomsForGroundingWithLinkStatusAsList(groundingName);
		String[] eety1Args = null;
		String eety1Atom = null;
		double eety1Belief = -1.0;
		String[] eety2Args = null;
		String eety2Atom = null;
		double eety2Belief = -1.0;
		// Assuming (for now) that Fufo is a closed predicate.
		for (Tuple atomToStatus : atomsToStatuses) {
			String[] predDetails = StringUtils.split(atomToStatus.get(0), '(');
			String predName = predDetails[0];
			String[] args = StringUtils.split(predDetails[1].substring(0, predDetails[1].length() - 1), ", ");
			if (predName.equals("Eety")) {
				double belief = rag.getValue(atomToStatus.get(0));
				if (eety1Args == null) {
					eety1Args = args;
					eety1Atom = atomToStatus.get(0);
					eety1Belief = belief;
				} else {
					eety2Args = args;
					eety2Atom = atomToStatus.get(0);
					eety2Belief = belief;
				}
			}
		}

		StringBuilder sb = new StringBuilder();
		sb.append(VERBALIZATION).append(" ");
		sb.append("\\url[");
		sb.append(escapeForURL(new EetyPred().verbalizeIdeaAsSentence(eety1Belief, eety1Args)));
		sb.append("]{").append(eety1Atom).append("}");
		sb.append(" and \\url[");
		sb.append(escapeForURL(new EetyPred().verbalizeIdeaAsSentence(eety2Belief, eety2Args)));
		sb.append("]{").append(eety2Atom).append("}");
		sb.append(". ");
		return sb.toString();
	}

}
