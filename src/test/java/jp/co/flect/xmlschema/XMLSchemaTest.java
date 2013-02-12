package jp.co.flect.xmlschema;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;
import org.junit.Test;
import org.junit.BeforeClass;
import java.util.Arrays;
import java.util.List;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.IOException;
import org.xml.sax.SAXException;
import jp.co.flect.io.FileUtils;
import jp.co.flect.xml.XMLUtils;
import jp.co.flect.xml.NodeIterator;
import jp.co.flect.xmlschema.template.GroovyTemplateBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class XMLSchemaTest 
{
	
	@BeforeClass
	public static void setup() {
		new File("target/test-output").mkdirs();
	}
	
	@Test
	public void parse() throws XMLSchemaException, IOException, SAXException {
		Document doc = XMLUtils.parse(new File("testdata/enterprise.wsdl"));
		Element elTypes = XMLUtils.getElementNS(doc.getDocumentElement(), XMLUtils.XMLNS_WSDL, "types");
		XMLSchemaBuilder builder = new XMLSchemaBuilder();
		builder.parse(elTypes, 1);
		List<XMLSchema> list = builder.getSchemas();
		assertEquals(3, list.size());
	}
	
	@Test 
	public void dump() throws Exception {
		String dumpFilename = "target/test-output/templateDump.txt";
		String baseFilename = "testdata/templateDump-base.txt";
		
		Document doc = XMLUtils.parse(new File("testdata/enterprise.wsdl"));
		Element elTypes = XMLUtils.getElementNS(doc.getDocumentElement(), XMLUtils.XMLNS_WSDL, "types");
		
		XMLSchemaBuilder schemaBuilder = new XMLSchemaBuilder();
		schemaBuilder.parse(elTypes, 1);
		List<XMLSchema> list = schemaBuilder.getSchemas();
		
		GroovyTemplateBuilder builder = new GroovyTemplateBuilder();
		builder.setIndent(4);
		for (int i=0; i<list.size(); i++) {
			String prefix = "ns" + Integer.toString(i+1);
			XMLSchema schema = list.get(i);
			builder.addSchema(prefix, schema);
		}
		OutputStream os = new FileOutputStream(dumpFilename);
		try {
			NodeIterator it = new NodeIterator(doc.getDocumentElement());
			while (it.hasNext()) {
				Node node = it.next();
				if (node.getLocalName().equals("part")) {
					String qname = ((Element)node).getAttribute("element");
					int idx = qname.indexOf(':');
					String prefix = qname.substring(0, idx);
					String name = qname.substring(idx+1);
					String nsuri = node.lookupNamespaceURI(prefix);
					
					String header = "*************** " + qname + "\n";
					os.write(header.getBytes());
					builder.writeTo(nsuri, name, null, os);
					os.write("\n\n".getBytes());
				}
			}
		} finally {
			os.close();
		}
		assertTrue(FileUtils.contentEquals(new File(dumpFilename), new File(baseFilename)));
	}
}
