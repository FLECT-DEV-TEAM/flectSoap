package jp.co.flect.xmlschema;

import java.io.IOException;
import java.io.ObjectInputStream;

public abstract class NodeDef extends SchemaDef {
	
	private static final long serialVersionUID = -3235020099411944742L;
	
	private boolean form;
	private TypeDef type;
	private NodeDef ref;
	private boolean toplevel;
	
	protected NodeDef(XMLSchema schema, String name, boolean toplevel) {
		super(schema, name);
		this.toplevel = toplevel;
	}
	
	@Override
	public String getName() { 
		return isRef() ? this.ref.getName() : super.getName();
	}
	
	public boolean isTopLevel() { return this.toplevel;}
	
	public boolean isFormQualified() { 
		return isRef() ? this.ref.form : this.form;
	}
	public void setFormQualified(boolean b) { this.form = b;}
	
	public TypeDef getType() { 
		return isRef() ? this.ref.type : this.type;
	}
	
	public void setType(TypeDef t) { this.type = t;}
	
	public NodeDef getRef() { return this.ref;}
	public void setRef(NodeDef ref) { this.ref = ref;}
	
	public boolean isRef() {
		return this.ref != null;
	}
	
	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
		in.defaultReadObject();
		if (this.type != null && XMLSchemaConstants.XSD_NSURI.equals(this.type.getNamespace())) {
			this.type = SimpleType.getBuiltinType(this.type.getName());
		}
	}
	
}
