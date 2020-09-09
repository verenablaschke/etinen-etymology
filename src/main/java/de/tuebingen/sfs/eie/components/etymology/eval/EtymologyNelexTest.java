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
	private Map<String, String> isoToLanguageId;
	private Map<String, String> conceptConverter;

	private boolean branchwise = true;

	private static String goldStandardFile = "src/test/resources/etymology/northeuralex-0.9-loanword-annotation-20200716.tsv";
	private static String parameterFile = "src/test/resources/northeuralex-0.9/parameters.csv";

	public EtymologyNelexTest() {
		ios = new IndexedObjectStore(
				LoadUtils.loadDatabase("src/test/resources/northeuralex-0.9", new InferenceLogger()), null);
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
					status = LoanwordStatus.LOANED_ACROSS_CONCEPT;
					break;
				default:
					System.err.println("Did not recognize the loanword status in the following line: " + line);
					System.err.println("(Skipping this entry.)");
					continue loop;
				}

				String concept = conceptConverter.get(cells[1].trim());
				String language = cells[2].trim();
				language = isoToLanguageId.getOrDefault(language, language);
				langs.add(language);
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
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void run(Set<String> concepts, PrintStream verboseOut, PrintStream out) {
		ProblemManager problemManager = ProblemManager.defaultProblemManager();
		EtymologyProblem problem = new EtymologyProblem(problemManager.getDbManager(), "EtymologyNelexProblem");
		EtymologyIdeaGenerator eig = EtymologyIdeaGenerator.initializeDefault(problem, ios);
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
		EtymologyRagFilter erf = (EtymologyRagFilter) result.getRag().getRagFilter();

		for (String concept : concepts) {
			Map<String, Etymology> inheritedGs = new HashMap<>();
			Map<String, Etymology> borrowedNelexGs = new HashMap<>();
			Map<String, Etymology> borrowedOutsideGs = new HashMap<>();
			for (Entry<String, Collection<Etymology>> entry : conceptToLanguageToEtymology.get(concept).entrySet()) {
				for (Etymology etym : entry.getValue()) {
					switch (etym.status) {
					case INHERITED:
						inheritedGs.put(entry.getKey(), etym);
						break;
					case LOANED:
						borrowedNelexGs.put(entry.getKey(), etym);
						break;
					case LOANED_ACROSS_CONCEPT:
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
			verboseOut.println("BORROWED ACROSS CONCEPTS\n");
			compare(concept, borrowedOutsideGs, eig, erf, "Eunk", verboseOut, out);
			verboseOut.println("INHERITED\n");
			compare(concept, inheritedGs, eig, erf, "Einh", verboseOut, out);
			verboseOut.println("==================================================");
		}
	}

	private void compare(String concept, Map<String, Etymology> goldStandard, EtymologyIdeaGenerator eig,
			EtymologyRagFilter erf, String expected, PrintStream verboseOut, PrintStream out) {
		int predMatches = 0;
		int atomMatches = 0;
		int count = 0;
		for (Entry<String, Etymology> entry : goldStandard.entrySet()) {
			verboseOut.println("Actual: " + entry.getValue());
			count++;
			verboseOut.println(entry.getKey() + " " + concept);
			verboseOut.println(ios.getFormsForLangAndConcepts(entry.getKey(), concept));
			for (Integer formId : ios.getFormsForLangAndConcepts(entry.getKey(), concept)) {
				String ortho = ios.getOrthoForForm(formId);
				verboseOut.println(ortho + " - " + entry.getValue().word);
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
				verboseOut.println(" - Best prediction: " + atoms.get(0));
				for (RankingEntry<String> atom : atoms.subList(1, atoms.size())) {
					if (atom.value < 0.1) {
						break;
					}
					String pred = atom.key.substring(0, 4);
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
		verboseOut.println("\n==================================================\n");
		if (expected.equals("Eloa"))
			out.print(String.format("\t%d/%d (%.2f%%)", atomMatches, count, 100 * ((double) atomMatches) / count));
		out.println();
	}

	public void runAll(int minLangs, PrintStream verboseOut, PrintStream out) {
		for (String concept : conceptToLanguageToEtymology.keySet()) {
			if (conceptToLanguageToEtymology.get(concept).keySet().size() >= minLangs)
				run(Collections.singleton(concept), verboseOut, out);
		}
	}

	private void checkInventory() {
		int atLeast20 = 0;
		int atLeast25 = 0;
		int atLeast30 = 0;
		for (String concept : conceptToLanguageToEtymology.keySet()) {
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
		try {
			PrintStream out = new PrintStream("src/test/resources/etymology/nelex-output.tsv");
			PrintStream verboseOut = new PrintStream("src/test/resources/etymology/nelex-output.log");
//			test.runAll(30, verboseOut, out);
			// test.run(Collections.singleton("BergN"), verboseOut, out);
			 test.run(Collections.singleton("SpracheN"), verboseOut, out);
			// test.run(Collections.singleton("KopfN"), verboseOut, out);
			 test.run(Collections.singleton("AbendN"), verboseOut, out);
			 test.run(Collections.singleton("LiedN"), verboseOut, out);
			 test.run(Collections.singleton("BogenWaffeN"), verboseOut, out);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	private enum LoanwordStatus {
		INHERITED, LOANED, LOANED_ACROSS_CONCEPT
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
