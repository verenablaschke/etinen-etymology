package de.tuebingen.sfs.eie.components.etymology.talk.rule;

import java.util.List;

import de.tuebingen.sfs.eie.components.etymology.talk.pred.EetyPred;
import de.tuebingen.sfs.eie.talk.pred.FsimPred;
import de.tuebingen.sfs.psl.engine.PslProblem;
import de.tuebingen.sfs.psl.engine.RuleAtomGraph;
import de.tuebingen.sfs.psl.talk.TalkingLogicalRule;
import de.tuebingen.sfs.psl.util.data.StringUtils;
import de.tuebingen.sfs.psl.util.data.Tuple;

public class FsimAndSsimToEetyRule extends TalkingLogicalRule {
	// TODO add restriction that ~Eety(X,Y), ~Eety(Y,X) ?

	private static final String RULE = "Fufo(X, F1) & Fufo(Y, F2) & Fsim(F1, F2) &" // phonetic
																					// similarity
			// + "Fsem(X, C1) & Fsem(Y, C2) & Ssim(C1, C2) &" // semantic
			// similarity TODO uncomment
			// + "Eety(X, Z) & (X != Y) & (Y != Z)" + "-> Eety(Y, Z)"; // ->
			// same source
			+ "Eety(X, Z) & (X != Y) & (Y != Z)" + "-> Eety(Y, Z)"; // -> same
																	// source
	private static final String VERBALIZATION = "If two words are phonetically and semantically similar, "
			+ "they are probably derived from the same source.";

	public FsimAndSsimToEetyRule(PslProblem pslProblem, double weight) {
		super("FsimAndSsimToEety", weight + ": " + RULE, pslProblem, VERBALIZATION);
	}

	@Override
	public String generateExplanation(String groundingName, String contextAtom, RuleAtomGraph rag,
			boolean whyExplanation) {
		List<Tuple> atomsToStatuses = rag.getLinkedAtomsForGroundingWithLinkStatusAsList(groundingName);
		String[] fsimArgs = null;
		String fsimAtom = null;
		double fsimBelief = -2.0;
		String[] eetyArgs = null;
		String eetyAtom = null;
		double eetyBelief = -2.0;
		// Assuming (for now) that Fufo is a closed predicate.
		for (Tuple atomToStatus : atomsToStatuses) {
			String[] predDetails = StringUtils.split(atomToStatus.get(0), '(');
			String predName = predDetails[0];
			String[] args = StringUtils.split(predDetails[1].substring(0, predDetails[1].length() - 1), ", ");
			double belief = rag.getValue(atomToStatus.get(0));
			if (predName.equals("Fsim")) {
				fsimArgs = args;
				fsimAtom = atomToStatus.get(0);
				fsimBelief = belief;
			} else if (predName.equals("Eety") && !atomToStatus.get(0).equals(contextAtom)) {
				eetyArgs = args;
				eetyAtom = atomToStatus.get(0);
				eetyBelief = belief;
			}
		}
		
		// TODO del
		System.out.println("\n");
		System.out.println(atomsToStatuses);
		System.out.println("Context: " + contextAtom);
		System.out.println(fsimAtom + " : " + fsimBelief);
		System.out.println(eetyAtom + " : " + eetyBelief);

		StringBuilder sb = new StringBuilder();
		sb.append(VERBALIZATION).append(" ");
		sb.append(escapeForURL(new FsimPred().verbalizeIdeaAsSentence(fsimBelief, fsimArgs)));
		sb.append(" and \\url[");
		sb.append(escapeForURL(new EetyPred().verbalizeIdeaAsSentence(eetyBelief, eetyArgs)));
		sb.append("]{").append(eetyAtom).append("}");
		sb.append(". ");
		
		System.out.println(sb);
		
		return sb.toString();
	}

}
