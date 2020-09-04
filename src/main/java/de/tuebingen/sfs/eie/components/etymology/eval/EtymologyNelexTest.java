package de.tuebingen.sfs.eie.components.etymology.eval;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import de.tuebingen.sfs.eie.components.etymology.ideas.EtymologyIdeaGenerator;
import de.tuebingen.sfs.eie.components.etymology.problems.EtymologyProblem;
import de.tuebingen.sfs.psl.engine.InferenceResult;
import de.tuebingen.sfs.psl.engine.ProblemManager;

public class EtymologyNelexTest {

	private Map<String, Map<String, Etymology>> conceptToLanguageToEtymology;

	private static String goldStandardFile = "src/test/resources/etymology/northeuralex-0.9-loanword-annotation-20200716.tsv";

	private void setUp() {
		conceptToLanguageToEtymology = new TreeMap<>();
		Set<String> langs = new HashSet<>();

		try (FileInputStream fis = new FileInputStream(goldStandardFile);
				InputStreamReader isr = new InputStreamReader(fis);
				BufferedReader br = new BufferedReader(isr)) {
			String line = "";
			loop: while ((line = br.readLine()) != null) {
				String[] cells = line.split("\t");
				String statusStr = cells[5].trim();
				LoanwordStatus status;
				switch (statusStr) {
				case "no, source provides full trace to proto-language":
				case "no, source provides partial trace to presumably inherited word":
					status = LoanwordStatus.INHERITED;
					break;
				case "yes, within NEL with donor candidates":
					status = LoanwordStatus.LOANED;
					break;
				case "yes, but source gives no same-concept reflexes in NEL for donor":
					status = LoanwordStatus.OUTSIDE_DB;
					break;
				default:
					System.err.println("Did not recognize the loanword status in the following line: " + line);
					System.err.println("(Skipping this entry.)");
					continue loop;
				}

				String concept = cells[1].trim();
				String language = cells[2].trim(); // TODO convert to ID
				langs.add(language);
				String source = cells[6].trim();
				if (source.contains("{") && source.contains(",")) {
					System.err.println("Entry contains several sources: " + line);
					System.err.println("(Skipping this entry.)");
					// TODO explore/handle
				}

				Map<String, Etymology> languageToEtymology = conceptToLanguageToEtymology.getOrDefault(concept,
						new TreeMap<>());
				languageToEtymology.put(language, new Etymology(source, status));
				conceptToLanguageToEtymology.put(concept, languageToEtymology);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

//		System.out.println(langs);
//
//		for (Entry<String, Etymology> entry : conceptToLanguageToEtymology.get("Berg::N").entrySet()) {
//			System.out.println(entry.getKey() + " - " + entry.getValue());
//		}
	}

	private void run(Set<String> concepts) {
		ProblemManager problemManager = ProblemManager.defaultProblemManager();
		EtymologyProblem problem = new EtymologyProblem(problemManager.getDbManager(), "EtymologyProblem");
		EtymologyIdeaGenerator eig = EtymologyIdeaGenerator.initializeDefault(problem, null);
		eig.setConcepts(concepts);
		Set<String> languages = new HashSet<>();
		for (String concept : concepts) {
			for (String language : conceptToLanguageToEtymology.get(concept).keySet()) {
				languages.add(language);
			}
		}
		eig.setLanguages(languages.stream().collect(Collectors.toList()));
		eig.generateAtoms();

		InferenceResult result = problemManager.registerAndRunProblem(problem);
		for (Entry<String, Double> entry : result.getInferenceValues().entrySet()) {
			System.out.println(entry);
		}
	}

	public static void main(String[] args) {
		EtymologyNelexTest test = new EtymologyNelexTest();
		test.setUp();
		Set<String> concepts = new HashSet<>();
		concepts.add("Berg::N");
		test.run(concepts);
	}

	private enum LoanwordStatus {
		INHERITED, LOANED, OUTSIDE_DB
	}

	private class Etymology {
		String source;
		String donorLanguage;
		String donorWord;
		LoanwordStatus status;

		public Etymology(String source, LoanwordStatus status) {
			this.source = source;
			this.status = status;

			if (status == LoanwordStatus.LOANED) {
				String[] sourceEntry = source.replaceAll("[{}'\\s+]", "").split(":");
				donorLanguage = sourceEntry[0]; // TODO convert to ID
				donorWord = sourceEntry[1];
			}
		}

		public String toString() {
			if (status == LoanwordStatus.LOANED) {
				return status + " : " + donorLanguage + " : " + donorWord;
			}
			return status + " : " + source;
		}

	}

}
