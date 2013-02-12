package jp.co.flect.xmlschema.template;

import javax.xml.namespace.QName;
import jp.co.flect.xmlschema.ElementDef;
import java.io.Serializable;

/**
 * テンプレート生成時に無視する要素を指定します。
 */
public class IgnoreHint extends AbstractTemplateHint implements Serializable {
	
	private static final long serialVersionUID = 6928934404327688779L;

	public static final String NAME = "ignore";
	
	private QName qname;
	
	public IgnoreHint(QName qname) {
		this.qname = qname;
	}
	
	public boolean isIgnoreElement(TemplateBuilderContext context, ElementDef el) {
		return equals(qname, el);
	}
	
	public String getAsString() {
		StringBuilder buf = new StringBuilder();
		buf.append(NAME)
			.append(":{")
			.append(qname.getNamespaceURI())
			.append("}")
			.append(qname.getLocalPart());
		return buf.toString();
	}
}