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
import java.util.TreeMap;

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
import de.tuebingen.sfs.psl.talk.TalkingPredicate;
import de.tuebingen.sfs.psl.talk.TalkingRule;
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

	private static EtymologyRagFilter runTestFictional(EtymologyConfig config, boolean synonyms, boolean moreLangsPerBranch,
			boolean moreBranches, boolean branchwiseBorrowing, boolean showAllEloa) {
		ProblemManager problemManager = ProblemManager.defaultProblemManager();
		EtymologyProblem problem = new EtymologyProblem(problemManager.getDbManager(), "TestDataEtymologyProblem",
				config);
		EtymologyIdeaGenerator.getIdeaGeneratorWithFictionalData(problem, synonyms, moreLangsPerBranch, moreBranches,
				branchwiseBorrowing).generateAtoms();
		InferenceResult result = problemManager.registerAndRunProblem(problem);
		RuleAtomGraph rag = result.getRag();
		if (showAllEloa) {
			for (RankingEntry<AtomTemplate> eloaResult : problemManager.getDbManager().getAtoms("Eloa",
					new AtomTemplate("Eloa", "?", "?"))) {
				System.out.println(eloaResult);
			}
		}
		return (EtymologyRagFilter) rag.getRagFilter();
	}

	private static void runTestLanguage(EtymologyConfig config, boolean largeConceptSet, boolean largeLanguageSet) {
		ProblemManager problemManager = ProblemManager.defaultProblemManager();
		EtymologyProblem problem = new EtymologyProblem(problemManager.getDbManager(), "EtymologyProblem", config);
		EtymologyIdeaGenerator.getIdeaGeneratorForTestingLanguage(problem, largeConceptSet, largeLanguageSet)
				.generateAtoms();
		InferenceResult result = problemManager.registerAndRunProblem(problem);
		RuleAtomGraph rag = result.getRag();
		EtymologyResultChecker.checkTestAnalysis((EtymologyRagFilter) rag.getRagFilter());
	}

	private static void runTestHead(EtymologyConfig config, boolean largeLanguageSet) {
		ProblemManager problemManager = ProblemManager.defaultProblemManager();
		EtymologyProblem problem = new EtymologyProblem(problemManager.getDbManager(), "EtymologyProblem", config);
		EtymologyIdeaGenerator.getIdeaGeneratorForTestingHead(problem, largeLanguageSet).generateAtoms();
		InferenceResult result = problemManager.registerAndRunProblem(problem);
		RuleAtomGraph rag = result.getRag();
		EtymologyResultChecker.checkTestAnalysis((EtymologyRagFilter) rag.getRagFilter());
	}

	private static void runTestMountain(EtymologyConfig config, boolean largeLanguageSet) {
		ProblemManager problemManager = ProblemManager.defaultProblemManager();
		EtymologyProblem problem = new EtymologyProblem(problemManager.getDbManager(), "EtymologyProblem", config);
		EtymologyIdeaGenerator.getIdeaGeneratorForTestingMountain(problem, largeLanguageSet).generateAtoms();
		InferenceResult result = problemManager.registerAndRunProblem(problem);
		RuleAtomGraph rag = result.getRag();
		EtymologyResultChecker.checkTestAnalysis((EtymologyRagFilter) rag.getRagFilter());
	}

	private static void serialize(ObjectMapper mapper, EtymologyConfig config) {
		config.export(mapper, "etinen-etymology/src/test/resources/serialization/config.json");
		ProblemManager problemManager = ProblemManager.defaultProblemManager();
		EtymologyProblem problem = new EtymologyProblem(problemManager.getDbManager(), "EtymologyProblem", config);
		EtymologyIdeaGenerator ideaGen = EtymologyIdeaGenerator.getIdeaGeneratorWithFictionalData(problem, false, false,
				false, true);
		ideaGen.generateAtoms();
		ideaGen.export(mapper, "etinen-etymology/src/test/resources/serialization/ideas.json");
		InferenceResult result = problemManager.registerAndRunProblem(problem);
		RuleAtomGraph rag = result.getRag();
		RuleAtomGraphIo.saveToFile(rag, problem, mapper);
		EtymologyResultChecker.checkTestAnalysis((EtymologyRagFilter) rag.getRagFilter());
	}

	private static void deserialize(ObjectMapper mapper) {
		EtymologyConfig config = EtymologyConfig.fromJson(mapper,
				"etinen-etymology/src/test/resources/serialization/config.json");
		ProblemManager problemManager = ProblemManager.defaultProblemManager();
		EtymologyProblem problem = new EtymologyProblem(problemManager.getDbManager(), "EtymologyProblem", config);
		EtymologyIdeaGenerator ideaGen = EtymologyIdeaGenerator.fromJson(problem, null, mapper,
				"etinen-etymology/src/test/resources/serialization/ideas.json");
		ideaGen.generateAtoms();
		InferenceResult result = problemManager.registerAndRunProblem(problem);
		System.out.println("(New) RAG from imported config and idea generator:");
		RuleAtomGraph rag = result.getRag();
		RuleAtomGraphIo.saveToFile(rag, problem, mapper);
		EtymologyResultChecker.checkTestAnalysis((EtymologyRagFilter) rag.getRagFilter());
		System.out.println("Deserialized version of the same RAG:");
		Map<String, TalkingPredicate> talkingPreds = new TreeMap<>();
		Map<String, TalkingRule> talkingRules = new TreeMap<>();
		rag = RuleAtomGraphIo.ragFromFile(mapper, talkingPreds, talkingRules);
		EtymologyResultChecker.checkTestAnalysis((EtymologyRagFilter) rag.getRagFilter());
		System.out.println("Talking predicates: " + talkingPreds);
		System.out.println("Talking rules: " + talkingRules);
	}

	public static void main(String[] args) {
		// gridSearch();
		int stop;

		boolean loadConfig = false;
		boolean branchwiseBorrowing = false;
		boolean printAllEloaValues = false;

		// Which tests should be run?
		boolean fictionalData = true;
		boolean language = false;
		boolean mountain = false;
		boolean head = false;

		ObjectMapper mapper = new ObjectMapper();
		EtymologyConfig config;

		if (loadConfig) {
			config = EtymologyConfig.fromJson(mapper, "etinen-etymology/src/test/resources/serialization/config.json");
		} else {
			config = new EtymologyConfig();
			config.addRuleWeight(EloaPriorRule.NAME, 5.0);
			config.addRuleWeight(EunkPriorRule.NAME, 6.0);
			config.addRuleToIgnoreList(EloaPlusEloaRule.NAME);
			config.addRuleToIgnoreList(FsimAndSsimToEetyRule.NAME);
			config.addRuleToIgnoreList(DirectEetyToFsimRule.NAME);
		}

		// serialize(mapper, config);
		// deserialize(mapper);

		if (fictionalData) {
			System.out.println("\nTEST 1");
			EtymologyRagFilter erf1 = runTestFictional(config, false, false, false, branchwiseBorrowing, printAllEloaValues);
			System.out.println("\nTEST 2 --- synonyms");
			EtymologyRagFilter erf2 = runTestFictional(config, true, false, false, branchwiseBorrowing, printAllEloaValues);
			System.out.println("\nTEST 3 --- more languages per branch");
			EtymologyRagFilter erf3 = runTestFictional(config, false, true, false, branchwiseBorrowing, printAllEloaValues);
			System.out.println("\nTEST 4 --- additional branch");
			EtymologyRagFilter erf4 = runTestFictional(config, false, false, true, branchwiseBorrowing, printAllEloaValues);
			System.out.println("\nTEST 5 --- synonyms, more languages, extra branch");
			EtymologyRagFilter erf5 = runTestFictional(config, true, true, true, branchwiseBorrowing, printAllEloaValues);
			
			// Print the results only now to skip the various status updates in the console:
			System.out.println("\nTEST 1");
			EtymologyResultChecker.checkTestAnalysis(erf1);
			System.out.println("\nTEST 2 --- synonyms");
			EtymologyResultChecker.checkTestAnalysis(erf2);
			System.out.println("\nTEST 3 --- more languages per branch");
			EtymologyResultChecker.checkTestAnalysis(erf3);
			System.out.println("\nTEST 4 --- additional branch");
			EtymologyResultChecker.checkTestAnalysis(erf4);
			System.out.println("\nTEST 5 --- synonyms, more languages, extra branch");
			EtymologyResultChecker.checkTestAnalysis(erf5);
		}

		if (language) {
			System.out.println("\n\"LANGUAGE\" 1 --- one concept, few languages");
			runTestLanguage(config, false, false);

			System.out.println("\n\"LANGUAGE\" 2 --- several concepts, few languages");
			runTestLanguage(config, true, false);

			System.out.println("\n\"LANGUAGE\" 3 --- one concept, many languages");
			runTestLanguage(config, false, false);
		}

		if (head) {
			System.out.println("\n\"HEAD\" 1 --- few languages");
			runTestHead(config, false);

			System.out.println("\n\"HEAD\" 2 --- many languages");
			runTestHead(config, true);
		}

		if (mountain) {
			System.out.println("\n\"MOUNTAIN\" 1 --- few languages");
			runTestMountain(config, false);

			System.out.println("\n\"MOUNTAIN\" 2 --- many languages");
			runTestMountain(config, true);
		}
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
