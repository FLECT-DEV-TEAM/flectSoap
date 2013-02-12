package jp.co.flect.xmlschema.template;

import javax.xml.namespace.QName;
import jp.co.flect.xmlschema.ElementDef;
import jp.co.flect.xmlschema.Choice;
import jp.co.flect.xmlschema.TypeDef;
import jp.co.flect.xmlschema.SchemaDef;

/**
 * テンプレート生成時にヒントを与えます
 */
public abstract class AbstractTemplateHint implements TemplateHint {
	
	protected static boolean equals(QName qname, SchemaDef def) {
		if (qname.getNamespaceURI() == null) {
			return def.getNamespace() == null && qname.getLocalPart().equals(def.getName());
		}
		return qname.getNamespaceURI().equals(def.getNamespace()) && qname.getLocalPart().equals(def.getName());
	}
	
	public boolean isIgnoreElement(TemplateBuilderContext context, ElementDef el) {
		return false;
	}
	
	public TypeDef getDerivedType(TemplateBuilderContext context, ElementDef el) {
		return null;
	}
	
	public ElementDef getChoicedElement(TemplateBuilderContext context, ElementDef parent, Choice choice) {
		return null;
	}
}