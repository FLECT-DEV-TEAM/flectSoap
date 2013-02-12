package jp.co.flect.soap.event;

import java.util.EventListener;

public interface SoapInvokeListener extends EventListener {
	
	public void beforeInvoke(SoapInvokeEvent e);
	public void afterInvoke(SoapInvokeEvent e);
}
