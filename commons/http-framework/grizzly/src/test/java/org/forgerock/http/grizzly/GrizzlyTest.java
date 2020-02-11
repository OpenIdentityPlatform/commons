/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions Copyright [year] [name of copyright owner]".
 *
 * Copyright 2016 ForgeRock AS.
 */
package org.forgerock.http.grizzly;

import static org.forgerock.http.grizzly.GrizzlySupport.*;

import org.forgerock.http.HttpApplication;
import org.forgerock.http.bindings.BindingTest;
import org.glassfish.grizzly.PortRange;
import org.glassfish.grizzly.http.server.HttpServer;

public class GrizzlyTest extends BindingTest {

    private HttpServer server;

    @Override
    protected void createServer() {
        server = HttpServer.createSimpleServer(null, new PortRange(6000, 7000));
    }

    @Override
    protected void stopServer() throws Exception {
        server.shutdownNow();
    }

    @Override
    protected int startServer() throws Exception {
        server.start();
        return server.getListeners().iterator().next().getPort();
    }

    @Override
    protected void addApplication(HttpApplication application) throws Exception {
        server.getServerConfiguration().addHttpHandler(newGrizzlyHttpHandler(application));
    }

}
