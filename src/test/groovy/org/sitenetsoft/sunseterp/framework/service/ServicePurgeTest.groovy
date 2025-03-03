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
package org.sitenetsoft.sunseterp.framework.service

import org.sitenetsoft.sunseterp.framework.base.util.UtilDateTime
import org.sitenetsoft.sunseterp.framework.entity.GenericValue
import org.sitenetsoft.sunseterp.framework.entity.util.EntityQuery
import org.sitenetsoft.sunseterp.framework.service.config.ServiceConfigUtil
import org.sitenetsoft.sunseterp.framework.testtools.GroovyScriptAssert

class ServicePurgeTest extends GroovyScriptAssert {

// ./gradlew "ofbiz --test component=service --test suitename=servicetests --test case=service-purge-test"

    void testRuntimeDataIsCleanedAfterServicePurge() {
        GenericValue sysUserLogin = delegator.findOne('UserLogin', true, 'userLoginId', 'system')
        String jobId = delegator.getNextSeqId('JobSandbox')

        Map createRuntimeResult = dispatcher.runSync('createRuntimeData', [
                runtimeInfo: 'This is a runtimeInfo',
                userLogin: sysUserLogin
        ])
        String runtimeDataId = createRuntimeResult.runtimeDataId

        dispatcher.runSync('createJobSandbox', [
                userLogin: sysUserLogin,
                poolId: ServiceConfigUtil.getServiceEngine().getThreadPool().getSendToPool(),
                jobId: jobId,
                runtimeDataId: runtimeDataId,
                statusId: 'SERVICE_FINISHED',
                serviceName: 'sendMail',
                finishDateTime: UtilDateTime.addDaysToTimestamp(UtilDateTime.nowTimestamp(), -10)
        ])

        dispatcher.runSync('purgeOldJobs', [userLogin: sysUserLogin])

        assert EntityQuery.use(delegator).from('JobSandbox').where('jobId', jobId).queryCount() == 0
        assert EntityQuery.use(delegator).from('RuntimeData').where('runtimeDataId', runtimeDataId).queryCount() == 0
    }

}
