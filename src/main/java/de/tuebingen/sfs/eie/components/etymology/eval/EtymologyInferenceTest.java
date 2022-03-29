package de.tuebingen.sfs.eie.components.etymology.eval;

import static de.tuebingen.sfs.psl.engine.AtomTemplate.ANY_CONST;

import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.tuebingen.sfs.cldfjava.data.CLDFWordlistDatabase;
import de.tuebingen.sfs.eie.components.etymology.filter.EtymologyRagFilter;
import de.tuebingen.sfs.eie.components.etymology.ideas.EtymologyIdeaGeneratorDEPRECATED;
import de.tuebingen.sfs.eie.components.etymology.problems.EtymologyProblem;
import de.tuebingen.sfs.eie.components.etymology.problems.EtymologyProblemConfigDEPRECATED;
import de.tuebingen.sfs.eie.components.etymology.talk.rule.DirectEetyToFsimRule;
import de.tuebingen.sfs.eie.components.etymology.talk.rule.EetyToFsimRule;
import de.tuebingen.sfs.eie.components.etymology.talk.rule.EloaPlusEloaRule;
import de.tuebingen.sfs.eie.components.etymology.talk.rule.EloaPriorRule;
import de.tuebingen.sfs.eie.components.etymology.talk.rule.EunkPriorRule;
import de.tuebingen.sfs.eie.components.etymology.talk.rule.FsimAndSsimToEetyRule;
import de.tuebingen.sfs.eie.shared.core.EtymologicalTheory;
import de.tuebingen.sfs.eie.shared.util.LoadUtils;
import de.tuebingen.sfs.psl.engine.AtomTemplate;
import de.tuebingen.sfs.psl.engine.InferenceResult;
import de.tuebingen.sfs.psl.engine.ProblemManager;
import de.tuebingen.sfs.psl.engine.RuleAtomGraph;
import de.tuebingen.sfs.psl.io.InferenceResultIo;
import de.tuebingen.sfs.psl.talk.TalkingPredicate;
import de.tuebingen.sfs.psl.talk.TalkingRule;
import de.tuebingen.sfs.psl.util.data.RankingEntry;
import de.tuebingen.sfs.psl.util.log.InferenceLogger;

public class EtymologyInferenceTest {
	// For debugging with a GUI, use the EtymologyFactViewer in the etinen
	// repository.

	private static void run(Map<String, Double> ruleWeights, PrintStream out) {
		ProblemManager problemManager = ProblemManager.defaultProblemManager();
		EtymologyProblemConfigDEPRECATED config = new EtymologyProblemConfigDEPRECATED(null, null, null, null, null, -1, null, null, null,
				ruleWeights, null, null, new InferenceLogger());
		config.addRuleToIgnoreList(EloaPlusEloaRule.NAME);
		config.addRuleToIgnoreList(FsimAndSsimToEetyRule.NAME);
		config.addRuleToIgnoreList(DirectEetyToFsimRule.NAME);
		config.setNonPersistableFeatures("EtymologyProblem", problemManager.getDbManager());
		EtymologyProblem problem = new EtymologyProblem(config);
		CLDFWordlistDatabase wordListDb = LoadUtils.loadDatabase(EtymologyProblemConfigDEPRECATED.TEST_DB_DIR,
				new InferenceLogger());
		EtymologicalTheory theory = new EtymologicalTheory(wordListDb);
		EtymologyIdeaGeneratorDEPRECATED ideaGen = EtymologyIdeaGeneratorsForTesting.getIdeaGeneratorForTestingLanguage(theory,
				problem, false, false);
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

	private static EtymologyRagFilter runTestFictional(EtymologyProblemConfigDEPRECATED config, boolean synonyms,
			boolean moreLangsPerBranch, boolean moreBranches, boolean branchwiseBorrowing, boolean showAllEloa) {
		ProblemManager problemManager = ProblemManager.defaultProblemManager();
		String problemId = "TestDataEtymologyProblem";
		config.setNonPersistableFeatures("EtymologyProblem", problemManager.getDbManager());
		EtymologyProblem problem = new EtymologyProblem(config);
		EtymologyIdeaGeneratorsForTesting.getIdeaGeneratorWithFictionalData(problem, synonyms, moreLangsPerBranch,
				moreBranches, branchwiseBorrowing).generateAtoms();
		InferenceResult result = problemManager.registerAndRunProblem(problem);
		RuleAtomGraph rag = result.getRag();
		if (showAllEloa) {
			List<RankingEntry<AtomTemplate>> eloaResults = problemManager.getDbManager().getAtoms("Eloa",
					new AtomTemplate("Eloa", ANY_CONST, ANY_CONST));
			Collections.sort(eloaResults, Collections.reverseOrder());
			for (RankingEntry<AtomTemplate> eloaResult : eloaResults) {
				System.out.println(eloaResult);
			}
		}

		return (EtymologyRagFilter) rag.getRagFilter();
	}

	private static void runTestLanguage(EtymologyProblemConfigDEPRECATED config, boolean largeConceptSet,
			boolean largeLanguageSet) {
		ProblemManager problemManager = ProblemManager.defaultProblemManager();
		config.setNonPersistableFeatures("EtymologyProblem", problemManager.getDbManager());
		EtymologyProblem problem = new EtymologyProblem(config);
		CLDFWordlistDatabase wordListDb = LoadUtils.loadDatabase(EtymologyProblemConfigDEPRECATED.TEST_DB_DIR,
				new InferenceLogger());
		EtymologicalTheory theory = new EtymologicalTheory(wordListDb);
		EtymologyIdeaGeneratorsForTesting
				.getIdeaGeneratorForTestingLanguage(theory, problem, largeConceptSet, largeLanguageSet).generateAtoms();
		InferenceResult result = problemManager.registerAndRunProblem(problem);
		RuleAtomGraph rag = result.getRag();
		EtymologyResultChecker.checkTestAnalysis((EtymologyRagFilter) rag.getRagFilter());
	}

	private static void runTestHead(EtymologyProblemConfigDEPRECATED config, boolean largeLanguageSet) {
		ProblemManager problemManager = ProblemManager.defaultProblemManager();
		config.setNonPersistableFeatures("EtymologyProblem", problemManager.getDbManager());
		EtymologyProblem problem = new EtymologyProblem(config);
		CLDFWordlistDatabase wordListDb = LoadUtils.loadDatabase(EtymologyProblemConfigDEPRECATED.TEST_DB_DIR,
				new InferenceLogger());
		EtymologicalTheory theory = new EtymologicalTheory(wordListDb);
		EtymologyIdeaGeneratorsForTesting.getIdeaGeneratorForTestingHead(theory, problem, largeLanguageSet)
				.generateAtoms();
		InferenceResult result = problemManager.registerAndRunProblem(problem);
		RuleAtomGraph rag = result.getRag();
		EtymologyResultChecker.checkTestAnalysis((EtymologyRagFilter) rag.getRagFilter());
	}

	private static void runTestMountain(EtymologyProblemConfigDEPRECATED config, boolean largeLanguageSet) {
		ProblemManager problemManager = ProblemManager.defaultProblemManager();
		config.setNonPersistableFeatures("EtymologyProblem", problemManager.getDbManager());
		EtymologyProblem problem = new EtymologyProblem(config);
		CLDFWordlistDatabase wordListDb = LoadUtils.loadDatabase(EtymologyProblemConfigDEPRECATED.TEST_DB_DIR,
				new InferenceLogger());
		EtymologicalTheory theory = new EtymologicalTheory(wordListDb);
		EtymologyIdeaGeneratorsForTesting.getIdeaGeneratorForTestingMountain(theory, problem, largeLanguageSet)
				.generateAtoms();
		InferenceResult result = problemManager.registerAndRunProblem(problem);
		RuleAtomGraph rag = result.getRag();
		EtymologyResultChecker.checkTestAnalysis((EtymologyRagFilter) rag.getRagFilter());
	}

	private static void serialize(ObjectMapper mapper, EtymologyProblemConfigDEPRECATED config) {
		config.export(mapper, "etinen-etymology/src/test/resources/serialization/config.json");
		ProblemManager problemManager = ProblemManager.defaultProblemManager();
		config.setNonPersistableFeatures("EtymologyProblem", problemManager.getDbManager());
		EtymologyProblem problem = new EtymologyProblem(config);
		EtymologyIdeaGeneratorDEPRECATED ideaGen = EtymologyIdeaGeneratorsForTesting.getIdeaGeneratorWithFictionalData(problem,
				false, false, false, true);
		ideaGen.generateAtoms();
		ideaGen.export(mapper, "etinen-etymology/src/test/resources/serialization/ideas.json");
		InferenceResult result = problemManager.registerAndRunProblem(problem);
		RuleAtomGraph rag = result.getRag();
		InferenceResultIo.saveToFile(result, problem, mapper);
		EtymologyResultChecker.checkTestAnalysis((EtymologyRagFilter) rag.getRagFilter());
	}

	private static void deserialize(ObjectMapper mapper) {
		EtymologyProblemConfigDEPRECATED config = EtymologyProblemConfigDEPRECATED.fromJson(mapper,
				"etinen-etymology/src/test/resources/serialization/config.json", new InferenceLogger());
		ProblemManager problemManager = ProblemManager.defaultProblemManager();
		config.setNonPersistableFeatures("EtymologyProblem", problemManager.getDbManager());
		EtymologyProblem problem = new EtymologyProblem(config);
		CLDFWordlistDatabase wordListDb = LoadUtils.loadDatabase(EtymologyProblemConfigDEPRECATED.TEST_DB_DIR,
				new InferenceLogger());
		EtymologicalTheory theory = new EtymologicalTheory(wordListDb);
		EtymologyIdeaGeneratorDEPRECATED ideaGen = EtymologyIdeaGeneratorDEPRECATED.fromJson(problem, theory, mapper,
				"etinen-etymology/src/test/resources/serialization/ideas.json", new InferenceLogger());
		ideaGen.generateAtoms();
		InferenceResult result = problemManager.registerAndRunProblem(problem);
		System.out.println("(New) RAG from imported config and idea generator:");
		RuleAtomGraph rag = result.getRag();
		InferenceResultIo.saveToFile(result, problem, mapper);
		EtymologyResultChecker.checkTestAnalysis((EtymologyRagFilter) rag.getRagFilter());
		System.out.println("Deserialized version of the same RAG:");
		Map<String, TalkingPredicate> talkingPreds = new TreeMap<>();
		Map<String, TalkingRule> talkingRules = new TreeMap<>();
		result = InferenceResultIo.fromFile(mapper, talkingPreds, talkingRules);
		rag = result.getRag();
		System.out.println(rag.getAtomNodes());
		System.out.println(result.getInferenceValues());
		EtymologyResultChecker.checkTestAnalysis((EtymologyRagFilter) rag.getRagFilter());
		System.out.println("Talking predicates: " + talkingPreds);
		System.out.println("Talking rules: " + talkingRules);
	}

	public static void main(String[] args) {
		// gridSearch();

		// String configPath = "";
		// String configPath =
		// "etinen-etymology/src/test/resources/serialization/config-languagelvl.json";
		String configPath = "etinen-etymology/src/test/resources/serialization/config-branchlvl.json";
		boolean branchwiseBorrowing = true;

		boolean printAllEloaValues = false;

		// Which tests should be run?
		boolean singleTest = true;
		boolean fictionalData = true;
		boolean language = false;
		boolean mountain = false;
		boolean head = false;

		ObjectMapper mapper = new ObjectMapper();
		EtymologyProblemConfigDEPRECATED config;

		if (!configPath.isEmpty()) {
			config = EtymologyProblemConfigDEPRECATED.fromJson(mapper, configPath, new InferenceLogger());
		} else {
			config = new EtymologyProblemConfigDEPRECATED();
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
			EtymologyRagFilter erf1 = runTestFictional(config, false, false, false, branchwiseBorrowing,
					printAllEloaValues);
			if (singleTest) {
				EtymologyResultChecker.checkTestAnalysis(erf1);
				return;
			}
			System.out.println("\nTEST 2 --- synonyms");
			EtymologyRagFilter erf2 = runTestFictional(config, true, false, false, branchwiseBorrowing,
					printAllEloaValues);
			System.out.println("\nTEST 3 --- more languages per branch");
			EtymologyRagFilter erf3 = runTestFictional(config, false, true, false, branchwiseBorrowing,
					printAllEloaValues);
			System.out.println("\nTEST 4 --- additional branch");
			EtymologyRagFilter erf4 = runTestFictional(config, false, false, true, branchwiseBorrowing,
					printAllEloaValues);
			System.out.println("\nTEST 5 --- synonyms, more languages, extra branch");
			EtymologyRagFilter erf5 = runTestFictional(config, true, true, true, branchwiseBorrowing,
					printAllEloaValues);

			// Print the results only now to skip the various status updates in
			// the console:
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
