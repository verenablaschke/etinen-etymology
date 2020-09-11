package de.tuebingen.sfs.eie.components.etymology.talk.rule;

import java.util.List;
import java.util.Locale;

import de.tuebingen.sfs.eie.core.EtinenConstantRenderer;
import de.tuebingen.sfs.eie.talk.pred.SsimPred;
import de.tuebingen.sfs.eie.talk.rule.EtinenTalkingLogicalRule;
import de.tuebingen.sfs.psl.engine.PslProblem;
import de.tuebingen.sfs.psl.engine.RuleAtomGraph;
import de.tuebingen.sfs.psl.talk.BeliefScale;
import de.tuebingen.sfs.psl.util.data.StringUtils;
import de.tuebingen.sfs.psl.util.data.Tuple;

public class EetyToSsimRule extends EtinenTalkingLogicalRule {

	public static final String NAME = "EetyToSsim";
	// Only Eety and Ssim can have a value other than 0 or 1.
	// Eety is the only open predicate.
	private static final String RULE = "%.1f: %s(X, Z) & %s(Y, Z) & (X != Y) & Fsem(X, C1) & Fsem(Y, C2) -> Ssim(C1, C2)";
	private static final String VERBALIZATION = "Words derived from the same source should be semantically similar.";

	// For serialization.
	public EetyToSsimRule() {
		super(NAME, RULE, VERBALIZATION);
	}

	public EetyToSsimRule(String eetyType1, String eetyType2, PslProblem pslProblem, double weight) {
		super(String.format("%sAnd%sToSsim", eetyType1, eetyType2),
				String.format(Locale.US, RULE, weight, eetyType1, eetyType2), pslProblem, VERBALIZATION);
	}

	@Override
	public String generateExplanation(EtinenConstantRenderer renderer, String groundingName, String contextAtom,
			RuleAtomGraph rag, boolean whyExplanation) {
		List<Tuple> atomsToStatuses = rag.getLinkedAtomsForGroundingWithLinkStatusAsList(groundingName);
		String[] eetyArgs = null;
		String eetyAtom = null;
		double eetyBelief = -1.0;
		String[] ssimArgs = null;
		double ssimBelief = -1.0;
		for (Tuple atomToStatus : atomsToStatuses) {
			String atom = atomToStatus.get(0);
			if (atom.equals(contextAtom)) {
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
			} else if (predName.equals("Ssim")) {
				ssimArgs = args;
				ssimBelief = belief;
			}
		}

		StringBuilder sb = new StringBuilder();
		sb.append(VERBALIZATION);
		sb.append(" It ").append(BeliefScale.verbalizeBeliefAsPredicate(eetyBelief));
		sb.append(" that ");
		sb.append("\\url[");
		sb.append(escapeForURL(eetyArgs[0])).append(" is also ");
		if (contextAtom.startsWith("Einh") && eetyAtom.startsWith("Einh")) {
			sb.append("inherited");
		} else if (contextAtom.startsWith("Eloa") && eetyAtom.startsWith("Eloa")) {
			sb.append("borrowed");
		} else {
			sb.append("derived");
		}
		sb.append(" from ").append(escapeForURL(eetyArgs[1]));
		sb.append("]{").append(eetyAtom).append("}");
		sb.append(", but ");
		sb.append(new SsimPred().verbalizeIdeaAsSentence(renderer, ssimBelief, ssimArgs));
		sb.append(" (" + (int) (100 * ssimBelief) + "%)");
		sb.append(". ");
		return sb.toString();
	}

}