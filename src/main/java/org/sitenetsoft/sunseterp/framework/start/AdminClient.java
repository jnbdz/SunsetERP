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
package org.sitenetsoft.sunseterp.framework.start;

/*import org.sitenetsoft.sunseterp.framework.base.container.AdminServerContainer.OfbizSocketCommand;

import java.io.*;
import java.net.ConnectException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;*/

/**
 * The AdminClient communicates with a running OFBiz server instance
 * through the AdminServer by sending commands like requesting server
 * status or requesting system shutdown
 */
class AdminClient {

    /**
     * Send a command through network to OFBiz server
     * to show its status (running, stopping, ...)
     * @param config OFBiz configuration
     * @return status OFBiz server status
     */
    /*static String requestStatus(Config config) {
        String status = null;
        try {
            status = sendSocketCommand(OfbizSocketCommand.STATUS, config);
        } catch (ConnectException e) {
            status = "Not Running";
        } catch (IOException e) {
            status = "IO Error when trying to connect to OFBiz: " + e.getMessage();
        }
        return status;
    }*/

    /**
     * Send a command through network to OFBiz server
     * to shut itself down.
     * @param config OFBiz configuration
     * @return shutdownMessage message from server
     *   on receiving shutdown request
     */
    /*static String requestShutdown(Config config) {
        String shutdownMessage = null;
        try {
            shutdownMessage = sendSocketCommand(OfbizSocketCommand.SHUTDOWN, config);
        } catch (IOException e) {
            shutdownMessage = "IO Error when trying to connect to OFBiz: " + e.getMessage();
        }
        return shutdownMessage;
    }

    private static String sendSocketCommand(OfbizSocketCommand socketCommand, Config config) throws IOException {
        String response = "OFBiz is Down";
        try (Socket socket = new Socket(config.getAdminAddress(), config.getAdminPort());
                PrintWriter writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {
            // send the command
            writer.println(config.getAdminKey() + ":" + socketCommand);
            writer.flush();
            // read the reply
            response = reader.readLine();
        } catch (ConnectException e) {
            System.out.println("Could not connect to " + config.getAdminAddress() + ":" + config.getAdminPort());
        }
        return response;
    }*/
}
