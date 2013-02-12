package jp.co.flect.xmlschema.type;

import jp.co.flect.xmlschema.XMLSchema;
import jp.co.flect.xmlschema.SimpleType;

public class IntType extends SimpleType {
	
	public static final String NAME = "int";
	
	private static final long serialVersionUID = 6201648609554344571L;

	public IntType(XMLSchema schema, String name) {
		super(schema, name);
	}
	
	@Override
	protected boolean doValid(Object o) {
		if (o instanceof Integer) {
			return true;
		}
		try {
			Integer.parseInt(o.toString());
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}
	
	@Override
	protected Object doParse(String s) {
		return Integer.valueOf(s);
	}
	
	@Override
	public boolean isNumberType() { 
		return true;
	}
	
}
