package jp.co.flect.xmlschema.template;

import java.io.IOException;
import jp.co.flect.xmlschema.ElementDef;
import jp.co.flect.template.TemplateEngine;
import jp.co.flect.template.GroovyTemplateEngine;
import jp.co.flect.xml.XMLWriter;

public class GroovyTemplateBuilder extends AbstractTemplateBuilder {
	
	private static final long serialVersionUID = -8228686653388031495L;
	
	private boolean ignoreRoot;
	private TemplateEngine engine = new GroovyTemplateEngine();
	
	public GroovyTemplateBuilder() {
		this(false);
	}
	
	public GroovyTemplateBuilder(boolean ignoreRoot) {
		this.ignoreRoot = ignoreRoot;
	}
	
	public boolean isIgnoreRoot() { return this.ignoreRoot;}
	public void setIgnoreRoot(boolean b) { this.ignoreRoot = b;}
	
	protected void startLoop(ElementDef el, TemplateBuilderContext context, XMLWriter writer) throws IOException {
		String contextName = context.buildContextPath(2, ignoreRoot);
		StringBuilder buf = new StringBuilder();
		if (writer.getIndent() > 0) {
			buf.append("\n");
		}
		buf.append("<% ")
			.append(contextName)
			.append(".each { ")
			.append(el.getName())
			.append(" ->%>");
		writer.write(buf.toString());
	}
	
	protected void writeVar(ElementDef el, TemplateBuilderContext context, XMLWriter writer) throws IOException {
		String contextName = context.buildContextPath(1, ignoreRoot);
		
		StringBuilder buf = new StringBuilder();
		buf.append("${")
			.append(contextName)
			.append("}");
		writer.write(buf.toString());
	}
	
	protected void endLoop(ElementDef el, TemplateBuilderContext context, XMLWriter writer) throws IOException {
		if (writer.getIndent() > 0) {
			writer.writeln();
		}
		writer.write("<%}%>");
	}
	
	public TemplateEngine getTemplateEngine() { return this.engine;}
	
}
