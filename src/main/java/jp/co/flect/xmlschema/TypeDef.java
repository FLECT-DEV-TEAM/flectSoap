package jp.co.flect.xmlschema;

public abstract class TypeDef extends SchemaDef {
	
	private static final long serialVersionUID = 2389379005401973753L;
	
	private TypeDef extensionBase;
	private TypeDef restrictionBase;
	
	protected TypeDef(XMLSchema schema, String name) {
		super(schema, name);
	}
	
	public TypeDef getExtensionBase() { return this.extensionBase;}
	void setExtensionBase(TypeDef t) { this.extensionBase = t;}
	
	public TypeDef getRestrictionBase() { return this.restrictionBase;}
	void setRestrictionBase(TypeDef t) { this.restrictionBase = t;}
	
	public boolean hasBaseType() { 
		return this.extensionBase != null || this.restrictionBase != null;
	}
	
	public TypeDef getBaseType() {
		if (this.extensionBase != null) return this.extensionBase;
		if (this.restrictionBase != null) return this.restrictionBase;
		
		return null;
	}
	
	public abstract boolean isSimpleType();
	public boolean isComplexType() { return !isSimpleType();}
	
}
