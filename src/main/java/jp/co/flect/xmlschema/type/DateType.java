package jp.co.flect.xmlschema.type;

import jp.co.flect.xmlschema.XMLSchema;

public class DateType extends DatetimeType {
	
	public static final String NAME = "date";
	
	private static final long serialVersionUID = 7578125954305286550L;

	public DateType(XMLSchema schema, String name) {
		super(schema, name);
	}
	
	@Override
	protected String getFormat() { return "yyyy-MM-dd";}
	
}
