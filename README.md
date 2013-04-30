flectSoap
-----------------
-----------------

Overview
--------
Simple SOAP client for Java.

Its features consist of 

- Generate SOAP template from WSDL
- Embed parameters to SOAP template
- Invoke SOAP request by http(s)

Dependencies
------------
If you want to build this library, you must build and install following maven projects.

- [flectCommon](https://github.com/shunjikonishi/flectCommon)

Usage
-----
    File wsdl = new File("partner.wsdl");
    SoapClient client = new SoapClient(wsdl);
    
    //Make input parameters
    ExtendedMap input = new ExtendedMap(true);
    input.putDeep("login.username", username);
    input.putDeep("login.password", password);
    
    //Invoke and get results
    SoapResponse res = invoke("login", null, input);
    ExtendedMap output = res.getAsMap();
    String sessionId = (String)output.getDeep("loginResponse.result.sessionId");
    int sessionLifetime = Integer.parseInt((String)output.getDeep("loginResponse.result.userInfo.sessionSecondsValid"));

License
-------
MIT
