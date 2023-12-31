/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *******************************************************************************/
package org.sitenetsoft.sunseterp.framework.service.jms;

import org.sitenetsoft.sunseterp.framework.base.util.Debug;
import org.sitenetsoft.sunseterp.framework.base.util.GeneralException;
import org.sitenetsoft.sunseterp.framework.base.util.JNDIContextFactory;
import org.sitenetsoft.sunseterp.framework.entity.Delegator;
import org.sitenetsoft.sunseterp.framework.service.GenericServiceException;

import javax.jms.*;
import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 * JmsQueueListener - Queue (P2P) Message Listener.
 */
public class JmsQueueListener extends AbstractJmsListener {

    private static final String MODULE = JmsQueueListener.class.getName();

    private QueueConnection con = null;
    private QueueSession session = null;
    private Queue queue = null;

    private String jndiServer;
    private String jndiName;
    private String queueName;
    private String userName;
    private String password;

    /**
     * Creates a new JmsQueueListener - Should only be called by the JmsListenerFactory.
     */
    public JmsQueueListener(Delegator delegator, String jndiServer, String jndiName, String queueName, String userName, String password) {
        super(delegator);
        this.jndiServer = jndiServer;
        this.jndiName = jndiName;
        this.queueName = queueName;
        this.userName = userName;
        this.password = password;
    }

    @Override
    public void close() throws GenericServiceException {
        try {
            session.close();
            con.close();
        } catch (JMSException e) {
            throw new GenericServiceException("Cannot close connection(s).", e);
        }
    }

    @Override
    public synchronized void load() throws GenericServiceException {
        try {
            InitialContext jndi = JNDIContextFactory.getInitialContext(jndiServer);
            QueueConnectionFactory factory = (QueueConnectionFactory) jndi.lookup(jndiName);

            if (factory != null) {
                con = factory.createQueueConnection(userName, password);
                con.setExceptionListener(this);
                session = con.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
                queue = (Queue) jndi.lookup(queueName);
                if (queue != null) {
                    QueueReceiver receiver = session.createReceiver(queue);

                    receiver.setMessageListener(this);
                    con.start();
                    this.setConnected(true);
                    Debug.logInfo("Listening to queue [" + queueName + "]...", MODULE);
                } else {
                    throw new GenericServiceException("Queue lookup failed.");
                }
            } else {
                throw new GenericServiceException("Factory (broker) lookup failed.");
            }
        } catch (NamingException ne) {
            throw new GenericServiceException("JNDI lookup problems; listener not running.", ne);
        } catch (JMSException je) {
            throw new GenericServiceException("JMS internal error; listener not running.", je);
        } catch (GeneralException ge) {
            throw new GenericServiceException("Problems with InitialContext; listener not running.", ge);
        }
    }
}
