/*
 * Copyright 2018–2022 University of Tübingen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tuebingen.sfs.eie.components.etymology.filter;

import de.tuebingen.sfs.psl.engine.RagFilter;
import de.tuebingen.sfs.psl.util.color.HslColor;
import de.tuebingen.sfs.psl.util.data.RankingEntry;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class EtymologyRagFilter extends RagFilter {

	static String[] staticPreds = new String[] { "Xinh", "Xloa", "Xsth", "Xdst", "#equal", "#notequal" };

	public EtymologyRagFilter() {
		super();
		initializeIgnoreInGui();
	}

	public EtymologyRagFilter(Map<String, Double> transparencyMap, Set<String> fixedAtoms, Set<String> hiddenAtoms) {
		super(transparencyMap, fixedAtoms, hiddenAtoms);
		initializeIgnoreInGui();
		initializePreventUserInteraction();
	}

	private void initializeIgnoreInGui() {
		for (String pred : staticPreds) {
			ignoreInGui.add(pred);
			ignoreInGui.add(pred.toUpperCase());
		}
	}

	private void initializePreventUserInteraction() {
		for (String pred : staticPreds) {
			preventUserInteraction.add(pred);
			preventUserInteraction.add(pred.toUpperCase());
		}
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
