package jp.co.flect.xmlschema.template;

import jp.co.flect.xmlschema.ElementDef;
import jp.co.flect.xmlschema.Choice;
import jp.co.flect.xmlschema.TypeDef;

/**
 * テンプレート生成時にヒントを与えます
 */
public interface TemplateHint {
	
	/**
	 * 対象のElementを無視する場合はtrueを返します
	 */
	public boolean isIgnoreElement(TemplateBuilderContext context, ElementDef el);
	
	/**
	 * 対象のElementの型を派生型で置き換える場合はその型を返します。
	 */
	public TypeDef getDerivedType(TemplateBuilderContext context, ElementDef el);
	
	/**
	 * Choiceの中から選択するElementを返します。
	 */
	public ElementDef getChoicedElement(TemplateBuilderContext context, ElementDef parent, Choice choice);
	
	/**
	 * このヒントの文字列表現
	 */
	public String getAsString();
}