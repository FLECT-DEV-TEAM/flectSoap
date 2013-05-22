package jp.co.flect.soap;

import org.w3c.dom.Document;
import java.io.IOException;
import java.io.StringReader;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedList;
import org.xml.sax.SAXException;
import org.xml.sax.InputSource;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;

import jp.co.flect.xml.XMLUtils;
import jp.co.flect.util.ExtendedMap;

public class SoapResponse implements Serializable {
	
	private static final long serialVersionUID = 3160385644758030538L;
	
	private int responseCode;
	private String body;
	private transient ExtendedMap map;
	private transient Document doc;
	private boolean bSoap12 = false;
	
	public SoapResponse(int responseCode, String body) {
		this.responseCode = responseCode;
		this.body = body;
	}
	
	public int getResponseCode() { return this.responseCode;}
	
	public String getAsString() { return this.body;}
	
	public ExtendedMap getAsMap() throws SoapException { 
		if (this.map == null) {
			this.map = new ExtendedMap(false);
			buildMap();
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
		if (this.map == null) {
			try {
				getAsMap();
			} catch (SoapException e) {
				e.printStackTrace();
			}
		}
		return bSoap12;
	}
	
	public boolean isSoap11() { return !isSoap12();}
	
	private void buildMap() throws SoapException {
		boolean startData = false;
		boolean hasAttr = false;
		String soapUri = null;
		ParseContext context = new ParseContext(this.map);
		
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
								this.bSoap12 = true;
							} else if (!XMLUtils.XMLNS_SOAP_ENVELOPE.equals(soapUri)) {
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
	
}
