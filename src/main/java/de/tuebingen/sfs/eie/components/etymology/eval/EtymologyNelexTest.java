package de.tuebingen.sfs.eie.components.etymology.eval;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
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
import de.tuebingen.sfs.psl.engine.AtomTemplate;
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
	private EtymologyProblem problem;
	private EtymologyIdeaGenerator eig;
	private ProblemManager problemManager;

	private boolean branchwise = true;

	private static String goldStandardFile = "src/test/resources/etymology/northeuralex-0.9-loanword-annotation-20200716.tsv";

	public EtymologyNelexTest() {
		problemManager = ProblemManager.defaultProblemManager();
		problem = new EtymologyProblem(problemManager.getDbManager(), "EtymologyNelexProblem");
		ios = new IndexedObjectStore(
				LoadUtils.loadDatabase("src/test/resources/northeuralex-0.9", new InferenceLogger()), null);
		isoToLanguageId = ios.getIsoToLanguageIdMap();
		eig = EtymologyIdeaGenerator.initializeDefault(problem, ios);
		setUp();
	}

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
					status = LoanwordStatus.LOANED_ACROSS_CONCEPT;
					break;
				default:
					System.err.println("Did not recognize the loanword status in the following line: " + line);
					System.err.println("(Skipping this entry.)");
					continue loop;
				}
				// TODO check: other conversion necessary? (vbl)
				String concept = cells[1].trim().replaceAll(":", "");
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

		// System.out.println(langs);
		//
		// for (Entry<String, Etymology> entry :
		// conceptToLanguageToEtymology.get("Berg::N").entrySet()) {
		// System.out.println(entry.getKey() + " - " + entry.getValue());
		// }
	}

	private void run(Set<String> concepts) {
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

		for (String pred : new String[] { "Eloa" }) {
			System.out.println("\n\n" + pred + "\n===========\n");
			List<RankingEntry<AtomTemplate>> atoms = problemManager.getDbManager().getAtomsAboveThreshold(pred, 0.1,
					new AtomTemplate(pred, "?", "?"));
			Collections.sort(atoms, Collections.reverseOrder());
			for (RankingEntry<AtomTemplate> atom : atoms) {
				System.out.println(atom);
			}
		}

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
			System.out.println("CONCEPT: " + concept + "========\n");
			System.out.println("\nBORROWED FROM SAME CONCEPT\n");
			compare(concept, borrowedNelexGs, erf, "Eloa");
			System.out.println("\nBORROWED ACROSS CONCEPTS\n");
			compare(concept, borrowedOutsideGs, erf, "Eunk");
			System.out.println("\nINHERITED\n");
			compare(concept, inheritedGs, erf, "Einh");
		}
	}

	private void compare(String concept, Map<String, Etymology> goldStandard, EtymologyRagFilter erf, String expected) {
		int predMatches = 0;
		int atomMatches = 0;
		int count = 0;
		// TODO count (mis)matches (vbl)
		for (Entry<String, Etymology> entry : goldStandard.entrySet()) {
			System.out.println("Actual: " + entry.getValue());
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
				System.out.println("Best prediction:" + atoms.get(0));
				for (RankingEntry<String> atom : atoms.subList(1, atoms.size())) {
					if (atom.value < 0.10) {
						break;
					}
					String pred = atom.key.substring(0, 4);
					int otherFormId = Integer
							.parseInt(atom.key.substring(5, atom.key.length() - 1).split(",")[1].trim());
					System.out.println(" - " + pred + " " + form + " < " + ios.getFormForFormId(otherFormId) + " ("
							+ ios.getLangForForm(otherFormId) + ") " + atom.value);
				}
			}
		}
		System.out.println(String.format("Predicted the etymology type of %d/%d entries (%.2f%%) correctly",
				predMatches, count, 100 * ((double) predMatches) / count));
		if (expected.equals("Eloa"))
			System.out.println(
					String.format("Predicted the etymology type and source of %d/%d entries (%.2f%%) correctly",
							atomMatches, count, 100 * ((double) atomMatches) / count));
		System.out.println("\n==================================================\n");
	}

	public static void main(String[] args) {
		EtymologyNelexTest test = new EtymologyNelexTest();
		Set<String> concepts = new HashSet<>();
		concepts.add("BergN");
		test.run(concepts);
		concepts = new HashSet<>();
		concepts.add("SpracheN");
		test.run(concepts);
		concepts = new HashSet<>();
		concepts.add("KopfN");
		test.run(concepts);
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
