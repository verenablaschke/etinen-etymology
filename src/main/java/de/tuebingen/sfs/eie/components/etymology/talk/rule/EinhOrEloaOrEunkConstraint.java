package de.tuebingen.sfs.eie.components.etymology.talk.rule;

import de.tuebingen.sfs.eie.components.etymology.ideas.EtymologyIdeaGenerator;
import de.tuebingen.sfs.eie.shared.talk.EtinenConstantRenderer;
import de.tuebingen.sfs.eie.shared.talk.pred.EinhPred;
import de.tuebingen.sfs.eie.shared.talk.pred.EloaPred;
import de.tuebingen.sfs.eie.shared.talk.pred.EtinenTalkingPredicate;
import de.tuebingen.sfs.eie.shared.talk.pred.EunkPred;
import de.tuebingen.sfs.eie.shared.talk.rule.EtinenTalkingArithmeticConstraint;
import de.tuebingen.sfs.psl.engine.PslProblem;
import de.tuebingen.sfs.psl.engine.RuleAtomGraph;
import de.tuebingen.sfs.psl.talk.BeliefScale;
import de.tuebingen.sfs.psl.util.data.StringUtils;
import de.tuebingen.sfs.psl.util.data.Tuple;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class EinhOrEloaOrEunkConstraint extends EtinenTalkingArithmeticConstraint {

    public static final String NAME = "EinhOrEloaOrEunk";
    private static final String RULE = "Einh(X, +Y) + Eloa(X, +Z) + Eunk(X) = 1 .";
    private static final String VERBALIZATION = "The possible explanations for a word's origin follow a probability distribution.";

    // For serialization.
    public EinhOrEloaOrEunkConstraint(String serializedParameters) {
        // No idiosyncrasies in this rule, just use default values:
        super(NAME, RULE, VERBALIZATION);
    }

    public EinhOrEloaOrEunkConstraint(PslProblem pslProblem) {
        super(NAME, RULE, pslProblem, VERBALIZATION);
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
        List<Result> nonCompetitors = new ArrayList<>();

        for (Tuple atomToStatus : rag.getLinkedAtomsForGroundingWithLinkStatusAsList(groundingName)) {
            String atom = atomToStatus.get(0);
            if (atom.equals(contextAtom)) {
                continue;
            }
            if (atom.startsWith(EloaPred.NAME) && atom.endsWith(EtymologyIdeaGenerator.CTRL_ARG + ")")) {
                // Eloa dummy atom to control the grounding
                continue;
            }
            double belief = rag.getValue(atom);
            String[] predDetails = StringUtils.split(atom, '(');
            Result res = new Result(atom, predDetails[0],
                    StringUtils.split(predDetails[1].substring(0, predDetails[1].length() - 1), ", "), belief);
            if (belief < threshold) {
                nonCompetitors.add(res);
            } else {
                competitors.add(res);
            }
        }

        StringBuilder sb = new StringBuilder();
        if (stringToPred(contextAtom) instanceof EunkPred) {
            sb.append(
                    "A word's origin is out of scope only if no explanation via inheritance or borrowing is available.");
        } else {
            sb.append(
                    "The last step in a word's history will be an inheritance or a borrowing, unless its origin is out of scope.");
        }
        sb.append("\n");

        if (competitors.size() == 0) {
            sb.append("In this case, there are no likely competing explanations.");
        } else if (competitors.size() == 1) {
            sb.append("An alternative explanation is that ");
            sb.append("\\url[");
            Result competitor = competitors.get(0);
            sb.append(escapeForURL(stringToPred(competitor.pred).verbalizeIdeaAsSentence(renderer, competitor.args)));
            sb.append("]{").append(competitor.atom).append("}");
            sb.append(", which ").append(BeliefScale.verbalizeBeliefAsPredicate(competitor.belief));
            sb.append(".");
            return sb.toString();
        } else {
            Collections.sort(competitors, new Comparator<Result>() {
                @Override
                public int compare(Result o1, Result o2) {
                    return -o1.belief.compareTo(o2.belief);
                }
            });
            sb.append("Other explanations are ");
            for (Result competitor : competitors) {
                sb.append("that ");
                sb.append("\\url[");
                sb.append(
                        escapeForURL(stringToPred(competitor.pred).verbalizeIdeaAsSentence(renderer, competitor.args)));
                sb.append("]{").append(competitor.atom).append("}");
                sb.append(" (which ").append(BeliefScale.verbalizeBeliefAsPredicate(competitor.belief));
                sb.append("), or ");
            }
            sb.delete(sb.length() - 5, sb.length());
            sb.append(".");
        }

        if (nonCompetitors.isEmpty()) {
            return sb.toString();
        }
        sb.append(" (");
        boolean first = true;
        for (Result nonCompetitor : nonCompetitors) {
            if (first) {
                sb.append("It ");
                first = false;
            } else {
                sb.append(", and it ");
            }
            sb.append(BeliefScale.verbalizeBeliefAsPredicate(nonCompetitor.belief));
            sb.append(" that ");
            sb.append("\\url[");
            sb.append(escapeForURL(
                    stringToPred(nonCompetitor.pred).verbalizeIdeaAsSentence(renderer, nonCompetitor.args)));
            sb.append("]{").append(nonCompetitor.atom).append("}");
        }
        sb.append(".)");
        return sb.toString();
    }

    @Override
    public String getSerializedParameters() {
        return "";
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
