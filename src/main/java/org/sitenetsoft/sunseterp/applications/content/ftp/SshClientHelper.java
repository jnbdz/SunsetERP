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
package org.sitenetsoft.sunseterp.applications.content.ftp;

import org.apache.sshd.client.ClientBuilder;
import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.config.keys.ClientIdentityLoader;
import org.apache.sshd.common.NamedFactory;
import org.apache.sshd.common.kex.BuiltinDHFactories;
import org.apache.sshd.common.signature.BuiltinSignatures;

import java.util.ArrayList;

public abstract class SshClientHelper {

    private static SshClient client = null;

    public static SshClient getSshClient() {
        if (client == null) {
            client = SshClient.setUpDefaultClient();
            client.setClientIdentityLoader(ClientIdentityLoader.DEFAULT);
            client.setKeyExchangeFactories(NamedFactory.setUpTransformedFactories(
                    false,
                    BuiltinDHFactories.VALUES,
                    ClientBuilder.DH2KEX));
            client.setSignatureFactories(new ArrayList<>(BuiltinSignatures.VALUES));
        }
        if (!client.isStarted()) {
            client.start();
        }
        return client;
    }

}
