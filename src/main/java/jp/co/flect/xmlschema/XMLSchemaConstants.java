package jp.co.flect.xmlschema;

import jp.co.flect.xml.XMLUtils;

public interface XMLSchemaConstants {
	
	public static final String XSD_NSURI       = XMLUtils.XMLNS_XSD;
	
	//Element name
	public static final String SCHEMA          = "schema";
	public static final String ELEMENT         = "element";
	public static final String ATTRIBUTE       = "attribute";
	public static final String COMPLEX_CONTENT = "complexContent";
	public static final String COMPLEX_TYPE    = "complexType";
	public static final String EXTENSION       = "extension";
	public static final String RESTRICTION     = "restriction";
	public static final String SEQUENCE        = "sequence";
	public static final String SIMPLE_CONTENT  = "simpleContent";
	public static final String SIMPLE_TYPE     = "simpleType";
	public static final String PATTERN         = "pattern";
	public static final String LENGTH          = "length";
	public static final String ANY             = "any";
	public static final String IMPORT          = "import";
	public static final String ENUMERATION     = "enumeration";
	
	//Attribute name
	public static final String ID                     = "id";
	public static final String BASE                   = "base";
	public static final String NAME                   = "name";
	public static final String NAMESPACE              = "namespace";
	public static final String TARGET_NAMESPACE       = "targetNamespace";
	public static final String ELEMENT_FORM_DEFAULT   = "elementFormDefault";
	public static final String ATTRIBUTE_FORM_DEFAULT = "atributeFormDefault";
	public static final String MIN_OCCURS             = "minOccurs";
	public static final String MAX_OCCURS             = "maxOccurs";
	public static final String NILLABLE               = "nillable";
	public static final String REF                    = "ref";
	public static final String TYPE                   = "type";
	public static final String PROCESS_CONTENTS       = "processContents";
	
	//Value
	public static final String QUALIFIED              = "qualified";
	public static final String UNQUALIFIED            = "unqualified";
	public static final String UNBOUNDED              = "unbounded";

}
