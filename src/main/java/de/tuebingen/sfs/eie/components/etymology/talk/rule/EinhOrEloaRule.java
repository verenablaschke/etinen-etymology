package de.tuebingen.sfs.eie.components.etymology.talk.rule;

import java.util.List;

import de.tuebingen.sfs.eie.components.etymology.talk.pred.EetyPred;
import de.tuebingen.sfs.psl.engine.PslProblem;
import de.tuebingen.sfs.psl.engine.RuleAtomGraph;
import de.tuebingen.sfs.psl.talk.BeliefScale;
import de.tuebingen.sfs.psl.talk.TalkingArithmeticRule;
import de.tuebingen.sfs.psl.util.data.Tuple;

public class EinhOrEloaRule extends TalkingArithmeticRule {
	
	public static final String VERBALIZATION = "A word is either inherited or loaned.";
	public static final EetyPred E_ETY = new EetyPred();

	public EinhOrEloaRule(PslProblem pslProblem) {
		super("EinhOrEloa", "Eety(X, Y) = Einh(X, Y) + Eloa(X, Y) .", pslProblem, VERBALIZATION);
	}
	
	@Override
	public String generateExplanation(String groundingName, String contextAtom, RuleAtomGraph rag,
			boolean whyExplanation) {
		String[] args = getArgs();
		List<Tuple> atomsToStatuses = rag.getLinkedAtomsForGroundingWithLinkStatusAsList(groundingName);
		StringBuilder sb = new StringBuilder();
		sb.append(VERBALIZATION).append(" ");
		sb.append(E_ETY.verbalizeIdeaAsSentence(args)).append(". ");
		sb.append(contextAtom).append(" competes with ");
		String competitor = "Eloa";
		if (contextAtom.startsWith("Eloa")){
			competitor = "Einh";
		}
		// TODO URL!
		sb.append(competitor).append(", whose belief value is ");
		double belief = -1.0;
		for (Tuple atomToStatus : atomsToStatuses){
			if (atomToStatus.get(0).startsWith(competitor)){
				belief = rag.getValue(atomToStatus.get(0));
				break;
			}
		}
		sb.append(BeliefScale.verbalizeBeliefAsAdjectiveHigh(belief)).append(".");
		return sb.toString();
	}


}
