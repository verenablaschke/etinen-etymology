package de.tuebingen.sfs.eie.components.etymology.talk.rule;

import java.util.List;
import java.util.Locale;

import de.tuebingen.sfs.eie.talk.EtinenConstantRenderer;
import de.tuebingen.sfs.eie.talk.pred.FsimPred;
import de.tuebingen.sfs.eie.talk.rule.EtinenTalkingLogicalRule;
import de.tuebingen.sfs.psl.engine.PslProblem;
import de.tuebingen.sfs.psl.engine.RuleAtomGraph;
import de.tuebingen.sfs.psl.talk.BeliefScale;
import de.tuebingen.sfs.psl.util.data.StringUtils;
import de.tuebingen.sfs.psl.util.data.Tuple;

public class EloaAndEetyToFsimRule extends EtinenTalkingLogicalRule {

	public static final String NAME = "EloaAndEetyToFsim";
	// Only Eety and Fsim can have a value other than 0 or 1.
	// Eety is the only open predicate.
	private static final String RULE = "%.1f: Eloa(X, Y) & %s(Y, Z) & %s(W, Z) & (X != W)  & (X != Z) & XFufo(X) & XFufo(W) & Fufo(X, F1) & Fufo(W, F2) -> Fsim(F1, F2)";
	private static final String VERBALIZATION = "A loanword should be similar to the word it is derived from and its relatives.";

	// For serialization.
	public EloaAndEetyToFsimRule() {
		super(NAME, RULE, VERBALIZATION);
	}

	public EloaAndEetyToFsimRule(String eetyType1, String eetyType2, PslProblem pslProblem, double weight) {
		super(String.format("EloaAnd%sAnd%sToFsim", eetyType1, eetyType2),
				String.format(Locale.US, RULE, weight, eetyType1, eetyType2), pslProblem, VERBALIZATION);
	}

	@Override
	public String generateExplanation(String groundingName, String contextAtom, RuleAtomGraph rag,
			boolean whyExplanation) {
		return generateExplanation(null, groundingName, contextAtom, rag, whyExplanation);
	}

	@Override
	public String generateExplanation(EtinenConstantRenderer renderer, String groundingName, String contextAtom,
			RuleAtomGraph rag, boolean whyExplanation) {
		List<Tuple> atomsToStatuses = rag.getLinkedAtomsForGroundingWithLinkStatusAsList(groundingName);
		String[] eety1Args = null;
		String eety1Atom = null;
		double eety1Belief = -1.0;
		String[] eety2Args = null;
		String eety2Atom = null;
		double eety2Belief = -1.0;
		String[] fsimArgs = null;
		double fsimBelief = -1.0;
		boolean foundContext = false;
		for (Tuple atomToStatus : atomsToStatuses) {
			String atom = atomToStatus.get(0);
			if ((!foundContext) && atom.equals(contextAtom)) {
				foundContext = true;
				continue;
			}
			String[] predDetails = StringUtils.split(atom, '(');
			String predName = predDetails[0];
			String[] args = StringUtils.split(predDetails[1].substring(0, predDetails[1].length() - 1), ", ");
			double belief = rag.getValue(atom);
			if (predName.equals("Einh") || predName.equals("Eloa")) {
				if (eety1Atom == null) {
					eety1Args = args;
					eety1Atom = atom;
					eety1Belief = belief;
				} else {
					eety2Args = args;
					eety2Atom = atom;
					eety2Belief = belief;
				}
			} else if (predName.equals("Fsim")) {
				fsimArgs = args;
				fsimBelief = belief;
			}
		}

		// TODO make sure order makes sense (vbl)
		StringBuilder sb = new StringBuilder();
		sb.append(VERBALIZATION);
		sb.append(" It ").append(BeliefScale.verbalizeBeliefAsPredicate(eety1Belief));
		sb.append(" that ");
		sb.append("\\url[");
		sb.append(escapeForURL(renderer.getFormRepresentation(eety1Args[0]))).append(" is ");
		if (eety1Atom.startsWith("Einh")) {
			sb.append("inherited");
		} else {
			sb.append("borrowed");
		}
		sb.append(" from ").append(escapeForURL(renderer.getFormRepresentation(eety1Args[1])));
		sb.append("]{").append(eety1Atom).append("}");
		sb.append(", and it ").append(BeliefScale.verbalizeBeliefAsPredicate(eety2Belief));
		sb.append(" that ");
		sb.append("\\url[");
		sb.append(escapeForURL(renderer.getFormRepresentation(eety2Args[0]))).append(" is also ");
		if (eety1Atom.startsWith("Einh") && eety2Atom.startsWith("Einh")) {
			sb.append("inherited");
		} else if (eety1Atom.startsWith("Eloa") && eety2Atom.startsWith("Eloa")) {
			sb.append("borrowed");
		} else {
			sb.append("derived");
		}
		sb.append(" from ").append(escapeForURL(renderer.getFormRepresentation(eety2Args[1])));
		sb.append("]{").append(eety2Atom).append("}");
		sb.append(", but ");
		sb.append(new FsimPred().verbalizeIdeaAsSentence(renderer, fsimBelief, fsimArgs));
		sb.append(" (" + (int) (100 * fsimBelief) + "%)");
		sb.append(". ");
		return sb.toString();
	}

}
