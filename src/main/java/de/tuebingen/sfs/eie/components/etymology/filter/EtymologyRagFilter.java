package de.tuebingen.sfs.eie.components.etymology.filter;

import java.awt.Color;
import java.util.Map;

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
		if (name.startsWith("Eloa")){
			return new HslColor(new Color(219, 74, 255));
		}
		if (name.startsWith("Einh")){
			return new HslColor(new Color(50, 130, 250));
		}
		return BASECOLOR;
	}

}
