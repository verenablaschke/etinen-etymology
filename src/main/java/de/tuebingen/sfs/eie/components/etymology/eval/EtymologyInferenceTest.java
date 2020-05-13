package de.tuebingen.sfs.eie.components.etymology.eval;

import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

import de.tuebingen.sfs.eie.components.etymology.filter.EtymologyRagFilter;
import de.tuebingen.sfs.eie.components.etymology.ideas.EtymologyIdeaGenerator;
import de.tuebingen.sfs.eie.components.etymology.problems.EtymologyProblem;
import de.tuebingen.sfs.eie.components.etymology.talk.rule.EetyToFsimRule;
import de.tuebingen.sfs.eie.components.etymology.talk.rule.EloaPriorRule;
import de.tuebingen.sfs.eie.components.etymology.talk.rule.FsimAndSsimToEetyRule;
import de.tuebingen.sfs.eie.gui.facts.StandaloneFactViewer;
import de.tuebingen.sfs.psl.engine.InferenceResult;
import de.tuebingen.sfs.psl.engine.ProblemManager;
import de.tuebingen.sfs.psl.engine.RuleAtomGraph;

public class EtymologyInferenceTest {
	// For debugging with a GUI, use the EtymologyFactViewer in the etinen repository.
	
	private static void run(Map<String, Double> ruleWeights, PrintStream out){
		ProblemManager problemManager = ProblemManager.defaultProblemManager();
		EtymologyProblem problem = new EtymologyProblem(problemManager.getDbManager(), "EtymologyProblem", ruleWeights);
		EtymologyIdeaGenerator ideaGen = EtymologyIdeaGenerator.getIdeaGeneratorForTestingLanguage(problem, false, false);
		ideaGen.generateAtoms();
		InferenceResult result = problemManager.registerAndRunProblem(problem);
		problem.printRules(out);
		result.getRag().getRagFilter().printInformativeValues(out);
	}
	
	private static void gridSearch(){
		Map<String, Double> ruleWeights = new HashMap<String, Double>();
		ruleWeights.put(EetyToFsimRule.NAME, 5.0);
		ruleWeights.put(FsimAndSsimToEetyRule.NAME, 5.0);
		try {
			run(ruleWeights, new PrintStream("C:/Users/vbl/Documents/NorthEuraLex/conf1.log"));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
		ruleWeights.put(EetyToFsimRule.NAME, 5.0);
		ruleWeights.put(FsimAndSsimToEetyRule.NAME, 8.0);
		try {
			run(ruleWeights, new PrintStream("C:/Users/vbl/Documents/NorthEuraLex/conf2.log"));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
		ruleWeights.put(EetyToFsimRule.NAME, 1.0);
		ruleWeights.put(FsimAndSsimToEetyRule.NAME, 1.0);
		try {
			run(ruleWeights, new PrintStream("C:/Users/vbl/Documents/NorthEuraLex/conf3.log"));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
		ruleWeights = new HashMap<String, Double>();
		ruleWeights.put(EloaPriorRule.NAME, 1.0);
		try {
			run(ruleWeights, new PrintStream("C:/Users/vbl/Documents/NorthEuraLex/conf4.log"));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		ruleWeights = new HashMap<String, Double>();
		ruleWeights.put(EetyToFsimRule.NAME, 8.0);
		ruleWeights.put(FsimAndSsimToEetyRule.NAME, 5.0);
		try {
			run(ruleWeights, new PrintStream("C:/Users/vbl/Documents/NorthEuraLex/conf5.log"));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		ruleWeights = new HashMap<String, Double>();
		ruleWeights.put(EetyToFsimRule.NAME, 8.0);
		ruleWeights.put(FsimAndSsimToEetyRule.NAME, 8.0);
		try {
			run(ruleWeights, new PrintStream("C:/Users/vbl/Documents/NorthEuraLex/conf6.log"));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
//		gridSearch();
		

		ProblemManager problemManager = ProblemManager.defaultProblemManager();
		EtymologyProblem problem = new EtymologyProblem(problemManager.getDbManager(), "EtymologyProblem");
		EtymologyIdeaGenerator ideaGen = EtymologyIdeaGenerator.getIdeaGeneratorForTestingMountain(problem, false);
		ideaGen.generateAtoms();
		InferenceResult result = problemManager.registerAndRunProblem(problem);
		RuleAtomGraph rag = result.getRag();
		rag.printToStream(System.out);
		result.printInferenceValues();
		problem.printRules(System.out);
		EtymologyResultChecker.checkMountainAnalysis((EtymologyRagFilter) result.getRag().getRagFilter());
	}
}
