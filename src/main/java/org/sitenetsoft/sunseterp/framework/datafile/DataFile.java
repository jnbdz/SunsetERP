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
package org.sitenetsoft.sunseterp.framework.datafile;

import org.sitenetsoft.sunseterp.framework.base.util.Debug;
import org.sitenetsoft.sunseterp.framework.base.util.UtilValidate;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 *  DataFile main class
 *
 */

public class DataFile {

    private static final String MODULE = DataFile.class.getName();

    /** List of record in the file, contains Record objects */
    private List<Record> records = new ArrayList<>();

    /** Contains the definition for the file */
    private ModelDataFile modelDataFile;

    /** Creates a DataFile object which will contain the parsed objects for the specified datafile, using the specified definition.
     * @param fileUrl The URL where the data file is located
     * @param definitionUrl The location of the data file definition XML file
     * @param dataFileName The data file model name, as specified in the definition XML file
     * @throws DataFileException Exception thown for various errors, generally has a nested exception
     * @return A new DataFile object with the specified file pre-loaded
     */
    static DataFile readFile(URL fileUrl, URL definitionUrl, String dataFileName) throws DataFileException {
        DataFile dataFile = makeDataFile(definitionUrl, dataFileName);

        dataFile.readDataFile(fileUrl);
        return dataFile;
    }

    /** Creates a DataFile object using the specified definition.
     * @param definitionUrl The location of the data file definition XML file
     * @param dataFileName The data file model name, as specified in the definition XML file
     * @throws DataFileException Exception thown for various errors, generally has a nested exception
     * @return A new DataFile object
     */
    public static DataFile makeDataFile(URL definitionUrl, String dataFileName) throws DataFileException {
        ModelDataFileReader reader = ModelDataFileReader.getModelDataFileReader(definitionUrl);

        if (reader == null) {
            throw new DataFileException("Could not load definition file located at \"" + definitionUrl + "\"");
        }
        ModelDataFile modelDataFile = reader.getModelDataFile(dataFileName);

        if (modelDataFile == null) {
            throw new DataFileException("Could not find file definition for data file named \"" + dataFileName + "\"");
        }
        DataFile dataFile = new DataFile(modelDataFile);

        return dataFile;
    }

    /** Construct a DataFile object setting the model, does not load it
     * @param modelDataFile The model of the DataFile to instantiate
     */
    public DataFile(ModelDataFile modelDataFile) {
        this.modelDataFile = modelDataFile;
    }

    protected DataFile() {
    }

    /**
     * Gets model data file.
     * @return the model data file
     */
    private ModelDataFile getModelDataFile() {
        return modelDataFile;
    }

    /**
     * Gets records.
     * @return the records
     */
    public List<Record> getRecords() {
        return records;
    }

    /**
     * Add record.
     * @param record the record
     */
    public void addRecord(Record record) {
        records.add(record);
    }

    /**
     * Make record record.
     * @param recordName the record name
     * @return the record
     */
    public Record makeRecord(String recordName) {
        ModelRecord modelRecord = getModelDataFile().getModelRecord(recordName);
        return new Record(modelRecord);
    }

    /** Loads (or reloads) the data file at the pre-specified location.
     * @param fileUrl The URL that the file will be loaded from
     * @throws DataFileException Exception thown for various errors, generally has a nested exception
     */
    private void readDataFile(URL fileUrl) throws DataFileException {
        if (fileUrl == null) {
            throw new IllegalStateException("File URL is null, cannot load file");
        }

        RecordIterator recordIterator = this.makeRecordIterator(fileUrl);
        while (recordIterator.hasNext()) {
            this.records.add(recordIterator.next());
        }
        // no need to manually close the stream since we are reading to the end of the file: recordIterator.close();
    }

    /** Populates (or reloads) the data file with the text of the given content
     * @param content The text data to populate the DataFile with
     * @throws DataFileException Exception thown for various errors, generally has a nested exception
     */
    public void readDataFile(String content) throws DataFileException {
        if (UtilValidate.isEmpty(content)) {
            throw new IllegalStateException("Content is empty, can't read file");
        }

        ByteArrayInputStream bis = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));

        readDataFile(bis, null);
    }

    /** Loads (or reloads) the data file from the given stream
     * @param dataFileStream A stream containing the text data for the data file
     * @param locationInfo Text information about where the data came from for exception messages
     * @throws DataFileException Exception thown for various errors, generally has a nested exception
     */
    private void readDataFile(InputStream dataFileStream, String locationInfo) throws DataFileException {
        if (modelDataFile == null) {
            throw new IllegalStateException("DataFile model is null, cannot load file");
        }
        if (locationInfo == null) {
            locationInfo = "unknown";
        }

        RecordIterator recordIterator = this.makeRecordIterator(dataFileStream, locationInfo);
        while (recordIterator.hasNext()) {
            this.records.add(recordIterator.next());
        }
        // no need to manually close the stream since we are reading to the end of the file: recordIterator.close();
    }

    /**
     * Make record iterator record iterator.
     * @param fileUrl the file url
     * @return the record iterator
     * @throws DataFileException the data file exception
     */
    public RecordIterator makeRecordIterator(URL fileUrl) throws DataFileException {
        return new RecordIterator(fileUrl, this.modelDataFile);
    }

    /**
     * Make record iterator record iterator.
     * @param dataFileStream the data file stream
     * @param locationInfo the location info
     * @return the record iterator
     * @throws DataFileException the data file exception
     */
    private RecordIterator makeRecordIterator(InputStream dataFileStream, String locationInfo) throws DataFileException {
        return new RecordIterator(dataFileStream, this.modelDataFile, locationInfo);
    }

    /**
     * Writes the records in this DataFile object to a text data file
     * @param filename
     *            The filename to put the data into
     * @throws DataFileException
     *             Exception thrown for various errors, generally has a nested
     *             exception
     */
    public void writeDataFile(String filename) throws DataFileException {
        File outFile = new File(filename);

        try (FileOutputStream fos = new FileOutputStream(outFile);) {
            writeDataFile(fos);
        } catch (IOException e) {
            throw new DataFileException("Error occured while writing data to file" + filename, e);
        }
    }

    /** Returns the records in this DataFile object as a plain text data file content
     * @throws DataFileException Exception thown for various errors, generally has a nested exception
     * @return A String containing what would go into a data file as plain text
     */
    public String writeDataFile() throws DataFileException {
        String outString = "";
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();) {
            writeDataFile(bos);
            outString = bos.toString("UTF-8");

        } catch (IOException e) {
            Debug.logWarning(e, MODULE);
        }
        return outString;
    }

    /** Writes the records in this DataFile object to the given OutputStream
     * @param outStream The Stream to put the data into
     * @throws DataFileException Exception thown for various errors, generally has a nested exception
     */
    public void writeDataFile(OutputStream outStream) throws DataFileException {
        writeRecords(outStream, this.records);
    }

    /**
     * Write records.
     * @param outStream the out stream
     * @param records the records
     * @throws DataFileException the data file exception
     */
    private void writeRecords(OutputStream outStream, List<Record> records) throws DataFileException {
        for (Record record : records) {
            String line = record.writeLineString(modelDataFile);

            try {
                outStream.write(line.getBytes(StandardCharsets.UTF_8));
            } catch (IOException e) {
                throw new DataFileException("Could not write to stream;", e);
            }

            if (UtilValidate.isNotEmpty(record.getChildRecords())) {
                writeRecords(outStream, record.getChildRecords());
            }
        }
    }
}
