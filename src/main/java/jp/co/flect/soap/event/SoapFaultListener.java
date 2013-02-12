package jp.co.flect.soap.event;

import java.util.EventListener;

public interface SoapFaultListener extends EventListener {
	
	public void soapFault(SoapFaultEvent e);
}
