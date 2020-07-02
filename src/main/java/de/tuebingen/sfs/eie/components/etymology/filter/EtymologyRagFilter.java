package de.tuebingen.sfs.eie.components.etymology.filter;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import de.tuebingen.sfs.psl.engine.RagFilter;
import de.tuebingen.sfs.psl.util.color.HslColor;
import de.tuebingen.sfs.psl.util.data.RankingEntry;

public class EtymologyRagFilter extends RagFilter {

	public EtymologyRagFilter() {
		super();
		System.out.println("INIT ETYM FILTER"); // TODO del 
		initializeIgnoreInGui();
	}

	public EtymologyRagFilter(Map<String, Double> transparencyMap) {
		super(transparencyMap);
		initializeIgnoreInGui();
	}

	private void initializeIgnoreInGui() {
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

	public List<RankingEntry<String>> getEetyForArgument(String argument) {
		List<RankingEntry<String>> entries = new ArrayList<>();
		for (String atom : beliefValues.keySet()) {
			if (atom.startsWith("Eloa(" + argument) || atom.startsWith("Einh(" + argument)
					|| atom.startsWith("Eunk(" + argument)) {
				entries.add(new RankingEntry<String>(atom, beliefValues.get(atom)));
			}
		}
		Collections.sort(entries, Collections.reverseOrder());
		return entries;
	}

	public RankingEntry<String> getHighestEetyForArgument(String argument) {
		return getEetyForArgument(argument).get(0);
	}

	public List<RankingEntry<String>> getHighestEetyPerArgument() {
		Set<String> arguments = new TreeSet<>();
		for (String atom : beliefValues.keySet()) {
			if (atom.startsWith("Eloa(") || atom.startsWith("Einh(") || atom.startsWith("Eunk(")) {
				arguments.add(atom.replace(")", "").split("\\(")[1].split(",")[0]);
			}
		}
		List<RankingEntry<String>> atoms = new ArrayList<>();
		for (String arg : arguments) {
			atoms.add(getHighestEetyForArgument(arg));
		}
		Collections.sort(atoms, Collections.reverseOrder());
		return atoms;
	}

}
