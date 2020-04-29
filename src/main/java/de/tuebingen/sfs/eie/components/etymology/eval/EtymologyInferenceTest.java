package de.tuebingen.sfs.eie.components.etymology.eval;

import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

import de.tuebingen.sfs.eie.components.etymology.ideas.EtymologyIdeaGenerator;
import de.tuebingen.sfs.eie.components.etymology.problems.EtymologyProblem;
import de.tuebingen.sfs.eie.components.etymology.talk.rule.EetyToFsimRule;
import de.tuebingen.sfs.eie.components.etymology.talk.rule.FsimAndSsimToEetyRule;
import de.tuebingen.sfs.psl.engine.InferenceResult;
import de.tuebingen.sfs.psl.engine.ProblemManager;

public class EtymologyInferenceTest {
	// For debugging with a GUI, use the EtymologyFactViewer in the etinen repository.
	
	// TODO run simultaneously
	private static void run(ProblemManager problemManager, Map<String, Double> ruleWeights, PrintStream out){
		EtymologyProblem problem = new EtymologyProblem(problemManager.getDbManager(), "EtymologyProblem", ruleWeights);
		problem.printRules(out);
		EtymologyIdeaGenerator ideaGen = EtymologyIdeaGenerator.getIdeaGeneratorForTesting(problem, false, false);
		ideaGen.generateAtoms();
		InferenceResult result = problemManager.registerAndRunProblem(problem);
		result.getRag().getRagFilter().printInformativeValues(out);
	}
	
	public static void main(String[] args) {
		ProblemManager problemManager = ProblemManager.defaultProblemManager();
		Map<String, Double> ruleWeights = new HashMap<String, Double>();
		ruleWeights.put(EetyToFsimRule.NAME, 5.0);
		ruleWeights.put(FsimAndSsimToEetyRule.NAME, 5.0);
		try {
			run(problemManager, ruleWeights, new PrintStream("C:/Users/vbl/Documents/NorthEuraLex/conf1.log"));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		ruleWeights.put(EetyToFsimRule.NAME, 5.0);
		ruleWeights.put(FsimAndSsimToEetyRule.NAME, 8.0);
		try {
			run(problemManager, ruleWeights, new PrintStream("C:/Users/vbl/Documents/NorthEuraLex/conf2.log"));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
}
