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
package org.sitenetsoft.sunseterp.framework.base.container;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.sitenetsoft.sunseterp.framework.start.Config;
import org.sitenetsoft.sunseterp.framework.start.Start;
import org.sitenetsoft.sunseterp.framework.start.Start.ServerState;
import org.sitenetsoft.sunseterp.framework.start.StartupCommand;
import org.sitenetsoft.sunseterp.framework.base.util.UtilValidate;

/**
 * The AdminServer provides a way to communicate with a running
 * OFBiz instance after it has started and send commands to that instance
 * such as inquiring on server status or requesting system shutdown
 */
public final class AdminServerContainer implements Container {
    /**
     * Commands communicated between AdminClient and AdminServer
     */
    public enum OfbizSocketCommand {
        SHUTDOWN, STATUS, FAIL
    }

    private String name;
    private Thread serverThread;
    private ServerSocket serverSocket;
    private Config cfg = Start.getInstance().getConfig();

    @Override
    public void init(List<StartupCommand> ofbizCommands, String name, String configFile) throws ContainerException {
        this.name = name;
        // TODO: Replace for Quarkus
        /*try {
            serverSocket = new ServerSocket(cfg.getAdminPort(), 1, cfg.getAdminAddress());
        } catch (IOException e) {
            String msg = "Couldn't create server socket(" + cfg.getAdminAddress() + ":" + cfg.getAdminPort() + ")";
            throw new ContainerException(msg, e);
        }

        if (cfg.getAdminPort() > 0) {
            serverThread = new Thread(this::run, "OFBiz-AdminServer");
        } else {
            serverThread = new Thread("OFBiz-AdminServer"); // Dummy thread
            System.out.println("Admin socket not configured; set to port 0");
        }
        serverThread.setDaemon(false);*/
    }

    // Listens for administration commands.
    private void run() {
        System.out.println("Admin socket configured on - " + cfg.getAdminAddress() + ":" + cfg.getAdminPort());
        // TODO: Replace to make it work with Quarkus
        /*while (!Thread.interrupted()) {
            try (Socket client = serverSocket.accept()) {
                System.out.println("Received connection from - " + client.getInetAddress() + " : " + client.getPort());
                processClientRequest(client);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }*/
    }

    @Override
    public boolean start() throws ContainerException {
        serverThread.start();
        return true;
    }

    @Override
    public void stop() throws ContainerException {
        if (serverThread.isAlive()) {
            serverThread.interrupt();
        }
    }

    @Override
    public String getName() {
        return name;
    }

    private void processClientRequest(Socket client) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8));
             PrintWriter writer = new PrintWriter(new OutputStreamWriter(client.getOutputStream(), StandardCharsets.UTF_8), true)) {

            // read client request and prepare response
            String clientRequest = reader.readLine();
            OfbizSocketCommand clientCommand = determineClientCommand(clientRequest);
            String serverResponse = prepareResponseToClient(clientCommand);

            // send response back to client
            writer.println(serverResponse);

            // if the client request is shutdown, execute shutdown sequence
            if (clientCommand.equals(OfbizSocketCommand.SHUTDOWN)) {
                writer.flush();
                // TODO: This was commented out... What to do next?
                //Start.getInstance().stop();
            }
        }
    }
    private OfbizSocketCommand determineClientCommand(String request) {
        if (!isValidRequest(request)) {
            return OfbizSocketCommand.FAIL;
        }
        return OfbizSocketCommand.valueOf(request.substring(request.indexOf(':') + 1));
    }

    /**
     * Validates if request is a suitable String
     * @param request
     * @return boolean which shows if request is suitable
     */
    private boolean isValidRequest(String request) {
        return UtilValidate.isNotEmpty(request)
                && request.contains(":")
                && request.substring(0, request.indexOf(':')).equals(cfg.getAdminKey())
                && !request.substring(request.indexOf(':') + 1).isEmpty();
    }
    private static String prepareResponseToClient(OfbizSocketCommand control) {
        String response = null;
        ServerState state = Start.getInstance().getCurrentState();
        switch (control) {
            case SHUTDOWN:
                if (state == ServerState.STOPPING) {
                    response = "IN-PROGRESS";
                } else {
                    response = "OK";
                }
                break;
            case STATUS:
                response = state.toString();
                break;
            case FAIL:
                response = "FAIL";
                break;
        }
        return response;
    }
}
