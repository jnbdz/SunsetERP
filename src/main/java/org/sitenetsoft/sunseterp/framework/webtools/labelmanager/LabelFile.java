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
package org.sitenetsoft.sunseterp.framework.webtools.labelmanager;

import java.io.File;

public class LabelFile {
    private static final String MODULE = LabelFile.class.getName();

    private final boolean fileLoaded = false;
    private final File file;
    private final String componentName;

    protected LabelFile(File file, String componentName) {
        this.file = file;
        this.componentName = componentName;
    }

    /**
     * Gets file.
     * @return the file
     */
    public File getFile() {
        return this.file;
    }

    /**
     * Gets file name.
     * @return the file name
     */
    public String getFileName() {
        return this.file.getName();
    }

    /**
     * Gets file path.
     * @return the file path
     */
    public String getFilePath() {
        return this.file.getPath();
    }

    /**
     * Gets component name.
     * @return the component name
     */
    public String getComponentName() {
        return componentName;
    }
}
