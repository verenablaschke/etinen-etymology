package de.tuebingen.sfs.eie.components.etymology.talk.rule;

import java.util.List;
import java.util.Locale;

import de.tuebingen.sfs.eie.talk.pred.EinhPred;
import de.tuebingen.sfs.eie.talk.pred.EloaPred;
import de.tuebingen.sfs.eie.talk.pred.FsimPred;
import de.tuebingen.sfs.psl.engine.PslProblem;
import de.tuebingen.sfs.psl.engine.RuleAtomGraph;
import de.tuebingen.sfs.psl.talk.BeliefScale;
import de.tuebingen.sfs.psl.talk.TalkingLogicalRule;
import de.tuebingen.sfs.psl.util.data.StringUtils;
import de.tuebingen.sfs.psl.util.data.Tuple;

public class EetyToFsimRule extends TalkingLogicalRule {

	// Only Eety and Fsim can have a value other than 0 or 1.
	// Eety is the only open predicate.
	private static final String RULE = "%f: %s(X, Z) & %s(Y, Z) & (X != Y) & XFufo(X) & XFufo(Y) & Fufo(X, F1) & Fufo(Y, F2) -> Fsim(F1, F2)";
	private static final String VERBALIZATION = "Words derived from the same source should be phonetically similar";
	private String rule = null;
	private String eetyType1 = null;
	private String eetyType2 = null;

	public EetyToFsimRule(String eetyType1, String eetyType2, PslProblem pslProblem, double weight) {
		super(String.format("%sAnd%sToFsim", eetyType1, eetyType2),
				String.format(Locale.US, RULE, weight, eetyType1, eetyType2), pslProblem, VERBALIZATION);
		this.rule = String.format(RULE, weight, eetyType1, eetyType2);
		this.eetyType1 = eetyType1;
		this.eetyType2 = eetyType2;
	}

	// TODO nicer generation when both eety types are identical (vbl)
	@Override
	public String generateExplanation(String groundingName, String contextAtom, RuleAtomGraph rag,
			boolean whyExplanation) {
		List<Tuple> atomsToStatuses = rag.getLinkedAtomsForGroundingWithLinkStatusAsList(groundingName);
		String[] eetyArgs = null;
		String eetyAtom = null;
		double eetyBelief = -1.0;
		String[] fufoArgs = null;
		double fufoBelief = -1.0;
		for (Tuple atomToStatus : atomsToStatuses) {
			String atom = atomToStatus.get(0);
			if (atom.equals(contextAtom)){
				continue;
			}
			String[] predDetails = StringUtils.split(atom, '(');
			String predName = predDetails[0];
			String[] args = StringUtils.split(predDetails[1].substring(0, predDetails[1].length() - 1), ", ");
			double belief = rag.getValue(atom);
			if (predName.equals("Einh") || predName.equals("Eloa")) {
				eetyArgs = args;
				eetyAtom = atom;
				eetyBelief = belief;
			} else if (predName.equals("Fufo")) {
				fufoArgs = args;
				fufoBelief = belief;
			}
		}

		StringBuilder sb = new StringBuilder();
		sb.append(VERBALIZATION).append(". ");
		sb.append("It ").append(BeliefScale.verbalizeBeliefAsPredicate(eetyBelief));
		sb.append(" that ");
		sb.append("\\url[");
		sb.append(escapeForURL(eetyArgs[0] + " is also derived from " + eetyArgs[1]));
		sb.append("]{").append(eetyAtom).append("}");
		sb.append(", and ");
		sb.append(new FsimPred().verbalizeIdeaAsSentence(fufoBelief, fufoArgs));
		sb.append(". ");
		return sb.toString();
	}

}