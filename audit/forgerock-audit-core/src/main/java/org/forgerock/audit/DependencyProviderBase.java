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
package org.forgerock.audit;

import java.util.HashMap;
import java.util.Map;

/**
 * Base DependencyProvider that has provides no dependencies.
 */
public class DependencyProviderBase implements DependencyProvider {

    private final Map<Class<?>, Object> dependencies = new HashMap<>();

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public <T> T getDependency(Class<T> clazz) throws ClassNotFoundException {
        T dependency = (T) dependencies.get(clazz);
        if (dependency == null) {
            throw new ClassNotFoundException("No instance registered for class: " + clazz.getName());
        } else {
            return dependency;
        }
    }

    /**
     * Register a new provided dependency.
     * @param <T> The type of the dependency.
     * @param clazz the class to register
     * @param obj the instance to provide
     * @return the previous values registered for {@literal clazz}
     */
    public <T> T register(Class<T> clazz, T obj) {
        return (T) dependencies.put(clazz, obj);
    }
}