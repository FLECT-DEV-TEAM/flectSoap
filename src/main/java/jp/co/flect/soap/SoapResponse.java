package jp.co.flect.soap;

import org.w3c.dom.Document;
import java.io.IOException;
import java.io.StringReader;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedList;
import java.util.Iterator;
import org.xml.sax.SAXException;
import org.xml.sax.InputSource;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;

import jp.co.flect.xml.XMLUtils;
import jp.co.flect.xmlschema.ComplexType;
import jp.co.flect.xmlschema.ElementDef;
import jp.co.flect.xmlschema.SimpleType;
import jp.co.flect.xmlschema.TypeDef;
import jp.co.flect.util.ExtendedMap;

public class SoapResponse implements Serializable {
	
	private static final long serialVersionUID = 3160385644758030538L;
	
	private int responseCode;
	private WSDL wsdl;
	private MessageDef msgDef;
	private String body;
	private transient ExtendedMap map;
	private transient Document doc;
	private transient ExtendedMap objectMap;
	private Boolean bSoap12 = null;
	
	public SoapResponse(int responseCode, WSDL wsdl, MessageDef msgDef, String body) {
		this.responseCode = responseCode;
		this.wsdl = wsdl;
		this.msgDef = msgDef;
		this.body = body;
	}
	
	public int getResponseCode() { return this.responseCode;}
	
	public String getAsString() { return this.body;}
	
	public ExtendedMap getAsMap() throws SoapException { 
		if (this.map == null) {
			this.map = buildMap();
		}
		return this.map;
	}
	
	public Document getAsDocument() throws SoapException {
		if (this.doc == null) {
			try {
				this.doc = XMLUtils.newDocumentBuilder(false, false)
					.parse(new InputSource(new StringReader(this.body)));
			} catch (IOException e) {
				//not occur
				e.printStackTrace();
			} catch (SAXException e) {
				throw new SoapException(e);
			}
		}
		return this.doc;
	}
	
	public boolean isSoap12() { 
		if (bSoap12 == null) {
			try {
				getAsMap();
			} catch (SoapException e) {
				e.printStackTrace();
			}
		}
		return bSoap12.booleanValue();
	}
	
	public boolean isSoap11() { return !isSoap12();}
	
	private ExtendedMap buildMap() throws SoapException {
		ExtendedMap map = new ExtendedMap(false);
		
		boolean startData = false;
		boolean hasAttr = false;
		String soapUri = null;
		ParseContext context = new ParseContext(map);
		
		XMLInputFactory factory = XMLInputFactory.newInstance();
		factory.setProperty(XMLInputFactory. IS_COALESCING, Boolean.TRUE);
		try {
			XMLStreamReader reader = factory.createXMLStreamReader(new StringReader(this.body));
			while (reader.hasNext()) {
				int event = reader.next();
				switch (event) {
					case XMLStreamReader.START_ELEMENT:
						if (soapUri == null) {
							soapUri = reader.getNamespaceURI();
							if (XMLUtils.XMLNS_SOAP12_ENVELOPE.equals(soapUri)) {
								this.bSoap12 = Boolean.TRUE;
							} else if (!XMLUtils.XMLNS_SOAP_ENVELOPE.equals(soapUri)) {
								this.bSoap12 = Boolean.FALSE;
								throw new SoapException("Invalid soap namespace: " + soapUri);
							}
						}
						String name = reader.getLocalName();
						hasAttr = false;
						if (startData) {
							context.startElement(name);
							for (int i=0; i<reader.getAttributeCount(); i++) {
								String auri = reader.getAttributeNamespace(i);
								String aname = reader.getAttributeLocalName(i);
								String avalue = reader.getAttributeValue(i);
								if (XMLUtils.XMLNS_XSI.equals(auri) && "nil".equals(aname)) {
									continue;
								}
								context.attr(aname, avalue);
								hasAttr = true;
							}
						} else {
							String nsuri = reader.getNamespaceURI();
							if (soapUri.equals(nsuri) && ("Body".equals(name) || "Header".equals(name))) {
								startData = true;
							}
						}
						break;
					case XMLStreamReader.END_ELEMENT:
						if (startData) {
							startData = context.endElement();
						}
						break;
					case XMLStreamReader.CHARACTERS:
					case XMLStreamReader.CDATA:
						if (startData && !reader.isWhiteSpace()) {
							String value = reader.getText();
							context.content(value, hasAttr);
						}
						break;
					case XMLStreamReader.END_DOCUMENT:
						reader.close();
						break;
					case XMLStreamReader.START_DOCUMENT:
					case XMLStreamReader.ATTRIBUTE:
					case XMLStreamReader.NAMESPACE:
					case XMLStreamReader.SPACE:
					case XMLStreamReader.COMMENT:
					case XMLStreamReader.PROCESSING_INSTRUCTION:
					case XMLStreamReader.ENTITY_REFERENCE:
					case XMLStreamReader.DTD:
						break;
				}
			}
			return map;
		} catch (XMLStreamException e) {
			throw new SoapException(e);
		}
	}
	
	private static class ParseContext {
		
		private ExtendedMap root;
		private String prevName = null;
		private boolean prevProcessed = false;
		
		private LinkedList<ExtendedMap> mapStack = new LinkedList<ExtendedMap>();
		
		public ParseContext(ExtendedMap map) {
			this.root = map;
		}
		
		public ExtendedMap getTarget() {
			return mapStack.size() > 0 ? mapStack.peek() : this.root;
		}
		
		private ExtendedMap processPrev() {
			ExtendedMap map = getTarget();
			ExtendedMap child = new ExtendedMap();
			mapStack.push(child);
			
			Object o = map.get(this.prevName);
			if (o != null) {
				if (o instanceof List) {
					List<ExtendedMap> list = (List<ExtendedMap>)o;
					list.add(child);
				} else if (o instanceof ExtendedMap) {
					List<ExtendedMap> list = new ArrayList<ExtendedMap>();
					list.add((ExtendedMap)o);
					list.add(child);
					map.put(this.prevName, list);
				} else {
					throw new IllegalStateException();
				}
			} else {
				map.put(this.prevName, child);
			}
			this.prevProcessed = true;
			return child;
		}
		
		public void startElement(String name) {
			if (this.prevName != null && !this.prevProcessed) {
				processPrev();
			} 
			prevName = name;
			this.prevProcessed = false;
		}
		
		public void attr(String name, String value) {
			ExtendedMap map = this.prevProcessed ? getTarget() : processPrev();
			map.put(name, value);
		}
		
		public void content(String value, boolean hasAttr) {
			ExtendedMap map = getTarget();
			String name = hasAttr ? "content" : prevName;
			Object o = map.get(name);
			if (o != null) {
				if (o instanceof List) {
					List<String> list = (List<String>)o;
					list.add(value);
				} else if (o instanceof String) {
					List<String> list = new ArrayList<String>();
					list.add((String)o);
					list.add(value);
					map.put(name, list);
				}
			} else {
				map.put(name, value);
			}
		}
		
		public boolean endElement() {
			if (this.prevProcessed) {
				mapStack.pop();
			}
			this.prevProcessed = true;
			this.prevName = null;
			return mapStack.size() > 0;
		}
	}
	
	//TypedObject
	public <T extends TypedObject> T getAsObject(Class<T> clazz) throws SoapException {
		return getAsObjectMap().searchClass(clazz);
	}
	
	public ExtendedMap getAsObjectMap() throws SoapException {
		if (this.objectMap == null) {
			this.objectMap = buildObjectMap();
		}
		return this.objectMap;
	}
	
	private ExtendedMap buildObjectMap() throws SoapException {
		ExtendedMap map = new ExtendedMap(false);
		if (this.msgDef == null) {
			throw new IllegalArgumentException("Message doesn't defined.");
		}
		
		String soapUri = null;
		boolean startData = false;
		XMLInputFactory factory = XMLInputFactory.newInstance();
		factory.setProperty(XMLInputFactory. IS_COALESCING, Boolean.TRUE);
		try {
			XMLStreamReader reader = factory.createXMLStreamReader(new StringReader(this.body));
			while (reader.hasNext()) {
				int event = reader.next();
				switch (event) {
					case XMLStreamReader.START_ELEMENT:
					{
						String nsuri = reader.getNamespaceURI();
						String name = reader.getLocalName();
						if (soapUri == null) {
							if (XMLUtils.XMLNS_SOAP12_ENVELOPE.equals(nsuri)) {
								this.bSoap12 = Boolean.TRUE;
							} else if (!XMLUtils.XMLNS_SOAP_ENVELOPE.equals(nsuri)) {
								this.bSoap12 = Boolean.FALSE;
								throw new SoapException("Invalid soap namespace: " + nsuri);
							}
							soapUri = nsuri;
						}
						if (startData) {
							ElementDef el = null;
							Iterator<QName> it = this.msgDef.getBodies();
							while (it.hasNext()) {
								QName qname = it.next();
								if (nsuri.equals(qname.getNamespaceURI()) && name.equals(qname.getLocalPart())) {
									el = this.wsdl.getElement(nsuri, name);
									break;
								}
							}
							processElement(map, nsuri, name, el, reader);
						} else if (soapUri.equals(nsuri) && ("Body".equals(name) || "Header".equals(name))) {
							startData = true;
						}
						break;
					}
					case XMLStreamReader.CHARACTERS:
					case XMLStreamReader.CDATA:
						if (startData && !reader.isWhiteSpace()) {
							throw new IllegalStateException();
						}
						break;
					case XMLStreamReader.END_DOCUMENT:
						reader.close();
						break;
					case XMLStreamReader.END_ELEMENT:
					{
						String nsuri = reader.getNamespaceURI();
						String name = reader.getLocalName();
						if (startData &&  nsuri.equals(soapUri) && ("Body".equals(name) || "Header".equals(name))) {
							startData = false;
						}
					}
					case XMLStreamReader.START_DOCUMENT:
					case XMLStreamReader.ATTRIBUTE:
					case XMLStreamReader.NAMESPACE:
					case XMLStreamReader.SPACE:
					case XMLStreamReader.COMMENT:
					case XMLStreamReader.PROCESSING_INSTRUCTION:
					case XMLStreamReader.ENTITY_REFERENCE:
					case XMLStreamReader.DTD:
						break;
				}
			}
			return map;
		} catch (XMLStreamException e) {
			throw new SoapException(e);
		}
	}
	
	private Object parseSimple(SimpleType type, XMLStreamReader reader) throws XMLStreamException {
		StringBuilder buf = new StringBuilder();
		while (reader.hasNext()) {
			int event = reader.next();
			switch (event) {
				case XMLStreamReader.CHARACTERS:
				case XMLStreamReader.CDATA:
					buf.append(reader.getText());
					break;
				case XMLStreamReader.END_ELEMENT:
					return buf.length() > 0 ? type.parse(buf.toString()) : null;
				case XMLStreamReader.START_DOCUMENT:
				case XMLStreamReader.END_DOCUMENT:
				case XMLStreamReader.START_ELEMENT:
				case XMLStreamReader.ATTRIBUTE:
				case XMLStreamReader.NAMESPACE:
				case XMLStreamReader.SPACE:
				case XMLStreamReader.COMMENT:
				case XMLStreamReader.PROCESSING_INSTRUCTION:
				case XMLStreamReader.ENTITY_REFERENCE:
				case XMLStreamReader.DTD:
					throw new IllegalStateException();
			}
		}
		throw new IllegalStateException();
	}
	
	private ExtendedMap parseComplex(ComplexType type, XMLStreamReader reader) throws XMLStreamException {
		ExtendedMap map = new ExtendedMap(false);
		while (reader.hasNext()) {
			int event = reader.next();
			switch (event) {
				case XMLStreamReader.START_ELEMENT:
					String nsuri = reader.getNamespaceURI();
					String name = reader.getLocalName();
					ElementDef el = type.getModel(nsuri, name);
					processElement(map, nsuri, name, el, reader);
					break;
				case XMLStreamReader.CHARACTERS:
				case XMLStreamReader.CDATA:
					if (!reader.isWhiteSpace()) {
						throw new IllegalStateException();
					}
					break;
				case XMLStreamReader.END_ELEMENT:
					return map.size() == 0 ? null : map;
				case XMLStreamReader.START_DOCUMENT:
				case XMLStreamReader.END_DOCUMENT:
				case XMLStreamReader.ATTRIBUTE:
				case XMLStreamReader.NAMESPACE:
				case XMLStreamReader.SPACE:
				case XMLStreamReader.COMMENT:
				case XMLStreamReader.PROCESSING_INSTRUCTION:
				case XMLStreamReader.ENTITY_REFERENCE:
				case XMLStreamReader.DTD:
					throw new IllegalStateException();
			}
		}
		throw new IllegalStateException();
	}
	
	private void processElement(ExtendedMap map, String nsuri, String name, ElementDef el, XMLStreamReader reader) throws XMLStreamException {
		if (el == null) {
			throw new IllegalStateException("Unknown element: " + nsuri + ", " + name);
		}
		Object value = null;
		TypeDef type = el.getType();
		if (type.isSimpleType()) {
			value = parseSimple((SimpleType)type, reader);
		} else {
			ComplexType ct = (ComplexType)type;
			TypedObjectConverter converter = ct.getTypedObjectConverter();
			if (ct.getName() != null && converter != null) {
				value = converter.toObject(reader);
			} else {
				value = parseComplex(ct, reader);
			}
		}
		if (value != null) {
			map.put(name, value);
		}
	}
}
