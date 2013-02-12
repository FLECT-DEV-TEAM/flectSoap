package jp.co.flect.soap;

/**
 * SOAP処理で発生するException
 */
public class SoapException extends Exception {
	
	private static final long serialVersionUID = 1694028446739560889L;
	
	private String responseBody;
	
	public SoapException(String msg) {
		super(msg);
	}
	
	public SoapException(String msg, Exception e) {
		super(msg, e);
	}
	
	public SoapException(Exception e) {
		super(e);
	}
	
	/**
	 * エラーがHTTPレスポンスを受信した後に発生した場合そのBodyを返します。
	 */
	public String getResponseBody() { return this.responseBody;}
	
	/**
	 * エラーがHTTPレスポンスを受信した後に発生した場合そのBodyを設定します。
	 */
	public void setResponseBody(String s) { this.responseBody = s;}
}
