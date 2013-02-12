package jp.co.flect.xmlschema.type;

import jp.co.flect.xmlschema.XMLSchema;
import jp.co.flect.xmlschema.SimpleType;

public class AnyType extends SimpleType {
	
	public static final String NAME = "anyType";
	
	private static final long serialVersionUID = 2699119407505250968L;

	public AnyType(XMLSchema schema, String name) {
		super(schema, name);
	}
	
	@Override
	public boolean isAnyType() { return true;}
	
}
