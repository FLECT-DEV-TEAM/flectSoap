package jp.co.flect.soap;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import jp.co.flect.xml.XMLUtils;
import jp.co.flect.xmlschema.ElementDef;
import jp.co.flect.xmlschema.TypeDef;
import jp.co.flect.xmlschema.XMLSchema;
import jp.co.flect.xmlschema.XMLSchemaBuilder;
import jp.co.flect.xmlschema.XMLSchemaException;

public class WSDL implements Serializable {
	
	private static final long serialVersionUID = 1068022955806966947L;
	
	private String endpoint;
	private Map<String, OperationDef> opMap = new HashMap<String, OperationDef>();
	private Map<String, XMLSchema> schemaMap = new HashMap<String, XMLSchema>();
	
	public WSDL(File f) throws IOException, SAXException, InvalidWSDLException, XMLSchemaException {
		this(XMLUtils.parse(f));
	}
	
	public WSDL(Document doc) throws InvalidWSDLException, XMLSchemaException {
		WSDLWrapper wsdl = new WSDLWrapper(doc);
		this.endpoint = wsdl.getEndpoint();
		for (OperationDef op : wsdl.getOperations()) {
			this.opMap.put(op.getName(), op);
		}
		
		XMLSchemaBuilder schemaBuilder = new XMLSchemaBuilder();
		schemaBuilder.parse(wsdl.getTypesElement(), 1);
		for (XMLSchema schema : schemaBuilder.getSchemas()) {
			this.schemaMap.put(schema.getTargetNamespace(), schema);
		}
	}
	
	public List<XMLSchema> getSchemaList() {
		return new ArrayList<XMLSchema>(schemaMap.values());
	}
	
	public XMLSchema getSchema(String namespace) {
		return this.schemaMap.get(namespace);
	}
	
	public List<OperationDef> getOperationList() {
		return new ArrayList<OperationDef>(opMap.values());
	}
	
	public OperationDef getOperation(String name) {
		return this.opMap.get(name);
	}
	
	public List<ElementDef> getElementList() {
		List<ElementDef> list = new ArrayList<ElementDef>();
		for (XMLSchema schema : this.schemaMap.values()) {
			list.addAll(schema.getElementList());
		}
		return list;
	}
	
	public ElementDef getElement(String namespace, String name) {
		XMLSchema schema = this.schemaMap.get(namespace);
		return schema == null ? null : schema.getElement(name);
	}
	
	public List<TypeDef> getTypeList() {
		List<TypeDef> list = new ArrayList<TypeDef>();
		for (XMLSchema schema : this.schemaMap.values()) {
			list.addAll(schema.getTypeList());
		}
		return list;
	}
	
	public TypeDef getType(String namespace, String name) {
		XMLSchema schema = this.schemaMap.get(namespace);
		return schema == null ? null : schema.getType(name);
	}
	
	public String getEndpoint() { return this.endpoint;}
	
}