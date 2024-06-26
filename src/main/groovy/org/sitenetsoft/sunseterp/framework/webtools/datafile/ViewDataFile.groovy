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
package org.sitenetsoft.sunseterp.framework.webtools.datafile

import org.sitenetsoft.sunseterp.framework.base.util.Debug
import org.sitenetsoft.sunseterp.framework.base.util.UtilProperties
import org.sitenetsoft.sunseterp.framework.base.util.UtilURL
import org.sitenetsoft.sunseterp.framework.datafile.DataFile
import org.sitenetsoft.sunseterp.framework.datafile.DataFile2EntityXml
import org.sitenetsoft.sunseterp.framework.datafile.ModelDataFileReader

uiLabelMap = UtilProperties.getResourceBundleMap('WebtoolsUiLabels', locale)
messages = []

dataFileSave = request.getParameter('DATAFILE_SAVE')

entityXmlFileSave = request.getParameter('ENTITYXML_FILE_SAVE')

dataFileLoc = request.getParameter('DATAFILE_LOCATION')
definitionLoc = request.getParameter('DEFINITION_LOCATION')
definitionName = request.getParameter('DEFINITION_NAME')
dataFileIsUrl = null != request.getParameter('DATAFILE_IS_URL')
definitionIsUrl = null != request.getParameter('DEFINITION_IS_URL')

try {
    dataFileUrl = dataFileIsUrl ? UtilURL.fromUrlString(dataFileLoc) : UtilURL.fromFilename(dataFileLoc)
}
catch (java.net.MalformedURLException e) {
    messages.add(e.getMessage())
}

try {
    definitionUrl = definitionIsUrl ? UtilURL.fromUrlString(definitionLoc) : UtilURL.fromFilename(definitionLoc)
}
catch (java.net.MalformedURLException e) {
    messages.add(e.getMessage())
}

definitionNames = null
if (definitionUrl) {
    try {
        ModelDataFileReader reader = ModelDataFileReader.getModelDataFileReader(definitionUrl)
        if (reader) {
            definitionNames = ((Collection)reader.getDataFileNames()).iterator()
            context.put('definitionNames', definitionNames)
        }
    }
    catch (Exception e) {
        messages.add(e.getMessage())
    }
}

dataFile = null
if (dataFileUrl && definitionUrl && definitionNames) {
    try {
        dataFile = DataFile.readFile(dataFileUrl, definitionUrl, definitionName)
        context.put('dataFile', dataFile)
    }
    catch (Exception e) {
        messages.add(e.toString()); Debug.log(e)
    }
}

if (dataFile) {
    modelDataFile = dataFile.getModelDataFile()
    context.put('modelDataFile', modelDataFile)
}

if (dataFile && dataFileSave) {
    try {
        dataFile.writeDataFile(dataFileSave)
        messages.add(uiLabelMap.WebtoolsDataFileSavedTo + dataFileSave)
    }
    catch (Exception e) {
        messages.add(e.getMessage())
    }
}

if (dataFile && entityXmlFileSave) {
    try {
        //dataFile.writeDataFile(entityXmlFileSave)
        DataFile2EntityXml.writeToEntityXml(entityXmlFileSave, dataFile)
        messages.add(uiLabelMap.WebtoolsDataEntityFileSavedTo + entityXmlFileSave)
    }
    catch (Exception e) {
        messages.add(e.getMessage())
    }
}
context.messages = messages
