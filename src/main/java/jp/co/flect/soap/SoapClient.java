package jp.co.flect.soap;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import javax.swing.event.EventListenerList;
import javax.xml.namespace.QName;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;

import jp.co.flect.log.Logger;
import jp.co.flect.log.LoggerFactory;
import jp.co.flect.net.ContentLengthExceedException;
import jp.co.flect.template.Template;
import jp.co.flect.template.TemplateException;
import jp.co.flect.soap.event.SoapFaultEvent;
import jp.co.flect.soap.event.SoapFaultListener;
import jp.co.flect.soap.event.SoapInvokeEvent;
import jp.co.flect.soap.event.SoapInvokeListener;
import jp.co.flect.util.ExtendedMap;
import jp.co.flect.net.HttpUtils;
import jp.co.flect.xml.XMLUtils;
import jp.co.flect.xml.XMLWriter;
import jp.co.flect.xmlschema.SimpleType;
import jp.co.flect.xmlschema.ComplexType;
import jp.co.flect.xmlschema.ElementDef;
import jp.co.flect.xmlschema.XMLSchema;
import jp.co.flect.xmlschema.XMLSchemaException;
import jp.co.flect.xmlschema.template.ParameterHint;
import jp.co.flect.xmlschema.template.TemplateBuilder;
import jp.co.flect.xmlschema.template.TemplateHint;
import jp.co.flect.xmlschema.template.VelocityTemplateBuilder;

/**
 * テンプレートベースのSOAPクライアント
 */
public class SoapClient implements Serializable {
	
	public static final String SOAP_PREFIX = "soap";
	public static final String XSD_PREFIX  = "xsd";
	public static final String XSI_PREFIX  = "xsi";
	
	private static final long serialVersionUID = -8821305948326815033L;
	
	private WSDL wsdl;
	private TemplateBuilder builder;
	private String endpoint;
	private Map<String, String> nsMap = new HashMap<String, String>();
	private String userAgent;
	
	private long lastInvokeElapsed;
	private long lastInvokedTime;
	private transient EventListenerList listeners = new EventListenerList();
	private int maxResponseSize;
	private Map<QName, Object> defaultMap;
	
	private ProxyInfo proxyInfo = null;
	
	private int soTimeout = 0;
	private int connectionTimeout = 0;
	private boolean alwaysCloseConnection = false;
	
	protected transient Logger log;
	
	/**
	 * Constructor
	 * @param wsdlFile WSDL file
	 */
	public SoapClient(File wsdlFile) throws IOException, SAXException, InvalidWSDLException, XMLSchemaException {
		this(wsdlFile, new VelocityTemplateBuilder());
	}
	
	/**
	 * Constructor
	 * @param doc document of WSDL
	 */
	public SoapClient(Document doc) throws InvalidWSDLException, XMLSchemaException {
		this(doc, new VelocityTemplateBuilder());
	}
	
	/**
	 * Constructor
	 * @param wsdl WSDL
	 */
	public SoapClient(WSDL wsdl) {
		this(wsdl, new VelocityTemplateBuilder());
	}
	
	/**
	 * コンストラクタ
	 * @param wsdlFile WSDLファイル
	 * @param builder テンプレートビルダー
	 */
	public SoapClient(File wsdlFile, TemplateBuilder builder) throws IOException, SAXException, InvalidWSDLException, XMLSchemaException {
		this(XMLUtils.parse(wsdlFile), builder);
	}
	
	/**
	 * コンストラクタ
	 * @param doc WSDLのDOMドキュメント
	 * @param builder テンプレートビルダー
	 */
	public SoapClient(Document doc, TemplateBuilder builder) throws InvalidWSDLException, XMLSchemaException {
		init(new WSDL(doc), builder, doc);
	}
	
	/**
	 * コンストラクタ
	 * @param wsdl WSDL
	 * @param builder テンプレートビルダー
	 */
	public SoapClient(WSDL wsdl, TemplateBuilder builder) {
		init(wsdl, builder, null);
	}
	
	private void init(WSDL wsdl, TemplateBuilder builder, Document doc) {
		this.log = LoggerFactory.getLogger(getClass());
		this.wsdl = wsdl;
		this.builder = builder;
		this.endpoint = wsdl.getEndpoint();
		
		String soapUri = wsdl.isSoap12() ? XMLUtils.XMLNS_SOAP12_ENVELOPE : XMLUtils.XMLNS_SOAP_ENVELOPE;
		
		this.nsMap.put(SOAP_PREFIX, soapUri);
		this.nsMap.put(XSD_PREFIX, XMLUtils.XMLNS_XSD);
		this.nsMap.put(XSI_PREFIX, XMLUtils.XMLNS_XSI);
		
		for (XMLSchema schema : wsdl.getSchemaList()) {
			String prefix = doc == null ? null : doc.getDocumentElement().lookupPrefix(schema.getTargetNamespace());
			if (prefix != null) {
				prefix = this.builder.addSchema(prefix, schema);
			} else {
				prefix = this.builder.addSchema(schema);
			}
			this.nsMap.put(prefix, schema.getTargetNamespace());
		}
	}
	
	/**
	 * コピーコンストラクタ<br>
	 * イベントリスナはコピーされません。
	 */
	public SoapClient(SoapClient client) {
		this.wsdl = client.wsdl;
		this.builder = (TemplateBuilder)client.builder.clone();
		this.endpoint = client.endpoint;
		this.nsMap.putAll(client.nsMap);
		this.log = client.log;
		this.soTimeout = client.soTimeout;
		this.connectionTimeout = client.connectionTimeout;
		this.alwaysCloseConnection = client.alwaysCloseConnection;
	}
	
	/** SO_TIMEOUTを返します。(ミリ秒単位) */
	public int getSoTimeout() { return this.soTimeout;}
	/** SO_TIMEOUTを設定します。(ミリ秒単位) */
	public void setSoTimeout(int n) { 
		this.soTimeout = n;
	}
	
	/** コネクションTIMEOUTを返します。(ミリ秒単位) */
	public int getConnectionTimeout() { return this.connectionTimeout;}
	/** コネクションTIMEOUTを設定します。(ミリ秒単位) */
	public void setConnectionTimeout(int n) { 
		this.connectionTimeout = n;
	}
	
	/** メソッド実行時にHttpヘッダに「Connection: close」を付加するかどうかを返します。 */
	public boolean isAlwaysCloseConnection() { return this.alwaysCloseConnection;}
	/** コネクションTIMEOUTを設定します。(ミリ秒単位) */
	/** メソッド実行時にHttpヘッダに「Connection: close」を付加するかどうかを設定します。 */
	public void setAlwaysCloseConnection(boolean b) { this.alwaysCloseConnection = b;}
	
	public Logger getLogger() { return this.log;}
	
	/**
	 * WSDLを返します
	 */
	public WSDL getWSDL() { return this.wsdl;}
	
	/**
	 * デバッグモードであるかどうかを返します。
	 */
	public boolean isDebug() { return this.builder.isOutputSchemaInfo();}
	
	/**
	 * デバッグモードであるかどうかを設定します。
	 * デバッグモードでは以下の動作になります。
	 * - 生成するテンプレートにスキーマ情報が付加されます。
	 */
	public void setDebug(boolean b) { 
		this.builder.setOutputSchemaInfo(b);
		if (b) {
			this.builder.addContextNamespace(TemplateBuilder.FSI_PREFIX, TemplateBuilder.FSI_NAMESPACE);
		} else {
			this.builder.removeContextNamespace(TemplateBuilder.FSI_NAMESPACE);
		}
	}
	
	public void addDefault(QName qname, Object value) {
		if (this.defaultMap == null) {
			this.defaultMap = new HashMap<QName, Object>();
		}
		this.defaultMap.put(qname, value);
	}
	
	/**
	 * 許容するレスポンスのContent-Lengthの最大値を返します
	 */
	public int getMaxResponseSize() { return this.maxResponseSize;}
	
	/**
	 * 許容するレスポンスのContent-Lengthの最大値を設定します
	 */
	public void setMaxResponseSize(int n) { this.maxResponseSize = n;}
	
	/**
	 * 最後に実行したメソッドの通信にかかった時間<br>
	 * テンプレート生成など通信以外の時間は含まれません。
	 */
	public long getLastInvokeElapsed() { return this.lastInvokeElapsed;}
	
	/**
	 * 最後にメソッドを実行した日時
	 */
	public Date getLastInvokedTime() { return new Date(this.lastInvokedTime);}
	
	/**
	 * TemplateBuilderを返します。
	 */
	public TemplateBuilder getTemplateBuilder() { return this.builder;}
	
	/**
	 * SOAPのendpointを返します。
	 */
	public String getEndpoint() { return this.endpoint;}
	
	/**
	 * SOAPのendpointを設定します。
	 */
	public void setEndpoint(String s) { this.endpoint = s;}
	
	/**
	 * HTTPヘッダのUser-Agetを返します。
	 */
	public String getUserAgent() { return this.userAgent;}
	
	/**
	 * HTTPヘッダのUser-Agetを設定します。
	 */
	public void setUserAgent(String s) { this.userAgent = s;}
	
	/**
	 * SoapFaultListenerを追加します。
	 */
	public void addSoapFaultListener(SoapFaultListener l) {
		this.listeners.add(SoapFaultListener.class, l);
	}
	
	/**
	 * SoapFaultListenerを削除します。
	 */
	public void removeSoapFaultListener(SoapFaultListener l) {
		this.listeners.remove(SoapFaultListener.class, l);
	}
	
	/**
	 * 設定されているSoapFaultListenerを取得します。
	 */
	public SoapFaultListener[] getSoapFaultListeners() {
		return this.listeners.getListeners(SoapFaultListener.class);
	}
	
	/**
	 * SoapInvokeListenerを追加します。
	 */
	public void addSoapInvokeListener(SoapInvokeListener l) {
		this.listeners.add(SoapInvokeListener.class, l);
	}
	
	/**
	 * SoapInvokeListenerを削除します。
	 */
	public void removeSoapInvokeListener(SoapInvokeListener l) {
		this.listeners.remove(SoapInvokeListener.class, l);
	}
	
	/**
	 * 設定されているSoapInvokeListenerを取得します。
	 */
	public SoapInvokeListener[] getSoapInvokeListeners() {
		return this.listeners.getListeners(SoapInvokeListener.class);
	}
	
	/**
	 * オペレーション定義を取得します
	 */
	public OperationDef getOperation(String name) { 
		return this.wsdl.getOperation(name);
	}
	
	/**
	 * テンプレートを生成します。
	 * @param name オペレーション名
	 * @param hints テンプレート生成に使用するヒントのリスト
	 */
	public SoapTemplate generateTemplate(String name, List<TemplateHint> hints) throws IOException {
		long t = System.currentTimeMillis();
		log.debug("generateTemplate start: name={0}", name);
		OperationDef op = getOperation(name);
		if (op == null) {
			return null;
		}
		MessageDef msg = op.getRequestMessage();
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		try {
			XMLWriter writer = new XMLWriter(os, "utf-8", this.builder.getIndent());
			this.builder.setOutputXMLDecl(false);
			
			writer.xmlDecl();
			writer.indent(false);
			
			writer.openElement(SOAP_PREFIX + ":Envelope");
			writer.attr("xmlns:" + SOAP_PREFIX, this.nsMap.get(SOAP_PREFIX));
			writer.attr("xmlns:" + XSD_PREFIX, this.nsMap.get(XSD_PREFIX));
			writer.attr("xmlns:" + XSI_PREFIX, this.nsMap.get(XSI_PREFIX));
			if (isDebug()) {
				writer.attr("xmlns:" + TemplateBuilder.FSI_PREFIX, TemplateBuilder.FSI_NAMESPACE);
			}
			writer.endTag();
			writer.indent(true);
			if (msg.getHeaderCount() > 0) {
				writer.openElement(SOAP_PREFIX+ ":Header");
				writer.endTag();
				
				writer.flush();
				Iterator<QName> it = msg.getHeaders();
				while (it.hasNext()) {
					QName qname = it.next();
					this.builder.setInitialIndent(writer.getIndentLevel() + 1);
					this.builder.writeTo(qname.getNamespaceURI(), qname.getLocalPart(), hints, os);
				}
				writer.indent(false);
				writer.endElement(SOAP_PREFIX + ":Header");
				writer.indent(false);
			}
			writer.openElement(SOAP_PREFIX + ":Body");
			writer.endTag();
			writer.flush();
			
			Iterator<QName> it = msg.getBodies();
			while (it.hasNext()) {
				QName qname = it.next();
				this.builder.setInitialIndent(writer.getIndentLevel() + 1);
				this.builder.writeTo(qname.getNamespaceURI(), qname.getLocalPart(), hints, os);
			}
			writer.indent(false);
			writer.endElement(SOAP_PREFIX + ":Body");
			writer.unindent();
			writer.endElement(SOAP_PREFIX + ":Envelope");
			writer.flush();
		} finally {
			os.close();
		}
		log.debug("generateTemplate generate: name={0}, time={1}ms", name, System.currentTimeMillis() - t);
		
		SoapTemplate ret = new SoapTemplate(name, new String(os.toByteArray(), "utf-8"), hints);
		try {
			ret.setTemplate(this.builder.getTemplateEngine().createTemplate(ret.getTemplateString()));
			log.debug("generateTemplate compile: name={0}, time={1}ms", name, System.currentTimeMillis() - t);
		} catch (TemplateException e) {
			//制限事項以外はTemplateBuilderのバグ
			if (!e.isLimitation()) {
				throw new IllegalStateException(e);
			}
		}
		log.trace("generateTemplate result:\n{0}", ret.getTemplateString());
		return ret;
	}
	
	/**
	 * リクエストを生成します。
	 * @param template テンプレート
	 * @param params パラメータ
	 */
	public String generateRequest(SoapTemplate template, Map params) throws TemplateException, IOException {
		long t = System.currentTimeMillis();
		log.debug("generateRequest start: name={0}", template.getName());
		log.trace(template.getTemplateString());
		
		OperationDef op = getOperation(template.getName());
		if (op == null) {
			throw new IllegalArgumentException("Unknown operation : " + template.getName());
		}
		MessageHelper helper = new MessageHelper(this.wsdl, op.getRequestMessage(), template.getHints());
		Template impl = template.getTemplate();
		if (impl == null) {
			impl = this.builder.getTemplateEngine().createTemplate(template.getTemplateString());
			template.setTemplate(impl);
		}
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		OutputStreamWriter writer = new OutputStreamWriter(os, "utf-8");
		impl.merge(new SoapParams(helper, null, params, null, false), writer);
		writer.close();
		
		String body = new String(os.toByteArray(), "utf-8");
		log.debug("generateRequest request-origin: name={0}, time={1}ms", template.getName(), System.currentTimeMillis() - t);
		log.trace(body);
		
		body = helper.normalize(body);
		log.debug("generateRequest request-strip: name={0}, time={1}ms", template.getName(), System.currentTimeMillis() - t);
		log.trace(body);
		
		return body;
	}
	
	private HttpClient createHttpClient() {
		BasicHttpParams params = new BasicHttpParams();
		HttpConnectionParams.setConnectionTimeout(params, this.connectionTimeout);
		HttpConnectionParams.setSoTimeout(params, this.soTimeout);
		
		DefaultHttpClient client = new DefaultHttpClient(params);
		if (this.proxyInfo != null) {
			HttpHost proxy = new HttpHost(proxyInfo.getHost(), proxyInfo.getPort());
			client.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);
			if (proxyInfo.getUserName() != null && proxyInfo.getPassword() != null) {
				client.getCredentialsProvider().setCredentials(
					new AuthScope(proxyInfo.getHost(), proxyInfo.getPort()),
					new UsernamePasswordCredentials(proxyInfo.getUserName(), proxyInfo.getPassword()));
			}
		}
		return client;
	}
	
	public SoapResponse send(String soapAction, String op, String msg, HttpResponseHandler handler) throws IOException, SoapException {
		if (handler == null) {
			handler = new DefaultHandler(soapAction, op);
		}
		long t = System.currentTimeMillis();
		log.debug("send start: soapAction={0}", soapAction);
		
		SoapInvokeEvent event = new SoapInvokeEvent(this, soapAction, op, msg);
		fireSoapInvoke(event, true);
		msg = event.getRequest();
		
		HttpClient client = createHttpClient();
		HttpPost post = new HttpPost(this.endpoint);
		post.addHeader("SOAPAction", "\"" + soapAction + "\"");
		if (this.userAgent != null) {
			post.addHeader("User-Agent", this.userAgent);
		}
		if (this.alwaysCloseConnection) {
			post.addHeader("Connection", "close");
		}
		log.trace("send request:\n{0}", msg);
		String contentType = this.wsdl.isSoap12() ? "application/soap+xml" : "text/xml";
		HttpEntity entity = new StringEntity(msg, contentType, "utf-8");
		post.setEntity(entity);
		long ct = System.currentTimeMillis();
		HttpResponse res = null;
		try {
			res = client.execute(post);
		} finally {
			this.lastInvokeElapsed = System.currentTimeMillis() - ct;
			this.lastInvokedTime = ct;
		}
		try {
			try {
				SoapResponse ret = handler.handleResponse(res);
				event.setResponse(ret);
				return ret;
			} catch (SoapFaultException e) {
				fireSoapFault(e);
				throw e;
			}
		} catch (IOException e) {
			event.setException(e);
			throw e;
		} catch (SoapException e) {
			event.setException(e);
			throw e;
		} finally {
			fireSoapInvoke(event, false);
		}
	}
	
	/**
	 * SOAPリクエストを送信します。
	 * @param soapAction SOAPActionヘッダの値
	 * @param msg 完全なSOAPリクエストメッセージ
	 */
	public SoapResponse send(String soapAction, String op, String msg) throws IOException, SoapException {
		return send(soapAction, op, msg, null);
	}
	
	/**
	 * テンプレートに値を埋めてSOAPリクエストを実行します。
	 * @param template テンプレート
	 * @param params パラメータのマップ
	 */
	public SoapResponse invoke(SoapTemplate template, Map params) throws IOException, TemplateException, SoapException {
		return invoke(template, params, null);
	}
	
	public SoapResponse invoke(SoapTemplate template, Map params, HttpResponseHandler h) throws IOException, TemplateException, SoapException {
		String body = generateRequest(template, params);
		OperationDef op = getOperation(template.getName());
		return send(op.getSoapAction(), op.getName(), body, h);
	}
	
	/**
	 * テンプレートを生成してそこ値を埋めてSOAPリクエストを実行します。
	 * @param name オペレーション名
	 * @param hints テンプレート生成に使用するヒントのリスト
	 * @param params パラメータのマップ
	 */
	public SoapResponse invoke(String op, List<TemplateHint> hints, Map params) throws IOException, TemplateException, SoapException {
		return invoke(op, hints, params, null);
	}
	
	public SoapResponse invoke(String op, List<TemplateHint> hints, Map params, HttpResponseHandler h) throws IOException, TemplateException, SoapException {
		if (params != null && params.size() > 0) {
			if (hints == null) {
				hints = new ArrayList<TemplateHint>();
			}
			hints.add(new ParameterHint(params));
		}
		SoapTemplate template = generateTemplate(op, hints);
		return invoke(template, params, h);
	}
	
	private void fireSoapInvoke(SoapInvokeEvent event, boolean before) {
		SoapInvokeListener[] ls = this.listeners.getListeners(SoapInvokeListener.class);
		if (ls == null || ls.length == 0) {
			return;
		}
		for (int i=0; i<ls.length; i++) {
			if (before) {
				ls[i].beforeInvoke(event);
			} else {
				ls[i].afterInvoke(event);
			}
		}
	}
	
	private void fireSoapFault(SoapFaultException e) {
		SoapFaultListener[] ls = this.listeners.getListeners(SoapFaultListener.class);
		if (ls == null || ls.length == 0) {
			return;
		}
		
		SoapFaultEvent event = new SoapFaultEvent(this, e);
		for (int i=0; i<ls.length; i++) {
			ls[i].soapFault(event);
		}
	}
	
	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
		in.defaultReadObject();
		this.listeners = new EventListenerList();
		this.log = LoggerFactory.getLogger(getClass());
	}
	
	/**
	 * マップの値がnullの場合は補完し、適切な型でない場合は型の変換を行うクラス
	 */
	private class SoapParams extends HashMap {
		
		private static final long serialVersionUID = 5369073919897446704L;
		
		private MessageHelper helper;
		private String parent;
		private SoapParams parentParams;
		private boolean autoGenerate;
		
		public SoapParams(MessageHelper helper, String parent, Map map, SoapParams parentParams, boolean autoGenerate) {
			this.helper = helper;
			this.parent = parent;
			this.parentParams = parentParams;
			this.autoGenerate = autoGenerate;
			if (map != null) {
				Iterator it = map.entrySet().iterator();
				while (it.hasNext()) {
					Map.Entry entry = (Map.Entry)it.next();
					Object key = entry.getKey();
					Object value = entry.getValue();
					if (value instanceof TypedObject) {
						String contextName = getContextName(key);
						ElementDef el = this.helper.getElementByPath(contextName);
						if (el == null || el.getType().isSimpleType()) {
							throw new IllegalArgumentException("Unknown object: " + value.getClass().getName());
						}
						TypedObjectConverter converter = ((ComplexType)el.getType()).getTypedObjectConverter();
						if (converter == null) {
							throw new IllegalArgumentException("Unknown object: " + value.getClass().getName());
						}
						value = converter.toMap((TypedObject)value);
					}
					if (value instanceof Map) {
						value = new SoapParams(helper, getContextName(key), (Map)value, this, false);
					} else if (value instanceof List) {
						List oldList = (List)value;
						List newList = new ArrayList(oldList.size());
						for (Object listValue : oldList) {
							if (listValue instanceof Map) {
								listValue = new SoapParams(helper, getContextName(key), (Map)listValue, this, false);
							}
							newList.add(listValue);
						}
						value = newList;
					} else if (!(value instanceof String)) {
						String contextName = getContextName(key);
						ElementDef el = this.helper.getElementByPath(contextName);
						if (el != null && el.getType().isSimpleType()) {
							value = ((SimpleType)el.getType()).format(value);
						}
					}
					put(key, value);
				}
			}
		}
		
		private String getContextName(Object name) {
			return parent == null ? name.toString() : parent + "." + name;
		}
		
		@Override
		public Object get(Object key) { 
			Object ret = doGet(key);
			return ret;
		}
		
		private Object doGet(Object key) {
			Object o = super.get(key);
			String contextName = getContextName(key);
			ElementDef el = this.helper.getElementByPath(contextName);
			if (el == null) {
				return o;
			}
			if (o == null) {
				if (el.hasOccurs()) {
					return Collections.EMPTY_LIST;
				}
				if (el.getType().isSimpleType()) {
					if (SoapClient.this.defaultMap != null) {
						o = defaultMap.get(el.getQName());
						if (o != null) {
							return o;
						}
						if (isGenerateTypeDefault(el)) {
							o = defaultMap.get(el.getType().getQName());
							if (o != null) {
								return o;
							}
						}
					}
					return "";
				}
				SoapParams params = new SoapParams(this.helper, contextName, null, this, true);
				put(key, params);
				return params;
			} else if (el.hasOccurs() && !(o instanceof List)) {
				List list = new ArrayList();
				list.add(o);
				put(key, list);
				return list;
			} else {
				return o;
			}
		}
		
		private boolean isGenerateTypeDefault(ElementDef el) {
			if (el.getMinOccurs() == 0) {
				return false;
			}
			if (!this.autoGenerate) {
				return true;
			}
			if (this.parentParams != null) {
				return this.parentParams.isGenerateTypeDefault(this.helper.getElementByPath(this.parent));
			}
			//not occur
			throw new IllegalStateException(el.getName() + ", " + parent);
		}
	}
	
	public interface HttpResponseHandler {
		public SoapResponse handleResponse(HttpResponse res) throws IOException, SoapException;
	}
	
	protected class DefaultHandler implements HttpResponseHandler {
		
		private String soapAction;
		private String op;
		
		public DefaultHandler(String soapAction, String op) {
			this.soapAction = soapAction;
			this.op = op;
		}
		
		public SoapResponse handleResponse(HttpResponse httpResponse) throws IOException, SoapException {
			long ct = System.currentTimeMillis();
			String responseBody = null;
			try {
				responseBody = HttpUtils.getContent(httpResponse, SoapClient.this.maxResponseSize);
			} catch (ContentLengthExceedException e) {
				throw e.setUrlOrOperation(this.op);
			}
			int code = httpResponse.getStatusLine().getStatusCode();
			OperationDef opDef = getOperation(this.op);
			SoapResponse ret = new SoapResponse(code, getWSDL(), opDef == null ? null : opDef.getResponseMessage(), responseBody);
			
			log.debug("send end: soapAction={0}, statusLine={1}, time={2}ms", this.soapAction, httpResponse.getStatusLine(), System.currentTimeMillis() - ct);
			if (log.isTraceEnabled()) {
				log.trace(httpResponse.getStatusLine());
				for (Header h : httpResponse.getAllHeaders()) {
					log.trace(h.toString());
				}
				log.trace("");
				log.trace(responseBody);
			}
			if (HttpUtils.isResponseOk(code)) {
				return ret;
			}
			
			//エラー処理
			SoapException ex = null;
			try {
				ExtendedMap map = ret.getAsMap();
				if (map.get("Fault") != null) {
					ex = new SoapFaultException(ret);
				} else {
					ex = new SoapException(httpResponse.getStatusLine().toString());
				}
			} catch (SoapException e) {
				ex = e;
			}
			ex.setResponseBody(responseBody);
			throw ex;
		}
	}
	
	public void setProxyInfo(String host, int port) {
		setProxyInfo(host, port, null, null);
	}
	
	public void setProxyInfo(String host, int port, String username, String password) {
		this.proxyInfo = new ProxyInfo(host, port, username, password);
	}
	
	public ProxyInfo getProxyInfo() { return this.proxyInfo;}
	
	public static class ProxyInfo implements Serializable {
		
		private static final long serialVersionUID = 994208152358025738L;
		
		private String host;
		private int port;
		private String username;
		private String password;
		
		public ProxyInfo(String host, int port) {
			this(host, port, null, null);
		}
		
		public ProxyInfo(String host, int port, String username, String password) {
			this.host = host;
			this.port = port;
			this.username = username;
			this.password = password;
		}
		
		public String getHost() { return this.host;}
		public int getPort() { return this.port;}
		public String getUserName() { return this.username;}
		public String getPassword() { return this.password;}
		
	}
	
}
