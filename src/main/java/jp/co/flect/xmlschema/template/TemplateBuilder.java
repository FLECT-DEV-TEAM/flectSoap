package jp.co.flect.xmlschema.template;

import jp.co.flect.xmlschema.XMLSchema;
import jp.co.flect.template.TemplateEngine;
import java.util.List;
import java.io.IOException;
import java.io.OutputStream;

/**
 * XMLSchema����e���v���[�g���쐬����Interface�ł��B
 */
public interface TemplateBuilder extends Cloneable {
	
	/** �X�L�[�}���o�͂�NamespacePrefix */
	public static final String FSI_PREFIX    = "fsi";
	/** �X�L�[�}���o�͂�Namespace */
	public static final String FSI_NAMESPACE = "http://www.flect.co.jp/application/xmlschema";
	
	/** �C���f���g����Ԃ��܂��B0�̏ꍇ�̓C���f���g���܂���B */
	public int getIndent();
	/** �C���f���g����ݒ肵�܂��B */
	public void setIndent(int n);
	
	/** XML�錾���o�͂��邩�ǂ�����Ԃ��܂��B */
	public boolean isOutputXMLDecl();
	/** XML�錾���o�͂��邩�ǂ�����ݒ肵�܂��B */
	public void setOutputXMLDecl(boolean b);
	
	/** �����C���f���g���x����Ԃ��܂��B */
	public int getInitialIndent();
	/** �����C���f���g���x����ݒ肵�܂��B */
	public void setInitialIndent(int n);
	
	/** 
	 * XMLSchema��ǉ����܂��B 
	 * @return schema�̖��O��ԂɑΉ�����prefix
	 */
	public String addSchema(XMLSchema schema);

	/** 
	 * NamespacePrefix���w�肵��XMLSchema��ǉ����܂��B 
	 * @return ������prefix
	 */
	public String addSchema(String prefix, XMLSchema schema);
	
	/** �ǉ����ꂽXMLSchema��List��Ԃ��܂��B */
	public List<XMLSchema> getSchemaList();
	
	/** �e���v���[�g���ɃX�L�[�}�����o�͂��邩�ǂ�����Ԃ��܂��B */
	public boolean isOutputSchemaInfo();
	/** �e���v���[�g���ɃX�L�[�}�����o�͂��邩�ǂ�����ݒ肵�܂��B */
	public void setOutputSchemaInfo(boolean b);
	
	/** �e���v���[�g�o�͎��ɗL����NamespacePrefix��URL��ǉ����܂��B */
	public void addContextNamespace(String prefix, String namespace);
	
	/** �e���v���[�g�o�͎��ɗL����NamespacePrefix���폜���܂��B */
	public void removeContextNamespace(String prefix);
	
	/** 
	 * �e���v���[�g��OutputStream�ɏo�͂��܂��B 
	 * @param namespace �o�͑Ώۗv�f��NamespaceURI
	 * @param name �o�͑Ώۗv�f��LocalName
	 * @param hints �X�L�[�}���߂Ɏg�p����q���g��List�Bnull�ł��ǂ�
	 * @param os �o�̓X�g���[��
	 */
	public void writeTo(String namespace, String name, List<TemplateHint> hints, OutputStream os) throws IOException;
	
	/** TemplateEngine��Ԃ��܂��B */
	public TemplateEngine getTemplateEngine();
	
	public Object clone();
}
