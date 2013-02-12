package jp.co.flect.xmlschema.type;

import jp.co.flect.xmlschema.XMLSchema;
import jp.co.flect.xmlschema.SimpleType;
import jp.co.flect.xml.XMLUtils;

public class QNameType extends SimpleType {
	
	public static final String NAME = "QName";
	
	private static final long serialVersionUID = 3385609032000671005L;

	public QNameType(XMLSchema schema, String name) {
		super(schema, name);
	}
	
	@Override
	protected boolean doValid(Object o) {
		String s = o.toString();
		int idx = s.indexOf(":");
		if (idx == -1) {
			return false;
		}
		return XMLUtils.isNCName(s.substring(0, idx)) && 
			XMLUtils.isNCName(s.substring(idx+1));
	}
	
}
