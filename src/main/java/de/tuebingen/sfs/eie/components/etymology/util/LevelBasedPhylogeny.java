package de.tuebingen.sfs.eie.components.etymology.util;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import de.tuebingen.sfs.eie.components.phylogeny.LanguageTree;

public class LevelBasedPhylogeny {

	// Turns a detailed phylogenetic tree into a phylogenetic tree with a fixed
	// number of levels by introducing or collapsing intermediary nodes
	// (such that each modern doculect has the edge-wise distance to the root)

	private LanguageTree tree;
	private int numAncestors;
	private List<String> languages;

	public LevelBasedPhylogeny(int numAncestors, LanguageTree tree) {
		this.tree = tree;
		this.numAncestors = numAncestors;
		List<String> languages = new ArrayList<>();
		tree.collectLeaves("ROOT", languages);
		init();
	}

	public LevelBasedPhylogeny(int numAncestors, String pathToNwkFile, List<String> languages) {
		try {
			tree = LanguageTree.fromNewickFile(pathToNwkFile);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		tree.reduceToLangs(languages.toArray(new String[0]));
		this.numAncestors = numAncestors;
		this.languages = languages;
		init();
	}

	public LevelBasedPhylogeny(int numAncestors, String pathToNwkFile, String[] languages) {
		try {
			tree = LanguageTree.fromNewickFile(pathToNwkFile);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		tree.reduceToLangs(languages);
		this.numAncestors = numAncestors;
		this.languages = new ArrayList<>(Arrays.asList(languages));
		init();
	}

	public LanguageTree getTree() {
		return tree;
	}

	public void renameChildren(Map<String, String> names) {
		List<String> leaves = new ArrayList<>();
		tree.collectLeaves(tree.root, leaves);
		for (String leaf : leaves) {
			if (names.containsKey(leaf)) {
				tree.renameNode(leaf, names.get(leaf));
			}
		}
	}

	// Level 0 = ROOT
	// Level numAncestors = leaves (modern languages)
	public int getLevel(String language) {
		return (tree.pathToRoot(language)).size();
	}

	public List<String> getAncestors(String language) {
		return tree.pathToRoot(language);
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

	public List<String> getModernLanguages() {
		return languages;
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

		List<String> desc = new ArrayList<String>();
		tree.collectDescendants("ROOT", desc);
		System.out.println(desc);
		System.out.println("Norwegian: " + tree.pathToRoot("nor"));
		System.out.println("Portuguese: " + tree.pathToRoot("por"));
		System.out.println("Croatian: " + tree.pathToRoot("hrv"));
		System.out.println(tree.toNewickStringWithBranchLengths());
	}

	private void moveChildrenUp(String oldParent) {
		String newParent = tree.parents.get(oldParent);

		for (String child : tree.children.get(oldParent)) {
			tree.children.get(newParent).add(child);
			tree.parents.put(child, newParent);
			System.out.println("Moved " + child + " from " + oldParent + " to " + newParent);
		}
		tree.children.remove(oldParent);
		tree.children.get(newParent).remove(oldParent);
	}

	private String addIntermediaryNode(String child, String parent) {
		String newNode = "Old_" + child;
		tree.children.get(parent).remove(child);
		tree.children.get(parent).add(newNode);
		tree.children.put(newNode, new TreeSet<String>() {
			private static final long serialVersionUID = 1L;

			{
				add(child);
			}
		});
		tree.parents.put(newNode, parent);
		tree.parents.put(child, newNode);
		System.out.println("Added " + newNode + " between " + child + " and " + parent);
		return newNode;
	}

}
