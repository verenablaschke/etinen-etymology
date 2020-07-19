package de.tuebingen.sfs.eie.components.etymology.ideas;

public class Entry {

	String id;
	int form;
	String language;
	String concept;

	public Entry(String id, int form, String language, String concept) {
		this.id = id;
		this.form = form;
		this.language = language;
		this.concept = concept;
	}

}
