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
*/
package org.sitenetsoft.sunseterp.framework.common

import org.sitenetsoft.sunseterp.framework.base.component.ComponentConfig
import org.sitenetsoft.sunseterp.framework.base.component.ComponentConfig.KeystoreInfo

cert = org.sitenetsoft.sunseterp.framework.base.util.KeyStoreUtil.pemToCert(certString)
if (cert) {
    context.certType = cert.getType()
    context.certName = cert.getSubjectX500Principal().getName()
    context.certSerialNumber = cert.getSerialNumber().toString(16)
    context.certPublicKey = cert.getPublicKey()
}

stores = []
store = []
Collection<ComponentConfig> allComponentConfigs = ComponentConfig.getAllComponents()
for (ComponentConfig cc: allComponentConfigs) {
    if (cc.getKeystoreInfos()) {
        componentName = cc.getComponentName()
        store = ['componentId': componentName]
        store.componentName = componentName
        cc.getKeystoreInfos().each { KeystoreInfo ks ->
            keystoreName = ks.getName()
            store.keystoreName = ks.getName()
        }
        stores.add(store)
    }
}
context.stores = stores
