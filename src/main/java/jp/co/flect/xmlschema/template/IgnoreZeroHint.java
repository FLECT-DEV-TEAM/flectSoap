package jp.co.flect.xmlschema.template;

import jp.co.flect.xmlschema.ElementDef;
import java.io.Serializable;

/**
 * minOccurs="0"の要素を無視します。
 */
public class IgnoreZeroHint extends AbstractTemplateHint implements Serializable {
	
	private static final long serialVersionUID = 2550691860487041552L;
	
	public static final String NAME = "ignoreZero";
	
	public boolean isIgnoreElement(TemplateBuilderContext context, ElementDef el) {
		return el.getMinOccurs() == 0;
	}
	
	public String getAsString() { return NAME + ":";}
}