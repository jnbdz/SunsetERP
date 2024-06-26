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
package org.sitenetsoft.sunseterp.applications.manufacturing.bom

import org.sitenetsoft.sunseterp.framework.entity.util.EntityUtil
import org.sitenetsoft.sunseterp.framework.entity.condition.EntityCondition
import org.sitenetsoft.sunseterp.framework.entity.condition.EntityOperator

condList = []
if (parameters.productId) {
    cond = EntityCondition.makeCondition('productId', EntityOperator.EQUALS, parameters.productId)
    condList.add(cond)
}
if (parameters.productIdTo) {
    cond = EntityCondition.makeCondition('productIdTo', EntityOperator.EQUALS, parameters.productIdTo)
    condList.add(cond)
}
if (parameters.productAssocTypeId) {
    cond = EntityCondition.makeCondition('productAssocTypeId', EntityOperator.EQUALS, parameters.productAssocTypeId)
    condList.add(cond)
} else {
    bomAssocTypeIds = EntityUtil.getFieldListFromEntityList(
            select('productAssocTypeId').from('ProductAssocType').where('parentTypeId', 'PRODUCT_COMPONENT').queryList(),
            'productAssocTypeId', true)
    cond = EntityCondition.makeCondition('productAssocTypeId', EntityOperator.IN, bomAssocTypeIds)
    condList.add(cond)
}
bomListIterator = select('productId', 'internalName', 'productAssocTypeId')
                    .from('ProductAndAssoc')
                    .where(condList)
                    .orderBy('productId', 'productAssocTypeId').distinct()
                    .cursorScrollInsensitive()
                    .cache(true)
                    .queryIterator()

context.ListProductBom = bomListIterator
