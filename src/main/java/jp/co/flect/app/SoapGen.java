package jp.co.flect.app;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import javax.xml.namespace.QName;
import org.w3c.dom.Document;
import jp.co.flect.xml.XMLUtils;
import jp.co.flect.xmlschema.template.TemplateBuilder;
import jp.co.flect.xmlschema.template.TemplateHint;
import jp.co.flect.xmlschema.template.IgnoreHint;
import jp.co.flect.xmlschema.template.IgnoreZeroHint;
import jp.co.flect.xmlschema.template.DerivedHint;
import jp.co.flect.xmlschema.template.GroovyTemplateBuilder;
import jp.co.flect.xmlschema.template.VelocityTemplateBuilder;
import jp.co.flect.xmlschema.template.EmptyTemplateBuilder;
import jp.co.flect.soap.SoapClient;
import jp.co.flect.soap.SoapTemplate;
/*
import jp.co.flect.xmlschema.XMLSchema;
import jp.co.flect.xmlschema.XMLSchemaBuilder;
import java.io.ByteArrayOutputStream;
*/


public class SoapGen {
	
	private static void printUsage() {
		System.err.println("Usage: soapgen  [options] wsdlFilename operationName");
		System.err.println("Options:");
		System.err.println("  -i <qnmae>          : 出力時に無視する要素のQNameを指定します");
		System.err.println("  -d <base> <derived> : 型の継承を行う場合に基本型と派生型のQNaemを指定します");
		System.err.println("  -s                  : スキーマ情報を出力します");
		System.err.println("  -I <NUM>            : インデント幅を指定します");
		System.err.println("  -t <template>       : テンプレート種別を指定します");
		System.err.println("                        現在有効な値は velocity,groovy,emptyです");
		System.err.println("  -r                  : テンプレート変数にトップレベル要素名を含めないようにします");
		System.err.println("  -z                  : minOccurs='0'の要素を無視します");
		System.err.println("QNameの指定にはWSDLファイルの文書要素で宣言されている名前空間を使用してください");
		System.exit(0);
	}
	
	private static QName resolveQName(String s, Document doc) {
		int idx = s.indexOf(':');
		if (idx == -1) {
			System.err.println("Invalid QName : " + s);
			printUsage();
		}
		String prefix = s.substring(0, idx);
		String name = s.substring(idx+1);
		String nsuri = doc.getDocumentElement().lookupNamespaceURI(prefix);
		if (nsuri == null) {
			System.err.println("Unknown prefix : " + s);
			printUsage();
		}
		return new QName(nsuri, name);
	}
	
	public static void main(String[] args) throws Exception {
		String wsdl = null;
		String op = null;
		
		boolean outputSchemaInfo = false;
		String builderType = "velocity";
		int indent = 4;
		boolean ignoreRoot = false;
		boolean ignoreZero = false;
		boolean serializeTest = false;
		
		List<String> ignore = new ArrayList<String>();
		Map<String, String> derived = new HashMap<String, String>();
		
		int len = args.length;
		for (int i=0; i<len; i++) {
			String s = args[i];
			if ("-i".equals(s)) {
				if (i+1 < len  && !args[i+1].startsWith("-")) {
					ignore.add(args[++i]);
				} else {
					printUsage();
				}
			} else if ("-d".equals(s)) {
				if (i+2 < len  && !args[i+1].startsWith("-") && !args[i+2].startsWith("-")) {
					derived.put(args[++i], args[++i]);
				} else {
					printUsage();
				}
			} else if ("-s".equals(s)) {
				outputSchemaInfo = true;
			} else if ("-I".equals(s)) {
				if (i+1 < len  && !args[i+1].startsWith("-")) {
					indent = Integer.parseInt(args[++i]);
				} else {
					printUsage();
				}
			} else if ("-t".equals(s)) {
				if (i+1 < len  && !args[i+1].startsWith("-")) {
					builderType = args[++i];
				} else {
					printUsage();
				}
			} else if ("-r".equals(s)) {
				ignoreRoot = true;
			} else if ("-z".equals(s)) {
				ignoreZero = true;
			} else if ("-st".equals(s)) {
				serializeTest = true;
			} else if (wsdl == null) {
				wsdl = s;
			} else if (op == null) {
				op = s;
			} else {
				printUsage();
			}
		}
		if (wsdl == null || op == null) {
			printUsage();
		}
		TemplateBuilder builder = null;
		if ("velocity".equals(builderType)) {
			builder = new VelocityTemplateBuilder(ignoreRoot);
		} else if ("groovy".equals(builderType)) {
			builder = new GroovyTemplateBuilder(ignoreRoot);
		} else if ("empty".equals(builderType)) {
			builder = new EmptyTemplateBuilder();
		} else {
			System.err.println("Unknown template type: " + builderType);
			printUsage();
		}
		builder.setIndent(indent);
		
		Document doc = XMLUtils.parse(new File(wsdl));
		List<TemplateHint> hints = new ArrayList<TemplateHint>();
		if (ignore.size() > 0) {
			for (String s : ignore) {
				hints.add(new IgnoreHint(resolveQName(s, doc)));
			}
		}
		if (derived.size() > 0) {
			for (Map.Entry<String, String> entry : derived.entrySet()) {
				QName b = resolveQName(entry.getKey(), doc);
				QName d = resolveQName(entry.getValue(), doc);
				hints.add(new DerivedHint(b, d));
			}
		}
		if (ignoreZero) {
			hints.add(new IgnoreZeroHint());
		}
		long t = System.currentTimeMillis();
		SoapClient client = new SoapClient(new File(wsdl), builder);
		client.setDebug(outputSchemaInfo);
		if (serializeTest) {
			System.out.println("Create soapClient: " + (System.currentTimeMillis() - t) + "ms");
			
			ByteArrayOutputStream os1 = new ByteArrayOutputStream();
			ByteArrayOutputStream os2 = new ByteArrayOutputStream();
			doOutput(client, op, hints, os1);
			doOutput(serialize(client), op, hints, os2);
			System.out.println(Arrays.equals(os1.toByteArray(), os2.toByteArray()) ? "OK" : "NG");
		} else {
			doOutput(client, op, hints, System.out);
		}
	}
	
	private static void doOutput(SoapClient client, String op, List<TemplateHint> hints, OutputStream os) throws IOException {
		SoapTemplate template = client.generateTemplate(op, hints);
		template.writeTo(os);
	}
	
	private static SoapClient serialize(SoapClient client) throws IOException, ClassNotFoundException {
		File f = new File("temp.txt");
		ObjectOutputStream os = new ObjectOutputStream(new FileOutputStream(f));
		try {
			os.writeObject(client);
		} finally {
			os.close();
		}
		long t = System.currentTimeMillis();
		ObjectInputStream is = new ObjectInputStream(new FileInputStream(f));
		try {
			return (SoapClient)is.readObject();
		} finally {
			is.close();
			System.out.println("Load soapClient: " + (System.currentTimeMillis() - t) + "ms");
		}
	}
}
