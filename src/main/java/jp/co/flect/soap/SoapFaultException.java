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
	
	public boolean isSoap12() { return this.response.isSoap12();}
	public boolean isSoap11() { return this.response.isSoap11();}
	
	public SoapFaultException(SoapResponse res) {
		super(getString(res, "Fault.faultstring"));
		this.response = res;
		setResponseBody(res.getAsString());
	}
	
	public SoapResponse getSoapResponse() { return this.response;}
	
	//For SOAP 1.1
	public String getFaultString() { return getMessage();}
	public String getFaultCode() { return getString(this.response, "Fault.faultcode");}
	public String getDetail() { return getString(this.response, "Fault.detail");}
	
	//For SOAP 1.2
	public String getCode() { return getString(this.response, "Fault.Code.Value");}
	public String getReason() { return getString(this.response, "Fault.Reason.Text");}
}
