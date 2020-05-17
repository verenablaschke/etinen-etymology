package de.tuebingen.sfs.eie.components.etymology.filter;

import java.awt.Color;
import java.util.AbstractMap;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import de.tuebingen.sfs.psl.engine.RagFilter;
import de.tuebingen.sfs.psl.util.color.HslColor;

public class EtymologyRagFilter extends RagFilter {

	public EtymologyRagFilter(Map<String, Double> transparencyMap) {
		super(transparencyMap);
		ignoreInGui.add("Flng");
		ignoreInGui.add("Fsem");
		ignoreInGui.add("Fufo");
		ignoreInGui.add("XFufo");
		ignoreInGui.add("Fsim");
		ignoreInGui.add("Ssim");
		ignoreInGui.add("Tanc");
		ignoreInGui.add("Tcnt");
		ignoreInGui.add("#equal");
		ignoreInGui.add("#notequal");
		ignoreInGui.add("FLNG");
		ignoreInGui.add("FSEM");
		ignoreInGui.add("FUFO");
		ignoreInGui.add("XFUFO");
		ignoreInGui.add("FSIM");
		ignoreInGui.add("SSIM");
		ignoreInGui.add("TANC");
		ignoreInGui.add("TCNT");
		ignoreInGui.add("#EQUAL");
		ignoreInGui.add("#NOTEQUAL");
	}

	@Override
	public HslColor atomToBaseColor(String name) {
		if (name.startsWith("Eloa")) {
			return new HslColor(new Color(219, 74, 255));
		}
		if (name.startsWith("Einh")) {
			return new HslColor(new Color(50, 130, 250));
		}
		return BASECOLOR;
	}

	public List<SimpleEntry<String, Double>> getEetyForArgument(String argument) {
		List<SimpleEntry<String, Double>> entries = new ArrayList<>();
		for (String atom : transparencyMap.keySet()) {
			if (atom.startsWith("Eloa(" + argument) || atom.startsWith("Einh(" + argument) || atom.startsWith("Eunk(" + argument)) {
				entries.add(new SimpleEntry<String, Double>(atom, transparencyMap.get(atom)));
			}
		}
		entries.sort((e1, e2) -> e2.getValue().compareTo(e1.getValue()));
		return entries;
	}

	public SimpleEntry<String, Double> getHighestEetyForArgument(String argument) {
		return getEetyForArgument(argument).get(0);
	}

	public List<SimpleEntry<String, Double>> getHighestEetyPerArgument() {
		Set<String> arguments = new TreeSet<>();
		for (String atom : transparencyMap.keySet()) {
			if (atom.startsWith("Eloa(") || atom.startsWith("Einh(") || atom.startsWith("Eunk(")) {
				arguments.add(atom.replace(")", "").split("\\(")[1].split(",")[0]);
			}
		}
		List<SimpleEntry<String, Double>> atoms = new ArrayList<>();
		for (String arg : arguments) {
			atoms.add(getHighestEetyForArgument(arg));
		}
		atoms.sort((e1, e2) -> e2.getValue().compareTo(e1.getValue()));
		return atoms;
	}

}