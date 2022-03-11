package de.tuebingen.sfs.eie.components.etymology.util;

import de.tuebingen.sfs.eie.shared.core.LanguageTree;
import de.tuebingen.sfs.eie.shared.core.TreeLayer;
import de.tuebingen.sfs.eie.shared.io.LanguageTreeStorage;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LevelBasedPhylogeny {

	// Turns a detailed phylogenetic tree into a phylogenetic tree with a fixed
	// number of levels by introducing or collapsing intermediary nodes
	// (such that each modern doculect has the edge-wise distance to the root)

	private LanguageTree tree;
	private int numAncestors;

	public LevelBasedPhylogeny(int numAncestors, LanguageTree tree) {
		this.tree = tree;
		this.numAncestors = numAncestors;
		List<String> languages = new ArrayList<>();
		tree.collectLeaves("ROOT", languages);
		init();
	}

	public LevelBasedPhylogeny(String pathToNwkFile) {
		try {
			tree = LanguageTreeStorage.fromNewickFile(pathToNwkFile);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		this.numAncestors = -1;
	}

	public LevelBasedPhylogeny(int numAncestors, String pathToNwkFile, List<String> languages) {
		this(numAncestors, pathToNwkFile, languages.toArray(new String[0]));
	}

	public LevelBasedPhylogeny(int numAncestors, String pathToNwkFile, String[] languages) {
		try {
			tree = LanguageTreeStorage.fromNewickFile(pathToNwkFile);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		tree.reduceToLangs(languages);
		this.numAncestors = numAncestors;
		init();
	}

	public LanguageTree getTree() {
		return tree;
	}

	// Level 0 = ROOT
	// Level numAncestors = leaves (modern languages)
	public int getLevel(String language) {
		TreeLayer layer = tree.nodesToLayers.get(language);
		if (layer.getIndex() == null) {
			return numAncestors;
		}
		return layer.getIndex();
	}

	public boolean isLeaf(String language) {
		return tree.nodesToLayers.get(language).equals(TreeLayer.leaves());
	}

	public List<String> getAncestors(String language) {
		return tree.pathToRoot(language);
	}

	public String getParent(String language) {
		return tree.parents.get(language);
	}

	public boolean decendsFrom(String language, String potentialAncestor) {
		return distanceToAncestor(potentialAncestor, language) > 0;
	}

	public int distanceToAncestor(String language, String potentialAncestor) {
		int dist = 0;
		if (language.equals(potentialAncestor)) {
			return 0;
		}
		while ((language = tree.parents.get(language)) != null) {
			dist++;
			if (language.equals(potentialAncestor)) {
				return dist;
			}
		}
		return -1;
	}

	public List<String> getAllLanguages() {
		return new ArrayList<String>(Arrays.asList(tree.getLangsAndProtoLangsExceptRoot()));
	}

	private void init() {
		List<String> languages = new ArrayList<>();
		tree.collectLeaves("ROOT", languages);
		for (String lang : languages) {
			List<String> ancestors = tree.pathToRoot(lang);
			if (ancestors.size() == numAncestors) {
				continue;
			}
			int numExtraAncestors = 0;
			while (ancestors.size() < numAncestors) {
				String child = numExtraAncestors == 0 ? lang : ancestors.get(numExtraAncestors - 1);
				String parent = ancestors.get(numExtraAncestors);
				String newNode = addIntermediaryNode(child, parent);
				ancestors.add(numExtraAncestors, newNode);
				numExtraAncestors++;
			}
			while (ancestors.size() > numAncestors) {
				moveChildrenUp(ancestors.get(0));
				ancestors.remove(0);
			}
		}
		tree.initializeLeafLayer();
		List<String> desc = new ArrayList<String>();
		tree.collectDescendants("ROOT", desc);
		System.err.println(tree.toNewickString());
	}

	private void moveChildrenUp(String oldParent) {
		String newParent = tree.parents.get(oldParent);
		TreeLayer previousLayer = tree.nodesToLayers.remove(oldParent);
		for (String child : tree.children.get(oldParent)) {
			tree.children.get(newParent).add(child);
			tree.parents.put(child, newParent);
			System.out.println("Moved " + child + " from " + oldParent + " to " + newParent);
			TreeLayer oldChildLayer = tree.nodesToLayers.get(child);
			tree.nodesToLayers.put(child, previousLayer);
		}
		tree.children.remove(oldParent);
		tree.children.get(newParent).remove(oldParent);
	}

	private String addIntermediaryNode(String child, String parent) {
		String newNode = "Old_" + child;
		tree.children.get(parent).remove(child);
		tree.children.get(parent).add(newNode);
		List<String> children = new ArrayList<>();
		children.add(child);
		tree.children.put(newNode, children);
		tree.parents.put(newNode, parent);
		tree.parents.put(child, newNode);
		TreeLayer previousLayer = tree.nodesToLayers.get(child);
		tree.nodesToLayers.put(newNode, previousLayer);
		int posInLayerList = tree.layersUnderNode.get("ROOT").indexOf(previousLayer);
		while (children != null) {
			for (String curChild : children) {
				TreeLayer layer = tree.layersUnderNode.get("ROOT").get(++posInLayerList);
				tree.nodesToLayers.put(curChild, layer);
				children = tree.children.get(curChild);
			}
		}
		System.err.println("Added " + newNode + " between " + child + " and " + parent);
		return newNode;
	}

}
