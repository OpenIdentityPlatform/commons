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

package org.forgerock.authz;

/**
 * Utility class for creating instances of classes from class names.
 */
public class InstanceCreator {

    /**
     * Creates an instance of the given class using the zero-arg constructor.
     *
     * @param className The name of the class to created.
     * @param type The type of the class.
     * @param <T> The type of the class.
     * @return An instance of the class
     * @throws ClassNotFoundException If the class could not be found.
     * @throws IllegalAccessException If the class could not be created.
     * @throws InstantiationException If the class could not be created.
     * @throws NullPointerException If the className is null.
     */
    public  <T> T createInstance(final String className, final Class<T> type) throws ClassNotFoundException,
            IllegalAccessException, InstantiationException {

        if (className == null) {
            throw new NullPointerException("Class name can not be null.");
        }

        Class<? extends T> clazz = Class.forName(className).asSubclass(type);
        return clazz.newInstance();
    }
}
