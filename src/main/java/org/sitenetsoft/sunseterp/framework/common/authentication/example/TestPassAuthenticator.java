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

package org.sitenetsoft.sunseterp.framework.common.authentication.example;

import org.sitenetsoft.sunseterp.framework.base.util.Debug;
import org.sitenetsoft.sunseterp.framework.common.authentication.api.AuthenticatorException;

/**
 * TestPassAuthenticator
 */
public class TestPassAuthenticator extends TestFailAuthenticator {

    private static final String MODULE = TestPassAuthenticator.class.getName();

    /**
     * Method to authenticate a user
     * @param username      User's username
     * @param password      User's password
     * @param isServiceAuth true if authentication is for a service call
     * @return true if the user is authenticated
     * @throws org.sitenetsoft.sunseterp.framework.common.authentication.api.AuthenticatorException
     *          when a fatal error occurs during authentication
     */
    @Override
    public boolean authenticate(String username, String password, boolean isServiceAuth) throws AuthenticatorException {
        Debug.logInfo(this.getClass().getName() + " Authenticator authenticate() -- returning false", MODULE);
        return true;
    }

    /**
     * Flag to test if this Authenticator is enabled
     * @return true if the Authenticator is enabled
     */
    @Override
    public boolean isEnabled() {
        return false;
    }
}