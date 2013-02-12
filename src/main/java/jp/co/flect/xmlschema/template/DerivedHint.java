package jp.co.flect.xmlschema.template;

import javax.xml.namespace.QName;
import jp.co.flect.xmlschema.ElementDef;
import jp.co.flect.xmlschema.TypeDef;
import jp.co.flect.xmlschema.XMLSchema;
import java.io.Serializable;

/**
 * テンプレート生成時に派生型で置き換える型を指定します。
 */
public class DerivedHint extends AbstractTemplateHint implements Serializable {
	
	private static final long serialVersionUID = -324349743531660393L;

	public static final String NAME = "derived";
	
	private QName base;
	private QName derived;
	
	public DerivedHint(QName base, QName derived) {
		this.base = base;
		this.derived = derived;
	}
	
	public TypeDef getDerivedType(TemplateBuilderContext context, ElementDef el) {
		if (equals(this.base, el.getType())) {
			XMLSchema schema = el.getSchema().getRelatedSchema(this.derived.getNamespaceURI());
			if (schema == null) {
				throw new IllegalArgumentException("Schema not found : " + this.derived);
			}
			TypeDef ret = schema.getType(this.derived.getLocalPart());
			if (ret == null) {
				throw new IllegalArgumentException("Schema not found : " + this.derived);
			}
			return ret;
		}
		return null;
	}
	
	public String getAsString() {
		StringBuilder buf = new StringBuilder();
		buf.append(NAME)
			.append(":{")
			.append(base.getNamespaceURI())
			.append("}")
			.append(base.getLocalPart())
			.append(",{")
			.append(derived.getNamespaceURI())
			.append("}")
			.append(derived.getLocalPart());
		return buf.toString();
	}
}