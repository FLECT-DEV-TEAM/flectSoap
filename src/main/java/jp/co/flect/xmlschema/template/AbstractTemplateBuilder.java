package jp.co.flect.xmlschema.template;

import jp.co.flect.xml.XMLWriter;
import jp.co.flect.xmlschema.XMLSchema;
import jp.co.flect.xmlschema.ElementDef;
import jp.co.flect.xmlschema.TypeDef;
import jp.co.flect.xmlschema.ComplexType;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;

public abstract class AbstractTemplateBuilder implements TemplateBuilder, Serializable {
	
	private static final long serialVersionUID = 5287264146608112734L;
	
	private List<SchemaInfo> schemaList = new ArrayList<SchemaInfo>();
	private int indent = 0;
	private boolean outputXMLDecl = true;
	private int initialIndent = 0;
	private boolean outputSchemaInfo = false;
	private Map<String, String> contextNamespaceMap = null;
	
	public int getIndent() { return this.indent;}
	public void setIndent(int n) { this.indent = n;}
	
	public boolean isOutputXMLDecl() { return this.outputXMLDecl;}
	public void setOutputXMLDecl(boolean b) { this.outputXMLDecl = b;}
	
	public int getInitialIndent() { return this.initialIndent;}
	public void setInitialIndent(int n) { this.initialIndent = n;}
	
	public boolean isOutputSchemaInfo() { return this.outputSchemaInfo;}
	public void setOutputSchemaInfo(boolean b) { this.outputSchemaInfo = b;}
	
	public void addContextNamespace(String prefix, String namespace) {
		if (this.contextNamespaceMap == null) {
			this.contextNamespaceMap = new HashMap<String, String>();
		}
		this.contextNamespaceMap.put(prefix, namespace);
	}
	
	public void removeContextNamespace(String prefix) {
		if (this.contextNamespaceMap == null) {
			return;
		}
		this.contextNamespaceMap.remove(prefix);
	}
	
	public String addSchema(XMLSchema schema) {
		String prefix = "ns";
		int idx = 1;
		while (getSchema(prefix + idx) != null) {
			idx++;
		}
		this.schemaList.add(new SchemaInfo(prefix + idx, schema));
		return prefix;
	}
	
	public String addSchema(String prefix, XMLSchema schema) {
		this.schemaList.add(new SchemaInfo(prefix, schema));
		return prefix;
	}
	
	public List<XMLSchema> getSchemaList() {
		List<XMLSchema> list = new ArrayList<XMLSchema>(this.schemaList.size());
		for (SchemaInfo info : this.schemaList) {
			list.add(info.schema);
		}
		return list;
	}
	
	protected ElementDef getElement(String namespace, String name) {
		XMLSchema schema = getSchema(namespace);
		if (schema == null) {
			throw new IllegalArgumentException("Namespace " + namespace + " is not registered");
		}
		ElementDef el = schema.getElement(name);
		if (el == null) {
			throw new IllegalArgumentException("Element not found " + name);
		}
		return el;
	}
	
	public void writeTo(String namespace, String name, List<TemplateHint> hints, OutputStream os) throws IOException {
		ElementDef el = getElement(namespace, name);
		XMLWriter writer = new XMLWriter(os, "utf-8", this.indent);
		try {
			writer.setIndentLevel(this.initialIndent);
			TemplateBuilderContext context = new TemplateBuilderContext(hints);
			if (this.contextNamespaceMap != null) {
				for (Map.Entry<String, String> entry : this.contextNamespaceMap.entrySet()) {
					context.pushNamespace(entry.getKey(), entry.getValue());
				}
			}
			if (this.outputXMLDecl) {
				writer.xmlDecl();
				this.outputXMLDecl = false;
			}
			writeElement(el, context, writer, false);
		} finally {
			writer.flush();
		}
	}
	
	private boolean writeElement(ElementDef el, TemplateBuilderContext context, XMLWriter writer, boolean indent) throws IOException {
		if (context.isRecursive(el)) {
			return false;
		}
		if (context.isIgnoreElement(el)) {
			return false;
		}
		context.pushElement(el);
		TypeDef type = context.resolveType(el);
		
		String namespace = el.getNamespace();
		String prefix = getPrefix(namespace);
		String qname = el.isFormQualified() ? 
			prefix + ":" + el.getName() : el.getName();
		
		int nsCnt = 0;
		if (el.hasOccurs()) {
			startLoop(el, context, writer);
		}
		writer.indent(indent);
		writer.openElement(qname);
		if (context.pushNamespace(prefix, namespace)) {
			String xmlns = "xmlns:" + prefix;
			writer.attr(xmlns, namespace);
			nsCnt++;
		}
		String typePrefix = getPrefix(type.getNamespace());
		if (typePrefix != null && !typePrefix.equals(prefix) && context.pushNamespace(typePrefix, type.getNamespace())) {
			writer.attr("xmlns:" + typePrefix, type.getNamespace());
			nsCnt++;
		}
		if (!type.equals(el.getType())) {
			//assert typePrefix != null
			writer.attr("xsi:type", typePrefix + ":" + type.getName());
		}
		if (this.outputSchemaInfo) {
			if (context.pushNamespace(FSI_PREFIX, FSI_NAMESPACE)) {
				writer.attr("xmlns:" + FSI_PREFIX, FSI_NAMESPACE);
				nsCnt++;
			}
			int min = el.getMinOccurs();
			int max = el.getMaxOccurs();
			String occurs = Integer.toString(min);
			if (min != max) {
				occurs += ".." + (max == ElementDef.UNBOUNDED ? "n" : Integer.toString(max));
			}
			writer.attr(FSI_PREFIX + ":occurs", occurs);
			if (el.isNillable()) {
				writer.attr(FSI_PREFIX + ":nillable", "true");
			}
		}
		//ToDo attr
		writer.endTag();
		
		if (type.isSimpleType()) {
			writeVar(el, context, writer);
		} else {
			Iterator<ElementDef> it = ((ComplexType)type).modelIterator(context.getHints());
			boolean first = true;
			while (it.hasNext()) {
				if (writeElement(it.next(), context, writer, first)) {
					first = false;
				}
			}
			if (first) {
				writer.indent(false);
			} else {
				writer.unindent();
			}
		}
		writer.endElement(qname);
		if (el.hasOccurs()) {
			endLoop(el, context, writer);
		}
		context.popElement();
		while (nsCnt > 0) {
			context.popNamespace();
			nsCnt--;
		}
		return true;
	}
	
	private XMLSchema getSchema(String s) {
		for (SchemaInfo info : this.schemaList) {
			if (s.equals(info.schema.getTargetNamespace())) return info.schema;
			if (s.equals(info.prefix)) return info.schema;
		}
		return null;
	}
	
	protected abstract void startLoop(ElementDef el, TemplateBuilderContext context, XMLWriter writer) throws IOException;
	protected abstract void writeVar(ElementDef el, TemplateBuilderContext context, XMLWriter writer)  throws IOException;
	protected abstract void endLoop(ElementDef el, TemplateBuilderContext context, XMLWriter writer) throws IOException;
	
	private String getPrefix(String namespace) {
		for (SchemaInfo info : this.schemaList) {
			if (info.schema.getTargetNamespace().equals(namespace)) {
				return info.prefix;
			}
		}
		return null;
	}
	
	private static class SchemaInfo {
		
		public String prefix;
		public XMLSchema schema;
		
		public SchemaInfo(String prefix, XMLSchema schema) {
			this.prefix = prefix;
			this.schema = schema;
		}
	}
	
	public Object clone() {
		try {
			AbstractTemplateBuilder ret = (AbstractTemplateBuilder)super.clone();
			ret.schemaList = new ArrayList(this.schemaList);
			if (this.contextNamespaceMap != null) {
				ret.contextNamespaceMap = new HashMap<String, String>(this.contextNamespaceMap);
			}
			return ret;
		} catch (CloneNotSupportedException e) {
			throw new IllegalStateException(e);
		}
	}
}
