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

package org.forgerock.http.servlet;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import java.util.Map;

import org.forgerock.http.HttpApplication;

/**
 * A {@code ServletContextListener} that sets {@code String} keyed
 * {@code HttpApplication} instances as attributes on the
 * {@code ServletContext}.
 */
public abstract class HttpFrameworkServletContextListener implements ServletContextListener {

    /**
     * Registers the {@link HttpApplication} instances in the
     * {@link ServletContext} attributes.
     *
     * @param event {@inheritDoc}
     */
    @Override
    public final void contextInitialized(ServletContextEvent event) {
        for (Map.Entry<String, HttpApplication> applications : getHttpApplications().entrySet()) {
            event.getServletContext().setAttribute(applications.getKey(), applications.getValue());
        }
        event.getServletContext().log("HTTP Application is ready.");
    }

    /**
     * No action performed.
     *
     * @param event {@inheritDoc}
     */
    @Override
    public final void contextDestroyed(ServletContextEvent event) {
        //Nothing to do
    }

    /**
     * Gets a {@code Map} of {@link HttpApplication} instances keyed by a
     * {@code String} application key.
     *
     * @return A {@code Map} containing application key to
     * {@code HttpApplication} instances.
     */
    protected abstract Map<String, HttpApplication> getHttpApplications();
}
