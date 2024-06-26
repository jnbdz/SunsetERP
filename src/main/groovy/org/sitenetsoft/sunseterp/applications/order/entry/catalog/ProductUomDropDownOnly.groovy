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
package org.sitenetsoft.sunseterp.applications.order.entry.catalog

product = from('Product').where('productId', parameters.productId).queryOne()
if (product) {
    productVirtualVariants = from('ProductAssoc')
            .where('productIdTo', product.productId , 'productAssocTypeId', 'ALTERNATIVE_PACKAGE')
            .cache(true).queryList()
    if (productVirtualVariants) {
        List mainProducts = []
        productVirtualVariants.each { virtualVariantKey ->
            mainProductMap = [:]
            mainProduct = virtualVariantKey.getRelatedOne('MainProduct', true)
            quantityUom = mainProduct.getRelatedOne('QuantityUom', true)
            mainProductMap.productId = mainProduct.productId
            mainProductMap.piecesIncluded = mainProduct.piecesIncluded
            mainProductMap.uomDesc = quantityUom.description
            mainProducts.add(mainProductMap)
        }
        context.mainProducts = mainProducts
    }
    context.product = product
}
