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

package org.sitenetsoft.sunseterp.framework.common.login;

/*
import org.sitenetsoft.sunseterp.framework.base.crypto.HashCrypt;
import org.sitenetsoft.sunseterp.framework.base.util.Debug;
import org.sitenetsoft.sunseterp.framework.base.util.UtilProperties;
import org.sitenetsoft.sunseterp.framework.base.util.UtilValidate;
import org.sitenetsoft.sunseterp.framework.entity.Delegator;
import org.sitenetsoft.sunseterp.framework.entity.GenericEntityException;
import org.sitenetsoft.sunseterp.framework.entity.GenericValue;
import org.sitenetsoft.sunseterp.framework.entity.transaction.GenericTransactionException;
import org.sitenetsoft.sunseterp.framework.entity.transaction.TransactionUtil;
import org.sitenetsoft.sunseterp.framework.entity.util.EntityQuery;
import org.sitenetsoft.sunseterp.framework.entity.util.EntityUtilProperties;
import org.sitenetsoft.sunseterp.framework.service.DispatchContext;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.transaction.Transaction;
import java.util.Map;
import java.util.Properties;
*/

/**
 * @TODO: Adapt for Quarkus
 * LDAP Authentication Services.
 */
public class LdapAuthenticationServices {}
/*public class LdapAuthenticationServices {

    private static final String MODULE = LdapAuthenticationServices.class.getName();

    public static boolean userLogin(DispatchContext ctx, Map<String, ?> context) {
        if (Debug.verboseOn()) {
            Debug.logVerbose("Starting LDAP authentication", MODULE);
        }
        Properties env = UtilProperties.getProperties("jndiLdap");
        String username = (String) context.get("login.username");
        if (username == null) {
            username = (String) context.get("username");
        }
        String password = (String) context.get("login.password");
        if (password == null) {
            password = (String) context.get("password");
        }
        String dn = null;
        Delegator delegator = ctx.getDelegator();
        boolean isServiceAuth = context.get("isServiceAuth") != null && (Boolean) context.get("isServiceAuth");
        GenericValue userLogin = null;
        try {
            userLogin = EntityQuery.use(delegator).from("UserLogin").where("userLoginId", username).cache(isServiceAuth).queryOne();
        } catch (GenericEntityException e) {
            Debug.logWarning(e, "", MODULE);
        }
        if (userLogin != null) {
            dn = userLogin.getString("userLdapDn");
        }
        if (UtilValidate.isEmpty(dn)) {
            String dnTemplate = (String) env.get("ldap.dn.template");
            if (dnTemplate != null) {
                dn = dnTemplate.replace("%u", username);
            }
            if (Debug.verboseOn()) {
                Debug.logVerbose("Using DN template: " + dn, MODULE);
            }
        } else {
            if (Debug.verboseOn()) {
                Debug.logVerbose("Using UserLogin.userLdapDn: " + dn, MODULE);
            }
        }
        env.put(Context.SECURITY_PRINCIPAL, dn);
        env.put(Context.SECURITY_CREDENTIALS, password);
        try {
            // Create initial context
            DirContext ldapCtx = new InitialDirContext(env);
            ldapCtx.close();
        } catch (NamingException e) {
            if (Debug.verboseOn()) {
                Debug.logVerbose("LDAP authentication failed: " + e.getMessage(), MODULE);
            }
            return false;
        }
        if (Debug.verboseOn()) {
            Debug.logVerbose("LDAP authentication succeeded", MODULE);
        }
        if (!"true".equals(env.get("ldap.synchronize.passwords"))) {
            return true;
        }
        // Synchronize user's OFBiz password with user's LDAP password
        if (userLogin != null) {
            boolean useEncryption = "true".equals(EntityUtilProperties.getPropertyValue("security", "password.encrypt", delegator));
            String currentPassword = userLogin.getString("currentPassword");
            boolean samePassword;
            if (useEncryption) {
                samePassword = HashCrypt.comparePassword(currentPassword, LoginServices.getHashType(), password);
            } else {
                samePassword = currentPassword.equals(password);
            }
            if (!samePassword) {
                if (Debug.verboseOn()) {
                    Debug.logVerbose("Starting password synchronization", MODULE);
                }
                userLogin.set("currentPassword", useEncryption ? HashCrypt.cryptUTF8(LoginServices.getHashType(), null, password) : password, false);
                Transaction parentTx = null;
                boolean beganTransaction = false;
                try {
                    try {
                        parentTx = TransactionUtil.suspend();
                    } catch (GenericTransactionException e) {
                        Debug.logError(e, "Could not suspend transaction: " + e.getMessage(), MODULE);
                    }
                    try {
                        beganTransaction = TransactionUtil.begin();
                        userLogin.store();
                    } catch (GenericEntityException e) {
                        Debug.logError(e, "Error saving UserLogin", MODULE);
                        try {
                            TransactionUtil.rollback(beganTransaction, "Error saving UserLogin", e);
                        } catch (GenericTransactionException e2) {
                            Debug.logError(e2, "Could not rollback nested transaction: " + e2.getMessage(), MODULE);
                        }
                    } finally {
                        try {
                            TransactionUtil.commit(beganTransaction);
                            if (Debug.verboseOn()) {
                                Debug.logVerbose("Password synchronized", MODULE);
                            }
                        } catch (GenericTransactionException e) {
                            Debug.logError(e, "Could not commit nested transaction: " + e.getMessage(), MODULE);
                        }
                    }
                } finally {
                    if (parentTx != null) {
                        try {
                            TransactionUtil.resume(parentTx);
                            if (Debug.verboseOn()) {
                                Debug.logVerbose("Resumed the parent transaction.", MODULE);
                            }
                        } catch (GenericTransactionException e) {
                            Debug.logError(e, "Could not resume parent nested transaction: " + e.getMessage(), MODULE);
                        }
                    }
                }
            }
        }
        return true;
    }
}*/
