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
 * Copyright 2015 ForgeRock AS.
 */

package org.forgerock.http.servlet.example;

import java.util.HashMap;
import java.util.Map;

import org.forgerock.http.HttpApplication;
import org.forgerock.http.servlet.HttpFrameworkServletContextListener;

/**
 * Example implementation of the {@link HttpFrameworkServletContextListener}
 * which registers two {@link HttpApplication}s.
 */
public class ExampleHttpFrameworkServletContextListener extends HttpFrameworkServletContextListener {

    @Override
    protected Map<String, org.forgerock.http.HttpApplication> getHttpApplications() {
        Map<String, org.forgerock.http.HttpApplication> applications = new HashMap<>();
        applications.put("adminApp", new ExampleHttpApplication("adminApp"));
        applications.put("app", new ExampleHttpApplication("app"));
        return applications;
    }
}
