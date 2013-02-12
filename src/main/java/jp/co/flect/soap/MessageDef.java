package jp.co.flect.soap;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import javax.xml.namespace.QName;

/**
 * SOAPのメッセージ定義
 */
public class MessageDef implements Serializable {
	
	private static final long serialVersionUID = -5338277691861891256L;
	
	private String name;
	private List<QName> headerList = null;
	private List<QName> bodyList = new ArrayList<QName>();
	private boolean fault;
	
	public MessageDef(String name, boolean fault) {
		this.name = name;
		this.fault = fault;
	}
	
	public String getName() { return this.name;}
	
	public boolean isFault() { return this.fault;}
	
	public void addBody(QName qname) {
		bodyList.add(qname);
	}
	
	public void addHeader(QName qname) {
		if (headerList == null) {
			headerList = new ArrayList<QName>();
		}
		headerList.add(qname);
	}
	
	public Iterator<QName> getHeaders() {
		return headerList == null ? Collections.EMPTY_LIST.iterator() : headerList.iterator();
	}
	
	public Iterator<QName> getBodies() {
		return bodyList.iterator();
	}
	
	public int getHeaderCount() { return headerList == null ? 0 : headerList.size();}
	public int getBodyCount() { return bodyList.size();}
}
