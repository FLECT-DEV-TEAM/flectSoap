package jp.co.flect.soap.event;

import jp.co.flect.xml.XMLUtils;
import jp.co.flect.soap.SoapFaultException;
import jp.co.flect.log.LoggerFactory;
import jp.co.flect.log.Logger;

import java.util.List;
import java.util.ArrayList;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import java.io.IOException;
import java.io.StringReader;

public class DebugListener implements SoapInvokeListener {
	
	private static final Logger log = LoggerFactory.getLogger(DebugListener.class);
	
	private List<String> ignoreList = new ArrayList();
	private long startTime;
	
	public void addIgnoreOperation(String op) { this.ignoreList.add(op);}
	public void removeIgnoreOperation(String op) { this.ignoreList.remove(op);}
	
	public List<String> getIgnoreOperations() { return this.ignoreList;}
	
	public void beforeInvoke(SoapInvokeEvent e) {
		this.startTime = System.currentTimeMillis();
		
		String op = e.getOperation();
		if (this.ignoreList.contains(op)) {
			return;
		}
		String msg = indent(e.getRequest());
		log.info("SoapRequest:" + op + "\trequest:" + msg);
	}
	
	public void afterInvoke(SoapInvokeEvent e) {
		long time = System.currentTimeMillis() - this.startTime;
		
		String op = e.getOperation();
		if (this.ignoreList.contains(op)) {
			log.info("SoapOperation: " + op + "\ttime:" + time + "ms");
			return;
		}
		Exception ex = e.getException();
		if (ex != null) {
			if (ex instanceof SoapFaultException) {
				SoapFaultException fault = (SoapFaultException)ex;
				String msg = indent(fault.getSoapResponse().getAsString());
				log.error("SoapFault:" + op + "\ttime:" + time + "ms\tresponse:" + msg);
			} else {
				log.error("SoapError:" + op + "\ttime:" + time + "ms\terror:" + ex.toString());
			}
			log.error(ex.getMessage(), ex);
		} else {
			String msg = indent(e.getResponse().getAsString());
			log.info("SoapResponse:" + op + "\ttime:" + time + "ms\tresponse:" + msg);
		}
	}

	private String indent(String xml) {
		try {
			Document doc = XMLUtils.parse(new StringReader(xml));
			return XMLUtils.getAsString(doc, true);
		} catch (IOException e) {
			throw new IllegalStateException(e);
		} catch (SAXException e) {
			throw new IllegalStateException(e);
		}
	}
}
