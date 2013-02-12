package jp.co.flect.xmlschema;

import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import jp.co.flect.xmlschema.template.TemplateHint;

public abstract class ModelGroup extends SchemaDef {
	
	private static final long serialVersionUID = 2789310716394132136L;

	public static final int UNBOUNDED = -1;
	
	private int minOccurs = 1;
	private int maxOccurs = 1;
	
	private List<SchemaDef> list = new ArrayList<SchemaDef>();
	
	protected ModelGroup(XMLSchema schema, String name) {
		super(schema, name);
	}
	
	public int getMinOccurs() { return this.minOccurs;}
	void setMinOccurs(int n) { this.minOccurs = n;}
	
	public int getMaxOccurs() { return this.maxOccurs;}
	void setMaxOccurs(int n) { this.maxOccurs = n;}
	
	public boolean isUnbounded() { return this.maxOccurs == UNBOUNDED;}
	public boolean hasOccurs() { 
		return this.maxOccurs == UNBOUNDED || this.maxOccurs > 1;
	}
	
	public void addModelGroup(ModelGroup group) {
		this.list.add(group);
	}
	
	public void addElement(ElementDef el) {
		this.list.add(el);
	}
	
	public Iterator<ElementDef> modelIterator() {
		return modelIterator(null);
	}
	
	public Iterator<ElementDef> modelIterator(List<TemplateHint> hints) {
		List<ElementDef> ret = new ArrayList<ElementDef>();
		for (SchemaDef def : this.list) {
			if (def instanceof ElementDef) {
				ret.add((ElementDef)def);
			} else {
				ModelGroup group = (ModelGroup)def;
				Iterator<ElementDef> it = group.modelIterator(hints);
				while (it.hasNext()) {
					ret.add(it.next());
				}
			}
		}
		return ret.iterator();
	}
	
}
