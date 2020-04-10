package de.tuebingen.sfs.eie.components.etymology.talk.rule;

import java.util.List;

import de.tuebingen.sfs.eie.components.etymology.talk.pred.EetyPred;
import de.tuebingen.sfs.eie.talk.pred.EinhPred;
import de.tuebingen.sfs.eie.talk.pred.EloaPred;
import de.tuebingen.sfs.psl.engine.PslProblem;
import de.tuebingen.sfs.psl.engine.RuleAtomGraph;
import de.tuebingen.sfs.psl.talk.BeliefScale;
import de.tuebingen.sfs.psl.talk.TalkingArithmeticRule;
import de.tuebingen.sfs.psl.talk.TalkingPredicate;
import de.tuebingen.sfs.psl.talk.TalkingRule;
import de.tuebingen.sfs.psl.util.data.StringUtils;
import de.tuebingen.sfs.psl.util.data.Tuple;

public class EinhOrEloaRule extends TalkingArithmeticRule {

	public static final String VERBALIZATION = "A word is either inherited or loaned.";
	public static final EetyPred E_ETY = new EetyPred();
	public static final EloaPred E_LOA = new EloaPred();
	public static final EinhPred E_INH = new EinhPred();

	public EinhOrEloaRule(PslProblem pslProblem) {
		super("EinhOrEloa", "Eety(X, Y) = Einh(X, Y) + Eloa(X, Y) .", pslProblem, VERBALIZATION);
	}

	@Override
	public String generateExplanation(String groundingName, String contextAtom, RuleAtomGraph rag,
			boolean whyExplanation) {
		List<Tuple> atomsToStatuses = rag.getLinkedAtomsForGroundingWithLinkStatusAsList(groundingName);
		String[] eetyArgs = null;
		double eetyBelief = -1.0;
		String competitorName = "Eloa";
		TalkingPredicate competitor = E_LOA;
		if (contextAtom.startsWith("Eloa")) {
			competitorName = "Einh";
			competitor = E_INH;
		}
		String competitorAtom = "";
		String[] competitorArgs = null;
		double competitorBelief = -1.0;
		for (Tuple atomToStatus : atomsToStatuses) {
			String[] predDetails = StringUtils.split(atomToStatus.get(0), '(');
			String predName = predDetails[0];
			String[] args = StringUtils.split(predDetails[1].substring(0, predDetails[1].length() - 1), ", ");
			double belief = rag.getValue(atomToStatus.get(0));
			if (predName.equals("Eety")) {
				eetyArgs = args;
				eetyBelief = belief;
			} else if (predName.equals(competitorName)){
				competitorArgs = args;
				competitorBelief = belief;
				competitorAtom = atomToStatus.get(0);
			}
		}

		StringBuilder sb = new StringBuilder();
		sb.append(VERBALIZATION).append(" ");
		sb.append(E_ETY.verbalizeIdeaAsSentence(eetyBelief, eetyArgs)).append(". ");
		sb.append(contextAtom).append(" competes with the possibility of ");
		sb.append("\\url[");
		sb.append(escapeForURL(competitor.verbalizeIdeaAsNP(competitorArgs)));
		sb.append("]");
		sb.append("{").append(competitorAtom).append("}");
		sb.append(", which ");
		sb.append(BeliefScale.verbalizeBeliefAsPredicate(competitorBelief)).append(".");
		return sb.toString();
	}

}
