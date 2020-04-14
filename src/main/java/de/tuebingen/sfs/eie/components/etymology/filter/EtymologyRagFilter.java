package de.tuebingen.sfs.eie.components.etymology.filter;

import java.util.Map;

import de.tuebingen.sfs.psl.engine.RagFilter;

public class EtymologyRagFilter extends RagFilter {
	
	public EtymologyRagFilter(Map<String, Double> transparencyMap) {
		super(transparencyMap);
//		ignoreList.add("Flng");
//		ignoreList.add("Fsem");
//		ignoreList.add("Fufo");
//		ignoreList.add("XFufo");
//		ignoreList.add("Fsim");
//		ignoreList.add("Ssim");
//		ignoreList.add("Tanc");
//		ignoreList.add("Tcnt");
//        ignoreList.add("#equal");
//        ignoreList.add("#notequal");
		ignoreList.add("FLNG");
		ignoreList.add("FSEM");
		ignoreList.add("FUFO");
		ignoreList.add("XFUFO");
		ignoreList.add("FSIM");
		ignoreList.add("SSIM");
		ignoreList.add("TANC");
		ignoreList.add("TCNT");
        ignoreList.add("#EQUAL");
        ignoreList.add("#NOTEQUAL");
	}

}
