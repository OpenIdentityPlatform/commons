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
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2016 ForgeRock AS.
 */
package org.forgerock.http.servlet;

import org.eclipse.jetty.server.NetworkConnector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.forgerock.http.HttpApplication;
import org.forgerock.http.bindings.BindingTest;

public class ServletTest extends BindingTest {

    private Server server;
    private ServletContextHandler context;

    @Override
    protected void createServer() {
        server = new Server(0);
        context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        server.setHandler(context);
    }

    @Override
    protected int startServer() throws Exception {
        server.start();
        return ((NetworkConnector)server.getConnectors()[0]).getLocalPort();
    }

    @Override
    protected void stopServer() throws Exception {
        server.stop();
    }

    @Override
    protected void addApplication(HttpApplication application) throws Exception {
        context.addServlet(new ServletHolder(new HttpFrameworkServlet(application)), "/*");
    }

}
