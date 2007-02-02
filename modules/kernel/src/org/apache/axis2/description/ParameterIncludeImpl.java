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
*/


package org.apache.axis2.description;

import org.apache.axiom.om.OMAttribute;
import org.apache.axiom.om.OMElement;
import org.apache.axis2.AxisFault;
import org.apache.axis2.deployment.DeploymentConstants;
import org.apache.axis2.util.ObjectStateUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.xml.namespace.QName;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

/**
 * Class ParameterIncludeImpl
 */
public class ParameterIncludeImpl implements ParameterInclude, Externalizable {

    /*
     * setup for logging
     */
    private static final Log log = LogFactory.getLog(ParameterIncludeImpl.class);

    private static final String myClassName = "ParameterIncludeImpl";

    /**
     * @serial The serialization version ID tracks the version of the class.
     * If a class definition changes, then the serialization/externalization
     * of the class is affected. If a change to the class is made which is
     * not compatible with the serialization/externalization of the class,
     * then the serialization version ID should be updated.
     * Refer to the "serialVer" utility to compute a serialization
     * version ID.
     */
    private static final long serialVersionUID = 8153736719090126891L;

    /**
     * @serial Tracks the revision level of a class to identify changes to the 
     * class definition that are compatible to serialization/externalization.
     * If a class definition changes, then the serialization/externalization
     * of the class is affected. 
     * Refer to the writeExternal() and readExternal() methods.
     */
    // supported revision levels, add a new level to manage compatible changes
    private static final int REVISION_1 = 1;
    // current revision level of this object
    private static final int revisionID = REVISION_1;


    /**
     * Field parmeters
     */
    protected final HashMap parameters;

    /**
     * Constructor ParameterIncludeImpl.
     */
    public ParameterIncludeImpl() {
        parameters = new HashMap();
    }

    /**
     * Method addParameter
     *
     * @param param
     */
    public void addParameter(Parameter param) {
        if (param != null) {
            parameters.put(param.getName(), param);
        }
    }

    public void removeParameter(Parameter param) throws AxisFault {
        parameters.remove(param.getName());
    }

    /**
     * Since at runtime it parameters may be modified
     * to get the original state this method can be used
     *
     * @param parameters <code>OMElement</code>
     * @throws AxisFault
     */
    public void deserializeParameters(OMElement parameters) throws AxisFault {
        Iterator iterator =
                parameters.getChildrenWithName(new QName(DeploymentConstants.TAG_PARAMETER));

        while (iterator.hasNext()) {

            // this is to check whether some one has locked the parmeter at the top level
            OMElement parameterElement = (OMElement) iterator.next();
            Parameter parameter = new Parameter();

            // setting parameterElement
            parameter.setParameterElement(parameterElement);

            // setting parameter Name
            OMAttribute paraName =
                    parameterElement.getAttribute(new QName(DeploymentConstants.ATTRIBUTE_NAME));

            parameter.setName(paraName.getAttributeValue());

            // setting parameter Value (the child element of the parameter)
            OMElement paraValue = parameterElement.getFirstElement();

            if (paraValue != null) {
                parameter.setValue(parameterElement);
                parameter.setParameterType(Parameter.OM_PARAMETER);
            } else {
                String paratextValue = parameterElement.getText();

                parameter.setValue(paratextValue);
                parameter.setParameterType(Parameter.TEXT_PARAMETER);
            }

            // setting locking attribute
            OMAttribute paraLocked =
                    parameterElement.getAttribute(new QName(DeploymentConstants.ATTRIBUTE_LOCKED));

            if (paraLocked != null) {
                String lockedValue = paraLocked.getAttributeValue();

                if ("true".equals(lockedValue)) {
                    parameter.setLocked(true);
                } else {
                    parameter.setLocked(false);
                }
            }

            addParameter(parameter);
        }
    }

    /**
     * Method getParameter.
     *
     * @param name
     * @return Returns parameter.
     */
    public Parameter getParameter(String name) {
        return (Parameter) parameters.get(name);
    }

    public ArrayList getParameters() {
        Collection col = parameters.values();
        ArrayList para_list = new ArrayList();

        for (Iterator iterator = col.iterator(); iterator.hasNext();) {
            Parameter parameter = (Parameter) iterator.next();

            para_list.add(parameter);
        }

        return para_list;
    }

    // to check whether the parameter is locked at any level
    public boolean isParameterLocked(String parameterName) {
        return false;
    }

    /* ===============================================================
     * Externalizable support 
     * ===============================================================
     */
    

    /**
     * Save the contents of this object.
     * <p>
     * NOTE: Transient fields and static fields are not saved.
     *       Also, objects that represent "static" data are
     *       not saved, except for enough information to be
     *       able to find matching objects when the message
     *       context is re-constituted.
     *
     * @param out    The stream to write the object contents to
     * 
     * @exception IOException
     */
    public void writeExternal(ObjectOutput out) throws IOException
    {
        // write out contents of this object

        //---------------------------------------------------------
        // in order to handle future changes to the message 
        // context definition, be sure to maintain the 
        // object level identifiers
        //---------------------------------------------------------
        // serialization version ID
        out.writeLong(serialVersionUID);

        // revision ID
        out.writeInt(revisionID);

        //---------------------------------------------------------
        // collection of parameters
        //---------------------------------------------------------
        ObjectStateUtils.writeHashMap(out, parameters, "ParameterIncludeImpl.parameters");

    }


    /**
     * Restore the contents of the object that was previously saved.
     * <p> 
     * NOTE: The field data must read back in the same order and type
     * as it was written.  Some data will need to be validated when 
     * resurrected.
     *
     * @param in    The stream to read the object contents from 
     * 
     * @exception IOException
     * @exception ClassNotFoundException
     */
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
    {
        // trace point
        if (log.isTraceEnabled())
        {
            log.trace(myClassName+":readExternal():  BEGIN  bytes available in stream ["+in.available()+"]  ");
        }

        // serialization version ID
        long suid = in.readLong();

        // revision ID
        int  revID = in.readInt();

        // make sure the object data is in a version we can handle
        if (suid != serialVersionUID)
        {
            throw new ClassNotFoundException(ObjectStateUtils.UNSUPPORTED_SUID);
        }

        // make sure the object data is in a revision level we can handle
        if (revID != REVISION_1)
        {
            throw new ClassNotFoundException(ObjectStateUtils.UNSUPPORTED_REVID);
        }

        //---------------------------------------------------------
        // collection of parameters
        //---------------------------------------------------------

        HashMap tmp = ObjectStateUtils.readHashMap(in, "ParameterIncludeImpl.parameters");

        if (tmp != null)
        {
            if (parameters != null)
            {
                parameters.putAll(tmp);
            }
            else
            {
                if (log.isTraceEnabled())
                {
                    log.trace(myClassName+":readExternal():  WARNING: parameters doesnot have a defined HashMap ");
                }
            }
        }

        //---------------------------------------------------------
        // done
        //---------------------------------------------------------

    }

}
