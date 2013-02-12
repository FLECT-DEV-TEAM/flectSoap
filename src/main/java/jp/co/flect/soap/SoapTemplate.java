package jp.co.flect.soap;

import jp.co.flect.template.Template;
import jp.co.flect.xmlschema.template.TemplateHint;
import jp.co.flect.xmlschema.template.HintBuilder;
import java.io.Serializable;
import java.util.List;
import java.util.ArrayList;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.FileOutputStream;
import java.io.BufferedWriter;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;

/**
 * SOAPオペレーションのテンプレート
 */
public class SoapTemplate implements Serializable {
	
	private static final long serialVersionUID = -6688417919619115134L;
	
	private String name;
	private String templateStr;
	private List<TemplateHint> hints;
	
	private transient Template template;
	
	/**
	 * コンストラクタ
	 * @param name SOAPオペレーション名
	 * @param templateStr テンプレート文字列
	 */
	public SoapTemplate(String name, String templateStr) {
		this(name, templateStr, null);
	}
	
	/**
	 * コンストラクタ
	 * @param name SOAPオペレーション名
	 * @param templateStr テンプレート文字列
	 * @param hints ヒント
	 */
	public SoapTemplate(String name, String templateStr, List<TemplateHint> hints) {
		this.name = name;
		this.templateStr = templateStr;
		this.hints = hints;
	}
	
	/**
	 * writeToメソッドで保存した内容から構築するコンストラクタ
	 */
	public SoapTemplate(InputStream is) throws IOException {
		loadFrom(is);
	}
	
	/**
	 * ファイルから構築するコンストラクタ
	 */
	public SoapTemplate(File f) throws IOException {
		loadFrom(new FileInputStream(f));
	}
	
	/**
	 * SOAPのオペレーション名を返します。
	 */
	public String getName() { return this.name;}
	
	/**
	 * テンプレートの文字列を返します。
	 */
	public String getTemplateString() { return this.templateStr;}
	
	/**
	 * テンプレート生成に使用したヒントを返します。
	 */
	public List<TemplateHint> getHints() { return this.hints;}
	
	
	//Package local method
	Template getTemplate() { return this.template;}
	void setTemplate(Template t) { this.template = t;}
	
	/**
	 * このテンプレートの内容をストリームに書き込みます。
	 */
	public void writeTo(OutputStream os) throws IOException {
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "utf-8"));
		try {
			writer.write("name: ");
			writer.write(this.name);
			writer.write("\r\n");
			if (this.hints != null) {
				for (TemplateHint hint : this.hints) {
					writer.write(hint.getAsString());
					writer.write("\r\n");
				}
			}
			writer.write("\r\n");
			writer.write(this.templateStr);
		} finally {
			writer.close();
		}
	}
	
	/**
	 * このテンプレートの内容をファイルに書き込みます。
	 */
	public void saveToFile(File f) throws IOException {
		writeTo(new FileOutputStream(f));
	}
	
	private void loadFrom(InputStream is) throws IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(is, "utf-8"));
		try {
			HintBuilder hintBuilder = new HintBuilder();
			List<TemplateHint> list = new ArrayList<TemplateHint>();
			
			boolean body = false;
			StringBuilder buf = new StringBuilder();
			String line = reader.readLine();
			while (line != null) {
				if (body) {
					if (buf.length() > 0) {
						buf.append("\n");
					}
					buf.append(line);
				} else if (line.length() == 0) {
					body = true;
				} else if (line.startsWith("name:")) {
					this.name = line.substring(5).trim();
				} else {
					TemplateHint hint = hintBuilder.parse(line);
					if (hint != null) {
						list.add(hint);
					}
				}
				line = reader.readLine();
			}
			if (list.size() > 0) {
				this.hints = list;
			}
			this.templateStr = buf.toString();
			if (this.name == null || buf.length() == 0) {
				throw new IOException("Invalid template format");
			}
		} finally {
			reader.close();
		}
	}
	
	public String toString() {
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		try {
			writeTo(os);
			return new String(os.toByteArray(), "utf-8");
		} catch (IOException e) {
			//not occur
			throw new IllegalStateException();
		}
	}
	
}
