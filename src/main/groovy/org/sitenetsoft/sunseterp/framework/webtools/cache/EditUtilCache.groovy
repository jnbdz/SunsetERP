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
package org.sitenetsoft.sunseterp.framework.webtools.cache

import org.sitenetsoft.sunseterp.framework.base.util.UtilFormatOut
import org.sitenetsoft.sunseterp.framework.base.util.cache.UtilCache

cacheName = parameters.UTIL_CACHE_NAME
context.cacheName = cacheName

if (cacheName) {
    utilCache = UtilCache.findCache(cacheName)
    if (utilCache) {
        cache = [
                cacheName: utilCache.getName(),
                cacheSize: UtilFormatOut.formatQuantity(utilCache.size()),
                hitCount: UtilFormatOut.formatQuantity(utilCache.getHitCount()),
                missCountTot: UtilFormatOut.formatQuantity(utilCache.getMissCountTotal()),
                missCountNotFound: UtilFormatOut.formatQuantity(utilCache.getMissCountNotFound()),
                missCountExpired: UtilFormatOut.formatQuantity(utilCache.getMissCountExpired()),
                missCountSoftRef: UtilFormatOut.formatQuantity(utilCache.getMissCountSoftRef()),
                removeHitCount: UtilFormatOut.formatQuantity(utilCache.getRemoveHitCount()),
                removeMissCount: UtilFormatOut.formatQuantity(utilCache.getRemoveMissCount()),
                maxInMemory: UtilFormatOut.formatQuantity(utilCache.getMaxInMemory()),
                expireTime: UtilFormatOut.formatQuantity(utilCache.getExpireTime()),
                useSoftReference: utilCache.getUseSoftReference().toString()
        ]

        exp = utilCache.getExpireTime()
        hrs = Math.floor(exp / (60 * 60 * 1000))
        exp = exp % (60 * 60 * 1000)
        mins = Math.floor(exp / (60 * 1000))
        exp = exp % (60 * 1000)
        secs = exp / 1000
        cache.hrs = hrs
        cache.mins = mins
        cache.secs = UtilFormatOut.formatPrice(secs)

        context.cache = cache
    }
}
