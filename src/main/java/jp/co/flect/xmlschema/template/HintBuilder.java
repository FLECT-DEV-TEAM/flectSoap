package jp.co.flect.xmlschema.template;

import javax.xml.namespace.QName;
import java.util.HashMap;
import java.util.Map;

/**
 * Hintの文字列からヒントを生成します。
 * 初期状態で対応しているクラスは以下です。
 * - IgnoreHint
 * - DerivedHint
 * - IgnoreZero
 */
public class HintBuilder {
	
	private static Map<String, Parser> MAP = new HashMap<String, Parser>();
	
	public static void registerParser(String name, Parser parser) {
		MAP.put(name, parser);
	}
	
	static {
		registerParser(IgnoreHint.NAME, new IgnoreParser());
		registerParser(DerivedHint.NAME, new DerivedParser());
		registerParser(IgnoreZeroHint.NAME, new IgnoreZeroParser());
	}
	
	public TemplateHint parse(String str) {
		int idx = str.indexOf(":");
		if (idx == -1) {
			return null;
		}
		String name = str.substring(0, idx);
		String value = str.substring(idx+1).trim();
		Parser parser = MAP.get(name);
		return parser == null ? null : parser.parse(value);
	}
	
	public interface Parser {
		
		public TemplateHint parse(String str);
		
	}
	
	private static QName parseQName(String str) {
		int idx1 = str.indexOf('{');
		int idx2 = str.indexOf('}');
		if (idx1 == -1 || idx2 == -1) {
			return null;
		}
		return new QName(str.substring(idx1+1, idx2).trim(), str.substring(idx2+1).trim());
	}
	
	private static class IgnoreParser implements Parser {
		
		public TemplateHint parse(String str) {
			QName qname = parseQName(str);
			return qname == null ? null : new IgnoreHint(qname);
		}
	}
	
	private static class DerivedParser implements Parser {
		
		public TemplateHint parse(String str) {
			String[] strs = str.split(",");
			if (strs.length != 2) {
				return null;
			}
			QName base = parseQName(strs[0].trim());
			QName derived = parseQName(strs[1].trim());
			return base != null && derived != null ? new DerivedHint(base, derived) : null;
		}
	}
	
	private static class IgnoreZeroParser implements Parser {
		
		public TemplateHint parse(String str) {
			return new IgnoreZeroHint();
		}
	}
	
}
