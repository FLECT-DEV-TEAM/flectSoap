package jp.co.flect.xmlschema.type;

import jp.co.flect.xmlschema.XMLSchema;
import jp.co.flect.xmlschema.SimpleType;
import java.math.BigDecimal;

public class DecimalType extends SimpleType {
	
	public static final String NAME = "decimal";
	
	private static final long serialVersionUID = -8214201654463901672L;
	
	public DecimalType(XMLSchema schema, String name) {
		super(schema, name);
	}
	
	@Override
	protected boolean doValid(Object o) {
		if (o instanceof Number) {
			return true;
		}
		try {
			new BigDecimal(o.toString());
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}
	
	@Override
	protected Object doParse(String s) {
		return new BigDecimal(s);
	}
	
	@Override
	public boolean isNumberType() { 
		return true;
	}
	
}
