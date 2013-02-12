package jp.co.flect.xmlschema;

import javax.xml.namespace.QName;
import java.io.Serializable;
import org.apache.commons.lang.StringUtils;

public abstract class SchemaDef implements Comparable<SchemaDef>, Serializable {
	
	private static final long serialVersionUID = 7764721839769833532L;

	private String name;//Allow null
	private XMLSchema schema;
	private String targetNamespace;
	
	protected SchemaDef(XMLSchema schema, String name) {
		this.name = name;
		this.schema = schema;
		this.targetNamespace = schema.getTargetNamespace();
	}
	
	public XMLSchema getSchema() { return this.schema;}
	public String getNamespace() { return this.targetNamespace;}
	public String getName() { return this.name;}
	
	public QName getQName() { return new QName(getNamespace(), getName());}
	
	public String toString() {
		return getClass().getSimpleName() + ": " + getName();
	}
	
	public int compareTo(SchemaDef o) {
		String nsuri1 = getNamespace();
		String nsuri2 = o.getNamespace();
		if (nsuri1 != null) {
			int ret = nsuri1.compareTo(nsuri2);
			if (ret != 0) {
				return ret;
			}
		}
		return getName().compareTo(o.getName());
	}
	
	public int hashCode() {
		String tns = getNamespace();
		String name = getName();
		if (tns == null) {
			return name == null ? getClass().getName().hashCode() : name.hashCode();
		}
		return ("{" + tns + "}" + name).hashCode();
	}
	
	public boolean equals(Object o) {
		if (o == this) {
			return true;
		}
		if (!getClass().equals(o.getClass())) {
			return false;
		}
		SchemaDef sd = (SchemaDef)o;
		return StringUtils.equals(this.getNamespace(), sd.getNamespace()) &&
			StringUtils.equals(this.getName(), sd.getName());
	}
}
