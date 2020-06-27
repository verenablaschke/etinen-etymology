package de.tuebingen.sfs.eie.components.etymology.eval;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.tuebingen.sfs.eie.components.etymology.filter.EtymologyRagFilter;
import de.tuebingen.sfs.eie.components.etymology.ideas.EtymologyIdeaGenerator;
import de.tuebingen.sfs.eie.components.etymology.problems.EtymologyConfig;
import de.tuebingen.sfs.eie.components.etymology.problems.EtymologyProblem;
import de.tuebingen.sfs.eie.components.etymology.talk.rule.DirectEetyToFsimRule;
import de.tuebingen.sfs.eie.components.etymology.talk.rule.EetyToFsimRule;
import de.tuebingen.sfs.eie.components.etymology.talk.rule.EloaPlusEloaRule;
import de.tuebingen.sfs.eie.components.etymology.talk.rule.EloaPriorRule;
import de.tuebingen.sfs.eie.components.etymology.talk.rule.EunkPriorRule;
import de.tuebingen.sfs.eie.components.etymology.talk.rule.FsimAndSsimToEetyRule;
import de.tuebingen.sfs.psl.engine.AtomTemplate;
import de.tuebingen.sfs.psl.engine.InferenceResult;
import de.tuebingen.sfs.psl.engine.ProblemManager;
import de.tuebingen.sfs.psl.engine.RuleAtomGraph;
import de.tuebingen.sfs.psl.io.RuleAtomGraphIo;
import de.tuebingen.sfs.psl.util.data.RankingEntry;

public class EtymologyInferenceTest {
	// For debugging with a GUI, use the EtymologyFactViewer in the etinen
	// repository.

	private static void run(Map<String, Double> ruleWeights, PrintStream out) {
		ProblemManager problemManager = ProblemManager.defaultProblemManager();
		EtymologyConfig config = new EtymologyConfig(ruleWeights);
		config.addRuleToIgnoreList(EloaPlusEloaRule.NAME);
		config.addRuleToIgnoreList(FsimAndSsimToEetyRule.NAME);
		config.addRuleToIgnoreList(DirectEetyToFsimRule.NAME);
		EtymologyProblem problem = new EtymologyProblem(problemManager.getDbManager(), "EtymologyProblem", config);
		EtymologyIdeaGenerator ideaGen = EtymologyIdeaGenerator.getIdeaGeneratorForTestingLanguage(problem, false,
				false);
		ideaGen.generateAtoms();
		InferenceResult result = problemManager.registerAndRunProblem(problem);
		problem.printRules(out);
		EtymologyResultChecker.checkLanguageAnalysis((EtymologyRagFilter) result.getRag().getRagFilter(), out);
		result.getRag().getRagFilter().printInformativeValues(out);
	}

	private static void gridSearch() {
		Map<String, Double> ruleWeights;

		int i = 0;
		Double[] eetyToFsimValues = new Double[] { 3.0, 5.0, 8.0 };
		// Double[] fsimAndSsimToEetyValues = new Double[] { 3.0, 5.0, 8.0 };
		// Double[] eunkPriors = new Double[] { 1.0, 2.5, 4.0 };
		// Double[] eloaPriors = new Double[] { 1.0, 2.0, 3.0 };
		for (double eetyToFsim : eetyToFsimValues) {
			// for (double fsimAndSsimToEety : fsimAndSsimToEetyValues) {
			// for (double eunkPrior : eunkPriors) {
			// for (double eloaPrior : eloaPriors) {
			ruleWeights = new HashMap<String, Double>();
			ruleWeights.put(EetyToFsimRule.NAME, eetyToFsim);
			// ruleWeights.put(FsimAndSsimToEetyRule.NAME, fsimAndSsimToEety);
			// ruleWeights.put(EunkPriorRule.NAME, eunkPrior);
			// ruleWeights.put(EloaPriorRule.NAME, eloaPrior);
			try {
				run(ruleWeights, new PrintStream("C:/Users/vbl/Documents/NorthEuraLex/conf" + i++ + ".log"));
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
			// }
			// }
			// }
		}
	}

	public static void main(String[] args) {
		// gridSearch();
		 int stop;

		ProblemManager problemManager;
		EtymologyProblem problem;
		InferenceResult result;

		// problemManager = ProblemManager.defaultProblemManager();
		// problem = new EtymologyProblem(problemManager.getDbManager(),
		// "MountainEtymologyProblem");
		// EtymologyIdeaGenerator.getIdeaGeneratorForTestingMountain(problem,
		// false).generateAtoms();
		// result = problemManager.registerAndRunProblem(problem);
		// RuleAtomGraph ragMountain = result.getRag();
		// ragMountain.printToStream(System.out);
		// result.printInferenceValues();
		// problem.printRules(System.out);

		ObjectMapper mapper = new ObjectMapper();
		EtymologyConfig config;
//		config = new EtymologyConfig();
//		config.addRuleWeight(EloaPriorRule.NAME, 5.0);
//		config.addRuleWeight(EunkPriorRule.NAME, 6.0);
//		config.addRuleToIgnoreList(EloaPlusEloaRule.NAME);
//		config.addRuleToIgnoreList(FsimAndSsimToEetyRule.NAME);
//		config.addRuleToIgnoreList(DirectEetyToFsimRule.NAME);
//		config.export(mapper, "etinen-etymology/src/test/resources/serialization/config.json");
		config = EtymologyConfig.fromJson(mapper, "etinen-etymology/src/test/resources/serialization/config.json");
		
		problemManager = ProblemManager.defaultProblemManager();
		problem = new EtymologyProblem(problemManager.getDbManager(), "TestDataEtymologyProblem", config);
//		EtymologyIdeaGenerator ideaGen = EtymologyIdeaGenerator.getIdeaGeneratorWithFictionalData(problem, false, false, false, true);
//		ideaGen.export(mapper, "etinen-etymology/src/test/resources/serialization/ideas.json");
		EtymologyIdeaGenerator ideaGen = EtymologyIdeaGenerator.fromJson(problem, mapper, 
				"etinen-etymology/src/test/resources/serialization/ideas.json");
//		stop = 1/0;
		ideaGen.generateAtoms();
		result = problemManager.registerAndRunProblem(problem);
		problemManager.getDbManager().getAtoms("Eloa", new AtomTemplate("Eloa", "?", "?"));
		RuleAtomGraph ragTest = result.getRag();
		RuleAtomGraphIo.saveToFile(ragTest, mapper);
		stop = 1/0;

//		problemManager = ProblemManager.defaultProblemManager();
//		problem = new EtymologyProblem(problemManager.getDbManager(), "TestDataEtymologyProblem", config);
//		EtymologyIdeaGenerator.getIdeaGeneratorWithFictionalData(problem, true, false, false, true).generateAtoms();
//		result = problemManager.registerAndRunProblem(problem);
//		RuleAtomGraph ragTest2 = result.getRag();
//		
//		problemManager = ProblemManager.defaultProblemManager();
//		problem = new EtymologyProblem(problemManager.getDbManager(), "TestDataEtymologyProblem", config);
//		EtymologyIdeaGenerator.getIdeaGeneratorWithFictionalData(problem, false, true, false, true).generateAtoms();
//		result = problemManager.registerAndRunProblem(problem);
//		RuleAtomGraph ragTest3 = result.getRag();
//		
//		problemManager = ProblemManager.defaultProblemManager();
//		problem = new EtymologyProblem(problemManager.getDbManager(), "TestDataEtymologyProblem", config);
//		EtymologyIdeaGenerator.getIdeaGeneratorWithFictionalData(problem, false, false, true, true).generateAtoms();
//		result = problemManager.registerAndRunProblem(problem);
//		RuleAtomGraph ragTest4 = result.getRag();
//		
//		problemManager = ProblemManager.defaultProblemManager();
//		problem = new EtymologyProblem(problemManager.getDbManager(), "TestDataEtymologyProblem", config);
//		EtymologyIdeaGenerator.getIdeaGeneratorWithFictionalData(problem, true, true, true, true).generateAtoms();
//		result = problemManager.registerAndRunProblem(problem);
//		RuleAtomGraph ragTest5 = result.getRag();
//		
//		problemManager = ProblemManager.defaultProblemManager();
//		problem = new EtymologyProblem(problemManager.getDbManager(), "LanguageEtymologyProblem", config);
//		EtymologyIdeaGenerator.getIdeaGeneratorForTestingLanguage(problem, false, false).generateAtoms();
////		int stop = 1/0;
//		result = problemManager.registerAndRunProblem(problem);
//		RuleAtomGraph ragLanguage = result.getRag();
//
//		problemManager = ProblemManager.defaultProblemManager();
//		problem = new EtymologyProblem(problemManager.getDbManager(), "LanguageEtymologyProblem2", config);
//		EtymologyIdeaGenerator.getIdeaGeneratorForTestingLanguage(problem, true, false).generateAtoms();
//		result = problemManager.registerAndRunProblem(problem);
//		RuleAtomGraph ragLanguage2 = result.getRag();
//
//		problemManager = ProblemManager.defaultProblemManager();
//		problem = new EtymologyProblem(problemManager.getDbManager(), "LanguageEtymologyProblem3", config);
//		EtymologyIdeaGenerator.getIdeaGeneratorForTestingLanguage(problem, false, true).generateAtoms();
//		result = problemManager.registerAndRunProblem(problem);
//		RuleAtomGraph ragLanguage3 = result.getRag();

		// problemManager = ProblemManager.defaultProblemManager();
		// problem = new EtymologyProblem(problemManager.getDbManager(),
		// "HeadEtymologyProblem");
		// EtymologyIdeaGenerator.getIdeaGeneratorForTestingHead(problem,
		// false).generateAtoms();
		// result = problemManager.registerAndRunProblem(problem);
		// RuleAtomGraph ragHead = result.getRag();
		//
		//
		// problemManager = ProblemManager.defaultProblemManager();
		// problem = new EtymologyProblem(problemManager.getDbManager(),
		// "HeadEtymologyProblem2");
		// EtymologyIdeaGenerator.getIdeaGeneratorForTestingHead(problem,
		// true).generateAtoms();
		// result = problemManager.registerAndRunProblem(problem);
		// RuleAtomGraph ragHead2 = result.getRag();
		//
		
		problem.printRules(System.out);
		System.out.println("\nTEST 1");
		EtymologyResultChecker.checkTestAnalysis((EtymologyRagFilter) ragTest.getRagFilter());
//		System.out.println("\nTEST 2 --- synonyms");
//		EtymologyResultChecker.checkTestAnalysis((EtymologyRagFilter) ragTest2.getRagFilter());
//		System.out.println("\nTEST 3 --- more languages per branch");
//		EtymologyResultChecker.checkTestAnalysis((EtymologyRagFilter) ragTest3.getRagFilter());
//		System.out.println("\nTEST 4 --- additional branch");
//		EtymologyResultChecker.checkTestAnalysis((EtymologyRagFilter) ragTest4.getRagFilter());
//		System.out.println("\nTEST 5 --- synonyms, more languages, extra branch");
//		EtymologyResultChecker.checkTestAnalysis((EtymologyRagFilter) ragTest5.getRagFilter());
		
		
		// EtymologyResultChecker.checkMountainAnalysis((EtymologyRagFilter)
		// ragMountain.getRagFilter());
//		EtymologyResultChecker.checkLanguageAnalysis((EtymologyRagFilter) ragLanguage.getRagFilter());
//		EtymologyResultChecker.checkLanguageAnalysis((EtymologyRagFilter) ragLanguage2.getRagFilter());
//		EtymologyResultChecker.checkLanguageAnalysis((EtymologyRagFilter) ragLanguage3.getRagFilter());
		// EtymologyResultChecker.checkHeadAnalysis((EtymologyRagFilter)
		// ragHead.getRagFilter());
		// EtymologyResultChecker.checkHeadAnalysis((EtymologyRagFilter)
		// ragHead2.getRagFilter());

		// Set<String> preds = new HashSet<>();
		// preds.add("Fsim");
		// List<RankingEntry<AtomTemplate>> res = problemManager.getDbManager()
		// .getAtomValuesByPredicate("EtymologyProblem", preds).getList("Fsim");
		// Collections.sort(res, Collections.reverseOrder());
		// for (RankingEntry<AtomTemplate> entry : res) {
		// String arg0 = entry.key.getArgs()[0];
		// String arg1 = entry.key.getArgs()[1];
		// double originalDist =
		// problemManager.getDbManager().getAtoms("Fsimorig", new
		// AtomTemplate[]{new AtomTemplate("Fsimorig", arg0,
		// arg1)}).get(0).value;
		// System.out.println(String.format("%s\t%s\t%.3f\t%.3f", arg0, arg1,
		// entry.value, originalDist));
		// }
	}
}
