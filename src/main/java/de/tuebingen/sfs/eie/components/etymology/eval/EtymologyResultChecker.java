package de.tuebingen.sfs.eie.components.etymology.eval;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import de.tuebingen.sfs.eie.components.etymology.filter.EtymologyRagFilter;

public class EtymologyResultChecker {

	public static void checkAnalysis(EtymologyRagFilter filter, Map<String, Double> specialCases) {
		for (Entry<String, Double> argAndThreshold : specialCases.entrySet()) {
			System.out.println("--------\n");
			for (Entry<String, Double> entry : filter.getEetyForArgument(argAndThreshold.getKey())) {
				if (entry.getValue() < argAndThreshold.getValue()) {
					break;
				}
				printEntry(entry);
			}
		}
		System.out.println("\n----MOST LIKELY EXPLANATION PER WORD----\n");
		for (Entry<String, Double> entry : filter.getHighestEetyPerArgument()) {
			printEntry(entry);
		}
	}

	public static void checkMountainAnalysis(EtymologyRagFilter filter) {
		Map<String, Double> specialCases = new HashMap<String, Double>();
		specialCases.put("eng:mountain:ˈmaʊntɪn:BergN", 0.06);
		checkAnalysis(filter, specialCases);
	}
	
	public static void checkLanguageAnalysis(EtymologyRagFilter filter) {
		Map<String, Double> specialCases = new HashMap<String, Double>();
		specialCases.put("eng:language:ˈlæŋɡwɪd͡ʒ:SpracheN", 0.06);
		checkAnalysis(filter, specialCases);
	}
	
	public static void checkHeadAnalysis(EtymologyRagFilter filter) {
		Map<String, Double> specialCases = new HashMap<String, Double>();
		specialCases.put("deu:Kopf:kɔp͡f:KopfN", 0.06);
		checkAnalysis(filter, specialCases);
	}
	
	private static void printEntry(Entry<String, Double> entry){
		String[] predAndArgs = entry.getKey().replace(")", "").split("\\(");
		String[] args = predAndArgs[1].split(",");
		System.out.print(args[0] + "\t\t< " + predAndArgs[0]);
		if (args.length > 1){
			System.out.print("\t" + args[1]);
		}
		System.out.println("\t\t" + (int) (100 * entry.getValue()) + "%");
	}

}
