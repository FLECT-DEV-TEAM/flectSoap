package jp.co.flect.xmlschema.template;

import java.io.IOException;
import jp.co.flect.xmlschema.ElementDef;
import jp.co.flect.template.TemplateEngine;
import jp.co.flect.template.EmptyTemplateEngine;
import jp.co.flect.xml.XMLWriter;

public class EmptyTemplateBuilder extends AbstractTemplateBuilder {
	
	private static final long serialVersionUID = -5413199497497232597L;
	
	private TemplateEngine engine = new EmptyTemplateEngine();
	
	public EmptyTemplateBuilder() {
	}
	
	protected void startLoop(ElementDef el, TemplateBuilderContext context, XMLWriter writer) throws IOException {
	}
	
	protected void writeVar(ElementDef el, TemplateBuilderContext context, XMLWriter writer) throws IOException {
	}
	
	protected void endLoop(ElementDef el, TemplateBuilderContext context, XMLWriter writer) throws IOException {
	}
	
	public TemplateEngine getTemplateEngine() { return this.engine;}
}
