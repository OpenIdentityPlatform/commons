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

import javax.servlet.FilterConfig;
import javax.servlet.ServletException;

/**
 * Provides a method to get an instance of a class by calling a static factory method on a class, which will
 * return the required class, using configuration settings defined in the filter configuration init params.
 *
 * @since 1.3.0
 */
public interface FilterConfiguration {

    /**
     * Gets an instance of a class by calling a static factory method on a class, as defined in the given FilterConfig
     * instance.
     * <p>
     * The class to call the factory method on is retrieved from the Filter Config using the given classNameParam,
     * similarly the method to call is retrieved from the Filter Config using the given methodNameParam. If the
     * methodNameParam is not present on the Filter Config then the method specified in the defaultMethodName will be
     * used instead.
     * <p>
     * The method to be called will be called firstly with the Filter Config instance as a parameter, and if no method
     * exists then it will be called with no args.
     *
     * @param config The FilterConfig instance.
     * @param classNameParam The name of the class to call the factory method on.
     * @param methodNameParam The name of the factory method to call.
     * @param defaultMethodName The default name of the factory method.
     * @param <T> The type of instance that the factory method will return.
     * @return An instance that the factory method returns.
     * @throws ServletException If there is an error accessing the values in the Filter Config.
     */
    public <T> T get(final FilterConfig config, final String classNameParam, final String methodNameParam,
            final String defaultMethodName) throws ServletException;
}
