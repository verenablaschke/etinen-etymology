package de.tuebingen.sfs.eie.components.etymology.talk.rule;

import java.util.ArrayList;
import java.util.List;

import de.tuebingen.sfs.eie.talk.pred.EinhPred;
import de.tuebingen.sfs.eie.talk.pred.EloaPred;
import de.tuebingen.sfs.eie.talk.pred.EunkPred;
import de.tuebingen.sfs.psl.engine.PslProblem;
import de.tuebingen.sfs.psl.engine.RuleAtomGraph;
import de.tuebingen.sfs.psl.talk.BeliefScale;
import de.tuebingen.sfs.psl.talk.TalkingArithmeticRule;
import de.tuebingen.sfs.psl.talk.TalkingPredicate;
import de.tuebingen.sfs.psl.util.data.StringUtils;
import de.tuebingen.sfs.psl.util.data.Tuple;

public class EinhOrEloaOrEunkRule extends TalkingArithmeticRule {

	private static final String NAME = "EinhOrEloaOrEunk";
	private static final String RULE = "Einh(X, +Y) + Eloa(X, +Z) + Eunk(X) = 1 .";
	private static final String VERBALIZATION = "The possible explanations for a word's origin follow a probability distribution.";

	public EinhOrEloaOrEunkRule(PslProblem pslProblem) {
		super(NAME, RULE, pslProblem, VERBALIZATION);
	}

	@Override
	public String generateExplanation(String groundingName, String contextAtom, RuleAtomGraph rag,
			boolean whyExplanation) {
		double threshold = 0.1;
		List<String> competitorAtoms = new ArrayList<>();
		List<String> competitorPreds = new ArrayList<>();
		List<String[]> competitorArgs = new ArrayList<>();
		List<Double> competitorBeliefs = new ArrayList<>();

		for (Tuple atomToStatus : rag.getLinkedAtomsForGroundingWithLinkStatusAsList(groundingName)) {
			String atom = atomToStatus.get(0);
			if (atom.equals(contextAtom)) {
				continue;
			}
			double belief = rag.getValue(atom);
			if (belief < threshold) {
				continue;
			}
			competitorAtoms.add(atom);
			String[] predDetails = StringUtils.split(atom, '(');
			competitorPreds.add(predDetails[0]);
			competitorArgs.add(StringUtils.split(predDetails[1].substring(0, predDetails[1].length() - 1), ", "));
			competitorBeliefs.add(belief);
		}

		StringBuilder sb = new StringBuilder();
		sb.append(
				"A word's origin can be explained as inheritance or borrowing (several sources may seem plausible), or it is unknown.");

		if (competitorPreds.size() == 0) {
			sb.append(" In this case, there are no likely competing explanations.");
			return sb.toString();
		}
		if (competitorPreds.size() == 1) {
			sb.append(" An alternative explanation is that ");
			sb.append("\\url[");
			sb.append(escapeForURL(stringToPred(competitorPreds.get(0)).verbalizeIdeaAsSentence(competitorArgs.get(0))));
			sb.append("]{").append(competitorAtoms.get(0)).append("}");
			sb.append(", which ").append(BeliefScale.verbalizeBeliefAsPredicate(competitorBeliefs.get(0)));
			sb.append(".");
			return sb.toString();
		}
		sb.append(" Other explanations are ");
		for (int i = 0; i < competitorPreds.size(); i++) {
			sb.append("that ");
			sb.append("\\url[");
			sb.append(escapeForURL(stringToPred(competitorPreds.get(i)).verbalizeIdeaAsSentence(competitorArgs.get(i))));
			sb.append("]{").append(competitorAtoms.get(i)).append("}");
			sb.append(" (which ").append(BeliefScale.verbalizeBeliefAsPredicate(competitorBeliefs.get(i)));
			sb.append(")");
			if (i < competitorPreds.size() - 1) {
				sb.append(", or ");
			} else {
				sb.append(", ");
			}
		}
		sb.append(".");
		return sb.toString();
	}

	private static TalkingPredicate stringToPred(String str) {
		switch (str) {
		case "Einh":
			return new EinhPred();
		case "Eloa":
			return new EloaPred();
		case "Eunk":
			return new EunkPred();
		}
		return null;
	}

}
