package jp.co.flect.xmlschema.template;

import java.util.Map;
import java.util.List;
import jp.co.flect.xmlschema.ElementDef;

/**
 * テンプレート生成時にパラメータに含まれない要素を無視します。
 */
public class ParameterHint extends AbstractTemplateHint {
	
	public static final String NAME = "parameter";
	
	private Map map;
	
	public ParameterHint(Map map) {
		this.map = map;
	}
	
	public boolean isIgnoreElement(TemplateBuilderContext context, ElementDef el) {
		if (context.getContextSize() == 0 || el.getMinOccurs() > 0) {
			return false;
		}
		Map target = this.map;
		for (int i=context.getContextSize()-1; i>=0; i--) {
			ElementDef parent = context.getContextElement(i);
			if (parent.getMinOccurs() > 0) {
				return false;
			}
			String name = parent.getName();
			Object o = target.get(name);
			if (o == null || o instanceof String) {
				return true;
			} else if (o instanceof Map) {
				target = (Map)o;
			} else if (o instanceof List) {
				//Listの要素をすべて検証するのはコストがかかるためこの場合はtrueを返す
				return false;
			} else {
				throw new IllegalArgumentException(this.map.toString());
			}
		}
		return !target.containsKey(el.getName());
	}
	
	public String getAsString() {
		StringBuilder buf = new StringBuilder();
		buf.append(NAME)
			.append(":")
			.append(this.map);
		return buf.toString();
	}
}