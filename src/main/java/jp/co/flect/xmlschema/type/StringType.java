package jp.co.flect.xmlschema.type;

import jp.co.flect.xmlschema.XMLSchema;
import jp.co.flect.xmlschema.SimpleType;

public class StringType extends SimpleType {
	
	public static final String NAME = "string";
	
	private static final long serialVersionUID = 746152990787110192L;

	public StringType(XMLSchema schema, String name) {
		super(schema, name);
	}
	
	@Override
	public boolean isStringType() { return true;}
}
