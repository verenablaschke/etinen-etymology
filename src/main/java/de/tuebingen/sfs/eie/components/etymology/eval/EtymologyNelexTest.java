package de.tuebingen.sfs.eie.components.etymology.eval;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import de.tuebingen.sfs.eie.components.etymology.filter.EtymologyRagFilter;
import de.tuebingen.sfs.eie.components.etymology.ideas.EtymologyIdeaGenerator;
import de.tuebingen.sfs.eie.components.etymology.problems.EtymologyProblem;
import de.tuebingen.sfs.eie.core.IndexedObjectStore;
import de.tuebingen.sfs.eie.gui.facts.StandaloneFactViewer;
import de.tuebingen.sfs.eie.talk.EtinenConstantRenderer;
import de.tuebingen.sfs.psl.engine.InferenceResult;
import de.tuebingen.sfs.psl.engine.ProblemManager;
import de.tuebingen.sfs.psl.util.data.Multimap;
import de.tuebingen.sfs.psl.util.data.Multimap.CollectionType;
import de.tuebingen.sfs.psl.util.data.RankingEntry;
import de.tuebingen.sfs.psl.util.log.InferenceLogger;
import de.tuebingen.sfs.util.LoadUtils;

public class EtymologyNelexTest {

	private Map<String, Multimap<String, Etymology>> conceptToLanguageToEtymology;
	private IndexedObjectStore ios;
	private EtinenConstantRenderer renderer;
	private Map<String, String> isoToLanguageId;
	private Map<String, String> conceptConverter;
	private Multimap<String, String> borrowedConceptToLanguages;
	private Multimap<String, String> unknownConceptToLanguages;

	private EtymologyProblem problem;

	private boolean branchwise = true;

	private static String goldStandardFile = "src/test/resources/etymology/northeuralex-0.9-loanword-annotation-20200716.tsv";
	private static String parameterFile = "src/test/resources/northeuralex-0.9/parameters.csv";

	public EtymologyNelexTest() {
		ios = new IndexedObjectStore(
				LoadUtils.loadDatabase("src/test/resources/northeuralex-0.9", new InferenceLogger()), null);
		renderer = EtinenConstantRenderer.newRenderer(ios, "", null);
		isoToLanguageId = ios.getIsoToLanguageIdMap();
		setUp();
	}

	private void setUp() {
		conceptConverter = new TreeMap<>();
		try (FileInputStream fis = new FileInputStream(parameterFile);
				InputStreamReader isr = new InputStreamReader(fis);
				BufferedReader br = new BufferedReader(isr)) {
			String line = "";
			while ((line = br.readLine()) != null) {
				String cells[] = line.split(",");
				try {
					conceptConverter.put(cells[1], cells[0]);
				} catch (IndexOutOfBoundsException e) {
					System.out.println("Skipping line: " + line);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		conceptToLanguageToEtymology = new TreeMap<>();
		borrowedConceptToLanguages = new Multimap<>(CollectionType.SET);
		unknownConceptToLanguages = new Multimap<>(CollectionType.SET);
		Set<String> langs = new HashSet<>();

		try (FileInputStream fis = new FileInputStream(goldStandardFile);
				InputStreamReader isr = new InputStreamReader(fis);
				BufferedReader br = new BufferedReader(isr)) {
			String line = "";
			loop: while ((line = br.readLine()) != null) {
				String[] cells = line.split("\t");
				String concept = conceptConverter.get(cells[1].trim());
				String language = cells[2].trim();
				language = isoToLanguageId.getOrDefault(language, language);

				String statusStr = cells[5].trim();
				LoanwordStatus status;
				switch (statusStr) {
				case "no, source provides full trace to proto-language":
				case "no, source provides partial trace to presumably inherited word":
					status = LoanwordStatus.INHERITED;
					break;
				case "yes, within NEL with donor candidates":
					status = LoanwordStatus.LOANED;
					borrowedConceptToLanguages.put(concept, language);
					break;
				case "yes, but source gives no same-concept reflexes in NEL for donor":
					status = LoanwordStatus.LOANED_ACROSS_CONCEPT_OR_OUTSIDE_NELEX;
					unknownConceptToLanguages.put(concept, language);
					break;
				default:
					System.err.println("Did not recognize the loanword status in the following line: " + line);
					System.err.println("(Skipping this entry.)");
					continue loop;
				}

				String word = cells[3].trim();
				String source = cells[6].trim();
				if (source.contains("{") && source.contains(",")) {
					System.err.println("Entry contains several sources: " + line);
					System.err.println("(Skipping this entry.)");
				}
				Multimap<String, Etymology> languageToEtymology = conceptToLanguageToEtymology.getOrDefault(concept,
						new Multimap<>(CollectionType.SET));
				languageToEtymology.put(language, new Etymology(word, language, source, status));
				conceptToLanguageToEtymology.put(concept, languageToEtymology);
				langs.add(language);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private InferenceResult run(String concept, PrintStream verboseOut, PrintStream out, boolean compare,
			boolean compareInherited) {
		ProblemManager problemManager = ProblemManager.defaultProblemManager();
		problem = new EtymologyProblem(problemManager.getDbManager(), "EtymologyNelexProblem");
		EtymologyIdeaGenerator eig = EtymologyIdeaGenerator.initializeDefault(problem, ios);
		eig.setConcepts(Collections.singleton(concept));
		Set<String> languages = new HashSet<>();
		for (String language : conceptToLanguageToEtymology.get(concept).keySet()) {
			languages.add(language);
		}
		eig.setLanguages(languages.stream().collect(Collectors.toList()));
		eig.addSiblingLanguages();
		Set<String> removed = eig.removeIsolates();

		Set<String> interestingCases = new HashSet<>();
		interestingCases.addAll(borrowedConceptToLanguages.getOrDefault(concept, new HashSet<>()));
		interestingCases.addAll(unknownConceptToLanguages.getOrDefault(concept, new HashSet<>()));
		interestingCases.removeAll(removed);
		if (interestingCases.isEmpty()) {
			String msg = "Removed all gold-standard Eloa/Eunk cases for " + concept + ". Skipping inference.";
			System.out.println(msg);
			verboseOut.println(msg);
			return null;
		}

		eig.generateAtoms();
		InferenceResult result = problemManager.registerAndRunProblem(problem);
		EtymologyRagFilter erf = (EtymologyRagFilter) result.getRag().getRagFilter();

		if (!compare) {
			return result;
		}

		Map<String, Etymology> inheritedGs = new HashMap<>();
		Map<String, Etymology> borrowedNelexGs = new HashMap<>();
		Map<String, Etymology> borrowedOutsideGs = new HashMap<>();
		for (Entry<String, Collection<Etymology>> entry : conceptToLanguageToEtymology.get(concept).entrySet()) {
			for (Etymology etym : entry.getValue()) {
				if (removed.contains(etym.language)) {
					continue;
				}
				switch (etym.status) {
				case INHERITED:
					inheritedGs.put(entry.getKey(), etym);
					break;
				case LOANED:
					borrowedNelexGs.put(entry.getKey(), etym);
					break;
				case LOANED_ACROSS_CONCEPT_OR_OUTSIDE_NELEX:
					borrowedOutsideGs.put(entry.getKey(), etym);
					break;
				default:
					break;
				}
			}
		}
		verboseOut.println("CONCEPT: " + concept + "\n========\n");
		verboseOut.println("BORROWED FROM SAME CONCEPT\n");
		compare(concept, borrowedNelexGs, eig, erf, "Eloa", verboseOut, out);
		verboseOut.println("BORROWED ACROSS CONCEPTS OR OUTSIDE NELEX\n");
		compare(concept, borrowedOutsideGs, eig, erf, "Eunk", verboseOut, out);
		if (compareInherited) {
			verboseOut.println("INHERITED\n");
			compare(concept, inheritedGs, eig, erf, "Einh", verboseOut, out);
		}
		verboseOut.println("==================================================");
		return result;
	}

	private void compare(String concept, Map<String, Etymology> goldStandard, EtymologyIdeaGenerator eig,
			EtymologyRagFilter erf, String expected, PrintStream verboseOut, PrintStream out) {
		if (goldStandard.isEmpty()) {
			out.println(String.format("%s\t%s\t---", concept, expected));
			verboseOut.println(String.format("No entries in the gold standard.\n"));
			verboseOut.println("==================================================\n");
			return;
		}
		int predMatches = 0;
		int atomMatches = 0;
		int count = 0;
		for (Entry<String, Etymology> entry : goldStandard.entrySet()) {
			verboseOut.println("Actual: " + entry.getValue());
			count++;
			for (Integer formId : ios.getFormsForLangAndConcepts(entry.getKey(), concept)) {
				String ortho = ios.getOrthoForForm(formId);
				if (ortho != null && !ortho.isEmpty() && !ortho.equals(entry.getValue().word)) {
					continue;
				}
				String form = ios.getFormForFormId(formId);
				List<RankingEntry<String>> atoms = erf.getEetyForArgument(formId + "");
				if (atoms.get(0).key.startsWith(expected + "(")) {
					predMatches++;
					if (expected.equals("Eloa")) {
						int otherFormId = Integer.parseInt(
								atoms.get(0).key.substring(5, atoms.get(0).key.length() - 1).split(",")[1].trim());
						String donorLanguage = entry.getValue().donorLanguage;
						if (branchwise) {
							// Get ancestor of donor.
							donorLanguage = eig.getTree().getTree().parents.get(donorLanguage);
							// TODO how to properly check for the correct form
							// when branchwise=true and the donor's ancestor has
							// multiple forms for the concept?? (vbl)
							if (ios.getLangForForm(otherFormId).equals(donorLanguage)) {
								atomMatches++;
							}
						} else {
							// This assumes that the donor has the same concept
							for (Integer donorFormId : ios.getFormsForLangAndConcepts(donorLanguage, concept)) {
								if (ios.getOrthoForForm(donorFormId).equals(entry.getValue().donorWord)) {
									if (donorFormId == otherFormId) {
										atomMatches++;
									}
									break;
								}
							}
						}
					}
				}
				for (RankingEntry<String> atom : atoms) {
					if (atom.value < 0.05) {
						break;
					}
					String pred = atom.key.substring(0, 4);
					if (pred.equals("Eunk")) {
						verboseOut.println(" - " + pred + " " + form + " < ???");
						continue;
					}
					int otherFormId = Integer
							.parseInt(atom.key.substring(5, atom.key.length() - 1).split(",")[1].trim());
					verboseOut.println(" - " + pred + " " + form + " < " + ios.getFormForFormId(otherFormId) + " ("
							+ ios.getLangForForm(otherFormId) + ") " + atom.value);
				}
			}
		}
		verboseOut.println(String.format("\nPredicted the etymology type of %d/%d entries (%.2f%%) correctly",
				predMatches, count, 100 * ((double) predMatches) / count));
		out.print(String.format("%s\t%s\t%d/%d (%.2f%%)", concept, expected, predMatches, count,
				100 * ((double) predMatches) / count));
		if (expected.equals("Eloa"))
			verboseOut.println(
					String.format("Predicted the etymology type and source of %d/%d entries (%.2f%%) correctly",
							atomMatches, count, 100 * ((double) atomMatches) / count));
		if (expected.equals("Eloa"))
			out.print(String.format("\t%d/%d (%.2f%%)", atomMatches, count, 100 * ((double) atomMatches) / count));
		out.println();
		verboseOut.println("\n==================================================\n");
	}

	public void runAll(int minLangs, PrintStream verboseOut, PrintStream out) {
		Set<String> borrowedConcepts = new HashSet<>();
		borrowedConcepts.addAll(borrowedConceptToLanguages.keySet());
		borrowedConcepts.addAll(unknownConceptToLanguages.keySet());
		for (String concept : conceptToLanguageToEtymology.keySet()) {
			if (!borrowedConcepts.contains(concept)) {
				continue;
			}
			if (conceptToLanguageToEtymology.get(concept).keySet().size() >= minLangs)
				run(concept, verboseOut, out, true, false);
		}
	}

	public void runAndShow(String concept) {
		InferenceResult result = run(concept, System.out, System.out, false, false);
		if (result != null) {
			StandaloneFactViewer.launchWithData(renderer, problem, result);
		}
	}

	private void checkInventory() {
		int atLeast20 = 0;
		int atLeast25 = 0;
		int atLeast30 = 0;
		Set<String> borrowedConcepts = new HashSet<>();
		borrowedConcepts.addAll(borrowedConceptToLanguages.keySet());
		borrowedConcepts.addAll(unknownConceptToLanguages.keySet());
		for (String concept : conceptToLanguageToEtymology.keySet()) {
			if (!borrowedConcepts.contains(concept)) {
				continue;
			}
			int size = conceptToLanguageToEtymology.get(concept).keySet().size();
			if (size >= 20) {
				atLeast20++;
				if (size >= 25) {
					atLeast25++;
					if (size >= 30) {
						atLeast30++;
					}
				}
			}
			// System.out.println(concept + " : " + size);
		}
		System.err.println("At least 20 languages: " + atLeast20);
		System.err.println("At least 25 languages: " + atLeast25);
		System.err.println("At least 30 languages: " + atLeast30);
	}

	public static void main(String[] args) {
		EtymologyNelexTest test = new EtymologyNelexTest();

		// test.checkInventory();

//		try {
//			PrintStream out = new PrintStream("src/test/resources/etymology/nelex-output.tsv");
//			PrintStream verboseOut = new PrintStream("src/test/resources/etymology/nelex-output.log");
//			test.runAll(20, verboseOut, out);
//		} catch (FileNotFoundException e) {
//			e.printStackTrace();
//		}

		 test.runAndShow("MeerN");

		// test.run("HonigN", System.out, System.out, true);
		
//		ProblemManager problemManager = ProblemManager.defaultProblemManager();
//		EtymologyProblem problem = new EtymologyProblem(problemManager.getDbManager(), "EtymologyNelexProblem");
//		EtymologyIdeaGenerator eig = EtymologyIdeaGenerator.initializeDefault(problem, test.ios);
//		eig.setConcepts(Collections.singleton("MeerN"));
//		Set<String> languages = new HashSet<>();
//		languages.add("french");
//		languages.add("italian");
//		eig.setLanguages(languages.stream().collect(Collectors.toList()));
//		eig.generateAtoms();
//		InferenceResult result = problemManager.registerAndRunProblem(problem);
//		EtymologyRagFilter erf = (EtymologyRagFilter) result.getRag().getRagFilter();
//		if (result != null) {
//			StandaloneFactViewer.launchWithData(test.renderer, problem, result);
//		}
	}

	private enum LoanwordStatus {
		INHERITED, LOANED, LOANED_ACROSS_CONCEPT_OR_OUTSIDE_NELEX
		// TODO!! cover the last one
	}

	private class Etymology {
		String language;
		String word;
		String source;
		String donorLanguage;
		String donorWord;

		LoanwordStatus status;

		public Etymology(String word, String language, String source, LoanwordStatus status) {
			this.word = word;
			this.source = source;
			this.status = status;
			this.language = language;

			if (status == LoanwordStatus.LOANED) {
				String[] sourceEntry = source.replaceAll("[{}'\\s+]", "").split(":");
				donorLanguage = isoToLanguageId.getOrDefault(sourceEntry[0], sourceEntry[0]);
				donorWord = sourceEntry[1];
			}
		}

		public String toString() {
			if (status == LoanwordStatus.LOANED) {
				return language + " " + word + " << " + donorLanguage + " " + donorWord;
			}
			return language + " " + word + " < " + source;
		}
	}

}
