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
package org.sitenetsoft.sunseterp.framework.catalina.container;

//import org.apache.catalina.realm.GenericPrincipal;
//import org.apache.catalina.realm.RealmBase;
import org.sitenetsoft.sunseterp.framework.base.util.Debug;
import org.sitenetsoft.sunseterp.framework.entity.Delegator;
import org.sitenetsoft.sunseterp.framework.entity.DelegatorFactory;
import org.sitenetsoft.sunseterp.framework.entity.GenericEntityException;
import org.sitenetsoft.sunseterp.framework.entity.GenericValue;
import org.sitenetsoft.sunseterp.framework.entity.util.EntityQuery;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;

// extends RealmBase
public class OFBizRealm {
    private static final String MODULE = OFBizRealm.class.getName();

    //@Override
    protected String getPassword(String username) {
        Delegator delegator = DelegatorFactory.getDelegator(null);
        try {
            GenericValue userLogin = EntityQuery.use(delegator).from("UserLogin").where("userLoginId", username).queryOne();
            if (userLogin != null) {
                return userLogin.getString("currentPassword");
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
        }
        return null;
    }

    //@Override
    // TODO: Fix this method
    /*protected Principal getPrincipal(String username) {
        List<String> roles = new ArrayList<>();
        return new GenericPrincipal(username,
                getPassword(username),
                roles);
    }*/
}
