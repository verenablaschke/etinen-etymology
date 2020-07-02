package de.tuebingen.sfs.eie.components.etymology.talk.rule;

import java.util.List;
import java.util.Locale;

import de.tuebingen.sfs.eie.talk.pred.FsimPred;
import de.tuebingen.sfs.psl.engine.PslProblem;
import de.tuebingen.sfs.psl.engine.RuleAtomGraph;
import de.tuebingen.sfs.psl.talk.BeliefScale;
import de.tuebingen.sfs.psl.talk.TalkingLogicalRule;
import de.tuebingen.sfs.psl.util.data.StringUtils;
import de.tuebingen.sfs.psl.util.data.Tuple;

public class DirectEetyToFsimRule extends TalkingLogicalRule {

	public static final String NAME = "DirectEetyToFsim";
	private static final String RULE = "%.1f: %s(X, Y) & XFufo(X) & XFufo(Y) & Fufo(X, F1) & Fufo(Y, F2) -> Fsim(F1, F2)";
	private static final String VERBALIZATION = "A word should be phonetically similar to its source form.";

	public DirectEetyToFsimRule(){
		// For serialization.
	}
	
	public DirectEetyToFsimRule(String eetyType1, PslProblem pslProblem, double weight) {
		super(String.format("%sToFsim", eetyType1),
				String.format(Locale.US, RULE, weight, eetyType1), pslProblem, VERBALIZATION);
	}

	@Override
	public String generateExplanation(String groundingName, String contextAtom, RuleAtomGraph rag,
			boolean whyExplanation) {
		List<Tuple> atomsToStatuses = rag.getLinkedAtomsForGroundingWithLinkStatusAsList(groundingName);
		String[] fsimArgs = null;
		double fsimBelief = -1.0;
		for (Tuple atomToStatus : atomsToStatuses) {
			String atom = atomToStatus.get(0);
			if (atom.equals(contextAtom)){
				continue;
			}
			String[] predDetails = StringUtils.split(atom, '(');
			String predName = predDetails[0];
			String[] args = StringUtils.split(predDetails[1].substring(0, predDetails[1].length() - 1), ", ");
			double belief = rag.getValue(atom);
			if (predName.equals("Fsim")) {
				fsimArgs = args;
				fsimBelief = belief;
				break;
			}
		}

		StringBuilder sb = new StringBuilder();
		sb.append(VERBALIZATION);
		sb.append(new FsimPred().verbalizeIdeaAsSentence(fsimBelief, fsimArgs));
		sb.append(" (" + (int) (100 * fsimBelief) + "%)");
		sb.append(". ");
		return sb.toString();
	}

}