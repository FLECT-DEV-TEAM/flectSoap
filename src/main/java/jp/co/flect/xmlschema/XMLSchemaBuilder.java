package jp.co.flect.xmlschema;

import jp.co.flect.xml.XMLUtils;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Attr;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

public class XMLSchemaBuilder implements XMLSchemaConstants {
	
	private static final int DELAY_EXTENSION_BASE   = 1;
	private static final int DELAY_RESTRICTION_BASE = 2;
	private static final int DELAY_ELEMENT_TYPE     = 3;
	private static final int DELAY_ELEMENT_REF      = 4;
	
	private static boolean isSchemaElement(Node node) {
		return node.getNodeType() == Node.ELEMENT_NODE && XSD_NSURI.equals(node.getNamespaceURI());
	}
	
	private Map<String, XMLSchema> schemaMap = new HashMap<String, XMLSchema>();
	private List<DelayDef> delayDefList = new ArrayList<DelayDef>();
	private List<DelayImport> delayImportList = new ArrayList<DelayImport>();
	
	public void parse(Element el) throws XMLSchemaException {
		parse(el, 0);
	}
	
	public void parse(Element el, int level) throws XMLSchemaException {
		if (XMLUtils.matchNS(el, XSD_NSURI, SCHEMA)) {
			doParse(el);
		}
		if (level > 0) {
			Node node = el.getFirstChild();
			while (node != null) {
				if (node.getNodeType() == Node.ELEMENT_NODE) {
					parse((Element)node, level-1);
				}
				node = node.getNextSibling();
			}
		}
	}
	
	private void doParse(Element elSchema) throws XMLSchemaException {
		String targetNamespace = elSchema.getAttribute(TARGET_NAMESPACE);
		XMLSchema schema = this.schemaMap.get(targetNamespace);
		if (schema == null) {
			boolean elementFormDefault = QUALIFIED.equals(elSchema.getAttribute(ELEMENT_FORM_DEFAULT));
			boolean attributeFormDefault = QUALIFIED.equals(elSchema.getAttribute(ATTRIBUTE_FORM_DEFAULT));
			schema = new XMLSchema(targetNamespace, elementFormDefault, attributeFormDefault);
		}
		Node node = elSchema.getFirstChild();
		while (node != null) {
			if (isSchemaElement(node)) {
				Element el = (Element)node;
				String name = el.getLocalName();
				if (name.equals(IMPORT)) {
					processImport(schema, el);
				} else if (name.equals(COMPLEX_TYPE)) {
					processComplexType(schema, el);
				} else if (name.equals(SIMPLE_TYPE)) {
					processSimpleType(schema, el);
				} else if (name.equals(ELEMENT)) {
					processElement(schema, el);
				} else {
					System.out.println("Not implemented yet. : " + name);
				}
			}
			node = node.getNextSibling();
		}
		this.schemaMap.put(targetNamespace, schema);
	}
	
	private void processImport(XMLSchema schema, Element el) {
		String namespace = el.getAttribute(NAMESPACE);
		XMLSchema importSchema = this.schemaMap.get(namespace);
		if (importSchema != null) {
			schema.importSchema(importSchema);
		} else {
			this.delayImportList.add(new DelayImport(schema, el));
		}
	}
	
	private void processComplexType(XMLSchema schema, Element el) throws XMLSchemaException {
		ComplexType type = createComplexType(schema, el);
		schema.addType(type);
	}
	
	private ComplexType createComplexType(XMLSchema schema, Element el) throws XMLSchemaException {
		String name = el.hasAttribute(NAME) ? el.getAttribute(NAME) : null;
		ComplexType type = new ComplexType(schema, name);
		processTypeChild(type, el, true);
		return type;
	}
	
	private void processSimpleType(XMLSchema schema, Element el) throws XMLSchemaException {
		SimpleType type = createSimpleType(schema, el);
		schema.addType(type);
	}
	
	private SimpleType createSimpleType(XMLSchema schema, Element el) throws XMLSchemaException {
		String name = el.hasAttribute(NAME) ? el.getAttribute(NAME) : null;
		SimpleType type = new SimpleType(schema, name);
		processContent(type, el);
		return type;
	}
	
	private void processTypeChild(TypeDef type, Element el, boolean allowContent) throws XMLSchemaException {
		boolean bEnd = false;
		Node node = el.getFirstChild();
		while (node != null) {
			if (isSchemaElement(node)) {
				if (bEnd) {
					throw new InvalidSchemaException("Too many definition", el);
				}
				Element child = (Element)node;
				String name = child.getLocalName();
				if (name.equals(COMPLEX_CONTENT)) {
					if (!allowContent) {
						throw new InvalidSchemaException(name + " not allowed", el);
					}
					processContent(type, child);
					bEnd = true;
				} else if (name.equals(SIMPLE_CONTENT)) {
					if (!allowContent) {
						throw new InvalidSchemaException(name + " not allowed", el);
					}
					processContent(type, child);
					bEnd = true;
				} else if (name.equals(SEQUENCE)) {
					if (type.isSimpleType() || (type.hasBaseType() && type.getBaseType().isSimpleType())) {
						throw new InvalidSchemaException(name + " not allowed", el);
					}
					processSequence((ComplexType)type, child, null);
				} else {
					System.out.println("Not implemented yet. : " + name);
				}
			}
			node = node.getNextSibling();
		}
	}
	
	private void processContent(TypeDef type, Element el) throws XMLSchemaException {
		boolean bEnd = false;
		Node node = el.getFirstChild();
		while (node != null) {
			if (isSchemaElement(node)) {
				if (bEnd) {
					throw new InvalidSchemaException("Too many definition", el);
				}
				Element child = (Element)node;
				String name = child.getLocalName();
				if (name.equals(EXTENSION)) {
					if (type.isSimpleType() || (type.hasBaseType() && type.getBaseType().isSimpleType())) {
						throw new InvalidSchemaException(name + " not allowed", el);
					}
					bEnd = true;
					TypeDef baseType = getType(type.getSchema(), el, child.getAttribute(BASE));
					if (baseType == null) {
						this.delayDefList.add(new DelayDef(type, child, DELAY_EXTENSION_BASE));
					} else {
						type.setExtensionBase(baseType);
					}
					processTypeChild(type, child, false);
				} else if (name.equals(RESTRICTION)) {
					bEnd = true;
					TypeDef baseType = getType(type.getSchema(), el, child.getAttribute(BASE));
					if (baseType == null) {
						this.delayDefList.add(new DelayDef(type, child, DELAY_RESTRICTION_BASE));
					} else {
						type.setRestrictionBase(baseType);
					}
					if (el.getLocalName().equals(COMPLEX_CONTENT)) {
						processTypeChild(type, child, false);
					} else {
						processRestriction(type, child);
					}
				}
			}
			node = node.getNextSibling();
		}
		if (!bEnd) {
			throw new InvalidSchemaException("extension or restriction is required", el);
		}
	}
	
	private void processSequence(ComplexType type, Element el, String name) throws XMLSchemaException {
		Sequence seq = createSequence(type.getSchema(), el, name);
		type.addModelGroup(seq);
	}
	
	private Sequence createSequence(XMLSchema schema, Element el, String name) throws XMLSchemaException {
		Sequence seq = new Sequence(schema, name);
		processModelGroup(seq, el);
		return seq;
	}
	
	private void processModelGroup(ModelGroup group, Element el) throws XMLSchemaException {
		NamedNodeMap attrs = el.getAttributes();
		if (attrs != null) {
			for (int i=0; i<attrs.getLength(); i++) {
				Attr a = (Attr)attrs.item(i);
				if (a.getNamespaceURI() != null) {
					continue;
				}
				String name = a.getName();
				String value = a.getValue();
				if (name.equals(ID)) {
					continue;
				} else if (name.equals(MIN_OCCURS)) {
					int n = Integer.parseInt(value);
					group.setMinOccurs(n);
				} else if (name.equals(MAX_OCCURS)) {
					int n = UNBOUNDED.equals(value) ? ModelGroup.UNBOUNDED : Integer.parseInt(value);
					group.setMaxOccurs(n);
				} else {
					System.out.println("Not implemented yet : " + name);
				}
			}
		}
		Node node = el.getFirstChild();
		while (node != null) {
			if (isSchemaElement(node)) {
				Element child = (Element)node;
				String name = child.getLocalName();
				if (name.equals(SEQUENCE)) {
					group.addModelGroup(createSequence(group.getSchema(), child, null));
				} else if (name.equals(ELEMENT)) {
					group.addElement(createElement(group.getSchema(), child, false));
				} else if (name.equals(ANY)) {
					Any any = new Any(group.getSchema());
					if (child.hasAttribute(NAMESPACE)) {
						any.setNamespace(child.getAttribute(NAMESPACE));
					}
					if (child.hasAttribute(PROCESS_CONTENTS)) {
						any.setProcessContents(child.getAttribute(PROCESS_CONTENTS));
					}
					group.addElement(any);
				} else {
					System.out.println("Not implemented yet : " + name);
				}
			}
			node = node.getNextSibling();
		}
	}
	
	private void processElement(XMLSchema schema, Element el) throws XMLSchemaException {
		ElementDef def = createElement(schema, el, true);
		if (def.getName() == null) {
			throw new InvalidSchemaException("name not found", el);
		}
		schema.addElement(def);
	}
	
	private ElementDef createElement(XMLSchema schema, Element el, boolean toplevel) throws XMLSchemaException {
		String name = el.hasAttribute(NAME) ? el.getAttribute(NAME) : null;
		ElementDef def = new ElementDef(schema, name, toplevel);
		NamedNodeMap attrs = el.getAttributes();
		boolean bType = false;
		if (attrs != null) {
			for (int i=0; i<attrs.getLength(); i++) {
				Attr a = (Attr)attrs.item(i);
				if (a.getNamespaceURI() != null) {
					continue;
				}
				name = a.getName();
				String value = a.getValue();
				if (name.equals(ID) || name.equals(NAME)) {
					continue;
				} else if (name.equals(MIN_OCCURS)) {
					int n = Integer.parseInt(value);
					def.setMinOccurs(n);
				} else if (name.equals(MAX_OCCURS)) {
					int n = UNBOUNDED.equals(value) ? ModelGroup.UNBOUNDED : Integer.parseInt(value);
					def.setMaxOccurs(n);
				} else if (name.equals(NILLABLE)) {
					def.setNillable(Boolean.valueOf(value).booleanValue());
				} else if (name.equals(TYPE)) {
					TypeDef type = getType(schema, el, value);
					if (type != null) {
						def.setType(type);
					} else {
						delayDefList.add(new DelayDef(def, el, DELAY_ELEMENT_TYPE));
					}
					bType = true;
				} else if (name.equals(REF)) {
					ElementDef ref = getElement(schema, el, value);
					if (ref != null) {
						def.setRef(ref);
					} else {
						delayDefList.add(new DelayDef(def, el, DELAY_ELEMENT_REF));
					}
					bType = true;
				} else {
					System.out.println("Not implemented yet : " + name);
				}
			}
		}
		Node node = el.getFirstChild();
		while (node != null) {
			if (isSchemaElement(node)) {
				Element child = (Element)node;
				name = child.getLocalName();
				if (name.equals(COMPLEX_TYPE)) {
					if (bType) {
						throw new InvalidSchemaException("Too many definitions", el);
					}
					ComplexType type = createComplexType(schema, child);
					def.setType(type);
					bType = true;
				} else if (name.equals(SIMPLE_TYPE)) {
					if (bType) {
						throw new InvalidSchemaException("Too many definitions", el);
					}
					System.out.println("Not implemented yet : " + name);
				} else {
					System.out.println("Not implemented yet : " + name);
				}
			}
			node = node.getNextSibling();
		}
		return def;
	}
	
	private void processRestriction(TypeDef type, Element el) throws XMLSchemaException {
//		System.out.println("not implemented yet : " + el);
	}
	
	private TypeDef getType(XMLSchema schema, Element el, String name) {
		String nsuri = null;
		int idx = name.indexOf(":");
		if (idx != -1) {
			nsuri = el.lookupNamespaceURI(name.substring(0, idx));
			name = name.substring(idx+1);
		}
		if (XSD_NSURI.equals(nsuri)) {
			return SimpleType.getBuiltinType(name);
		}
		if (!schema.getTargetNamespace().equals(nsuri)) {
			schema = schema.getImportedSchema(nsuri);
			if (schema == null) {
				return null;
			}
		}
		return schema.getType(name);
	}
	
	private ElementDef getElement(XMLSchema schema, Element el, String name) {
		String nsuri = null;
		int idx = name.indexOf(":");
		if (idx != -1) {
			nsuri = el.lookupNamespaceURI(name.substring(0, idx));
			name = name.substring(idx+1);
		}
		if (!schema.getTargetNamespace().equals(nsuri)) {
			schema = schema.getImportedSchema(nsuri);
			if (schema == null) {
				return null;
			}
		}
		return schema.getElement(name);
	}
	
	private void resolveDelay() throws XMLSchemaException {
		for (DelayImport di : this.delayImportList) {
			String namespace = di.element.getAttribute(NAMESPACE);
			XMLSchema schema = this.schemaMap.get(namespace);
			if (schema == null) {
				throw new InvalidSchemaException("Import not resolved: ", di.element);
			}
			di.schema.importSchema(schema);
		}
		for (DelayDef dd : this.delayDefList) {
			switch (dd.kind) {
				case DELAY_EXTENSION_BASE:
				case DELAY_RESTRICTION_BASE:
				{
					TypeDef type = (TypeDef)dd.def;
					TypeDef baseType = getType(type.getSchema(), dd.element, dd.element.getAttribute(BASE));
					if (baseType == null) {
						throw new InvalidSchemaException("base not resolved: ", dd.element);
					}
					if (dd.kind == DELAY_EXTENSION_BASE) {
						type.setExtensionBase(baseType);
					} else {
						type.setRestrictionBase(baseType);
					}
					break;
				}
				case DELAY_ELEMENT_TYPE:
				{
					ElementDef el = (ElementDef)dd.def;
					TypeDef type = getType(el.getSchema(), dd.element, dd.element.getAttribute(TYPE));
					if (type == null) {
						throw new InvalidSchemaException("type not resolved: ", dd.element);
					}
					el.setType(type);
					break;
				}
				case DELAY_ELEMENT_REF:
				{
					ElementDef el = (ElementDef)dd.def;
					ElementDef ref = getElement(el.getSchema(), dd.element, dd.element.getAttribute(REF));
					if (ref == null) {
						throw new InvalidSchemaException("ref not resolved: ", dd.element);
					}
					el.setRef(ref);
					break;
				}
			}
		}
	
	}
	
	public List<XMLSchema> getSchemas() throws XMLSchemaException {
		resolveDelay();
		return new ArrayList<XMLSchema>(this.schemaMap.values());
	}
	
	private static class DelayImport {
		
		public XMLSchema schema;
		public Element element;
		
		public DelayImport(XMLSchema schema, Element el) {
			this.schema = schema;
			this.element = el;
		}
	}
	
	private static class DelayDef {
		
		public SchemaDef def;
		public Element element;
		public int kind;
		
		public DelayDef(SchemaDef def, Element el, int kind) {
			this.def = def;
			this.element = el;
			this.kind = kind;
		}
	}
}
