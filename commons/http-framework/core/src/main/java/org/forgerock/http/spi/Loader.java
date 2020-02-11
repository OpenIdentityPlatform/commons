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
package org.forgerock.http.spi;

import java.util.Iterator;
import java.util.ServiceLoader;

import org.forgerock.util.Options;

/**
 * An SPI interface for implementing alternative service loading strategies. By
 * default the HTTP framework will use a strategy based on {@link ServiceLoader}
 * , but applications may choose to override with their own strategy if needed,
 * for example when running in OSGI environments.
 */
public interface Loader {
    /**
     * The default {@link Loader} implementation used throughout the HTTP
     * framework. This implementation uses {@link ServiceLoader} for loading
     * services.
     */
    Loader SERVICE_LOADER = new Loader() {
        @Override
        public <S> S load(final Class<S> service, final Options options) {
            final ServiceLoader<S> loader = ServiceLoader.load(service);
            final Iterator<S> i = loader.iterator();
            return i.hasNext() ? i.next() : null;
        };
    };

    /**
     * Loads a service of the specified type. Implementations may customize
     * their behavior based on the provided options, e.g. by using a user
     * provided class loader.
     *
     * @param <S>
     *            The type of service to load.
     * @param service
     *            The type of service to load.
     * @param options
     *            The user provided options.
     * @return The loaded service, or {@code null} if no corresponding service
     *         was found.
     */
    <S> S load(final Class<S> service, final Options options);
}
