package jp.co.flect.soap;

import jp.co.flect.util.ExtendedMap;

/**
 * SOAPFault
 */
public class SoapFaultException extends SoapException {
	
	private static final long serialVersionUID = 6091979926376206130L;
	
	private SoapResponse response;
	
	private static String getString(SoapResponse res, String path) {
		try {
			ExtendedMap map = res.getAsMap();
			Object o = map.getDeep(path);
			return o == null ? null : o.toString();
		} catch (SoapException e) {
			//not occur
			e.printStackTrace();
			throw new IllegalStateException(e);
		}
	}
	
	public SoapFaultException(SoapResponse res) {
		super(getString(res, "Fault.faultstring"));
		this.response = res;
		setResponseBody(res.getAsString());
	}
	
	public SoapResponse getSoapResponse() { return this.response;}
	
	public String getFaultString() { return getMessage();}
	public String getFaultCode() { return getString(this.response, "Fault.faultcode");}
	public String getDetail() { return getString(this.response, "Fault.detail");}
}
