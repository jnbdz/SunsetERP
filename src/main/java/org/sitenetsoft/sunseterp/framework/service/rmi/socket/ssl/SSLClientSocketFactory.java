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

package org.sitenetsoft.sunseterp.framework.service.rmi.socket.ssl;

import org.sitenetsoft.sunseterp.framework.base.config.GenericConfigException;
import org.sitenetsoft.sunseterp.framework.base.util.Debug;
import org.sitenetsoft.sunseterp.framework.base.util.SSLUtil;

import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.io.Serializable;
import java.net.Socket;
import java.rmi.server.RMIClientSocketFactory;
import java.security.GeneralSecurityException;

/**
 * RMI SSL Client Socket Factory
 */
@SuppressWarnings("serial")
public class SSLClientSocketFactory implements RMIClientSocketFactory, Serializable {

    private static final String MODULE = SSLClientSocketFactory.class.getName();

    @Override
    public Socket createSocket(String host, int port) throws IOException {
        try {
            SSLSocketFactory factory = SSLUtil.getSSLSocketFactory();
            return factory.createSocket(host, port);
        } catch (GeneralSecurityException | GenericConfigException e) {
            Debug.logError(e, MODULE);
            throw new IOException(e.getMessage());
        }
    }
}
