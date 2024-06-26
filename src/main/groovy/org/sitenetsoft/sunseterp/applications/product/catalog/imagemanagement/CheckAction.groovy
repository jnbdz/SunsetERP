/*
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
package org.sitenetsoft.sunseterp.applications.product.catalog.imagemanagement

import org.sitenetsoft.sunseterp.framework.base.util.UtilHttp

Map paramMap = UtilHttp.getParameterMap(request)
String result
int rowCount = UtilHttp.getMultiFormRowCount(paramMap)
if (rowCount > 1) {
    for (int i = 0; i < rowCount; i++) {
        String thisSuffix = UtilHttp.MULTI_ROW_DELIMITER + i
        if (paramMap.get('action' + thisSuffix)) {
            result = paramMap.get('action' + thisSuffix)
        }
    }
}
else {
    result = paramMap.get('action_o_0')
}
if (result == null) {
    result = 'noAction'
}
return result
