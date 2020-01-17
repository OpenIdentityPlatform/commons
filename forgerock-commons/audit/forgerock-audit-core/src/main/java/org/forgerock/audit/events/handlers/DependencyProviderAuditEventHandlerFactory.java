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
 * Copyright 2015-2016 ForgeRock AS.
 */

package org.forgerock.audit.events.handlers;

import org.forgerock.audit.AuditException;
import org.forgerock.audit.DependencyProvider;
import org.forgerock.audit.events.EventTopicsMetaData;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import javax.inject.Inject;

/**
 * AuditEventFactory capable of performing construction injection by resolving dependencies using a DependencyProvider.
 */
public class DependencyProviderAuditEventHandlerFactory implements AuditEventHandlerFactory {

    private final DependencyProvider dependencyProvider;

    /**
     * Construct a new instance.
     *
     * @param dependencyProvider
     *          Dependency lookup abstraction for obtaining resources or objects from the product which
     *          integrates this AuditEventHandler.
     */
    public DependencyProviderAuditEventHandlerFactory(DependencyProvider dependencyProvider) {
        this.dependencyProvider = dependencyProvider;
    }

    @Override
    public <T extends AuditEventHandler> T create(
            String name,
            Class<T> clazz,
            EventHandlerConfiguration configuration,
            EventTopicsMetaData eventTopicsMetaData) throws AuditException {

        Constructor<T> constructor = getConstructorForInjection(clazz);
        Object[] parameters = getConstructorParameters(constructor, name, configuration, eventTopicsMetaData);

        try {
            return constructor.newInstance(parameters);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            String errorMessage = "Unable to create " + clazz.getSimpleName() + " '" + name + "': " + e.getMessage();
            throw new AuditException(errorMessage, e);
        }
    }

    @SuppressWarnings("unchecked")
    private <T extends AuditEventHandler>Constructor<T> getConstructorForInjection(Class<T> clazz) {
        Constructor<?>[] constructors = clazz.getConstructors();
        if (constructors.length == 1) {
            return (Constructor<T>) constructors[0];
        }
        for (Constructor<?> candidateConstructor : constructors) {
            if (hasInjectAnnotation(candidateConstructor)) {
                // TODO: Ensure that only one constructor is marked with the @Inject annotation
                return (Constructor<T>) candidateConstructor;
            }
        }
        throw new IllegalStateException(clazz.getSimpleName()
                + " should have a single public constructor. If multiple public constructors "
                + "are required, annotate one with @Inject.");
    }

    private boolean hasInjectAnnotation(Constructor<?> constructor) {
        for (Annotation annotation : constructor.getDeclaredAnnotations()) {
            if (annotation.annotationType().equals(Inject.class)) {
                return true;
            }
        }
        return false;
    }

    private <T extends AuditEventHandler> Object[] getConstructorParameters(
            Constructor<T> constructor,
            String name,
            EventHandlerConfiguration configuration,
            EventTopicsMetaData eventTopicsMetaData) throws AuditException {

        final Class<?>[] parameterTypes = constructor.getParameterTypes();
        final Object[] parameters = new Object[parameterTypes.length];

        for (int i = 0; i < parameterTypes.length; i++) {
            if (parameterTypes[i].equals(String.class)) {
                parameters[i] = name;
            } else if (parameterTypes[i].isAssignableFrom(configuration.getClass())) {
                parameters[i] = configuration;
            } else if (parameterTypes[i].equals(EventTopicsMetaData.class)) {
                parameters[i] = eventTopicsMetaData;
            } else {
                try {
                    parameters[i] = dependencyProvider.getDependency(parameterTypes[i]);
                } catch (ClassNotFoundException e) {
                    parameters[i] = null;
                }
            }
        }

        return parameters;
    }

}
