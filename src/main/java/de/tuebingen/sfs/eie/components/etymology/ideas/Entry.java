package de.tuebingen.sfs.eie.components.etymology.ideas;

import de.tuebingen.sfs.cldfjava.data.CLDFForm;

public class Entry {

	String id;
	CLDFForm form;
	String language;
	String concept;

	public Entry(String id, CLDFForm form, String language, String concept) {
		this.id = id;
		this.form = form;
		this.language = language;
		this.concept = concept;
	}

}
