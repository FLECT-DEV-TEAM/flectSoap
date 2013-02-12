package jp.co.flect.xmlschema;

import jp.co.flect.xmlschema.type.StringType;
import jp.co.flect.xmlschema.type.DoubleType;
import jp.co.flect.xmlschema.type.IntType;
import jp.co.flect.xmlschema.type.BooleanType;
import jp.co.flect.xmlschema.type.DatetimeType;
import jp.co.flect.xmlschema.type.DateType;
import jp.co.flect.xmlschema.type.TimeType;
import jp.co.flect.xmlschema.type.Base64BinaryType;
import jp.co.flect.xmlschema.type.QNameType;
import jp.co.flect.xmlschema.type.AnyType;
import java.io.ObjectStreamException;

public class SimpleType extends TypeDef {
	
	private static final long serialVersionUID = 1505016885823600078L;
	private static final XMLSchema XSD = new XMLSchema(XMLSchemaConstants.XSD_NSURI, false, false);
	
	static {
		XSD.addType(new StringType(XSD, "string"));
		XSD.addType(new DoubleType(XSD, "double"));
		XSD.addType(new IntType(XSD, "int"));
		XSD.addType(new BooleanType(XSD, "boolean"));
		XSD.addType(new DatetimeType(XSD, "dateTime"));
		XSD.addType(new TimeType(XSD, "time"));
		XSD.addType(new DateType(XSD, "date"));
		XSD.addType(new Base64BinaryType(XSD, "base64Binary"));
		XSD.addType(new QNameType(XSD, "QName"));
		XSD.addType(new AnyType(XSD, "anyType"));
	}
	
	public static SimpleType getBuiltinType(String name) {
		SimpleType type = (SimpleType)XSD.getType(name);
		if (type == null) {
			System.out.println("type not found: " + name);
			type = new SimpleType(XSD, name);
			XSD.addType(type);
		}
		return type;
	}
	
	public SimpleType(XMLSchema schema, String name) {
		super(schema, name);
	}
	
	public boolean isSimpleType() { return true;}
	
	public final boolean isValid(Object o) { 
		if (o == null) {
			return true;
		}
		SimpleType type = hasBaseType() ? (SimpleType)getBaseType() : this;
		return type.doValid(o);
	}
	
	//For override
	protected boolean doValid(Object o) {
		return true;
	}
	
	public final String format(Object o) { 
		if (o == null) {
			return null;
		}
		if (hasBaseType()) {
			return ((SimpleType)getBaseType()).format(o);
		}
		return doFormat(o);
	}
	
	public final Object parse(String s) {
		if (s == null) {
			return null;
		}
		if (hasBaseType()) {
			return ((SimpleType)getBaseType()).parse(s);
		}
		return doParse(s);
	}
	
	//For override
	protected String doFormat(Object o) {
		return o.toString();
	}
	
	protected Object doParse(String s) {
		return s;
	}
	
	public boolean isAnyType() { return false;}
	
	public boolean isNumberType() { 
		return hasBaseType() ? ((SimpleType)getBaseType()).isNumberType() : false;
	}
	
	public boolean isDateType() { 
		return hasBaseType() ? ((SimpleType)getBaseType()).isDateType() : false;
	}
	
	public boolean isBooleanType() { 
		return hasBaseType() ? ((SimpleType)getBaseType()).isBooleanType() : false;
	}
	
	public boolean isBinaryType() { 
		return hasBaseType() ? ((SimpleType)getBaseType()).isBinaryType() : false;
	}
	
	public boolean isStringType() { 
		return hasBaseType() ? ((SimpleType)getBaseType()).isStringType() : false;
	}
	
	
	private Object readResolve() throws ObjectStreamException {
		Object ret = this;
		if (XMLSchemaConstants.XSD_NSURI.equals(getNamespace())) {
			ret = getBuiltinType(getName());
		}
		return ret;
	}
}
