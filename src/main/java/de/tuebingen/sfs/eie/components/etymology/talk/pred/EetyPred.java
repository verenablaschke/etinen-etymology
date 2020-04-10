package de.tuebingen.sfs.eie.components.etymology.talk.pred;

import de.tuebingen.sfs.psl.talk.BeliefScale;
import de.tuebingen.sfs.psl.talk.TalkingPredicate;

public class EetyPred extends TalkingPredicate {

	public EetyPred() {
		super("Eety", 2);
	}

	@Override
	public String verbalizeIdea(String... args) {
		return verbalizeIdeaWithBelief(-1, args);
	}

	@Override
	public String verbalizeIdeaWithBelief(double belief, String... args) {
		boolean hasBelief = belief >= 0 && belief <= 1;
		StringBuilder sb = new StringBuilder();
		if (hasBelief) {
			sb.append("It ").append(BeliefScale.verbalizeBeliefAsPredicate(belief)).append(" that ");
		}
		sb.append(verbalizeIdeaAsSentence(args));
		sb.append(".");
		return sb.toString();
	}

	@Override
	public String verbalizeIdeaAsSentence(String... args) {
		return verbalizeIdeaAsSentence(-1, args);
	}

	public String verbalizeIdeaAsSentence(double belief, String... args) {
		boolean hasBelief = belief >= 0 && belief <= 1;
		StringBuilder sb = new StringBuilder();
		sb.append(args[0]).append(" is ");
		if (hasBelief)
			sb.append(BeliefScale.verbalizeBeliefAsAdverb(belief));
		sb.append(" etymologically derived from ").append(args[1]);
		return sb.toString();
	}

	@Override
	public String verbalizeIdeaAsNP(String... args) {
		StringBuilder sb = new StringBuilder();
		sb.append("etymological derivation of ").append(args[0]).append(" from ").append(args[1]);
		return sb.toString();
	}

}
