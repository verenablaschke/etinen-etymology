package de.tuebingen.sfs.eie.components.etymology.eval;

import de.tuebingen.sfs.eie.components.etymology.filter.EtymologyRagFilter;
import de.tuebingen.sfs.psl.util.data.RankingEntry;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class EtymologyResultChecker {

    private static void checkAnalysis(EtymologyRagFilter filter, Map<String, Double> specialCases, PrintStream out) {
        for (Entry<String, Double> argAndThreshold : specialCases.entrySet()) {
            out.println("--------\n");
            for (RankingEntry<String> entry : filter.getEetyForArgument(argAndThreshold.getKey())) {
                if (entry.value < argAndThreshold.getValue()) {
                    break;
                }
                printEntry(entry, out);
            }
        }
        out.println("\n----MOST LIKELY EXPLANATION PER WORD----\n");
        for (RankingEntry<String> entry : filter.getHighestEetyPerArgument()) {
            printEntry(entry, out);
        }
    }

    public static void checkMountainAnalysis(EtymologyRagFilter filter, PrintStream out) {
        Map<String, Double> specialCases = new HashMap<String, Double>();
        specialCases.put("eng:mountain:ˈmaʊntɪn:BergN", 0.05);
        checkAnalysis(filter, specialCases, out);
    }

    public static void checkMountainAnalysis(EtymologyRagFilter filter) {
        checkMountainAnalysis(filter, System.out);
    }

    public static void checkLanguageAnalysis(EtymologyRagFilter filter, PrintStream out) {
        Map<String, Double> specialCases = new HashMap<String, Double>();
        specialCases.put("eng:language:ˈlæŋɡwɪd͡ʒ:SpracheN", 0.05);
        checkAnalysis(filter, specialCases, out);
    }

    public static void checkLanguageAnalysis(EtymologyRagFilter filter) {
        checkLanguageAnalysis(filter, System.out);
    }

    public static void checkHeadAnalysis(EtymologyRagFilter filter, PrintStream out) {
        Map<String, Double> specialCases = new HashMap<String, Double>();
        specialCases.put("deu:Kopf:kɔp͡f:KopfN", 0.05);
        checkAnalysis(filter, specialCases, out);
    }

    public static void checkHeadAnalysis(EtymologyRagFilter filter) {
        checkHeadAnalysis(filter, System.out);
    }

    public static void checkTestAnalysis(EtymologyRagFilter filter) {
        checkTestAnalysis(filter, System.out);
    }

    public static void checkTestAnalysis(EtymologyRagFilter filter, PrintStream out) {
        Map<String, Double> specialCases = new HashMap<String, Double>();
        specialCases.put("b1:langBorrowed:læŋ:SpracheN", 0.05);
        specialCases.put("a4:isu:isu:SpracheN", 0.05);
        checkAnalysis(filter, specialCases, out);
    }

    private static void printEntry(RankingEntry<String> entry, PrintStream out) {
        String[] predAndArgs = entry.key.replace(")", "").split("\\(");
        String[] args = predAndArgs[1].split(",");
        out.print(args[0] + "\t\t< " + predAndArgs[0]);
        if (args.length > 1) {
            out.print("\t" + args[1]);
        }
        out.println("\t\t" + (int) (100 * entry.value) + "%");
    }

}
