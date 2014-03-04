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

package org.forgerock.authz.modules.oauth2;

import org.forgerock.authz.AuthorizationException;
import org.forgerock.json.fluent.JsonValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * Factory class for creating OAuth2AccessTokenValidators from a given fully qualified class name.
 *
 * @since 1.4.0
 */
public class AccessTokenValidatorFactory {

    private final Logger logger = LoggerFactory.getLogger(AccessTokenValidatorFactory.class);

    /**
     * Creates an instance of the given OAuth2AccessTokenValidator class.
     * <br/>
     * Initially tries to use a single-arg constructor which takes a JsonValue containing the module configuration, if
     * that fails then uses the zero-arg constructor.
     *
     * @param className The fully qualified class name of the OAuth2AccessTokenValidator.
     * @param config The module configuration.
     * @return An instance of the OAuth2AccessTokenValidator.
     * @throws AuthorizationException If the OAuth2AccessTokenValidator could not be constructed.
     * @throws NullPointerException If the className is <code>null</code>.
     */
    public OAuth2AccessTokenValidator getInstance(final String className, final JsonValue config) {

        if (className == null) {
            throw new NullPointerException("OAuth2AccessTokenValidator class name cannot be null.");
        }

        try {
            Class<? extends OAuth2AccessTokenValidator> accessTokenValidatorClass =
                    Class.forName(className).asSubclass(OAuth2AccessTokenValidator.class);

            Constructor<? extends OAuth2AccessTokenValidator> constructor;
            Object[] params = new Object[]{config};
            try {
                constructor = accessTokenValidatorClass.getConstructor(JsonValue.class);
            } catch (NoSuchMethodException e) {
                constructor = accessTokenValidatorClass.getConstructor();
                params = new Object[0];
            }

            return constructor.newInstance(params);

        } catch (ClassNotFoundException e) {
            logger.error("Failed to find AccessTokenValidatorClass.", e);
            throw new AuthorizationException("Failed to find AccessTokenValidatorClass.", e);
        } catch (InstantiationException e) {
            logger.error("Failed to create AccessTokenValidatorClass.", e);
            throw new AuthorizationException("Failed to create AccessTokenValidatorClass.", e);
        } catch (IllegalAccessException e) {
            logger.error("Failed to create AccessTokenValidatorClass.", e);
            throw new AuthorizationException("Failed to create AccessTokenValidatorClass.", e);
        } catch (NoSuchMethodException e) {
            logger.error("AccessTokenValidatorClass must have a zero-arg constructor or a single arg constructor "
                    + "that accepts a JsonValue.", e);
            throw new AuthorizationException("AccessTokenValidatorClass must have a zero-arg constructor or a "
                    + "single arg constructor that accepts a JsonValue.", e);
        } catch (InvocationTargetException e) {
            logger.error("Failed to create AccessTokenValidatorClass.", e);
            throw new AuthorizationException("Failed to create AccessTokenValidatorClass.", e);
        }
    }
}
