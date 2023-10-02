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
package org.sitenetsoft.framework.service.jms;

import org.sitenetsoft.framework.base.util.Debug;
import org.sitenetsoft.framework.base.util.GeneralException;
import org.sitenetsoft.framework.base.util.JNDIContextFactory;
import org.sitenetsoft.framework.entity.Delegator;
import org.sitenetsoft.framework.service.GenericServiceException;

import javax.jms.*;
import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 * JmsTopicListener - Topic (Pub/Sub) Message Listener.
 */
public class JmsTopicListener extends AbstractJmsListener {

    private static final String MODULE = JmsTopicListener.class.getName();

    private TopicConnection con = null;
    private TopicSession session = null;
    private Topic topic = null;

    private String jndiServer;
    private String jndiName;
    private String topicName;
    private String userName;
    private String password;

    /**
     * Creates a new JmsTopicListener - Should only be called by the JmsListenerFactory.
     */
    public JmsTopicListener(Delegator delegator, String jndiServer, String jndiName, String topicName, String userName, String password) {
        super(delegator);
        this.jndiServer = jndiServer;
        this.jndiName = jndiName;
        this.topicName = topicName;
        this.userName = userName;
        this.password = password;
    }

    @Override
    public void close() throws GenericServiceException {
        try {
            if (session != null) {
                session.close();
            }
            if (con != null) {
                con.close();
            }
        } catch (JMSException e) {
            throw new GenericServiceException("Cannot close connection(s).", e);
        }
    }

    @Override
    public synchronized void load() throws GenericServiceException {
        try {
            InitialContext jndi = JNDIContextFactory.getInitialContext(jndiServer);
            TopicConnectionFactory factory = (TopicConnectionFactory) jndi.lookup(jndiName);

            if (factory != null) {
                con = factory.createTopicConnection(userName, password);
                con.setExceptionListener(this);
                session = con.createTopicSession(false, Session.AUTO_ACKNOWLEDGE);
                topic = (Topic) jndi.lookup(topicName);
                if (topic != null) {
                    TopicSubscriber subscriber = session.createSubscriber(topic);
                    subscriber.setMessageListener(this);
                    con.start();
                    this.setConnected(true);
                    if (Debug.infoOn()) {
                        Debug.logInfo("Listening to topic [" + topicName + "] on [" + jndiServer + "]...", MODULE);
                    }
                } else {
                    throw new GenericServiceException("Topic lookup failed.");
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