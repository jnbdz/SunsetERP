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

package org.sitenetsoft.sunseterp.framework.common.authentication;

/**
 * AuthHelper
 * @TODO: Adapt for Quarkus
 */
public final class AuthHelper {}
/*public final class AuthHelper {

    private static final String MODULE = AuthHelper.class.getName();
    private static List<Authenticator> authenticators = new ArrayList<>();
    private static boolean authenticatorsLoaded = false;

    private AuthHelper() {
    }

    public static boolean authenticate(String username, String password, boolean isServiceAuth)
            throws AuthenticatorException {
        if (!authenticatorsLoaded) {
            throw new AuthenticatorException(
                    "Authenticators never loaded; be sure to call AuthHelper.loadAuthenticators()");
        }
        for (Authenticator auth : authenticators) {
            boolean pass = auth.authenticate(username, password, isServiceAuth);
            if (pass) {
                return true;
            } else if (auth.isSingleAuthenticator()) {
                throw new AuthenticatorException();
            }
        }
        return false;
    }

    public static void logout(String username) throws AuthenticatorException {
        if (!authenticatorsLoaded) {
            throw new AuthenticatorException(
                    "Authenticators never loaded; be sure to call AuthHelper.loadAuthenticators()");
        }
        for (Authenticator auth : authenticators) {
            auth.logout(username);
        }
    }

    public static void syncUser(String username) throws AuthenticatorException {
        if (!authenticatorsLoaded) {
            throw new AuthenticatorException(
                    "Authenticators never loaded; be sure to call AuthHelper.loadAuthenticators()");
        }
        for (Authenticator auth : authenticators) {
            if (auth.isUserSynchronized()) {
                auth.syncUser(username);
            }
        }
    }

    public static void updatePassword(String username, String password, String newPassword)
            throws AuthenticatorException {
        if (!authenticatorsLoaded) {
            throw new AuthenticatorException(
                    "Authenticators never loaded; be sure to call AuthHelper.loadAuthenticators()");
        }
        for (Authenticator auth : authenticators) {
            auth.updatePassword(username, password, newPassword);
        }
    }

    public static boolean authenticatorsLoaded() {
        return authenticatorsLoaded;
    }

    public static void loadAuthenticators(LocalDispatcher dispatcher) {
        if (!authenticatorsLoaded) {
            loadAuthenticatorsInternal(dispatcher);
        }
    }

    private static synchronized void loadAuthenticatorsInternal(LocalDispatcher dispatcher) {
        if (!authenticatorsLoaded) {
            Iterator<Authenticator> it = ServiceLoader.load(Authenticator.class, getContextClassLoader()).iterator();
            while (it.hasNext()) {
                try {
                    Authenticator auth = it.next();
                    if (auth.isEnabled()) {
                        auth.initialize(dispatcher);
                        authenticators.add(auth);
                    }
                } catch (ClassCastException e) {
                    Debug.logError(e, MODULE);
                }
            }

            authenticators.sort(new AuthenticationComparator());
            authenticatorsLoaded = true;
        }
    }

    /*
     * Do not move this into a shared global util class; doing so would mean the
     * method would have to be public, and then it could be called by any other
     * non-secure source.
     */
    /*private static ClassLoader getContextClassLoader() {
        return AccessController.doPrivileged(
                (PrivilegedAction<ClassLoader>) () -> {
                    ClassLoader cl = null;
                    try {
                        cl = Thread.currentThread().getContextClassLoader();
                    } catch (SecurityException e) {
                        Debug.logError(e, e.getMessage(), MODULE);
                    }
                    return cl;
                });
    }
}*/
