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
 * Copyright 2013 ForgeRock AS.
 */

package org.forgerock.jaspi.utils;

import org.forgerock.auth.common.DebugLogger;
import org.forgerock.jaspi.logging.LogFactory;

import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import java.lang.reflect.Method;

/**
 * The singleton implementation of the FilterConfiguration interface.
 *
 * @since 1.3.0
 */
public enum FilterConfigurationImpl implements FilterConfiguration {

    /**
     * The Singleton instance of the FilterConfigurationImpl.
     */
    INSTANCE;

    private static final DebugLogger LOGGER = LogFactory.getDebug();

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public <T> T get(final FilterConfig config, final String classNameParam, final String methodNameParam,
            final String defaultMethodName) throws ServletException {

        if (config != null) {
            // Check for configured connection factory class first.
            final String className = config.getInitParameter(classNameParam);
            if (className != null) {
                try {
                    final Class<?> cls = Class.forName(className);
                    final String tmp = config.getInitParameter(methodNameParam);
                    final String methodName = tmp != null ? tmp : defaultMethodName;
                    try {
                        final Method factoryMethod = cls.getDeclaredMethod(methodName);
                        return (T) factoryMethod.invoke(null, config);
                    } catch (final IllegalArgumentException e) {
                        // Try no-arg method.
                        final Method factoryMethod = cls.getDeclaredMethod(methodName);
                        return (T) factoryMethod.invoke(null);
                    }
                } catch (final Exception e) {
                    LOGGER.error("Could not construct instance of, " + className, e);
                    throw new ServletException(e);
                }
            }
        }
        return null;
    }
}
