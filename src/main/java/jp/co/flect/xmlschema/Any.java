package jp.co.flect.xmlschema;

public class Any extends ElementDef {
	
	private static final long serialVersionUID = -1300018969471579471L;
	
	private String namespace = "##any";
	private String processContents = "strict";
	
	public Any(XMLSchema schema) {
		super(schema, null, false);
	}
	
	public String getNamespace() { return this.namespace;}
	void setNamespace(String s) { this.namespace = s;}
	
	public String getProcessContents() { return this.processContents;}
	void setProcessContents(String s) { this.processContents = s;}
	
}
