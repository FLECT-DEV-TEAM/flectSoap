package jp.co.flect.xmlschema;

import org.w3c.dom.Element;

public class InvalidSchemaException extends XMLSchemaException {
	
	private static final long serialVersionUID = -1910806122487904847L;

	private Element element;
	
	public InvalidSchemaException(String msg, Element el) {
		super(msg);
		this.element = el;
	}
	
	public Element getElement() { return this.element;}
	
}
