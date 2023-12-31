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
package org.sitenetsoft.sunseterp.framework.base.util;

import org.sitenetsoft.sunseterp.framework.base.config.GenericConfigException;
import org.sitenetsoft.sunseterp.framework.base.config.JNDIConfigUtil;
import org.sitenetsoft.sunseterp.framework.base.util.cache.UtilCache;

import javax.naming.Context;
import javax.naming.InitialContext;
import java.util.Hashtable;

/**
 * JNDIContextFactory - central source for JNDI Contexts by helper name
 *
 */
public class JNDIContextFactory {

    private static final String MODULE = JNDIContextFactory.class.getName();
    // FIXME: InitialContext instances are not thread-safe! They should not be cached.
    private static final UtilCache<String, InitialContext> CONTEXTS = UtilCache.createUtilCache("entity.JNDIContexts", 0, 0);

    /**
     * Return the initial context according to the entityengine.xml parameters that correspond to the given prefix
     * @return the JNDI initial context
     */
    public static InitialContext getInitialContext(String jndiServerName) throws GenericConfigException {
        InitialContext ic = CONTEXTS.get(jndiServerName);

        if (ic == null) {
            JNDIConfigUtil.JndiServerInfo jndiServerInfo = JNDIConfigUtil.getJndiServerInfo(jndiServerName);

            if (jndiServerInfo == null) {
                throw new GenericConfigException("ERROR: no jndi-server definition was found with the name " + jndiServerName
                        + " in jndiservers.xml");
            }

            try {
                if (UtilValidate.isEmpty(jndiServerInfo.getContextProviderUrl())) {
                    ic = new InitialContext();
                } else {
                    Hashtable<String, Object> h = new Hashtable<>();

                    h.put(Context.INITIAL_CONTEXT_FACTORY, jndiServerInfo.getInitialContextFactory());
                    h.put(Context.PROVIDER_URL, jndiServerInfo.getContextProviderUrl());
                    if (UtilValidate.isNotEmpty(jndiServerInfo.getUrlPkgPrefixes())) {
                        h.put(Context.URL_PKG_PREFIXES, jndiServerInfo.getUrlPkgPrefixes());
                    }

                    if (UtilValidate.isNotEmpty(jndiServerInfo.getSecurityPrincipal())) {
                        h.put(Context.SECURITY_PRINCIPAL, jndiServerInfo.getSecurityPrincipal());
                    }
                    if (UtilValidate.isNotEmpty(jndiServerInfo.getSecurityCredentials())) {
                        h.put(Context.SECURITY_CREDENTIALS, jndiServerInfo.getSecurityCredentials());
                    }

                    ic = new InitialContext(h);
                }
            } catch (Exception e) {
                String errorMsg = "Error getting JNDI initial context for server name " + jndiServerName;

                Debug.logError(e, errorMsg, MODULE);
                throw new GenericConfigException(errorMsg, e);
            }

            ic = CONTEXTS.putIfAbsentAndGet(jndiServerName, ic);
        }

        return ic;
    }
    /**
     * Removes an entry from the JNDI cache.
     * @param jndiServerName
     */
    public static void clearInitialContext(String jndiServerName) {
        CONTEXTS.remove(jndiServerName);
    }
}
