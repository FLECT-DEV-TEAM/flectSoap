package jp.co.flect.soap.event;

import java.util.EventObject;
import jp.co.flect.soap.SoapClient;
import jp.co.flect.soap.SoapFaultException;

public class SoapFaultEvent extends EventObject {
	
	private static final long serialVersionUID = -6813388467755290999L;
	
	private SoapFaultException fault;
	
	public SoapFaultEvent(SoapClient client, SoapFaultException e) {
		super(client);
		this.fault = e;
	}
	
	public SoapClient getClient() { return (SoapClient)getSource();}
	public SoapFaultException getSoapFault() { return this.fault;}
}
