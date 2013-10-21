package jp.co.flect.xmlschema;

import java.io.Serializable;
import java.util.ArrayList;
//import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import jp.co.flect.soap.TypedObject;
import jp.co.flect.soap.TypedObjectConverter;

public class XMLSchema implements Serializable {
	
	private static final long serialVersionUID = 2557565652329923400L;

	private String targetNamespace;
	private boolean elementFormDefault;
	private boolean attributeFormDefault;
	
	private Map<String, XMLSchema> importMap = null;
	
	private Map<String, TypeDef> typeMap = new HashMap<String, TypeDef>();
	private Map<String, ElementDef> elMap = new HashMap<String, ElementDef>();
	
	private Map<String, TypedObjectConverter> objectMap = null;
	
	public XMLSchema(String targetNamespace, boolean elementFormDefault, boolean attributeFormDefault) {
		this.targetNamespace = targetNamespace;
		this.elementFormDefault = elementFormDefault;
		this.attributeFormDefault = attributeFormDefault;
	}
	
	public String getTargetNamespace() { return this.targetNamespace;}
	public boolean isElementFormDefault() { return this.elementFormDefault;}
	public boolean isAttributeFormDefault() { return this.attributeFormDefault;}
	
	public XMLSchema getImportedSchema(String nsuri) {
		return this.importMap == null ? null : this.importMap.get(nsuri);
	}
	
	public XMLSchema getRelatedSchema(String nsuri) {
		if (this.targetNamespace.equals(nsuri)) {
			return this;
		}
		return getImportedSchema(nsuri);
	}
	
	public TypeDef getType(String name) {
		return this.typeMap.get(name);
	}
	
	public ElementDef getElement(String name) {
		return this.elMap.get(name);
	}
	
	public List<ElementDef> getElementList() {
		List<ElementDef> list = new ArrayList<ElementDef>(this.elMap.values());
		//Collections.sort(list);
		return list;
	}
	
	public List<TypeDef> getTypeList() {
		List<TypeDef> list = new ArrayList<TypeDef>(this.typeMap.values());
		//Collections.sort(list);
		return list;
	}
	
	//For SchemaBuilder
	void importSchema(XMLSchema schema) {
		if (this.importMap == null) {
			this.importMap = new HashMap<String, XMLSchema>();
		}
		this.importMap.put(schema.getTargetNamespace(), schema);
	}
	
	public void addType(TypeDef type) {
		this.typeMap.put(type.getName(), type);
	}
	
	public void addElement(ElementDef el) {
		this.elMap.put(el.getName(), el);
	}
	
	public void addTypedObjectConverter(TypedObjectConverter converter) {
		if (this.objectMap == null) {
			this.objectMap = new HashMap<String, TypedObjectConverter>();
		}
		this.objectMap.put(converter.getTargetName(), converter);
	}
	
	public TypedObjectConverter getTypedObjectConverter(String name) {
		if (this.objectMap == null) {
			return null;
		}
		return this.objectMap.get(name);
	}
}
