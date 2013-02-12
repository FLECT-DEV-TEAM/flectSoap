package jp.co.flect.xmlschema.template;

import java.util.LinkedList;
import java.util.List;
import java.util.Iterator;
import jp.co.flect.xmlschema.ElementDef;
import jp.co.flect.xmlschema.TypeDef;

public class TemplateBuilderContext {
	
	private LinkedList<ElementDef> elStack = new LinkedList<ElementDef>();
	private LinkedList<NsInfo> nsStack = new LinkedList<NsInfo>();
	private List<TemplateHint> hints;
	
	public TemplateBuilderContext(List<TemplateHint> hints) {
		this.hints = hints;
	}
	
	public int getContextSize() { return this.elStack.size();}
	
	public ElementDef getContextElement(int idx) { return this.elStack.get(idx);}
	
	public String getContextName() {
		return elStack.size() == 0 ? null : elStack.get(0).getName();
	}
	
	public String buildContextPath(int stopAtLoop, boolean ignoreRoot) {
		if (elStack.size() == 0 || (ignoreRoot && elStack.size() == 1)) {
			return null;
		}
		ElementDef loopEl = null;
		if (stopAtLoop > 0) {
			int cnt = 0;
			Iterator<ElementDef> it = elStack.iterator();
			while (it.hasNext()) {
				ElementDef el = it.next();
				if (el.hasOccurs()) {
					cnt++;
					if (stopAtLoop == cnt) {
						loopEl = el;
						break;
					}
				}
			}
		}
		Iterator<ElementDef> it = elStack.descendingIterator();
		if (ignoreRoot) {
			it.next();
		}
		StringBuilder buf = new StringBuilder();
		while (it.hasNext()) {
			ElementDef el = it.next();
			if (loopEl == el) {
				buf.setLength(0);
			}
			if (buf.length() > 0) {
				buf.append(".");
			}
			buf.append(el.getName());
		}
		return buf.toString();
	}
	
	public List<TemplateHint> getHints() { return this.hints;}
	
	public boolean pushNamespace(String prefix, String namespace) {
		for (NsInfo info : this.nsStack) {
			if (info.namespace.equals(namespace)) {
				return false;
			}
		}
		this.nsStack.push(new NsInfo(prefix, namespace));
		return true;
	}
	
	public void popNamespace() {
		this.nsStack.pop();
	}
	
	public void pushElement(ElementDef el) {
		this.elStack.push(el);
	}
	
	public void popElement() {
		this.elStack.pop();
	}
	
	public boolean isRecursive(ElementDef el) {
		TypeDef type = resolveType(el);
		if (type.isSimpleType()) {
			return false;
		}
		for (ElementDef parent : this.elStack) {
			if (resolveType(parent).equals(type)) {
				return true;
			}
		}
		return false;
	}
	
	public boolean isIgnoreElement(ElementDef el) {
		if (this.hints == null) {
			return false;
		}
		for (TemplateHint h : this.hints) {
			if (h.isIgnoreElement(this, el)) {
				return true;
			}
		}
		return false;
	}
	
	public TypeDef resolveType(ElementDef el) {
		if (this.hints == null) {
			return el.getType();
		}
		for (TemplateHint h : this.hints) {
			TypeDef ret = h.getDerivedType(this, el);
			if (ret != null) {
				return ret;
			}
		}
		return el.getType();
	}
	
	private static class NsInfo {
		
		public String prefix;
		public String namespace;
		
		public NsInfo(String p, String n) {
			this.prefix = p;
			this.namespace = n;
		}
	}
}
