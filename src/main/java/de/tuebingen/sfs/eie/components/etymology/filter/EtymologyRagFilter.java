package de.tuebingen.sfs.eie.components.etymology.filter;

import java.util.Map;

import de.tuebingen.sfs.psl.engine.RagFilter;

public class EtymologyRagFilter extends RagFilter {
	
	// TODO fix. the ignore list should only apply to the rendering, not to the grounding
	
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

}
