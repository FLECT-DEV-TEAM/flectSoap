package jp.co.flect.soap;

/**
 * ComplexTypeを介してやり取りするオブジェクトのインターフェース
 */
public interface TypedObject {
	
	/**
	 * WSDLで定義されたオブジェクトのNamespaceURI<br>
	 * ObjectNameがWSDL内のすべてのスキーマをまたいでユニークの場合はnullを返す実装となっていても構いません。
	 */
	public String getObjectNamespaceURI();
	
	/**
	 * WSDLで定義されたオブジェクト名
	 */
	public String getObjectName();
}
