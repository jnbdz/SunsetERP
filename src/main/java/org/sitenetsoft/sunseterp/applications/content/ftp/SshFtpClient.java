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
package org.sitenetsoft.sunseterp.applications.content.ftp;

import org.apache.commons.io.IOUtils;
import org.sitenetsoft.sunseterp.framework.base.util.UtilValidate;
import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.sftp.client.SftpClient;
import org.apache.sshd.sftp.client.SftpClientFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Basic client to copy files to an ssh ftp server
 */
public class SshFtpClient implements FtpClientInterface {

    private static final String MODULE = SshFtpClient.class.getName();

    private final SshClient client;
    private SftpClient sftp;
    private ClientSession session;

    public SshFtpClient() {
        client = SshClientHelper.getSshClient();
    }

    @Override
    public void connect(String hostname, String username, String password, Long port, Long timeout) throws IOException {
        if (port == null) port = 22L;
        if (timeout == null) timeout = 10000L;

        session = client.connect(username, hostname, port.intValue()).verify(timeout.intValue()).getSession();
        session.addPasswordIdentity(password);
        session.auth().verify();
        sftp = SftpClientFactory.instance().createSftpClient(session).singleSessionInstance();
    }

    @Override
    public void copy(String path, String fileName, InputStream file) throws IOException {
        OutputStream os = sftp.write((UtilValidate.isNotEmpty(path) ? path + "/" : "") + fileName);
        IOUtils.copy(file, os);
        os.close();
    }

    @Override
    public List<String> list(String path) throws IOException {
        SftpClient.CloseableHandle handle = sftp.openDir((UtilValidate.isNotEmpty(path) ? path + "/" : ""));
        List<String> fileNames = new ArrayList<>();
        for (SftpClient.DirEntry dirEntry : sftp.listDir(handle)) {
            fileNames.add(dirEntry.getFilename());
        }
        return fileNames;
    }

    @Override
    public void setBinaryTransfer(boolean isBinary) throws IOException {
    }

    @Override
    public void setPassiveMode(boolean isPassive) throws IOException {
    }

    @Override
    public void closeConnection() throws IOException {
        if (sftp != null) sftp.close();
        if (session != null) session.close();
    }
}
