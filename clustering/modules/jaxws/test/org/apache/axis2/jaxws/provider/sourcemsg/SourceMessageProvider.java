/*
 * Copyright 2004,2005 The Apache Software Foundation.
 * Copyright 2006 International Business Machines Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.axis2.jaxws.provider.sourcemsg;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringWriter;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.ws.Provider;
import javax.xml.ws.Service;
import javax.xml.ws.ServiceMode;
import javax.xml.ws.WebServiceProvider;

@WebServiceProvider()
@ServiceMode(value=Service.Mode.MESSAGE)
public class SourceMessageProvider implements Provider<Source> {
    String responseAsString = new String("<?xml version=\"1.0\" encoding=\"UTF-8\"?><soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\"><soapenv:Header/><soapenv:Body><ns2:ReturnType xmlns:ns2=\"http://test\"><return_str>some response</return_str></ns2:ReturnType></soapenv:Body></soapenv:Envelope>");
    public Source invoke(Source source) {
    	System.out.println(">> SourceMessageProvider: Request received.");
    	//System.out.println(">> Source toString: \n"+source.toString());
    	
    	try{
    		StringWriter writer = new StringWriter();
	        Transformer t = TransformerFactory.newInstance().newTransformer();
	        Result result = new StreamResult(writer);
	        t.transform(source, result);
	        System.out.println(">> Source Request on Server: \n"+writer.getBuffer().toString());
	        
	    	byte[] bytes = responseAsString.getBytes();
	        ByteArrayInputStream stream = new ByteArrayInputStream(bytes);
	        Source srcStream = new StreamSource((InputStream) stream);
	        return srcStream;
    	}catch(Exception e){
    		e.printStackTrace();
    	}
    	return null;
    }
    	

}