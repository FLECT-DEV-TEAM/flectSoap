package jp.co.flect.xmlschema;

public class ElementDef extends NodeDef {
	
	private static final long serialVersionUID = -7277971420280389172L;

	public static final int UNBOUNDED = -1;
	
	private int minOccurs = 1;
	private int maxOccurs = 1;
	private boolean nillable = false;
	
	public ElementDef(XMLSchema schema, String name, boolean toplevel) {
		super(schema, name, toplevel);
		setFormQualified(schema.isElementFormDefault());
	}
	
	private ElementDef getRefElement() {
		return (ElementDef)getRef();
	}
	
	public int getMinOccurs() { 
		return isRef() ? getRefElement().minOccurs : this.minOccurs;
	}
	
	public void setMinOccurs(int n) { this.minOccurs = n;}
	
	public int getMaxOccurs() { 
		return isRef() ? getRefElement().maxOccurs : this.maxOccurs;
	}
	
	public void setMaxOccurs(int n) { this.maxOccurs = n;}
	
	public boolean isUnbounded() { return getMaxOccurs() == UNBOUNDED;}
	public boolean hasOccurs() { 
		int n = getMaxOccurs();
		return n == UNBOUNDED || n > 1;
	}
	
	public boolean isNillable() {
		return isRef() ? getRefElement().nillable : this.nillable;
	}
	
	public void setNillable(boolean b) { this.nillable = b;}
	
}
