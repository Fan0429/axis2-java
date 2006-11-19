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
package org.apache.axis2.jaxws.message.databinding.impl;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.List;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.JAXBIntrospector;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.util.StAXUtils;
import org.apache.axis2.jaxws.ExceptionFactory;
import org.apache.axis2.jaxws.message.Message;
import org.apache.axis2.jaxws.message.MessageException;
import org.apache.axis2.jaxws.message.XMLPart;
import org.apache.axis2.jaxws.message.attachments.JAXBAttachmentMarshaller;
import org.apache.axis2.jaxws.message.attachments.JAXBAttachmentUnmarshaller;
import org.apache.axis2.jaxws.message.databinding.JAXBBlock;
import org.apache.axis2.jaxws.message.databinding.JAXBBlockContext;
import org.apache.axis2.jaxws.message.databinding.JAXBUtils;
import org.apache.axis2.jaxws.message.databinding.XSDListUtils;
import org.apache.axis2.jaxws.message.factory.BlockFactory;
import org.apache.axis2.jaxws.message.impl.BlockImpl;
import org.apache.axis2.jaxws.util.XMLRootElementUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * JAXBBlockImpl
 * 
 * A Block containing a JAXB business object (either a JAXBElement or an object with @XmlRootElement)
 */
public class JAXBBlockImpl extends BlockImpl implements JAXBBlock {

    private static final Log log = LogFactory.getLog(JAXBBlockImpl.class);
    
	/**
	 * Called by JAXBBlockFactory
	 * @param busObject..The business object must be a JAXBElement or an object
     * with an @XMLRootElement.  This is assertion is validated in the JAXBFactory.
	 * @param busContext
	 * @param qName
	 * @param factory
	 */
	JAXBBlockImpl(Object busObject, JAXBBlockContext busContext, QName qName, BlockFactory factory) throws JAXBException {
		super(busObject, 
				busContext, 
				(qName==null) ? getQName(busObject, busContext): qName , 
				factory);
	}

	/**
	 * Called by JAXBBlockFactory
	 * @param omelement
	 * @param busContext
	 * @param qName
	 * @param factory
	 */
	JAXBBlockImpl(OMElement omElement, JAXBBlockContext busContext, QName qName, BlockFactory factory) {
		super(omElement, busContext, qName, factory);
	}

	@Override
	protected Object _getBOFromReader(XMLStreamReader reader, Object busContext) throws XMLStreamException, MessageException {
	    // Get the JAXBBlockContext.  All of the necessry information is recorded on it
        JAXBBlockContext ctx = (JAXBBlockContext) busContext;
        try {
            // TODO Re-evaluate Unmarshall construction w/ MTOM
			Unmarshaller u = JAXBUtils.getJAXBUnmarshaller(ctx.getJAXBContext());
            
            // If MTOM is enabled, add in the AttachmentUnmarshaller
            if (isMTOMEnabled()) {
                if (log.isDebugEnabled()) 
                    log.debug("Adding JAXBAttachmentUnmarshaller to Unmarshaller");
                
                XMLPart xp = getParent();
                Message msg = xp.getParent();
                
                // TODO Pool ?
                JAXBAttachmentUnmarshaller aum = new JAXBAttachmentUnmarshaller();
                aum.setMessage(msg);
                u.setAttachmentUnmarshaller(aum);
            }
            Object jaxb = null;
            
            // Unmarshal into the business object.
            if (ctx.getRPCType() == null) {
                jaxb = unmarshalByElement(u, reader); // preferred and always used for style=document
            } else {
                jaxb = unmarshalByType(u, reader, ctx.getRPCType());
            }
            
            // Set the qname 
            QName qName = XMLRootElementUtil.getXmlRootElementQName(jaxb);
            if (qName != null) {  // qname should always be non-null
                setQName(qName); 
            }
            
            // Successfully unmarshalled the object
            // TODO remove attachment unmarshaller ?
            JAXBUtils.releaseJAXBUnmarshaller(ctx.getJAXBContext(), u);
            return jaxb;
		} catch(JAXBException je) {
            if (log.isDebugEnabled()) {
                try {
                    log.debug("JAXBContext for unmarshal failure:" + ctx.getJAXBContext());
                } catch (Exception e) {
                }
            }
			throw ExceptionFactory.makeMessageException(je);
		}
	}

	@Override
	protected XMLStreamReader _getReaderFromBO(Object busObj, Object busContext) throws XMLStreamException, MessageException {
		// TODO Review and determine if there is a better solution
		
		// This is hard because JAXB does not expose a reader from the business object.
		// The solution is to write out the object and use a reader to read it back in.
		// First create an XMLStreamWriter backed by a writer
		StringWriter sw = new StringWriter();
		XMLStreamWriter writer = StAXUtils.createXMLStreamWriter(sw);
		
		// Write the business object to the writer
		_outputFromBO(busObj, busContext, writer);
		
		// Flush the writer and get the String
		writer.flush();
		sw.flush();
		String str = sw.toString();
        writer.close();
		
		// Return a reader backed by the string
		StringReader sr = new StringReader(str);
		return StAXUtils.createXMLStreamReader(sr);
	}

	@Override
	protected void _outputFromBO(Object busObject, Object busContext, XMLStreamWriter writer) throws XMLStreamException, MessageException {
        JAXBBlockContext ctx = (JAXBBlockContext) busContext;
        try {
			// Very easy, use the Context to get the Marshaller.
			// Use the marshaller to write the object.  
			Marshaller m = JAXBUtils.getJAXBMarshaller(ctx.getJAXBContext());
			
			// TODO Should MTOM be inside getMarshaller ?
			// If MTOM is enabled, add in the AttachmentMarshaller.
            if (isMTOMEnabled()) {
                if (log.isDebugEnabled())
                    log.debug("Adding JAXBAttachmentMarshaller to Marshaller");
                
                XMLPart xp = getParent();
                Message msg = xp.getParent();
                
                // Pool
                JAXBAttachmentMarshaller am = new JAXBAttachmentMarshaller();
                am.setMessage(msg);
                m.setAttachmentMarshaller(am);
            }   
            
            // Marshal the object
            if (ctx.getRPCType() == null) {
                marshalByElement(busObject, m, writer);
            } else {
                marshalByType(busObject, m, writer, ctx.getRPCType());
            }
            
            // Successfully marshalled the data
            // TODO remove attachment marshaller ?
            JAXBUtils.releaseJAXBMarshaller(ctx.getJAXBContext(), m);
		} catch(JAXBException je) {
            if (log.isDebugEnabled()) {
                try {
                    log.debug("JAXBContext for marshal failure:" + ctx.getJAXBContext());
                } catch (Exception e) {
                }
            }
			throw ExceptionFactory.makeMessageException(je);
		}
	}

	/**
	 * Get the QName from the jaxb object
	 * @param jaxb
	 * @param jbc
	 * @throws MessageException
	 */
	private static QName getQName(Object jaxb, JAXBBlockContext ctx) throws JAXBException {
		JAXBIntrospector jbi = JAXBUtils.getJAXBIntrospector(ctx.getJAXBContext());
		QName qName = jbi.getElementName(jaxb);
		JAXBUtils.releaseJAXBIntrospector(ctx.getJAXBContext(), jbi);
		return qName;
	}
    
    private boolean isMTOMEnabled() {
        XMLPart xp = getParent();
        if (xp != null) {
            Message msg = xp.getParent();
            if (msg != null && msg.isMTOMEnabled())
                return true;
        }
        
        return false;
    }
    
    /**
     * Preferred way to marshal objects.  
     * @param b Object that can be rendered as an element and the element name is known by the Marshaller
     * @param m Marshaller
     * @param writer XMLStreamWriter
     */
    private static void marshalByElement(Object b, Marshaller m, XMLStreamWriter writer) throws MessageException {
        // TODO Log and trace here would be helpful
        try {
            m.marshal(b, writer);
        } catch (Exception e) {
            throw ExceptionFactory.makeMessageException(e);
        }
    }
    
    /**
     * Preferred way to unmarshal objects
     * @param u Unmarshaller
     * @param reader XMLStreamReader
     * @return Object that represents an element 
     * @throws MessageException
     */
    private static Object unmarshalByElement(Unmarshaller u, XMLStreamReader reader) throws MessageException {
        // TODO Log and trace here would be helpful
        try {
            return u.unmarshal(reader);
        } catch (Exception e) {
            throw ExceptionFactory.makeMessageException(e);
        }
    }
    
    /**
     * Marshal objects by type  
     * @param b Object that can be rendered as an element, but the element name is not known to the schema (i.e. rpc)
     * @param m Marshaller
     * @param writer XMLStreamWriter
     * @param type
     */
    private static void marshalByType(Object b, Marshaller m, XMLStreamWriter writer, Class type) throws MessageException {
        // TODO Log and trace here would be helpful
        try {
            
            // NOTE
            // Example:
            // <xsd:simpleType name="LongList">
            //   <xsd:list>
            //     <xsd:simpleType>
            //       <xsd:restriction base="xsd:unsignedInt"/>
            //     </xsd:simpleType>
            //   </xsd:list>
            // </xsd:simpleType>
            // <element name="myLong" nillable="true" type="impl:LongList"/>
            //
            // LongList will be represented as an int[]
            // On the wire myLong will be represented as a list of integers
            // with intervening whitespace
            //   <myLong>1 2 3</myLong>
            //
            // Unfortunately, we are trying to marshal by type.  Therefore
            // we want to marshal an element (foo) that is unknown to schema.
            // If we use the normal marshal code, the wire will look like
            // this (which is incorrect):
            //  <foo><item>1</item><item>2</item><item>3</item></foo>
            //
            // The solution is to detect this situation and marshal the 
            // String instead.  Then we get the correct wire format:
            // <foo>1 2 3</foo>
            if (isXSDList(type)) {
                QName qName = XMLRootElementUtil.getXmlRootElementQName(b);
                String text = XSDListUtils.toXSDListString(XMLRootElementUtil.getTypeEnabledObject(b));
                b = XMLRootElementUtil.getElementEnabledObject(qName.getNamespaceURI(), qName.getLocalPart(), String.class, text);
            }
            m.marshal(b, writer);
        } catch (Exception e) {
            throw ExceptionFactory.makeMessageException(e);
        }
    }
    
    /**
     * The root element being read is defined by schema/JAXB; however its contents are known by schema/JAXB.
     * Therefore we use unmarshal by the declared type
     * (This method is used to unmarshal rpc elements)
     * @param u Unmarshaller
     * @param reader XMLStreamReader
     * @param type Class
     * @return Object 
     * @throws MessageException
     */
    private static Object unmarshalByType(Unmarshaller u, XMLStreamReader reader, Class type) throws MessageException {
        // TODO Log and trace here would be helpful
        try {
            // Unfortunately RPC is type based.  Thus a
            // declared type must be used to unmarshal the xml.
            Object jaxb;
            
            
            
            
            if (!isXSDList(type) ) {
                // Normal case: We are not unmarshalling an xsd:list
                jaxb = u.unmarshal(reader, type);
            } else {
                // If this is an xsd:list, we need to return the appropriate list or array (see NOTE above)
                // First unmarshal as a String
                jaxb = u.unmarshal(reader, String.class);
                
                // Second convert the String into a list or array
                if (XMLRootElementUtil.getTypeEnabledObject(jaxb) instanceof String) {
                    QName qName = XMLRootElementUtil.getXmlRootElementQName(jaxb);
                    Object obj = XSDListUtils.fromXSDListString((String) XMLRootElementUtil.getTypeEnabledObject(jaxb), type);
                    jaxb = XMLRootElementUtil.getElementEnabledObject(qName.getNamespaceURI(), qName.getLocalPart(), type, obj);
                }
            } 
            return jaxb;
        } catch (Exception e) {
            throw ExceptionFactory.makeMessageException(e);
        }
    }
    
    /**
     * Detect if t represents an xsd:list
     * @param t
     * @return 
     */
    private static boolean isXSDList(Class t) {
        // TODO This code returns true if the 
        // class is an array or List.  The correct solution
        // is to probably pass this information into the
        // JAXBBlockContext.  I noticed that JAX-WS marks
        // each xsd:list param/return with an @XmlList annotation.
        
        // 
        // Example:
        // <xsd:simpleType name="LongList">
        //   <xsd:list>
        //     <xsd:simpleType>
        //       <xsd:restriction base="xsd:unsignedInt"/>
        //     </xsd:simpleType>
        //   </xsd:list>
        // </xsd:simpleType>
        return (t.isArray() || List.class.isAssignableFrom(t));
    }
}
