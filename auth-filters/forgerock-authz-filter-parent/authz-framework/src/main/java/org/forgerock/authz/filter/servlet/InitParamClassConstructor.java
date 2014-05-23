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
 * Copyright 2014 ForgeRock AS.
 */

package org.forgerock.authz.filter.servlet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import java.lang.reflect.Method;

/**
 * <p>A {@code InitParamClassConstructor} provides a way to create an instance of a class by using the
 * {@code FilterConfig}s init params.</p>
 *
 * <p>The factory method init param is optional, if not set the default method name will be used.</p>
 *
 * @since 1.5.0
 */
class InitParamClassConstructor {

    private final Logger logger = LoggerFactory.getLogger(InitParamClassConstructor.class);

    /**
     * <p>Creates an instance of a class by calling a static factory method on a factory class, as defined as an init
     * param in the given {@link FilterConfig}.</p>
     *
     * <p>The factory class to call the factory method on is retrieved from the {@code FilterConfig} using the given
     * {@code classNameParam}, similarly the method to call is retrieved from the {@code FilterConfig} using the given
     * {@code methodNameParam}. If the {@code methodNameParam} is not present on the {@code FilterConfig} then the
     * method specified in the {@code defaultMethodName} will be used instead.</p>
     *
     * <p>The method to be called will be called firstly with no args, and if no method exists then it will be called
     * with the {@code FilterConfig} instance as a parameter.</p>
     *
     * @param config The {@code FilterConfig} instance. Must not be {@code null}.
     * @param factoryClassParam The name of the init param containing the name of the factory class.
     * @param factoryMethodNameParam The name of the init param containing the name of the factory method.
     * @param defaultMethodName The default name of the default factory method.
     * @param <T> The type of instance that the factory method will return.
     * @return An instance that the factory method returns.
     * @throws ServletException If there is an error accessing the values in the {@code FilterConfig} or calling the
     * factory method fails.
     */
    @SuppressWarnings("unchecked")
    <T> T construct(FilterConfig config, String factoryClassParam, String factoryMethodNameParam,
            String defaultMethodName) throws ServletException {

        String className = config.getInitParameter(factoryClassParam);
        if (className != null) {
            try {
                Class<?> cls = Class.forName(className);
                String tmp = config.getInitParameter(factoryMethodNameParam);
                String methodName = tmp != null ? tmp : defaultMethodName;
                try {
                    // Try no-arg method first.
                    Method factoryMethod = cls.getDeclaredMethod(methodName);
                    return (T) factoryMethod.invoke(null);
                } catch (NoSuchMethodException e) {
                    // Then try method which accepts FilterConfig.
                    Method factoryMethod = cls.getDeclaredMethod(methodName, FilterConfig.class);
                    return (T) factoryMethod.invoke(null, config);
                }
            } catch (Exception e) {
                logger.error("Could not create instance of, {}",  className);
                throw new ServletException("Could not create instance of, " + className, e);
            }
        }

        return null;
    }
}
