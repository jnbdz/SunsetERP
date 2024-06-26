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
package org.sitenetsoft.sunseterp.framework.webtools.labelmanager

import org.sitenetsoft.sunseterp.framework.base.util.FileUtil
import org.sitenetsoft.sunseterp.framework.base.util.UtilXml
import org.w3c.dom.Document

fileString = ''
if (parameters.fileName) {
    file = new File(parameters.fileName)
    if (parameters.fileName.endsWith('.xml')) {
        Document document = UtilXml.readXmlDocument(file.toURL(), false)
        ByteArrayOutputStream os = new ByteArrayOutputStream()
        UtilXml.writeXmlDocument(document, os, 'UTF-8', true, true, 4)
        os.close()
        fileString = os.toString()
    } else if (parameters.fileName.endsWith('.properties')) {
        fileString = FileUtil.readString('UTF-8', file)
    }
    rows = fileString.split(System.getProperty('line.separator'))
    context.rows = rows.size()
}
context.fileString = fileString
