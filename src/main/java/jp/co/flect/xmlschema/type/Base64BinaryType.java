package jp.co.flect.xmlschema.type;

import jp.co.flect.xmlschema.XMLSchema;
import jp.co.flect.xmlschema.SimpleType;
import jp.co.flect.util.Base64;

public class Base64BinaryType extends SimpleType {
	
	public static final String NAME = "base64Binary";
	
	private static final long serialVersionUID = -6484746308217023524L;

	public Base64BinaryType(XMLSchema schema, String name) {
		super(schema, name);
	}
	
	@Override
	protected boolean doValid(Object o) {
		if (o instanceof byte[]) {
			return true;
		}
		return Base64.isBase64(o.toString());
	}
	
	@Override
	protected String doFormat(Object o) {
		if (o instanceof byte[]) {
			return new String(Base64.encode((byte[])o));
		}
		return o.toString();
	}
	
	@Override
	protected Object doParse(String s) {
		return Base64.decode(s);
	}
	
	@Override
	public boolean isBinaryType() { 
		return true;
	}
}
