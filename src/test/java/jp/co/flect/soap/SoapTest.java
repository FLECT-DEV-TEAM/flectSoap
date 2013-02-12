package jp.co.flect.soap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import org.junit.Test;
import org.junit.BeforeClass;
import java.util.ArrayList;
import java.util.List;
import java.io.File;
import java.io.IOException;
import org.xml.sax.SAXException;
import org.w3c.dom.Node;
import javax.xml.namespace.QName;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;
import jp.co.flect.util.ExtendedMap;
import jp.co.flect.io.FileUtils;
import jp.co.flect.xml.XMLUtils;
import jp.co.flect.xmlschema.template.TemplateBuilder;
import jp.co.flect.xmlschema.template.GroovyTemplateBuilder;
import jp.co.flect.xmlschema.template.VelocityTemplateBuilder;
import jp.co.flect.xmlschema.template.EmptyTemplateBuilder;
import jp.co.flect.xmlschema.template.TemplateHint;
import jp.co.flect.xmlschema.template.IgnoreHint;
import jp.co.flect.xmlschema.template.DerivedHint;
import jp.co.flect.xmlschema.XMLSchemaException;

public class SoapTest 
{
	
	private static final String SF_ENTERPRISE_URI = "urn:enterprise.soap.sforce.com";
	private static final String SF_SOBJECT_URI    = "urn:sobject.enterprise.soap.sforce.com";
	
	private static WSDLWrapper wsdl;
	private static SoapClient soapClient;
	
	@BeforeClass
	public static void setup() {
		try {
			wsdl = new WSDLWrapper(new File("testdata/enterprise.wsdl"));
//			TemplateBuilder builder = new GroovyTemplateBuilder();
//			TemplateBuilder builder = new EmptyTemplateBuilder();
			TemplateBuilder builder = new VelocityTemplateBuilder();
			builder.setIndent(4);
			soapClient = new SoapClient(wsdl.getDocument(), builder);
			
			new File("target/test-output").mkdirs();
		} catch (XMLSchemaException e) {
			e.printStackTrace();
		} catch (InvalidWSDLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		}
	}
	
	@Test
	public void endpoint() throws InvalidWSDLException {
		assertEquals(wsdl.getEndpoint(), "https://login.salesforce.com/services/Soap/c/24.0");
	}
	
	@Test
	public void operations() throws InvalidWSDLException {
		List<String> list = wsdl.getOperationNames();
		for (String s : list) {
			System.out.println("OperationName: " + s);
		}
		assertNotNull(list);
		assertTrue(list.size() > 0);
		assertTrue(list.contains("login") && list.contains("describeSObject"));
		List<OperationDef> list2 = wsdl.getOperations();
		assertEquals(list.size(), list2.size());
		OperationDef op = getOperation(list2, "query");
		assertNotNull(op);
		assertEquals("Create a Query Cursor", op.getDocumentation());
		assertEquals(6, op.getFaultCount());
		
		MessageDef msg = op.getRequestMessage();
		assertEquals(4, msg.getHeaderCount());
		assertEquals(1, msg.getBodyCount());
	}
	
	private OperationDef getOperation(List<OperationDef> list, String name) {
		for (OperationDef op : list) {
			if (op.getName().equals(name)) {
				return op;
			}
		}
		return null;
	}
	
	@Test
	public void templateTest() throws Exception {
		templateTest("login", null);
		templateTest("describeSObject", null);
		List<TemplateHint> hints = new ArrayList<TemplateHint>();
		hints.add(new IgnoreHint(new QName(SF_SOBJECT_URI, "DebuggingHeader")));
		hints.add(new DerivedHint(new QName(SF_SOBJECT_URI, "sObject"), new QName(SF_SOBJECT_URI, "Account")));
		soapClient.setDebug(true);
		templateTest("update", hints);
		soapClient.setDebug(false);
		templateTest("create", hints);
	}
	
	private void templateTest(String name, List<TemplateHint> hints) throws IOException {
		SoapTemplate template = soapClient.generateTemplate(name, hints);
		File f1 = new File("target/test-output/" + name + ".xml");
		File f2 = new File("testdata/" + name + "-base.xml");
		FileUtils.writeFile(f1, template.getTemplateString().getBytes("utf-8"));
		if (f2.exists()) {
			assertTrue(name, FileUtils.contentEquals(f1, f2));
		} else {
			f1.renameTo(f2);
		}
	}
	
	@Test
	public void messageTest() throws Exception {
		List<TemplateHint> hints = new ArrayList<TemplateHint>();
		hints.add(new IgnoreHint(new QName(SF_SOBJECT_URI, "DebuggingHeader")));
		hints.add(new DerivedHint(new QName(SF_SOBJECT_URI, "sObject"), new QName(SF_SOBJECT_URI, "Account")));
		
		MessageDef msg = soapClient.getOperation("create").getRequestMessage();
		MessageHelper helper = new MessageHelper(soapClient.getWSDL(), msg, hints);
		assertEquals(helper.getElementByPath("create.sObjects.AccountContactRoles.done").getName(), "done");
	}
	
	@Test 
	public void saveTemplate() throws Exception {
		try {
			List<TemplateHint> hints = new ArrayList<TemplateHint>();
			hints.add(new IgnoreHint(new QName(SF_ENTERPRISE_URI, "DebuggingHeader")));
			hints.add(new IgnoreHint(new QName(SF_ENTERPRISE_URI, "EmailHeader")));
			hints.add(new DerivedHint(new QName(SF_SOBJECT_URI, "sObject"), new QName(SF_SOBJECT_URI, "Campaign")));
			
			File f1 = new File("target/test-output/upsert-base.txt");
			File f2 = new File("target/test-output/upsert.txt");
			SoapTemplate op1 = soapClient.generateTemplate("upsert", hints);
			op1.saveToFile(f1);
			
			SoapTemplate op2 = new SoapTemplate(f1);
			op2.saveToFile(f2);
			assertTrue("Compare soapTemplate", FileUtils.contentEquals(f1, f2));
		} catch (Throwable e) {
			e.printStackTrace();
			fail(e.toString());
		}
	}
	
}
