package jp.co.flect.xmlschema;

public class TypedValue {
	
	private final SimpleType type;
	private final Object value;
	
	public TypedValue(SimpleType type, Object value) {
		this.type = type;
		this.value = value;
	}
	
	public SimpleType getType() { return this.type;}
	public Object getValue() { return this.value;}
	
	public String toString() { return this.type.format(this.value);}
	
}