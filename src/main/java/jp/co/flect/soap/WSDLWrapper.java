package jp.co.flect.soap;

import java.io.File;
import java.io.IOException;
import org.xml.sax.SAXException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Attr;
import jp.co.flect.xml.XMLUtils;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import javax.xml.namespace.QName;

public class WSDLWrapper {
	
	private static final String TARGETNAMESPACE = "targetNamespace";
	private static final String SERVICE         = "service";
	private static final String PORT            = "port";
	private static final String ADDRESS         = "address";
	private static final String PORTTYPE        = "portType";
	private static final String OPERATION       = "operation";
	private static final String NAME            = "name";
	private static final String LOCATION        = "location";
	private static final String MESSAGE         = "message";
	private static final String BINDING         = "binding";
	private static final String OUTPUT          = "output";
	private static final String INPUT           = "input";
	private static final String SOAPACTION      = "soapAction";
	private static final String HEADER          = "header";
	private static final String BODY            = "body";
	private static final String DOCUMENTATION   = "documentation";
	private static final String FAULT           = "fault";
	private static final String PART            = "part";
	private static final String PARTS           = "parts";
	private static final String ELEMENT         = "element";
	private static final String TYPES           = "types";
	
	private String XMLNS_SOAP = XMLUtils.XMLNS_WSDL_SOAP;
	private String XMLNS_WSDL = XMLUtils.XMLNS_WSDL;
	private String XMLNS_XSD  = XMLUtils.XMLNS_XSD;
	private boolean bSoap12 = false;
	
	private Document doc;
	//Cached value
	private String endpoint;
	private Map<String, Element> cache = new HashMap<String, Element>();
	
	public WSDLWrapper(File f) throws IOException, SAXException {
		this(XMLUtils.parse(f));
	}
	
	public WSDLWrapper(Document doc) {
		this.doc = doc;
		makeCache();
	}
	
	public Document getDocument() { return this.doc;}
	
	public boolean isSoap12() { return bSoap12;}
	public boolean isSoap11() { return !bSoap12;}
	
	public String getTargetNamespace() {
		return this.doc.getDocumentElement().getAttribute(TARGETNAMESPACE);
	}
	
	public String getEndpoint() throws InvalidWSDLException {
		if (this.endpoint != null) {
			return this.endpoint;
		}
		Element el = XMLUtils.getElementNS(this.doc.getDocumentElement(), XMLNS_WSDL, SERVICE);
		if (el == null) {
			elementNotFound(SERVICE);
		}
		el = XMLUtils.getElementNS(el, XMLNS_WSDL, PORT);
		if (el == null) {
			elementNotFound(PORT);
		}
		el = XMLUtils.getElementNS(el, XMLNS_SOAP, ADDRESS);
		if (el == null) {
			elementNotFound(ADDRESS);
		}
		this.endpoint = el.getAttribute(LOCATION);
		return this.endpoint;
	}
	
	public List<String> getOperationNames() throws InvalidWSDLException {
		//定義順で返すためにキャッシュは使用しない
		List<String> list = new ArrayList<String>();
		Element el = XMLUtils.getElementNS(this.doc.getDocumentElement(), XMLNS_WSDL, PORTTYPE);
		if (el == null) {
			elementNotFound(PORTTYPE);
		}
		
		Node node = el.getFirstChild();
		while (node != null) {
			if (XMLUtils.matchNS(node, XMLNS_WSDL, OPERATION)) {
				Element elOp = (Element)node;
				list.add(elOp.getAttribute(NAME));
			}
			node = node.getNextSibling();
		}
		return list;
	}
	
	public List<OperationDef> getOperations() throws InvalidWSDLException {
		List<String> names = getOperationNames();
		List<OperationDef> list = new ArrayList<OperationDef>(names.size());
		for (String name : names) {
			OperationDef op = createOperationDef(name);
			if (op != null) {
				list.add(op);
			}
		}
		return list;
	}
	
	private OperationDef createOperationDef(String name) {
		Element elOp = getOperationElement(name);
		Element elSoap = getSoapOperationElement(name);
		if (elOp == null || elSoap == null) {
			return null;
		}
		MessageDef input = createMessageDef(INPUT, false,
			XMLUtils.getElementNS(elOp, XMLNS_WSDL, INPUT),
			XMLUtils.getElementNS(elSoap, XMLNS_WSDL, INPUT));
		MessageDef output = createMessageDef(OUTPUT, false,
			XMLUtils.getElementNS(elOp, XMLNS_WSDL, OUTPUT),
			XMLUtils.getElementNS(elSoap, XMLNS_WSDL, OUTPUT));
		if (input == null || output == null) {
			return null;
		}
		OperationDef op = new OperationDef(name, input, output);
		Element elSoapAction = XMLUtils.getElementNS(elSoap, XMLNS_SOAP, BINDING);
		if (elSoapAction != null) {
			op.setSoapAction(elSoapAction.getAttribute(SOAPACTION));
		}
		Node node = elOp.getFirstChild();
		while (node != null) {
			if (node.getNodeType() == Node.ELEMENT_NODE && XMLNS_WSDL.equals(node.getNamespaceURI())) {
				Element el = (Element)node;
				if (FAULT.equals(el.getLocalName())) {
					String faultName = el.getAttribute(NAME);
					MessageDef fault = createMessageDef(faultName, true, el, getFaultElement(elSoap, faultName));
					if (fault != null) {
						op.addFault(fault);
					}
				} else if (DOCUMENTATION.equals(el.getLocalName())) {
					op.setDocumentation(el.getTextContent());
				}
			}
			node = node.getNextSibling();
		}
		return op;
	}
	
	private MessageDef createMessageDef(String name, boolean fault, Element opEl, Element soapEl) {
		if (opEl == null || soapEl == null) {
			return null;
		}
		Element elMsg = getMessageElement(opEl, opEl.getAttribute(MESSAGE));
		if (elMsg == null) {
			return null;
		}
		MessageDef msg = new MessageDef(name, fault);
		Node node = soapEl.getFirstChild();
		while (node != null) {
			if (node.getNodeType() == Node.ELEMENT_NODE && XMLNS_SOAP.equals(node.getNamespaceURI())) {
				Element el = (Element)node;
				if (HEADER.equals(el.getLocalName())) {
					Element elHeaderMsg = getMessageElement(el, el.getAttribute(MESSAGE));
					if (elHeaderMsg != null) {
						QName qname = getPartQName(elHeaderMsg, el.getAttribute(PART));
						if (qname != null) {
							msg.addHeader(qname);
						}
					}
				} else if (BODY.equals(el.getLocalName())) {
					if (el.hasAttribute(PARTS)) {
						String[] parts = el.getAttribute(PARTS).split(" ");
						for (int i=0; i<parts.length; i++) {
							QName qname = getPartQName(elMsg, parts[i]);
							if (qname != null) {
								msg.addBody(qname);
							}
						}
					} else {
						Node msgChild = elMsg.getFirstChild();
						while (msgChild != null) {
							if (XMLUtils.matchNS(msgChild, XMLNS_WSDL, PART)) {
								Element elPart = (Element)msgChild;
								msg.addBody(getQName(elPart, elPart.getAttribute(ELEMENT)));
							}
							msgChild = msgChild.getNextSibling();
						}
					}
				} else if (FAULT.equals(el.getLocalName())) {
					QName qname = getPartQName(elMsg, el.getAttribute(NAME));
					if (qname != null) {
						msg.addBody(qname);
					}
				}
			}
			node = node.getNextSibling();
		}
		return msg;
	}
	
	private void makeCache() {
		Element root = this.doc.getDocumentElement();
		Element portType = null;
		Element binding = null;
		
		//Check SOAP version
		NamedNodeMap attrs = root.getAttributes();
		String soapUri = null;
		for (int i=0; i<attrs.getLength(); i++) {
			Attr attr = (Attr)attrs.item(i);
			if ("xmlns".equals(attr.getPrefix())) {
				String value = attr.getNodeValue();
				if (XMLUtils.XMLNS_SOAP.equals(value)) {
					soapUri = value;
					break;
				} else if (XMLUtils.XMLNS_SOAP12.equals(value)) {
					soapUri = value;
					this.bSoap12 = true;
					break;
				}
			}
		}
		if (soapUri != null) {
			this.XMLNS_SOAP = soapUri;
		}
		//message
		Node node = root.getFirstChild();
		while (node != null) {
			if (node.getNodeType() == Node.ELEMENT_NODE && XMLNS_WSDL.equals(node.getNamespaceURI())) {
				Element el = (Element)node;
				String name = el.getLocalName();
				if (MESSAGE.equals(name)) {
					this.cache.put(MESSAGE + "." + el.getAttribute(NAME), el);
				} else if (PORTTYPE.equals(name)) {
					portType = el;
				} else if (BINDING.equals(name)) {
					binding = el;
				}
			}
			node = node.getNextSibling();
		}
		if (portType == null || binding == null) {
			return;//OperationDef作成不可
		}
		//operation
		node = portType.getFirstChild();
		while (node != null) {
			if (XMLUtils.matchNS(node, XMLNS_WSDL, OPERATION)) {
				Element el = (Element)node;
				this.cache.put(OPERATION + "." + el.getAttribute(NAME), el);
			}
			node = node.getNextSibling();
		}
		//soapBinding
		node = binding.getFirstChild();
		while (node != null) {
			if (XMLUtils.matchNS(node, XMLNS_WSDL, OPERATION)) {
				Element el = (Element)node;
				this.cache.put(BINDING + "." + el.getAttribute(NAME), el);
			}
			node = node.getNextSibling();
		}
	}
	
	public Element getTypesElement() throws InvalidWSDLException {
		Element el = XMLUtils.getElementNS(this.doc.getDocumentElement(), XMLNS_WSDL, TYPES);
		if (el == null) {
			elementNotFound(TYPES);
		}
		return el;
	}
	
	public Element getOperationElement(String name) {
		return this.cache.get(OPERATION + "." + name);
	}
	
	public Element getSoapOperationElement(String name) {
		return this.cache.get(BINDING + "." + name);
	}
	
	private Element getMessageElement(Element context, String name) {
		QName qname = getQName(context, name);
		return getMessageElement(qname.getNamespaceURI(), qname.getLocalPart());
	}
	
	public Element getMessageElement(String nsuri, String name) {
		if (!getTargetNamespace().equals(nsuri)) {
			//ToDo wsdl:import
			return null;
		}
		return this.cache.get(MESSAGE + "." + name);
	}
	
	private Element getFaultElement(Element parent, String name) {
		Node node = parent.getFirstChild();
		while (node != null) {
			if (XMLUtils.matchNS(node, XMLNS_WSDL, FAULT)) {
				Element elFault = (Element)node;
				if (name.equals(elFault.getAttribute(NAME))) {
					return elFault;
				}
			}
			node = node.getNextSibling();
		}
		return null;
	}
	
	private QName getPartQName(Element elMsg, String name) {
		Node node = elMsg.getFirstChild();
		while (node != null) {
			if (XMLUtils.matchNS(node, XMLNS_WSDL, PART)) {
				Element elPart = (Element)node;
				if (name.equals(elPart.getAttribute(NAME))) {
					return getQName(elPart, elPart.getAttribute(ELEMENT));
				}
			}
			node = node.getNextSibling();
		}
		return null;
	}
	
	private QName getQName(Element context, String qname) {
		String nsuri = null;
		String name = qname;
		int idx = qname.indexOf(':');
		if (idx != -1) {
			String prefix = qname.substring(0, idx);
			name = qname.substring(idx+1);
			nsuri = context.lookupNamespaceURI(prefix);
		}
		return new QName(nsuri, name);
	}
	
	private void elementNotFound(String name) throws InvalidWSDLException {
		throw new InvalidWSDLException(name + " element is not found");
	}
	
}
