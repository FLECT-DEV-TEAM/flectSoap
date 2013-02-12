package jp.co.flect.xmlschema;

import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import jp.co.flect.xmlschema.template.TemplateHint;

public class ComplexType extends TypeDef {
	
	private static final long serialVersionUID = -1977837276673180506L;
	
	private List<ModelGroup> list = new ArrayList<ModelGroup>();
	
	public ComplexType(XMLSchema schema) {
		super(schema, null);
	}
	
	public ComplexType(XMLSchema schema, String name) {
		super(schema, name);
	}
	
	public boolean isSimpleType() { return false;}
	
	public void addModelGroup(ModelGroup group) {
		this.list.add(group);
	}
	
	public Iterator<ElementDef> modelIterator() {
		return modelIterator(null);
	}
	
	public Iterator<ElementDef> modelIterator(List<TemplateHint> hints) {
		List<ElementDef> ret = new ArrayList<ElementDef>();
		TypeDef extType = getExtensionBase();
		if (extType instanceof ComplexType) {
			Iterator<ElementDef> it = ((ComplexType)extType).modelIterator(hints);
			while (it.hasNext()) {
				ret.add(it.next());
			}
		}
		for (ModelGroup group : this.list) {
			//ToDo choice
			Iterator<ElementDef> it = group.modelIterator(hints);
			while (it.hasNext()) {
				ret.add(it.next());
			}
		}
		return ret.iterator();
	}
	
	public ElementDef getModel(String nsuri, String name) {
		TypeDef extType = getExtensionBase();
		if (extType instanceof ComplexType) {
			ElementDef ret = getModel(((ComplexType)extType).list, nsuri, name);
			if (ret != null) {
				return ret;
			}
		}
		return getModel(this.list, nsuri, name);
	}
	
	private static ElementDef getModel(List<ModelGroup> list, String nsuri, String name) {
		for (ModelGroup model : list) {
			Iterator<ElementDef> it = model.modelIterator();
			while (it.hasNext()) {
				ElementDef el = it.next();
				if (el.getNamespace().equals(nsuri) && el.getName().equals(name)) {
					return el;
				}
			}
		}
		return null;
	}
	
}
