package de.tuebingen.sfs.eie.components.etymology.ideas;

public class Entry {

	String id;
	Integer formId = null;
	String form = null;
	String language;
	String concept;

	public Entry(String id, int formId, String language, String concept) {
		this.id = id;
		this.formId = formId;
		this.language = language;
		this.concept = concept;
	}
	
	public Entry(String id, String form, String language, String concept) {
		this.id = id;
		this.form = form;
		this.language = language;
		this.concept = concept;
	}

	public String toString(){
		return id + "(" + formId == null? form : formId + ", " + language + ", " + concept + ")";
	}

}
