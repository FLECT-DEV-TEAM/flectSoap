package jp.co.flect.xmlschema;

import jp.co.flect.xmlschema.type.StringType;
import jp.co.flect.xmlschema.type.DoubleType;
import jp.co.flect.xmlschema.type.IntType;
import jp.co.flect.xmlschema.type.BooleanType;
import jp.co.flect.xmlschema.type.DatetimeType;
import jp.co.flect.xmlschema.type.DateType;
import jp.co.flect.xmlschema.type.TimeType;
import jp.co.flect.xmlschema.type.DecimalType;
import jp.co.flect.xmlschema.type.Base64BinaryType;
import jp.co.flect.xmlschema.type.QNameType;
import jp.co.flect.xmlschema.type.AnyType;
import java.io.ObjectStreamException;

public class SimpleType extends TypeDef {
	
	private static final long serialVersionUID = 1505016885823600078L;
	private static final XMLSchema XSD = new XMLSchema(XMLSchemaConstants.XSD_NSURI, false, false);
	
	public static final SimpleType STRING       = new StringType(XSD, "string");
	public static final SimpleType DOUBLE       = new DoubleType(XSD, "double");
	public static final SimpleType DECIMAL      = new DecimalType(XSD, "decimal");
	public static final SimpleType INT          = new IntType(XSD, "int");
	public static final SimpleType BOOLEAN      = new BooleanType(XSD, "boolean");
	public static final SimpleType DATETIME     = new DatetimeType(XSD, "dateTime");
	public static final SimpleType TIME         = new TimeType(XSD, "time");
	public static final SimpleType DATE         = new DateType(XSD, "date");
	public static final SimpleType BASE64BINARY = new Base64BinaryType(XSD, "base64Binary");
	public static final SimpleType QNAME        = new QNameType(XSD, "QName");
	public static final SimpleType ANYTYPE      = new AnyType(XSD, "anyType");
	
	static {
		XSD.addType(STRING);
		XSD.addType(DOUBLE);
		XSD.addType(DECIMAL);
		XSD.addType(INT);
		XSD.addType(BOOLEAN);
		XSD.addType(DATETIME);
		XSD.addType(TIME);
		XSD.addType(DATE);
		XSD.addType(BASE64BINARY);
		XSD.addType(QNAME);
		XSD.addType(ANYTYPE);
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
