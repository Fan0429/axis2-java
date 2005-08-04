package org.apache.axis2.soap12testing.webservices;

/*
 * Copyright 2004,2005 The Apache Software Foundation.
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
 *
 * 
 */

/**
 * Author : Deepal Jayasinghe
 * Date: Jul 26, 2005
 * Time: 3:00:47 PM
 */

import org.apache.axis2.om.OMElement;

public class SOAP12TestWebServiceDefault {
    public OMElement echo(OMElement element) {
        if (element != null) {
            if (element.getLocalName().equals("echoOk")) {
                element.setLocalName("responseOk");

            } else if (element.getLocalName().equals("returnVoid")) {
                element.setLocalName("returnVoidResponse");
            }
        }
        return element;
    }
}
