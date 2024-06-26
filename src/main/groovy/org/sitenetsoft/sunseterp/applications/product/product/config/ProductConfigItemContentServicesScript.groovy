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
package org.sitenetsoft.sunseterp.applications.product.product.config

import org.sitenetsoft.sunseterp.framework.base.util.UtilDateTime
import org.sitenetsoft.sunseterp.framework.entity.GenericValue
import org.sitenetsoft.sunseterp.framework.service.ServiceUtil

// ProductConfigItemContent
/**
 * Create Content For ProductConfigItem
 */
Map createProductConfigItemContent() {
    Map result = success()
    GenericValue newEntity = makeValue('ProdConfItemContent', parameters)
    newEntity.fromDate = newEntity.fromDate ?: UtilDateTime.getTimestamp(System.currentTimeSeconds() * 1000)
    newEntity.create()

    run service: 'updateContent', with: parameters

    result.contentId = newEntity.contentId
    result.configItemId = newEntity.configItemId
    result.confItemContentTypeId = newEntity.confItemContentTypeId

    return result
}

/**
 * Update Content For ProductConfigItem
 */
Map updateProductConfigItemContent() {
    GenericValue pkParameters = makeValue('ProdConfItemContent')
    pkParameters.setPKFields(parameters)

    GenericValue lookedUpValue = from('ProdConfItemContent').where(pkParameters).queryOne()
    lookedUpValue.setNonPKFields(parameters)
    lookedUpValue.store()

    run service: 'updateContent', with: parameters

    return success()
}

// Specialized
/**
 * Create Simple Text Content For Product
 */
Map createSimpleTextContentForProductConfigItem() {
    Map createProductConfigItemContent = parameters
    Map serviceResult = run service: 'createSimpleTextContent', with: parameters
    if (!ServiceUtil.isSuccess(serviceResult)) {
        return serviceResult
    }
    createProductConfigItemContent.contentId = serviceResult.contentId
    run service: 'createProductConfigItemContent', with: createProductConfigItemContent

    return success()
}
