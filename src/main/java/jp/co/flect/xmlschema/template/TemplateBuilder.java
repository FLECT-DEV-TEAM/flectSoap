package jp.co.flect.xmlschema.template;

import jp.co.flect.xmlschema.XMLSchema;
import jp.co.flect.template.TemplateEngine;
import java.util.List;
import java.io.IOException;
import java.io.OutputStream;

/**
 * XMLSchemaからテンプレートを作成するInterfaceです。
 */
public interface TemplateBuilder extends Cloneable {
	
	/** スキーマ情報出力のNamespacePrefix */
	public static final String FSI_PREFIX    = "fsi";
	/** スキーマ情報出力のNamespace */
	public static final String FSI_NAMESPACE = "http://www.flect.co.jp/application/xmlschema";
	
	/** インデント幅を返します。0の場合はインデントしません。 */
	public int getIndent();
	/** インデント幅を設定します。 */
	public void setIndent(int n);
	
	/** XML宣言を出力するかどうかを返します。 */
	public boolean isOutputXMLDecl();
	/** XML宣言を出力するかどうかを設定します。 */
	public void setOutputXMLDecl(boolean b);
	
	/** 初期インデントレベルを返します。 */
	public int getInitialIndent();
	/** 初期インデントレベルを設定します。 */
	public void setInitialIndent(int n);
	
	/** 
	 * XMLSchemaを追加します。 
	 * @return schemaの名前空間に対応するprefix
	 */
	public String addSchema(XMLSchema schema);

	/** 
	 * NamespacePrefixを指定してXMLSchemaを追加します。 
	 * @return 引数のprefix
	 */
	public String addSchema(String prefix, XMLSchema schema);
	
	/** 追加されたXMLSchemaのListを返します。 */
	public List<XMLSchema> getSchemaList();
	
	/** テンプレート内にスキーマ情報を出力するかどうかを返します。 */
	public boolean isOutputSchemaInfo();
	/** テンプレート内にスキーマ情報を出力するかどうかを設定します。 */
	public void setOutputSchemaInfo(boolean b);
	
	/** テンプレート出力時に有効なNamespacePrefixとURLを追加します。 */
	public void addContextNamespace(String prefix, String namespace);
	
	/** テンプレート出力時に有効なNamespacePrefixを削除します。 */
	public void removeContextNamespace(String prefix);
	
	/** 
	 * テンプレートをOutputStreamに出力します。 
	 * @param namespace 出力対象要素のNamespaceURI
	 * @param name 出力対象要素のLocalName
	 * @param hints スキーマ解釈に使用するヒントのList。nullでも良い
	 * @param os 出力ストリーム
	 */
	public void writeTo(String namespace, String name, List<TemplateHint> hints, OutputStream os) throws IOException;
	
	/** TemplateEngineを返します。 */
	public TemplateEngine getTemplateEngine();
	
	public Object clone();
}
