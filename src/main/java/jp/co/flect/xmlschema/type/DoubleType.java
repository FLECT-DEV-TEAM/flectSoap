package jp.co.flect.xmlschema.type;

import jp.co.flect.xmlschema.XMLSchema;
import jp.co.flect.xmlschema.SimpleType;

public class DoubleType extends SimpleType {
	
	public static final String NAME = "double";
	
	private static final long serialVersionUID = 8184569051646409412L;

	public DoubleType(XMLSchema schema, String name) {
		super(schema, name);
	}
	
	@Override
	protected boolean doValid(Object o) {
		if (o instanceof Double) {
			return true;
		}
		try {
			Double.parseDouble(o.toString());
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}
	
	@Override
	protected Object doParse(String s) {
		return Double.valueOf(s);
	}
	
	@Override
	public boolean isNumberType() { 
		return true;
	}
	
}
