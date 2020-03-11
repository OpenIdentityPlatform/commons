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
 * Copyright 2014-2015 ForgeRock AS.
 */

package org.forgerock.guice.core;

import java.util.Iterator;

/**
 * Simple wrapper around the Java ServiceLoader to help facilitate testing code which requires the use of the Java
 * ServiceLoader.
 *
 * @since 1.0.0
 */
public class ServiceLoaderWrapper {

    /**
     * Creates a new Java ServiceLoader for the given service type, using the current thread's
     * {@linkplain java.lang.Thread#getContextClassLoader context class loader}.
     *
     * @see java.util.ServiceLoader#load(Class)
     * @param service The interface or abstract class representing the service.
     * @param <S> The type of the service.
     * @return An Iterable backed by a Java ServiceLoader.
     */
    <S> Iterable<S> load(Class<S> service) {
        return new ServiceSet<S>(java.util.ServiceLoader.load(service));
    }

    /**
     * <p>Simple wrapper around the java.util.ServiceLoader that is returned when loading services.</p>
     *
     * <p>This enables better testing as we cannot mock the java.util.ServiceLoader class as it is marked final.</p>
     *
     * @param <T> The type of the Service.
     */
    private static final class ServiceSet<T> implements Iterable<T> {

        private final java.util.ServiceLoader<T> serviceLoader;

        /**
         * Constructs a new ServiceSet.
         *
         * @param serviceLoader The java.util.ServiceLoader that backs this iterable.
         */
        private ServiceSet(java.util.ServiceLoader<T> serviceLoader) {
            this.serviceLoader = serviceLoader;
        }

        /**
         * Returns the iterator or the underlying java.util.ServiceLoader.
         *
         * @return {@inheritDoc}
         */
        public Iterator<T> iterator() {
            return serviceLoader.iterator();
        }
    }
}
