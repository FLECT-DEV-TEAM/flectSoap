package jp.co.flect.soap;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Iterator;
import java.io.IOException;
import java.io.StringWriter;
import java.io.StringReader;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;

import jp.co.flect.xml.XMLWriter;
import jp.co.flect.xmlschema.template.TemplateBuilder;
import jp.co.flect.xmlschema.template.TemplateBuilderContext;
import jp.co.flect.xmlschema.template.TemplateHint;
import jp.co.flect.xmlschema.ElementDef;
import jp.co.flect.xmlschema.TypeDef;
import jp.co.flect.xmlschema.ComplexType;
import jp.co.flect.xmlschema.XMLSchema;

public class MessageHelper {
	
	private WSDL wsdl;
	private List<TemplateHint> hints;
	private List<ElementDef> elList = new ArrayList<ElementDef>();
	
	public MessageHelper(WSDL wsdl, MessageDef msg, List<TemplateHint> hints) {
		this.wsdl = wsdl;
		this.hints = hints;
		
		Iterator<QName> it = msg.getHeaders();
		while (it.hasNext()) {
			ElementDef el = getElement(it.next());
			if (el != null) {
				elList.add(el);
			}
		}
		it = msg.getBodies();
		while (it.hasNext()) {
			ElementDef el = getElement(it.next());
			if (el != null) {
				elList.add(el);
			}
		}
	}
	
	private ElementDef getElement(QName qname) {
		XMLSchema schema = wsdl.getSchema(qname.getNamespaceURI());
		if (schema == null) {
			return null;
		}
		return schema.getElement(qname.getLocalPart());
	}
	
	public ElementDef getElementByPath(String path) {
		String[] strs = path.split("\\.");
		for (ElementDef el : elList) {
			if (el.getName().equals(strs[0])) {
				if (strs.length == 1) {
					return el;
				}
				return getElementByPath(el, strs, 1, new TemplateBuilderContext(hints));
			}
		}
		return null;
	}
	
	private ElementDef getElementByPath(ElementDef el, String[] strs, int idx, TemplateBuilderContext context) {
		String curName = strs[idx];
		context.pushElement(el);
		TypeDef type = context.resolveType(el);
		if (type.isSimpleType()) {
			return null;
		}
		Iterator<ElementDef> it = ((ComplexType)type).modelIterator(context.getHints());
		while (it.hasNext()) {
			ElementDef el2 = it.next();
			if (el2.getName().equals(curName)) {
				if (strs.length == idx + 1) {
					return el2;
				}
				return getElementByPath(el2, strs, idx+1, context);
			}
		}
		return null;
	}
	
	public String normalize(String msg) throws IOException {
		Normalizer normalizer = new Normalizer();
		XMLInputFactory factory = XMLInputFactory.newInstance();
		factory.setProperty(XMLInputFactory. IS_COALESCING, Boolean.TRUE);
		try {
			normalizer.getWriter().xmlDecl();
			
			XMLStreamReader reader = factory.createXMLStreamReader(new StringReader(msg));
			while (reader.hasNext()) {
				int event = reader.next();
				switch (event) {
					case XMLStreamReader.START_ELEMENT:
						normalizer.startElement(reader);
						break;
					case XMLStreamReader.END_ELEMENT:
						normalizer.endElement(reader);
						break;
					case XMLStreamReader.CHARACTERS:
						normalizer.characters(reader);
						break;
					case XMLStreamReader.CDATA:
						normalizer.cdata(reader);
						break;
					case XMLStreamReader.END_DOCUMENT:
						reader.close();
						break;
					case XMLStreamReader.START_DOCUMENT:
						//START_DOCUMENTは最初のnext実行前なので発生しない
					case XMLStreamReader.ATTRIBUTE:
					case XMLStreamReader.NAMESPACE:
					case XMLStreamReader.SPACE:
					case XMLStreamReader.COMMENT:
					case XMLStreamReader.PROCESSING_INSTRUCTION:
					case XMLStreamReader.ENTITY_REFERENCE:
					case XMLStreamReader.DTD:
						System.out.println("other: " + event);
						break;
				}
			}
		} catch (XMLStreamException e) {
			e.printStackTrace();
			throw new IOException(e);
		}
		return normalizer.toString();
	}
	
	private String getQName(String prefix, String localName) {
		return prefix == null || prefix.length() == 0 ? localName : prefix + ":" + localName;
	}
	
	private class Normalizer {
		
		private StringWriter sw;
		private TemplateBuilderContext context;
		private XMLWriter writer;
		private LinkedList<ElementInfo> list;
		
		public Normalizer() {
			this.sw = new StringWriter();
			this.writer = new XMLWriter(sw, "utf-8", 0);
			this.context = new TemplateBuilderContext(MessageHelper.this.hints);
			this.list = new LinkedList<ElementInfo>();
		}
		
		public XMLWriter getWriter() { return this.writer;}
		
		private ElementDef getElementDef(String nsuri, String name) {
			if (this.list.size() > 0) {
				ElementInfo parent = this.list.peekLast();
				TypeDef type = context.resolveType(parent.elDef);
				if (type instanceof ComplexType) {
					return ((ComplexType)type).getModel(nsuri, name);
				}
			} else {
				XMLSchema schema = wsdl.getSchema(nsuri);
				if (schema != null) {
					return schema.getElement(name);
				}
			}
			return null;
		}
		
		private boolean isStripable() {
			for (ElementInfo info : this.list) {
				if (!info.write && info.elDef.getMinOccurs() == 0) {
					return true;
				}
			}
			return false;
		}
		
		private void processList() throws IOException {
			for (ElementInfo info : this.list) {
				if (!info.write) {
					processInfo(info, false, false);
					info.write = true;
				}
			}
		}
		
		private void processInfo(ElementInfo info, boolean nil, boolean empty) throws IOException {
			writer.openElement(info.name);
			if (info.attrs != null) {
				for (AttrInfo a : info.attrs) {
					writer.attr(a.name, a.value);
				}
			}
			if (nil) {
				writer.attr(SoapClient.XSI_PREFIX + ":nil", "true");
			}
			if (empty) {
				writer.emptyTag();
			} else {
				writer.endTag();
			}
			if (info.childs != null) {
				for (ElementInfo child : info.childs) {
					boolean cNil = child.childs == null && child.elDef.isNillable();
					boolean cEmpty = child.childs == null;
					processInfo(child, cNil, cEmpty);
				}
				info.childs = null;
			}
		}
		
		public void startElement(XMLStreamReader reader) throws IOException {
			ElementDef el = getElementDef(reader.getNamespaceURI(), reader.getLocalName());
			if (el == null) {
				processList();
				writer.openElement(getQName(reader.getPrefix(), reader.getLocalName()));
				for (int i=0; i<reader.getNamespaceCount(); i++) {
					String prefix = reader.getNamespacePrefix(i);
					String nsuri = reader.getNamespaceURI(i);
					if (nsuri.equals(TemplateBuilder.FSI_NAMESPACE)) {
						continue;
					}
					String xmlns = prefix == null || prefix.length() == 0 ? "xmlns" : "xmlns:" + prefix;
					writer.attr(xmlns, nsuri);
				}
				for (int i=0; i<reader.getAttributeCount(); i++) {
					String nsuri = reader.getAttributeNamespace(i);
					if (TemplateBuilder.FSI_NAMESPACE.equals(nsuri)) {
						continue;
					}
					String name = getQName(reader.getAttributePrefix(i), reader.getAttributeLocalName(i));
					String value = reader.getAttributeValue(i);
					writer.attr(name, value);
				}
				writer.endTag();
			} else {
				String qname = getQName(reader.getPrefix(), reader.getLocalName());
				ElementInfo info = new ElementInfo(qname, el);
				for (int i=0; i<reader.getNamespaceCount(); i++) {
					String prefix = reader.getNamespacePrefix(i);
					String nsuri = reader.getNamespaceURI(i);
					if (nsuri.equals(TemplateBuilder.FSI_NAMESPACE)) {
						continue;
					}
					String xmlns = prefix == null || prefix.length() == 0 ? "xmlns" : "xmlns:" + prefix;
					info.addAttr(xmlns, nsuri);
				}
				for (int i=0; i<reader.getAttributeCount(); i++) {
					String nsuri = reader.getAttributeNamespace(i);
					if (TemplateBuilder.FSI_NAMESPACE.equals(nsuri)) {
						continue;
					}
					String name = getQName(reader.getAttributePrefix(i), reader.getAttributeLocalName(i));
					String value = reader.getAttributeValue(i);
					info.addAttr(name, value);
				}
				this.list.add(info);
				this.context.pushElement(el);
			}
		}
		
		public void endElement(XMLStreamReader reader) throws IOException {
			String qname = getQName(reader.getPrefix(), reader.getLocalName());
			if (list.size() == 0) {
				writer.endElement(qname);
				return;
			}
			ElementInfo info = list.removeLast();
			context.popElement();
			if (info.write) {
				writer.endElement(qname);
				return;
			}
			//要素内容が空の場合の処理
			if (!qname.equals(info.name)) {
				throw new IllegalStateException(qname + ": " + list);
			}
			ElementDef el = info.elDef;
			if (el.getMinOccurs() == 0) {
				return;
			}
			if (isStripable()) {
				list.peekLast().addChild(info);
			} else {
				processList();
				processInfo(info, el.isNillable(), true);
			}
		}
		
		public void characters(XMLStreamReader reader) throws IOException {
			if (reader.isWhiteSpace()) {
				return;
			}
			processList();
			writer.content(reader.getText());
		}
		
		public void cdata(XMLStreamReader reader) throws IOException {
			if (reader.isWhiteSpace()) {
				return;
			}
			processList();
			writer.startCDATASection();
			writer.write(reader.getText());
			writer.endCDATASection();
		}
		
		public String toString() { return sw.toString();}
	}
	
	private class ElementInfo {
		
		public String name;
		public ElementDef elDef;
		public List<AttrInfo> attrs;
		public List<ElementInfo> childs;
		public boolean write = false;
		
		public ElementInfo(String name, ElementDef elDef) {
			this.name = name;
			this.elDef = elDef;
		}
		
		public void addAttr(String name, String value) {
			if (attrs == null) {
				attrs = new ArrayList<AttrInfo>();
			}
			attrs.add(new AttrInfo(name, value));
		}
		
		public void addChild(ElementInfo el) {
			if (childs == null) {
				childs = new ArrayList<ElementInfo>();
			}
			childs.add(el);
		}
		
		public String toString() {
			return name;
		}
	}
	
	private static class AttrInfo {
	
		public String name;
		public String value;
		
		public AttrInfo(String name, String value) {
			this.name = name;
			this.value = value;
		}
	}
}
