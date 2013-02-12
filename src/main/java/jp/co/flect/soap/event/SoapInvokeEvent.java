package jp.co.flect.soap.event;

import java.util.EventObject;
import jp.co.flect.soap.SoapClient;
import jp.co.flect.soap.SoapResponse;

public class SoapInvokeEvent extends EventObject {
	
	private String action;
	private String operation;
	private String request;
	private SoapResponse response;
	private Exception exception;
	
	public SoapInvokeEvent(SoapClient client, String action, String operation, String request) {
		super(client);
		this.action = action;
		this.operation = operation;
		this.request = request;
	}
	
	public SoapClient getClient() { return (SoapClient)getSource();}
	
	public String getSoapAction() { return this.action;}
	public String getOperation() { return this.operation;}
	
	public String getRequest() { return this.request;}
	public void setRequest(String s) { this.request = s;}
	
	public SoapResponse getResponse() { return this.response;}
	public void setResponse(SoapResponse res) { this.response = res;}
	
	public Exception getException() { return this.exception;}
	public void setException(Exception ex) { this.exception = ex;}
}
