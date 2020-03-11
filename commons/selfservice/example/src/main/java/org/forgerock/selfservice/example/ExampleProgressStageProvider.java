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
 * Copyright 2015 ForgeRock AS.
 */

package org.forgerock.selfservice.example;

import org.forgerock.http.Client;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.selfservice.core.ProgressStage;
import org.forgerock.selfservice.core.ProgressStageProvider;
import org.forgerock.selfservice.core.config.StageConfig;
import org.forgerock.selfservice.core.config.StageConfigException;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * Simple example progress stage provider that uses reflection to instantiate a commons progress stage.
 *
 * @since 0.8.0
 */
final class ExampleProgressStageProvider implements ProgressStageProvider {

    private final ConnectionFactory connectionFactory;
    private final Client client;

    ExampleProgressStageProvider(ConnectionFactory connectionFactory, Client client) {
        this.connectionFactory = connectionFactory;
        this.client = client;
    }

    @Override
    public ProgressStage<StageConfig> get(Class<? extends ProgressStage<StageConfig>> progressStageClass) {
        Constructor<?>[] constructors = progressStageClass.getConstructors();

        if (constructors.length > 1) {
            throw new StageConfigException("Only expected one constructor for the configured progress stage "
                    + progressStageClass);
        }

        try {
            Constructor<? extends ProgressStage<StageConfig>> constructor =
                    progressStageClass.getConstructor(constructors[0].getParameterTypes());

            Object[] parameters = getParameters(constructor);
            return constructor.newInstance(parameters);

        } catch (NoSuchMethodException | InvocationTargetException
                | IllegalAccessException | InstantiationException e) {
            throw new StageConfigException("Unable to instantiate the configured progress stage", e);
        }
    }

    private Object[] getParameters(Constructor<?> constructor) {
        Class<?>[] parameterTypes = constructor.getParameterTypes();
        Object[] parameters = new Object[parameterTypes.length];

        for (int i = 0; i < parameterTypes.length; i++) {
            if (parameterTypes[i].equals(ConnectionFactory.class)) {
                parameters[i] = connectionFactory;
            } else if (parameterTypes[i].equals(Client.class)) {
                parameters[i] = client;
            } else {
                throw new StageConfigException("Unexpected parameter type for configured progress stage "
                        + parameters[i]);
            }
        }

        return parameters;
    }

}
