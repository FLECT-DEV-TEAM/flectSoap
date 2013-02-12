package jp.co.flect.soap;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * SOAPのオペレーション定義
 */
public class OperationDef implements Serializable {
	
	private static final long serialVersionUID = -7011392191514950348L;

	private String action = "";
	private String name;
	private MessageDef input;
	private MessageDef output;
	private List<MessageDef> faults = null;
	private String documentation;
	
	public OperationDef(String name, MessageDef input, MessageDef output) {
		this.name = name;
		this.input = input;
		this.output = output;
	}
	
	public String getName() { return this.name;}
	public MessageDef getRequestMessage() { return this.input;}
	public MessageDef getResponseMessage() { return this.output;}
	
	public String getSoapAction() { return this.action;}
	public void setSoapAction(String s) { this.action = s;}
	
	public String getDocumentation() { return this.documentation;}
	public void setDocumentation(String s) { this.documentation = s;}
	
	public void addFault(MessageDef m) {
		if (this.faults == null) {
			this.faults = new ArrayList<MessageDef>();
		}
		this.faults.add(m);
	}
	
	public Iterator<MessageDef> getFaults() { 
		return this.faults == null ? Collections.EMPTY_LIST.iterator() : this.faults.iterator();
	}
	
	public int getFaultCount() { return this.faults == null ? 0 : this.faults.size();}
}
