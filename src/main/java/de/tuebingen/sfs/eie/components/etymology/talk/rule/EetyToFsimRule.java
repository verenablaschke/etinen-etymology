package de.tuebingen.sfs.eie.components.etymology.talk.rule;

import java.util.List;
import java.util.Locale;

import de.tuebingen.sfs.eie.talk.pred.EinhPred;
import de.tuebingen.sfs.eie.talk.pred.EloaPred;
import de.tuebingen.sfs.psl.engine.PslProblem;
import de.tuebingen.sfs.psl.engine.RuleAtomGraph;
import de.tuebingen.sfs.psl.talk.TalkingLogicalRule;
import de.tuebingen.sfs.psl.util.data.StringUtils;
import de.tuebingen.sfs.psl.util.data.Tuple;

public class EetyToFsimRule extends TalkingLogicalRule {

	// Only Eety and Fsim can have a value other than 0 or 1.
	private static final String RULE = "%f: %s(X, Z) & %s(Y, Z) & (X != Y) & XFufo(X) & XFufo(Y) & Fufo(X, F1) & Fufo(Y, F2) -> Fsim(F1, F2)";
	private static final String VERBALIZATION = "Words derived from the same source should be phonetically similar";
	private String rule = null;
	private String eetyType1 = null;
	private String eetyType2 = null;

	public EetyToFsimRule(String eetyType1, String eetyType2, PslProblem pslProblem, double weight) {
		super(String.format("%sAnd%sToFsim", eetyType1, eetyType2), String.format(Locale.US, RULE, weight, eetyType1, eetyType2),
				pslProblem, VERBALIZATION);
		this.rule = String.format(RULE, weight, eetyType1, eetyType2);
		this.eetyType1 = eetyType1;
		this.eetyType2 = eetyType2;
	}

	// TODO change so this does not contain the context atom, but does contain the fufo value
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
			if (predName.equals("Einh") || predName.equals("Eloa")) {
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
		if (eetyType1.equals("Einh")) {
			sb.append(escapeForURL(new EinhPred().verbalizeIdeaAsSentence(eety1Belief, eety1Args)));
		} else {
			sb.append(escapeForURL(new EloaPred().verbalizeIdeaAsSentence(eety1Belief, eety1Args)));
		}
		sb.append("]{").append(eety1Atom).append("}");
		sb.append(" and \\url[");
		if (eetyType2.equals("Einh")) {
			sb.append(escapeForURL(new EinhPred().verbalizeIdeaAsSentence(eety2Belief, eety2Args)));
		} else {
			sb.append(escapeForURL(new EloaPred().verbalizeIdeaAsSentence(eety2Belief, eety2Args)));
		}
		sb.append("]{").append(eety2Atom).append("}");
		sb.append(". ");
		return sb.toString();
	}

}