package jp.co.flect.xmlschema.type;

import jp.co.flect.xmlschema.XMLSchema;

public class TimeType extends DatetimeType {
	
	public static final String NAME = "time";
	
	private static final long serialVersionUID = 7972144531356124339L;

	public TimeType(XMLSchema schema, String name) {
		super(schema, name);
	}
	
	@Override
	protected String getFormat() { return "HH:mm:ss.SSSZZ";}
	
}
