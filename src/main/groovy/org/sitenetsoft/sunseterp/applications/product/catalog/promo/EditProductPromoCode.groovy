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
package org.sitenetsoft.sunseterp.applications.product.catalog.promo

productPromoCodeId = request.getParameter('productPromoCodeId') ?: request.getAttribute('productPromoCodeId')
productPromoCode = from('ProductPromoCode').where('productPromoCodeId', productPromoCodeId).queryOne()

productPromoId = null
if (productPromoCode) {
    productPromoId = productPromoCode.productPromoId
} else {
    productPromoId = request.getParameter('productPromoId')
}

productPromo = null
if (productPromoId) {
    productPromo = from('ProductPromo').where('productPromoId', productPromoId).queryOne()
}

productPromoCodeEmails = null
productPromoCodeParties = null
if (productPromoCode) {
    productPromoCodeEmails = productPromoCode.getRelated('ProdPromoCodeContactMech', null, null, false)
    productPromoCodeParties = productPromoCode.getRelated('ProductPromoCodeParty', null, null, false)
}

context.productPromoId = productPromoId
context.productPromo = productPromo
context.productPromoCodeId = productPromoCodeId
context.productPromoCode = productPromoCode
context.productPromoCodeEmails = productPromoCodeEmails
context.productPromoCodeParties = productPromoCodeParties
