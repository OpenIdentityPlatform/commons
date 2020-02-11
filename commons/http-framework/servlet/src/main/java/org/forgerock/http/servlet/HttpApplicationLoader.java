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

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;

import com.google.inject.ConfigurationException;
import com.google.inject.ProvisionException;
import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.http.HttpApplication;

/**
 * An enum containing the possible methods of loading the {@code HttpApplication}.
 */
enum HttpApplicationLoader {

    /**
     * Uses the {@link ServiceLoader} framework to locate all instances of
     * {@code HttpApplication}s on the classpath.
     *
     * <p>If no {@code HttpApplication} implementation is found or multiple are
     * found then a {@link ServletException} will be thrown.</p>
     *
     * <p>The following is an example Servlet declaration:
     * <pre>
     * &lt;servlet&gt;
     *     &lt;servlet-name&gt;ServletPathHttpServlet&lt;/servlet-name&gt;
     *     &lt;servlet-class&gt;org.forgerock.http.servlet.HttpFrameworkServlet&lt;/servlet-class&gt;
     *     &lt;async-supported&gt;true&lt;/async-supported&gt;
     *     &lt;init-param&gt;
     *         &lt;param-name&gt;application-loader&lt;/param-name&gt;
     *         &lt;param-value&gt;service_loader&lt;/param-value&gt; &lt;!--This is the default so can be omitted--&gt;
     *     &lt;/init-param&gt;
     *     &lt;init-param&gt;
     *         &lt;param-name&gt;routing-base&lt;/param-name&gt;
     *         &lt;param-value&gt;servlet_path&lt;/param-value&gt; &lt;!--This is the default so can be omitted--&gt;
     *     &lt;/init-param&gt;
     * &lt;/servlet&gt;
     * </pre></p>
     *
     * @see ServletRoutingBase
     */
    SERVICE_LOADER {
        @Override
        HttpApplication load(ServletConfig config) throws ServletException {
            ServiceLoader<HttpApplication> configurations = ServiceLoader.load(HttpApplication.class);
            Iterator<HttpApplication> iterator = configurations.iterator();

            if (!iterator.hasNext()) {
                throw new ServletException("No HttpApplication implementation registered.");
            }

            HttpApplication configuration = iterator.next();

            if (iterator.hasNext()) {
                // Multiple ServletConfigurations registered!
                List<Object> messageParams = new ArrayList<Object>();
                messageParams.add(iterator.next().getClass().getName());

                String message = "Multiple HttpApplication implementations registered.\n"
                        + "%d configurations found: %s";

                while (iterator.hasNext()) {
                    messageParams.add(iterator.next().getClass().getName());
                    message += ", %s";
                }
                messageParams.add(0, messageParams.size());

                throw new ServletException(String.format(message, messageParams.toArray()));
            }
            return configuration;
        }
    },

    /**
     * Uses the attributes of the {@link ServletContext} to find the
     * {@code HttpApplication} using a key provided by an
     * {@literal application-key} init-param. The attributes on the
     * {@code ServletContext} must be set before the
     * {@code HttpFrameworkServlet} is initialised.
     *
     * <p>If no {@literal application-key} init-param is set, no
     * {@code HttpApplication} defined for the key or the object defined for
     * the key is not a {@code HttpApplication} then a {@link ServletException}
     * will be thrown.</p>
     *
     * <p>The following is an example Servlet declaration:
     * <pre>
     * &lt;listener&gt;
     *     &lt;listener-class&gt;
     *         org.forgerock.http.servlet.example.multiple.ExampleHttpFrameworkServletContextListener
     *     &lt;/listener-class&gt;
     * &lt;/listener&gt;
     *
     * &lt;servlet&gt;
     *     &lt;servlet-name&gt;AdminHttpApplicationServlet&lt;/servlet-name&gt;
     *     &lt;servlet-class&gt;org.forgerock.http.servlet.HttpFrameworkServlet&lt;/servlet-class&gt;
     *     &lt;async-supported&gt;true&lt;/async-supported&gt;
     *     &lt;init-param&gt;
     *         &lt;param-name&gt;application-loader&lt;/param-name&gt;
     *         &lt;param-value&gt;servlet_context&lt;/param-value&gt;
     *     &lt;/init-param&gt;
     *     &lt;init-param&gt;
     *         &lt;param-name&gt;application-key&lt;/param-name&gt;
     *         &lt;param-value&gt;adminApp&lt;/param-value&gt;
     *     &lt;/init-param&gt;
     * &lt;/servlet&gt;
     * </pre></p>
     *
     * @see HttpFrameworkServletContextListener
     */
    SERVLET_CONTEXT {
        private final static String APPLICATION_KEY_INIT_PARAM_NAME = "application-key";

        @Override
        HttpApplication load(ServletConfig config) throws ServletException {
            String applicationKey = config.getInitParameter(APPLICATION_KEY_INIT_PARAM_NAME);
            if (applicationKey == null) {
                throw new ServletException("No " + APPLICATION_KEY_INIT_PARAM_NAME + " init-param set.");
            }
            Object application = config.getServletContext().getAttribute(applicationKey);
            if (application == null) {
                throw new ServletException("No HttpApplication found for key: " + applicationKey);
            } else if (!(application instanceof HttpApplication)) {
                throw new ServletException("Invalid type: " + applicationKey.getClass().getName()
                        + ". Application MUST BE an instance of " + HttpApplication.class.getName());
            }
            return (HttpApplication) application;
        }
    },

    /**
     * Uses {@literal Guice} to locate the {@code HttpApplication} instance.
     * This application loader can only be used if
     * {@literal org.forgerock.commons:forgerock-guice-core} is on the
     * classpath.
     *
     * <p>If no {@literal application-class} init-param is set the
     * {@literal Guice} will be asked to get an instance of the
     * {@code HttpApplication} interface.</p>
     *
     * <p>The following is an example Servlet declaration:
     * <pre>
     * &lt;servlet&gt;
     *     &lt;servlet-name&gt;ServletPathHttpServlet&lt;/servlet-name&gt;
     *     &lt;servlet-class&gt;org.forgerock.http.servlet.HttpFrameworkServlet&lt;/servlet-class&gt;
     *     &lt;async-supported&gt;true&lt;/async-supported&gt;
     *     &lt;init-param&gt;
     *         &lt;param-name&gt;application-loader&lt;/param-name&gt;
     *         &lt;param-value&gt;guice&lt;/param-value&gt;
     *     &lt;/init-param&gt;
     *     &lt;init-param&gt;
     *         &lt;!--Defaults to HttpApplication if omitted--&gt;
     *         &lt;param-name&gt;application-class&lt;/param-name&gt;
     *         &lt;param-value&gt;org.forgerock.http.servlet.example.ExampleHttpApplication&lt;/param-value&gt;
     *     &lt;/init-param&gt;
     *     &lt;init-param&gt;
     *         &lt;param-name&gt;routing-base&lt;/param-name&gt;
     *         &lt;param-value&gt;servlet_path&lt;/param-value&gt; &lt;!--This is the default so can be omitted--&gt;
     *     &lt;/init-param&gt;
     * &lt;/servlet&gt;
     * </pre></p>
     *
     * @see InjectorHolder
     */
    GUICE {
        @Override
        HttpApplication load(ServletConfig config) throws ServletException {
            return LazilyLinkGuice.load(config);
        }
    };

    // Force lazy linkage to Guice because the Guice dependency is optional.
    private static class LazilyLinkGuice {
        private static HttpApplication load(ServletConfig config) throws ServletException {
            String applicationClassName = config.getInitParameter("application-class");
            try {
                if (applicationClassName == null) {
                    return InjectorHolder.getInstance(HttpApplication.class);
                } else {
                    return InjectorHolder.getInstance(Class.forName(applicationClassName)
                            .asSubclass(HttpApplication.class));
                }
            } catch (ClassNotFoundException e) {
                throw new ServletException("Failed to find the Http Application class: " + applicationClassName, e);
            } catch (ConfigurationException | ProvisionException e) {
                throw new ServletException("Failed to load the Http Application class: " + applicationClassName, e);
            }
        }
    }

    /**
     * Loads the {@code HttpApplication}.
     *
     * @param config The {@code ServletConfig}.
     * @return The {@code HttpApplication} instance.
     * @throws ServletException If the {@code HttpApplication} could not be
     * loaded.
     */
    abstract HttpApplication load(ServletConfig config) throws ServletException;
}
