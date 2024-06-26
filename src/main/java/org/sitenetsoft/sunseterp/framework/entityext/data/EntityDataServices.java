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
package org.sitenetsoft.sunseterp.framework.entityext.data;

import org.apache.commons.codec.binary.Base64;
import org.sitenetsoft.sunseterp.framework.base.crypto.DesCrypt;
import org.sitenetsoft.sunseterp.framework.base.util.*;
import org.sitenetsoft.sunseterp.framework.entity.Delegator;
import org.sitenetsoft.sunseterp.framework.entity.GenericEntityException;
import org.sitenetsoft.sunseterp.framework.entity.GenericValue;
import org.sitenetsoft.sunseterp.framework.entity.datasource.GenericHelperInfo;
import org.sitenetsoft.sunseterp.framework.entity.jdbc.DatabaseUtil;
import org.sitenetsoft.sunseterp.framework.entity.model.ModelEntity;
import org.sitenetsoft.sunseterp.framework.entity.model.ModelField;
import org.sitenetsoft.sunseterp.framework.entity.util.EntityListIterator;
import org.sitenetsoft.sunseterp.framework.entity.util.EntityQuery;
import org.sitenetsoft.sunseterp.framework.security.Security;
import org.sitenetsoft.sunseterp.framework.service.DispatchContext;
import org.sitenetsoft.sunseterp.framework.service.GenericServiceException;
import org.sitenetsoft.sunseterp.framework.service.LocalDispatcher;
import org.sitenetsoft.sunseterp.framework.service.ServiceUtil;
import org.apache.shiro.crypto.cipher.AesCipherService;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Entity Data Import/Export Services
 *
 */
public class EntityDataServices {

    private static final String MODULE = EntityDataServices.class.getName();
    private static final String RESOURCE = "EntityExtUiLabels";

    public static Map<String, Object> exportDelimitedToDirectory(DispatchContext dctx, Map<String, Object> context) {
        Locale locale = (Locale) context.get("locale");
        return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "EntityExtThisServiceIsNotYetImplemented", locale));
    }

    public static Map<String, Object> importDelimitedFromDirectory(DispatchContext dctx, Map<String, Object> context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Security security = dctx.getSecurity();
        Locale locale = (Locale) context.get("locale");

        // check permission
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        if (!security.hasPermission("ENTITY_MAINT", userLogin)) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "EntityExtServicePermissionNotGranted", locale));
        }

        // get the directory & delimiter
        String rootDirectory = (String) context.get("rootDirectory");
        URL rootDirectoryUrl = UtilURL.fromResource(rootDirectory);
        // URL rootDirectoryUrl = UtilResourceLocator.locateResource(rootDirectory);
        if (rootDirectoryUrl == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "EntityExtUnableToLocateRootDirectory",
                    UtilMisc.toMap("rootDirectory", rootDirectory), locale));
        }

        String delimiter = (String) context.get("delimiter");
        if (delimiter == null) {
            // default delimiter is tab
            delimiter = "\t";
        }

        File root = null;
        try {
            root = new File(new URI(rootDirectoryUrl.toExternalForm()));
        } catch (URISyntaxException e) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "EntityExtUnableToLocateRootDirectoryURI", locale));
        }

        if (!root.exists() || !root.isDirectory() || !root.canRead()) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "EntityExtRootDirectoryDoesNotExists", locale));
        }

        // get the file list
        List<File> files = getFileList(root);
        if (UtilValidate.isNotEmpty(files)) {
            for (File file: files) {
                try {
                    Map<String, Object> serviceCtx = UtilMisc.toMap("file", file, "delimiter", delimiter, "userLogin", userLogin);
                    dispatcher.runSyncIgnore("importDelimitedEntityFile", serviceCtx);
                } catch (GenericServiceException e) {
                    Debug.logError(e, MODULE);
                }
            }
        } else {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "EntityExtNoFileAvailableInTheRootDirectory",
                    UtilMisc.toMap("rootDirectory", rootDirectory), locale));
        }

        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> importDelimitedFile(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        Security security = dctx.getSecurity();
        Locale locale = (Locale) context.get("locale");

        // check permission
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        if (!security.hasPermission("ENTITY_MAINT", userLogin)) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "EntityExtServicePermissionNotGranted", locale));
        }

        String delimiter = (String) context.get("delimiter");
        if (delimiter == null) {
            // default delimiter is tab
            delimiter = "\t";
        }

        long startTime = System.currentTimeMillis();

        File file = (File) context.get("file");
        int records = 0;
        try {
            records = readEntityFile(file, delimiter, delegator);
        } catch (GeneralException e) {
            return ServiceUtil.returnError(e.getMessage());
        } catch (FileNotFoundException e) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "EntityExtFileNotFound", UtilMisc.toMap("fileName",
                    file.getName()), locale));
        } catch (IOException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "EntityExtProblemReadingFile",
                    UtilMisc.toMap("fileName", file.getName()), locale));
        }

        long endTime = System.currentTimeMillis();
        long runTime = endTime - startTime;

        Debug.logInfo("Imported/Updated [" + records + "] from : " + file.getAbsolutePath() + " [" + runTime + "ms]", MODULE);
        Map<String, Object> result = ServiceUtil.returnSuccess();
        result.put("records", records);
        return result;
    }

    private static List<File> getFileList(File root) {
        List<File> fileList = new LinkedList<>();

        // check for a file list file
        File listFile = new File(root, "FILELIST.txt");
        Debug.logInfo("Checking file list - " + listFile.getPath(), MODULE);
        if (listFile.exists()) {
            BufferedReader reader = null;
            try {
                reader = new BufferedReader(new InputStreamReader(new FileInputStream(listFile), StandardCharsets.UTF_8));
            } catch (FileNotFoundException e) {
                Debug.logError(e, MODULE);
            }
            if (reader != null) {
                // read each line as a file name to load
                String line;
                try {
                    while ((line = reader.readLine()) != null) {
                        line = line.trim();
                        File thisFile = new File(root, line);
                        if (thisFile.exists()) {
                            fileList.add(thisFile);
                        }
                    }
                } catch (IOException e) {
                    Debug.logError(e, MODULE);
                }

                // close the reader
                try {
                    reader.close();
                } catch (IOException e) {
                    Debug.logError(e, MODULE);
                }
                Debug.logInfo("Read file list : " + fileList.size() + " entities.", MODULE);
            }
        } else {
            File[] files = root.listFiles();
            if (files != null) {
                for (File file : files) {
                    String fileName = file.getName();
                    if (!fileName.startsWith("_") && fileName.endsWith(".txt")) {
                        fileList.add(file);
                    }
                }
            }
            Debug.logInfo("No file list found; using directory order : " + fileList.size() + " entities.", MODULE);
        }

        return fileList;
    }

    private static String[] readEntityHeader(File file, String delimiter, BufferedReader dataReader) throws IOException {
        String filePath = file.getPath().replace('\\', '/');

        String[] header = null;
        File headerFile = new File(FileUtil.getFile(filePath.substring(0, filePath.lastIndexOf('/'))), "_" + file.getName());

        if (headerFile.exists()) {
            try (
                    BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(headerFile),
                            StandardCharsets.UTF_8));) {

                String firstLine = reader.readLine();
                if (firstLine != null) {
                    header = firstLine.split(delimiter);
                }
            } catch (IOException | SecurityException e) {
                Debug.logError(e, MODULE);
            }
        } else {
            BufferedReader reader = dataReader;
            String firstLine = reader.readLine();
            if (firstLine != null) {
                header = firstLine.split(delimiter);
            }
        }
        // read one line from either the header file or the data file if no header file exists
        return header;
    }

    private static int readEntityFile(File file, String delimiter, Delegator delegator) throws IOException, GeneralException {
        String entityName = file.getName().substring(0, file.getName().lastIndexOf('.'));

        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8));
        String[] header = readEntityHeader(file, delimiter, reader);

        //Debug.logInfo("Opened data file [" + file.getName() + "] now running...", MODULE);
        GeneralException exception = null;
        String line = null;
        int lineNumber = 1;
        while ((line = reader.readLine()) != null) {
            // process the record
            String fields[] = line.split(delimiter);
            //Debug.logInfo("Split record", MODULE);
            if (fields.length < 1) {
                exception = new GeneralException("Illegal number of fields [" + file.getName() + " / " + lineNumber);
                break;
            }

            GenericValue newValue = makeGenericValue(delegator, entityName, header, fields);
            //Debug.logInfo("Made value object", MODULE);
            newValue = delegator.createOrStore(newValue);
            //Debug.logInfo("Stored record", MODULE);

            if (lineNumber % 500 == 0 || lineNumber == 1) {
                Debug.logInfo("Records Stored [" + file.getName() + "]: " + lineNumber, MODULE);
                //Debug.logInfo("Last record : " + newValue, MODULE);
            }

            lineNumber++;
        }
        reader.close();

        // now that we closed the reader; throw the exception
        if (exception != null) {
            throw exception;
        }

        return lineNumber;
    }

    private static GenericValue makeGenericValue(Delegator delegator, String entityName, String[] header, String[] line) {
        GenericValue newValue = delegator.makeValue(entityName);
        for (int i = 0; i < header.length; i++) {
            String name = header[i].trim();

            String value = null;
            if (i < line.length) {
                value = line[i];
            }

            // check for null values
            if (UtilValidate.isNotEmpty(value)) {
                char first = value.charAt(0);
                if (first == 0x00) {
                    value = null;
                }

                // trim non-null values
                if (value != null) {
                    value = value.trim();
                }

                if (value != null && value.isEmpty()) {
                    value = null;
                }
            } else {
                value = null;
            }

            // convert and set the fields
            newValue.setString(name, value);
        }
        return newValue;
    }

    public static Map<String, Object> rebuildAllIndexesAndKeys(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        Security security = dctx.getSecurity();
        Locale locale = (Locale) context.get("locale");

        // check permission
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        if (!security.hasPermission("ENTITY_MAINT", userLogin)) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "EntityExtServicePermissionNotGranted", locale));
        }

        String groupName = (String) context.get("groupName");
        Boolean fixSizes = (Boolean) context.get("fixColSizes");
        if (fixSizes == null) fixSizes = Boolean.FALSE;
        List<String> messages = new LinkedList<>();

        GenericHelperInfo helperInfo = delegator.getGroupHelperInfo(groupName);
        DatabaseUtil dbUtil = new DatabaseUtil(helperInfo);
        Map<String, ModelEntity> modelEntities;
        try {
            modelEntities = delegator.getModelEntityMapByGroup(groupName);
        } catch (GenericEntityException e) {
            Debug.logError(e, "Error getting list of entities in group: " + e.toString(), MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "EntityExtErrorGettingListOfEntityInGroup",
                    UtilMisc.toMap("errorString", e.toString()), locale));
        }

        // step 1 - remove FK indices
        Debug.logImportant("Removing all foreign key indices", MODULE);
        for (ModelEntity modelEntity: modelEntities.values()) {
            dbUtil.deleteForeignKeyIndices(modelEntity, messages);
        }

        // step 2 - remove FKs
        Debug.logImportant("Removing all foreign keys", MODULE);
        for (ModelEntity modelEntity: modelEntities.values()) {
            dbUtil.deleteForeignKeys(modelEntity, modelEntities, messages);
        }

        // step 3 - remove PKs
        Debug.logImportant("Removing all primary keys", MODULE);
        for (ModelEntity modelEntity: modelEntities.values()) {
            dbUtil.deletePrimaryKey(modelEntity, messages);
        }

        // step 4 - remove declared indices
        Debug.logImportant("Removing all declared indices", MODULE);
        for (ModelEntity modelEntity: modelEntities.values()) {
            dbUtil.deleteDeclaredIndices(modelEntity, messages);
        }

        // step 5 - repair field sizes
        if (fixSizes) {
            Debug.logImportant("Updating column field size changes", MODULE);
            List<String> fieldsWrongSize = new LinkedList<>();
            dbUtil.checkDb(modelEntities, fieldsWrongSize, messages, true, true, true, true);
            if (!fieldsWrongSize.isEmpty()) {
                dbUtil.repairColumnSizeChanges(modelEntities, fieldsWrongSize, messages);
            } else {
                String thisMsg = "No field sizes to update";
                messages.add(thisMsg);
                Debug.logImportant(thisMsg, MODULE);
            }
        }

        // step 6 - create PKs
        Debug.logImportant("Creating all primary keys", MODULE);
        for (ModelEntity modelEntity: modelEntities.values()) {
            dbUtil.createPrimaryKey(modelEntity, messages);
        }

        // step 7 - create FK indices
        Debug.logImportant("Creating all foreign key indices", MODULE);
        for (ModelEntity modelEntity: modelEntities.values()) {
            dbUtil.createForeignKeyIndices(modelEntity, messages);
        }

        // step 8 - create FKs
        Debug.logImportant("Creating all foreign keys", MODULE);
        for (ModelEntity modelEntity: modelEntities.values()) {
            dbUtil.createForeignKeys(modelEntity, modelEntities, messages);
        }

        // step 8 - create FKs
        Debug.logImportant("Creating all declared indices", MODULE);
        for (ModelEntity modelEntity: modelEntities.values()) {
            dbUtil.createDeclaredIndices(modelEntity, messages);
        }

        // step 8 - checkdb
        Debug.logImportant("Running DB check with add missing enabled", MODULE);
        dbUtil.checkDb(modelEntities, messages, true);

        Map<String, Object> result = ServiceUtil.returnSuccess();
        result.put("messages", messages);
        return result;
    }

    public static Map<String, Object> unwrapByteWrappers(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        String entityName = (String) context.get("entityName");
        String fieldName = (String) context.get("fieldName");
        Locale locale = (Locale) context.get("locale");

        try (EntityListIterator eli = EntityQuery.use(delegator)
                .from(entityName)
                .queryIterator()) {

            GenericValue currentValue;
            while ((currentValue = eli.next()) != null) {
                byte[] bytes = currentValue.getBytes(fieldName);
                if (bytes != null) {
                    currentValue.setBytes(fieldName, bytes);
                    currentValue.store();
                }
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, "Error unwrapping ByteWrapper records: " + e.toString(), MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "EntityExtErrorUnwrappingRecords",
                    UtilMisc.toMap("errorString", e.toString()), locale));
        }

        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> reencryptPrivateKeys(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        Security security = dctx.getSecurity();
        Locale locale = (Locale) context.get("locale");

        // check permission
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        if (!security.hasPermission("ENTITY_MAINT", userLogin)) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "EntityExtServicePermissionNotGranted", locale));
        }
        String oldKey = (String) context.get("oldKey");
        String newKey = (String) context.get("newKey");
        AesCipherService cipherService = new AesCipherService();
        try {
            List<GenericValue> rows = EntityQuery.use(delegator).from("EntityKeyStore").queryList();
            for (GenericValue row: rows) {
                byte[] keyBytes = Base64.decodeBase64(row.getString("keyText"));
                Debug.logInfo("Processing entry " + row.getString("keyName") + " with key: " + row.getString("keyText"), MODULE);
                if (oldKey != null) {
                    Debug.logInfo("Decrypting with old key: " + oldKey, MODULE);
                    try {
                        keyBytes = cipherService.decrypt(keyBytes, Base64.decodeBase64(oldKey)).getClonedBytes();
                    } catch (Exception e) {
                        Debug.logInfo("Failed to decrypt with Shiro cipher; trying with old cipher", MODULE);
                        try {
                            keyBytes = DesCrypt.decrypt(DesCrypt.getDesKey(Base64.decodeBase64(oldKey)), keyBytes);
                        } catch (Exception e1) {
                            Debug.logError(e1, MODULE);
                            return ServiceUtil.returnError(e1.getMessage());
                        }
                    }
                }
                String newKeyText;
                if (newKey != null) {
                    Debug.logInfo("Encrypting with new key: " + oldKey, MODULE);
                    newKeyText = cipherService.encrypt(keyBytes, Base64.decodeBase64(newKey)).toBase64();
                } else {
                    newKeyText = Base64.encodeBase64String(keyBytes);
                }
                Debug.logInfo("Storing new encrypted value: " + newKeyText, MODULE);
                row.setString("keyText", newKeyText);
                row.store();
            }
        } catch (GenericEntityException gee) {
            Debug.logError(gee, MODULE);
            return ServiceUtil.returnError(gee.getMessage());
        }
        delegator.clearAllCaches();
        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> reencryptFields(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        Security security = dctx.getSecurity();
        Locale locale = (Locale) context.get("locale");

        // check permission
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        if (!security.hasPermission("ENTITY_MAINT", userLogin)) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "EntityExtServicePermissionNotGranted", locale));
        }

        String groupName = (String) context.get("groupName");

        Map<String, ModelEntity> modelEntities;
        try {
            modelEntities = delegator.getModelEntityMapByGroup(groupName);
        } catch (GenericEntityException e) {
            Debug.logError(e, "Error getting list of entities in group: " + e.toString(), MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "EntityExtErrorGettingListOfEntityInGroup",
                    UtilMisc.toMap("errorString", e.toString()), locale));
        }

        for (ModelEntity modelEntity: modelEntities.values()) {
            List<ModelField> fields = modelEntity.getFieldsUnmodifiable();
            for (ModelField field: fields) {
                if (field.getEncryptMethod().isEncrypted()) {
                    try {
                        List<GenericValue> rows = EntityQuery.use(delegator).from(modelEntity.getEntityName()).select(field.getName()).queryList();
                        for (GenericValue row: rows) {
                            row.setString(field.getName(), row.getString(field.getName()));
                            row.store();
                        }
                    } catch (GenericEntityException gee) {
                        return ServiceUtil.returnError(gee.getMessage());
                    }
                }
            }
        }
        return ServiceUtil.returnSuccess();
    }
}
