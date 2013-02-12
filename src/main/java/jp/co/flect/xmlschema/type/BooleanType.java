package jp.co.flect.xmlschema.type;

import jp.co.flect.xmlschema.XMLSchema;
import jp.co.flect.xmlschema.SimpleType;

public class BooleanType extends SimpleType {
	
	public static final String NAME = "boolean";
	
	private static final long serialVersionUID = -6103047843504620143L;

	public BooleanType(XMLSchema schema, String name) {
		super(schema, name);
	}
	
	@Override
	protected String doFormat(Object o) {
		if (o instanceof Boolean) {
			return o.toString();
		}
		return Boolean.valueOf(o.toString()).toString();
	}
	
	@Override
	protected Object doParse(String s) {
		return Boolean.valueOf(s);
	}
	
	@Override
	public boolean isBooleanType() { 
		return true;
	}
	
}
