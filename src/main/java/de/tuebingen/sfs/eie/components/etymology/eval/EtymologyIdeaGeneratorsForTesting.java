package de.tuebingen.sfs.eie.components.etymology.eval;

import java.util.ArrayList;
import java.util.List;

import de.jdellert.iwsa.corrmodel.CorrespondenceModel;
import de.jdellert.iwsa.tokenize.IPATokenizer;
import de.tuebingen.sfs.cldfjava.data.CLDFWordlistDatabase;
import de.tuebingen.sfs.eie.components.etymology.ideas.EtymologyIdeaGeneratorDEPRECATED;
import de.tuebingen.sfs.eie.components.etymology.problems.EtymologyProblem;
import de.tuebingen.sfs.eie.components.etymology.problems.EtymologyProblemConfigDEPRECATED;
import de.tuebingen.sfs.eie.shared.core.EtymologicalTheory;
import de.tuebingen.sfs.eie.shared.util.LoadUtils;
import de.tuebingen.sfs.eie.shared.util.PhoneticSimilarityHelper;
import de.tuebingen.sfs.psl.util.log.InferenceLogger;

public class EtymologyIdeaGeneratorsForTesting extends EtymologyIdeaGeneratorDEPRECATED {
	
	public EtymologyIdeaGeneratorsForTesting(EtymologyProblem problem, EtymologicalTheory theory,
			PhoneticSimilarityHelper phonSimHelper, CLDFWordlistDatabase wordListDb) {
		super(problem, theory, phonSimHelper, wordListDb);
	}

	public static EtymologyIdeaGeneratorDEPRECATED getIdeaGeneratorForTestingMountain(EtymologicalTheory theory,
			EtymologyProblem problem, boolean largeLanguageSet) {
		return getIdeaGeneratorForTestingMountain(theory, problem, largeLanguageSet, false);
	}

	public static EtymologyIdeaGeneratorDEPRECATED getIdeaGeneratorForTestingMountain(EtymologicalTheory theory,
			EtymologyProblem problem, boolean largeLanguageSet, boolean branchwiseBorrowing) {
		List<String> concepts = new ArrayList<>();
		concepts.add("BergN");
		return getIdeaGeneratorForTesting(theory, problem, concepts, largeLanguageSet, branchwiseBorrowing);
	}

	public static EtymologyIdeaGeneratorDEPRECATED getIdeaGeneratorForTestingHead(EtymologicalTheory theory,
			EtymologyProblem problem, boolean largeLanguageSet) {
		return getIdeaGeneratorForTestingHead(theory, problem, largeLanguageSet, false);
	}

	public static EtymologyIdeaGeneratorDEPRECATED getIdeaGeneratorForTestingHead(EtymologicalTheory theory,
			EtymologyProblem problem, boolean largeLanguageSet, boolean branchwiseBorrowing) {
		List<String> concepts = new ArrayList<>();
		concepts.add("KopfN");
		return getIdeaGeneratorForTesting(theory, problem, concepts, largeLanguageSet, branchwiseBorrowing);
	}

	public static EtymologyIdeaGeneratorDEPRECATED getIdeaGeneratorForTestingLanguage(EtymologicalTheory theory,
			EtymologyProblem problem, boolean largeConceptSet, boolean largeLanguageSet) {
		return getIdeaGeneratorForTestingLanguage(theory, problem, largeConceptSet, largeLanguageSet, false);
	}

	public static EtymologyIdeaGeneratorDEPRECATED getIdeaGeneratorForTestingLanguage(EtymologicalTheory theory,
			EtymologyProblem problem, boolean largeConceptSet, boolean largeLanguageSet, boolean branchwiseBorrowing) {
		List<String> concepts = new ArrayList<>();
		concepts.add("SpracheN");
		if (largeConceptSet) {
			concepts.add("ZungeN");
		}
		return getIdeaGeneratorForTesting(theory, problem, concepts, largeLanguageSet, branchwiseBorrowing);
	}

	private static EtymologyIdeaGeneratorDEPRECATED getIdeaGeneratorForTesting(EtymologicalTheory theory,
			EtymologyProblem problem, List<String> concepts, boolean largeLanguageSet, boolean branchwiseBorrowing) {
		List<String> languages = new ArrayList<>();
		languages.add("eng");
		languages.add("deu");
		languages.add("swe");
		languages.add("nor");
		languages.add("dan");
		languages.add("fra");
		languages.add("spa");
		languages.add("ita");
		languages.add("cat");
		if (largeLanguageSet) {
			languages.add("isl");
			languages.add("nld");

			languages.add("por");
			languages.add("cat");
			languages.add("ron");
			languages.add("lat");

			languages.add("lit");
			languages.add("lav");
			languages.add("rus");
			languages.add("bel");
			languages.add("ukr");
			languages.add("pol");
			languages.add("ces");
			languages.add("slv");
			languages.add("slk");
			languages.add("hrv");
		}

		InferenceLogger logger = problem.getConfig().getLogger();
		CLDFWordlistDatabase wordListDb = LoadUtils.loadDatabase(EtymologyProblemConfigDEPRECATED.DB_DIR, logger);
		IPATokenizer tokenizer = new IPATokenizer();
		PhoneticSimilarityHelper phonSimHelper = new PhoneticSimilarityHelper(
				LoadUtils.loadCorrModel(EtymologyProblemConfigDEPRECATED.DB_DIR, false, tokenizer, logger), theory);
		((EtymologyProblemConfigDEPRECATED) problem.getConfig()).setTreeDepth(4);

		return new EtymologyIdeaGeneratorDEPRECATED(problem, theory, phonSimHelper, wordListDb);
	}

	public static EtymologyIdeaGeneratorDEPRECATED getIdeaGeneratorWithFictionalData(EtymologyProblem problem, boolean synonyms,
			boolean moreLangsPerBranch, boolean moreBranches, boolean branchwiseBorrowing) {
		IPATokenizer tokenizer = new IPATokenizer();

		List<String> languages = new ArrayList<>();
		languages.add("a1");
		languages.add("a2");
		languages.add("a3");
		languages.add("b1");
		languages.add("b2");
		languages.add("b3");
		languages.add("c1");
		languages.add("c2");
		languages.add("c3");

		// Languages with several entries for one concept
		if (synonyms) {
			languages.add("a4");
		}

		if (moreLangsPerBranch) {
			languages.add("a5");
			languages.add("a6");
			languages.add("b4");
		}

		if (moreBranches) {
			languages.add("d1");
			languages.add("d2");
			languages.add("d3");
			languages.add("d4");
		}

		List<String> concepts = new ArrayList<>();
		concepts.add("SpracheN");

		InferenceLogger logger = problem.getConfig().getLogger();
		CLDFWordlistDatabase wordListDb = LoadUtils.loadDatabase(EtymologyProblemConfigDEPRECATED.TEST_DB_DIR, logger);
		CorrespondenceModel corres = LoadUtils.loadCorrModel(EtymologyProblemConfigDEPRECATED.DB_DIR, false, tokenizer, logger);
		EtymologicalTheory theory = new EtymologicalTheory(wordListDb);
		PhoneticSimilarityHelper phonSimHelper = new PhoneticSimilarityHelper(corres, theory);
		((EtymologyProblemConfigDEPRECATED) problem.getConfig()).setTreeDepth(2);

		return new EtymologyIdeaGeneratorDEPRECATED(problem, theory, phonSimHelper, wordListDb);
	}

}
