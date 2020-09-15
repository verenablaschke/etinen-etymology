package de.tuebingen.sfs.eie.components.etymology.talk.rule;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import de.tuebingen.sfs.eie.talk.EtinenConstantRenderer;
import de.tuebingen.sfs.eie.talk.pred.EinhPred;
import de.tuebingen.sfs.eie.talk.pred.EloaPred;
import de.tuebingen.sfs.eie.talk.pred.EtinenTalkingPredicate;
import de.tuebingen.sfs.eie.talk.pred.EunkPred;
import de.tuebingen.sfs.eie.talk.rule.EtinenTalkingArithmeticRule;
import de.tuebingen.sfs.psl.engine.PslProblem;
import de.tuebingen.sfs.psl.engine.RuleAtomGraph;
import de.tuebingen.sfs.psl.talk.BeliefScale;
import de.tuebingen.sfs.psl.util.data.StringUtils;
import de.tuebingen.sfs.psl.util.data.Tuple;

public class EinhOrEloaOrEunkRule extends EtinenTalkingArithmeticRule {

	public static final String NAME = "EinhOrEloaOrEunk";
	private static final String RULE = "Einh(X, +Y) + Eloa(X, +Z) + Eunk(X) = 1 .";
	private static final String VERBALIZATION = "The possible explanations for a word's origin follow a probability distribution.";

	// For serialization.
	public EinhOrEloaOrEunkRule() {
		super(NAME, RULE, VERBALIZATION);
	}

	public EinhOrEloaOrEunkRule(PslProblem pslProblem) {
		super(NAME, RULE, pslProblem, VERBALIZATION);
	}

	@Override
	public String generateExplanation(String groundingName, String contextAtom, RuleAtomGraph rag,
			boolean whyExplanation) {
		return generateExplanation(null, groundingName, contextAtom, rag, whyExplanation);
	}

	@Override
	public String generateExplanation(EtinenConstantRenderer renderer, String groundingName, String contextAtom,
			RuleAtomGraph rag, boolean whyExplanation) {
		double threshold = 0.1;
		List<Result> competitors = new ArrayList<>();

		for (Tuple atomToStatus : rag.getLinkedAtomsForGroundingWithLinkStatusAsList(groundingName)) {
			String atom = atomToStatus.get(0);
			if (atom.equals(contextAtom)) {
				continue;
			}
			double belief = rag.getValue(atom);
			if (belief < threshold) {
				continue;
			}
			String[] predDetails = StringUtils.split(atom, '(');
			competitors.add(new Result(atom, predDetails[0],
					StringUtils.split(predDetails[1].substring(0, predDetails[1].length() - 1), ", "), belief));
		}

		StringBuilder sb = new StringBuilder();
		sb.append(
				"A word's origin can be explained as inheritance or borrowing (several sources may seem plausible), or it is unknown.");

		if (competitors.size() == 0) {
			sb.append(" In this case, there are no likely competing explanations.");
			return sb.toString();
		}
		if (competitors.size() == 1) {
			sb.append(" An alternative explanation is that ");
			sb.append("\\url[");
			Result competitor = competitors.get(0);
			sb.append(escapeForURL(stringToPred(competitor.pred).verbalizeIdeaAsSentence(renderer, competitor.args)));
			sb.append("]{").append(competitor.atom).append("}");
			sb.append(", which ").append(BeliefScale.verbalizeBeliefAsPredicate(competitor.belief));
			sb.append(".");
			return sb.toString();
		}
		Collections.sort(competitors, new Comparator<Result>() {
			@Override
			public int compare(Result o1, Result o2) {
				return -o1.belief.compareTo(o2.belief);
			}
		});
		sb.append(" Other explanations are ");
		for (Result competitor : competitors) {
			sb.append("that ");
			sb.append("\\url[");
			sb.append(escapeForURL(stringToPred(competitor.pred).verbalizeIdeaAsSentence(renderer, competitor.args)));
			sb.append("]{").append(competitor.atom).append("}");
			sb.append(" (which ").append(BeliefScale.verbalizeBeliefAsPredicate(competitor.belief));
			sb.append("), or ");
		}
		sb.delete(sb.length() - 5, sb.length());
		sb.append(".");
		return sb.toString();
	}

	private static EtinenTalkingPredicate stringToPred(String str) {
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

	private class Result {
		String atom;
		String pred;
		String args[];
		Double belief;

		Result(String atom, String pred, String[] args, double belief) {
			this.atom = atom;
			this.pred = pred;
			this.args = args;
			this.belief = belief;
		}
	}

}
