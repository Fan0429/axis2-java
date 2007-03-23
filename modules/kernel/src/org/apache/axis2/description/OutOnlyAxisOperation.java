/*
* Copyright 2004,2006 The Apache Software Foundation.
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

import org.apache.axiom.om.util.UUIDGenerator;
import org.apache.axis2.AxisFault;
import org.apache.axis2.client.OperationClient;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.async.Callback;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.ContextFactory;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.context.OperationContext;
import org.apache.axis2.context.ServiceContext;
import org.apache.axis2.engine.AxisEngine;
import org.apache.axis2.i18n.Messages;
import org.apache.axis2.wsdl.WSDLConstants;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.HashMap;

public class OutOnlyAxisOperation extends AxisOperation {

    private AxisMessage inFaultMessage;

    // just to keep the inflow , there won't be any usage
    private ArrayList inPhases;

    private AxisMessage outFaultMessage;

    private AxisMessage outMessage;

    public OutOnlyAxisOperation() {
        super();
        //setup a temporary name
        QName tmpName = new QName(this.getClass().getName() + "_" + UUIDGenerator.getUUID());
        this.setName(tmpName);
        createMessage();
        setMessageExchangePattern(WSDL20_2006Constants.MEP_URI_OUT_ONLY);
    }

    public OutOnlyAxisOperation(QName name) {
        super(name);
        createMessage();
        setMessageExchangePattern(WSDL20_2006Constants.MEP_URI_OUT_ONLY);
    }

    public void addMessage(AxisMessage message, String label) {
        if (WSDLConstants.MESSAGE_LABEL_OUT_VALUE.equals(label)) {
            outMessage = message;
        } else {
            throw new UnsupportedOperationException("Not yet implemented");
        }
    }

    public void addMessageContext(MessageContext msgContext,
                                  OperationContext opContext) throws AxisFault {
        if (!opContext.isComplete()) {
            opContext.getMessageContexts().put(MESSAGE_LABEL_OUT_VALUE,
                                               msgContext);
            opContext.setComplete(true);
        } else {
            throw new AxisFault(Messages.getMessage("mepcompleted"));
        }
    }

    public void addFaultMessageContext(MessageContext msgContext, OperationContext opContext)
            throws AxisFault {
        HashMap mep = opContext.getMessageContexts();
        MessageContext faultMessageCtxt = (MessageContext) mep.get(MESSAGE_LABEL_FAULT_VALUE);

        if (faultMessageCtxt != null) {
            throw new AxisFault(Messages.getMessage("mepcompleted"));
        } else {
            mep.put(MESSAGE_LABEL_FAULT_VALUE, msgContext);
            opContext.setComplete(true);
            opContext.cleanup();
        }
    }

    private void createMessage() {
        outMessage = new AxisMessage();
        outMessage.setDirection(WSDLConstants.WSDL_MESSAGE_DIRECTION_OUT);
        outMessage.setParent(this);
        inFaultMessage = new AxisMessage();
        inFaultMessage.setParent(this);
        outFaultMessage = new AxisMessage();
        outFaultMessage.setParent(this);
        inPhases = new ArrayList();
    }

    public AxisMessage getMessage(String label) {
        if (WSDLConstants.MESSAGE_LABEL_OUT_VALUE.equals(label)) {
            return outMessage;
        } else {
            throw new UnsupportedOperationException("Not yet implemented");
        }
    }

    public ArrayList getPhasesInFaultFlow() {
        return inFaultMessage.getMessageFlow();
    }

    public ArrayList getPhasesOutFaultFlow() {
        return outFaultMessage.getMessageFlow();
    }

    public ArrayList getPhasesOutFlow() {
        return outMessage.getMessageFlow();
    }

    public ArrayList getRemainingPhasesInFlow() {
        return inPhases;
    }

    public void setPhasesInFaultFlow(ArrayList list) {
        inFaultMessage.setMessageFlow(list);
    }

    public void setPhasesOutFaultFlow(ArrayList list) {
        outFaultMessage.setMessageFlow(list);
    }

    public void setPhasesOutFlow(ArrayList list) {
        outMessage.setMessageFlow(list);
    }

    public void setRemainingPhasesInFlow(ArrayList list) {
        inPhases = list;
    }

    /**
     * Returns a MEP client for an Out-only operation. This client can be used to
     * interact with a server which is offering an In-only operation. To use the
     * client, you must call addMessageContext() with a message context and then
     * call execute() to execute the client. Note that the execute method's
     * block parameter is ignored by this client and also the setMessageReceiver
     * method cannot be used.
     *
     * @param sc      The service context for this client to live within. Cannot be
     *                null.
     * @param options Options to use as defaults for this client. If any options are
     *                set specifically on the client then those override options
     *                here.
     */
    public OperationClient createClient(ServiceContext sc, Options options) {
        return new OutOnlyAxisOperationClient(this, sc, options);
    }
}

/**
 * MEP client for moi.
 */
class OutOnlyAxisOperationClient extends OperationClient {

    private MessageContext mc;

    OutOnlyAxisOperationClient(OutOnlyAxisOperation axisOp, ServiceContext sc,
                               Options options) {
        super(axisOp, sc, options);
    }

    /**
     * Adds a message context to the client for processing. This method must not
     * process the message - it only records it in the MEP client. Processing
     * only occurs when execute() is called.
     *
     * @param mc the message context
     * @throws AxisFault if this is called inappropriately.
     */
    public void addMessageContext(MessageContext mc) throws AxisFault {
        if (this.mc != null) {
            throw new AxisFault(Messages.getMessage("cannotaddmsgctx"));
        }
        this.mc = mc;
        if (mc.getMessageID() == null) {
            setMessageID(mc);
        }
        mc.setServiceContext(sc);
        axisOp.registerOperationContext(mc, oc);
        this.completed = false;
    }

    /**
     * Returns a message from the client - will return null if the requested
     * message is not available.
     *
     * @param messageLabel the message label of the desired message context
     * @return Returns the desired message context or null if its not available.
     * @throws AxisFault if the message label is invalid
     */
    public MessageContext getMessageContext(String messageLabel)
            throws AxisFault {
        if (messageLabel.equals(WSDLConstants.MESSAGE_LABEL_OUT_VALUE)) {
            return mc;
        }
        throw new AxisFault(Messages.getMessage("unknownMsgLabel", messageLabel));
    }

    /**
     * Sets the message receiver to be executed when a message comes into the MEP
     * and the MEP is executed. This is the way the MEP client provides
     * notification that a message has been received by it. Exactly when its
     * executed and under what conditions is a function of the specific MEP
     * client.
     */
    public void setCallback(Callback callback) {
        throw new UnsupportedOperationException(
                "This feature is not supported by this MEP");
    }

    /**
     * Executes the MEP. What this does depends on the specific MEP client. The
     * basic idea is to have the MEP client execute and do something with the
     * messages that have been added to it so far. For example, if its an Out-In
     * MEP, then if the Out message has been set, then executing the client asks
     * it to send the message and get the In message, possibly using a different
     * thread.
     *
     * @param block Indicates whether execution should block or return ASAP. What
     *              block means is of course a function of the specific MEP
     *              client. IGNORED BY THIS MEP CLIENT.
     * @throws AxisFault if something goes wrong during the execution of the MEP.
     */
    public void execute(boolean block) throws AxisFault {
        if (completed) {
            throw new AxisFault(Messages.getMessage("mepiscomplted"));
        }
        ConfigurationContext cc = sc.getConfigurationContext();
        prepareMessageContext(cc, mc);
        // setting message ID if it null

        // create the operation context for myself
        OperationContext oc = ContextFactory.createOperationContext(axisOp, sc);
        oc.addMessageContext(mc);
        // ship it out
        AxisEngine engine = new AxisEngine(cc);
        if (!block) {
            mc.setProperty(MessageContext.TRANSPORT_NON_BLOCKING, Boolean.TRUE);
        }
        engine.send(mc);
        // all done
        completed = true;
    }
}
