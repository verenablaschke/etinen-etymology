package de.tuebingen.sfs.eie.components.etymology.eval;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import de.tuebingen.sfs.eie.shared.core.LanguagePhylogeny;
import de.tuebingen.sfs.eie.shared.core.LanguageTree;
import de.tuebingen.sfs.eie.shared.util.Pair;

public class ProtoFormSimilarityDummy {

	static final double similarityDecay = 0.1;
	static final double minSim = 0.1;

	public static void main(String[] args) {
//		LanguagePhylogeny phylo = new LanguagePhylogeny(new LanguageTree());
//		Map<Pair<String, String>, Integer> distances = new HashMap<>(); // Based on inheritance only.
//		readTree("etinen-etymology/src/test/resources/sampledata/languages.txt", phylo, distances);

		Map<Pair<String, String>, Integer> distancesInherited = new HashMap<>();
		readEtymology("etinen-etymology/src/test/resources/sampledata/inherited.txt", distancesInherited);

		Map<Pair<String, String>, Integer> distancesWithBorrowing = new HashMap<>();
		readEtymology("etinen-etymology/src/test/resources/sampledata/withborrowing.txt", distancesWithBorrowing);

		String[] forms = new String[] { "w1", "w2", "w3", "w4", "w5", "w6", "w7", "w8", "w9", "w10", "w11", "w12", };
		System.out.println("      " + String.join("     ", forms));
		for (int i = 0; i < forms.length; i++) {
			String form1 = forms[i];
			System.out.print(form1 + " ");
			if (form1.length() < 3) {
				System.out.print(" ");
			}
			for (int j = 0; j < forms.length; j++) {
				if (i > j) {
					System.out.print("       ");
				} else {
					String form2 = forms[j];
					int distInh = distancesInherited.get(new Pair<>(form1, form2));
					int distLoa = distancesWithBorrowing.get(new Pair<>(form1, form2));
					if (distInh != distLoa) {
						System.out.print(" *%.1f* ".formatted(distToExpectedSim(distLoa)));
					} else {
						System.out.print("  %.1f  ".formatted(distToExpectedSim(distLoa)));
					}
				}
			}
			System.out.println();
		}
	}

	private static void readTree(String treeFile, LanguagePhylogeny phylo,
			Map<Pair<String, String>, Integer> distances) {
		try (BufferedReader reader = new BufferedReader(
				new InputStreamReader(new FileInputStream(new File(treeFile)), StandardCharsets.UTF_8))) {
			Stack<String> parentStack = new Stack<>();
			parentStack.push(LanguageTree.root);
			distances.put(new Pair<>(LanguageTree.root, LanguageTree.root), 0);
			int prevIndent = -1;
			int curIndent;
			for (String line = reader.readLine(); line != null; line = reader.readLine()) {
				if (line.isEmpty())
					continue;
				for (curIndent = 0; curIndent < line.length() - 1; curIndent++) {
					if (line.charAt(curIndent) != ' ') {
						break;
					}
				}

				// Language tree link
				String[] langs = line.split("<-");
				String lang = langs[0].strip().toUpperCase();

				distances.put(new Pair<>(lang, LanguageTree.root), curIndent + 1);
				distances.put(new Pair<>(LanguageTree.root, lang), curIndent + 1);

				int indentMod = 0;
				while (curIndent + indentMod++ <= prevIndent) {
					parentStack.pop();
				}
				String parent = parentStack.peek();
				phylo.parents.put(lang, parent);
				distances.put(new Pair<>(lang, parent), 1);
				distances.put(new Pair<>(parent, lang), 1);
				distances.put(new Pair<>(lang, lang), 0);
				List<String> siblings = phylo.children.get(parent);
				if (siblings == null) {
					siblings = new ArrayList<>();
				}
				siblings.add(lang);
				phylo.children.put(parent, siblings);
				parentStack.push(lang);
				prevIndent = curIndent;

				// Contacts
				if (langs.length > 1) {
					for (String contact : langs[1].split(",")) {
						phylo.addInfluence(langs[1].strip().toUpperCase(), lang);
					}
				}
			}

			for (String lang1 : phylo.parents.keySet()) {
				for (String lang2 : phylo.parents.keySet()) {
					if (distances.containsKey(new Pair<>(lang1, lang2))) {
						continue;
					}
					int dist = distances.get(new Pair<>(lang1, LanguageTree.root))
							+ distances.get(new Pair<>(lang2, LanguageTree.root)) - 2 * distances
									.get(new Pair<>(phylo.lowestCommonAncestor(lang1, lang2), LanguageTree.root));
					distances.put(new Pair<>(lang1, lang2), dist);
					distances.put(new Pair<>(lang2, lang1), dist);
				}
			}

			// TODO
			String[] langs = new String[] { "L1", "L2", "L3", "L4", "L5", "L6", "L7", "L8", "L9", "L10", "L11", "L12",
					"L13" };
			System.out.println("     " + String.join("     ", langs));
			for (int i = 0; i < langs.length; i++) {
				String lang1 = langs[i];
				System.out.print(lang1 + "  ");
				if (lang1.length() < 3) {
					System.out.print(" ");
				}
				for (int j = 0; j < langs.length; j++) {
					if (i > j) {
						System.out.print("     ");
					} else {
						String lang2 = langs[j];
						System.out
								.print("  %.1f  ".formatted(distToExpectedSim(distances.get(new Pair<>(lang1, lang2)))));
					}
				}
				System.out.println();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void readEtymology(String file, Map<Pair<String, String>, Integer> distances) {
		try (BufferedReader reader = new BufferedReader(
				new InputStreamReader(new FileInputStream(new File(file)), StandardCharsets.UTF_8))) {
			Map<String, String> formToSource = new HashMap<>();
			Stack<String> sourceStack = new Stack<>();
			sourceStack.push(LanguageTree.root);
			distances.put(new Pair<>(LanguageTree.root, LanguageTree.root), 0);
			int prevIndent = -1;
			int curIndent;
			for (String line = reader.readLine(); line != null; line = reader.readLine()) {
				if (line.isEmpty())
					continue;
				for (curIndent = 0; curIndent < line.length() - 1; curIndent++) {
					if (line.charAt(curIndent) != ' ') {
						break;
					}
				}

				// Language tree link
				String form = line.replace("->", "").strip();

				distances.put(new Pair<>(form, LanguageTree.root), curIndent + 1);
				distances.put(new Pair<>(LanguageTree.root, form), curIndent + 1);

				int indentMod = 0;
				while (curIndent + indentMod++ <= prevIndent) {
					sourceStack.pop();
				}
				String source = sourceStack.peek();
				formToSource.put(form, source);
				distances.put(new Pair<>(form, source), 1);
				distances.put(new Pair<>(source, form), 1);
				distances.put(new Pair<>(form, form), 0);
				sourceStack.push(form);
				prevIndent = curIndent;
			}

			for (String form1 : formToSource.keySet()) {
				for (String form2 : formToSource.keySet()) {
					if (distances.containsKey(new Pair<>(form1, form2))) {
						continue;
					}
					int dist = distances.get(new Pair<>(form1, LanguageTree.root))
							+ distances.get(new Pair<>(form2, LanguageTree.root)) - 2 * distances.get(
									new Pair<>(lowestCommonAncestor(form1, form2, formToSource), LanguageTree.root));
					distances.put(new Pair<>(form1, form2), dist);
					distances.put(new Pair<>(form2, form1), dist);
				}
			}

			// TODO
			String[] forms = new String[] { "w1", "w2", "w3", "w4", "w5", "w6", "w7", "w8", "w9", "w10", "w11",
					"w12", };
			System.out.println("     " + String.join("   ", forms));
			for (int i = 0; i < forms.length; i++) {
				String lang1 = forms[i];
				System.out.print(lang1 + "  ");
				if (lang1.length() < 3) {
					System.out.print(" ");
				}
				for (int j = 0; j < forms.length; j++) {
					if (i > j) {
						System.out.print("     ");
					} else {
						String lang2 = forms[j];
						System.out
								.print("%.1f  ".formatted(distToExpectedSim(distances.get(new Pair<>(lang1, lang2)))));
					}
				}
				System.out.println();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public static double distToExpectedSim(int dist) {
		double sim = 1 - similarityDecay * dist;
		return sim < minSim ? minSim : sim;
	}

	private static String lowestCommonAncestor(String node1, String node2, Map<String, String> formToSource) {
		List<String> ancestors1 = new ArrayList<>();
		String anc = node1;
		while (formToSource.containsKey(anc)) {
			anc = formToSource.get(anc);
			ancestors1.add(anc);
		}
		List<String> ancestors2 = new ArrayList<>();
		anc = node2;
		while (formToSource.containsKey(anc)) {
			anc = formToSource.get(anc);
			ancestors2.add(anc);
		}
		for (String anc2 : ancestors2) {
			if (ancestors1.contains(anc2)) {
				return anc2;
			}
		}
		return LanguageTree.root;
	}

}
