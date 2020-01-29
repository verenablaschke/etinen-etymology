package de.tuebingen.sfs.eie.components.etymology.eval;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.tuebingen.sfs.eie.components.etymology.problems.EtymologyProblem;
import de.tuebingen.sfs.eie.components.etymology.util.LevelBasedPhylogeny;
import de.tuebingen.sfs.eie.components.soundlaws.SoundlawProblem;
import de.tuebingen.sfs.eie.shared.util.SemanticNetwork;
import de.tuebingen.sfs.eie.gui.facts.StandaloneFactViewer;
import de.tuebingen.sfs.iwsa.tokenize.IPATokenizer;
import de.tuebingen.sfs.psl.engine.core.RuleAtomGraph;
import de.tuebingen.sfs.psl.engine.filter.RagFilter;

public class EtymologyInferenceTest {

	public static void main(String[] args) {
		String dbDir = "src/test/resources/northeuralex-0.9";
		String networkEdgesFile = "src/test/resources/etymology/clics2-network-edges.txt";
		String networkIdsFile = "src/test/resources/etymology/clics2-network-ids.txt";
		String northeuralexConceptsFile = "src/test/resources/northeuralex-0.9/parameters.csv";
		String treeFile = "src/test/resources/northeuralex-0.9/tree.nwk";

		List<String> languageStrings = new ArrayList<>();
		languageStrings.add("english");
		languageStrings.add("german");
		languageStrings.add("dutch");
		languageStrings.add("icelandic");
		languageStrings.add("swedish");
		languageStrings.add("danish");
		languageStrings.add("norwegianbokmal");
		languageStrings.add("french");
		languageStrings.add("italian");
		languageStrings.add("spanish");
		languageStrings.add("portuguese");
		languageStrings.add("romanian");
		languageStrings.add("japanese");
		languageStrings.add("russian");
		languageStrings.add("ukrainian");

		Set<String> testConcepts = new HashSet<>();
		// testConcepts.add("BergN");
		// testConcepts.add("KopfN");
		testConcepts.add("SpracheN");
		// testConcepts.add("ZungeN");

		IPATokenizer tokenizer = new IPATokenizer();
		SoundlawProblem model = SoundlawProblem.fromCLDFPath(dbDir, tokenizer, languageStrings);

		String[] langs = new String[] { "eng", "deu", "isl", "swe", "nor", "dan", "fra", "spa", "por", "cat", "fra",
				"ita", "ron", "lat", "lit", "lav", "rus", "bel", "ukr", "pol", "ces", "slv", "slk", "hrv", "nld" };
		LevelBasedPhylogeny phylogeny = new LevelBasedPhylogeny(4, treeFile, langs);

		SemanticNetwork net = new SemanticNetwork(networkEdgesFile, networkIdsFile, northeuralexConceptsFile, 2);
		EtymologyProblem psl = new EtymologyProblem(model.getDatabase(), model.getCorrModel(), net, phylogeny);
		psl.generateDataAtoms(testConcepts);
		psl.getAtoms().printToConsoleWithValue();
		psl.addInteractionRules();
		// psl.atoms.fixateAtoms("Wsim", "?", "?");
		// psl.atoms.fixateAtoms("Semsim", "?", "?");
		psl.runInference();
		psl.printResult();

		Map<String, Double> etymResult = psl.extractResult(false);
		RuleAtomGraph.GROUNDING_OUTPUT = true;
		RuleAtomGraph.ATOM_VALUE_OUTPUT = true;
		RuleAtomGraph rag = new RuleAtomGraph(psl, new RagFilter(etymResult));
		rag.printToStream(System.out);
		StandaloneFactViewer.launchWithData(psl, rag, etymResult);

	}
}
