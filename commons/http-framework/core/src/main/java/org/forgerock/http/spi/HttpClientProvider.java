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
 * Copyright 2014-2015 ForgeRock AS.
 */

package org.forgerock.http.spi;

import org.forgerock.http.HttpApplicationException;
import org.forgerock.util.Options;

/**
 * A provider interface for obtaining {@link HttpClient} instances. A
 * {@link HttpClientProvider} is loaded during construction of a new HTTP
 * {@link org.forgerock.http.Client Client}. A provider is selected and its
 * {@link #newHttpClient(Options)} method invoked in order to construct and
 * configure a new {@link HttpClient}.
 */
public interface HttpClientProvider {
    /**
     * Returns a new {@link HttpClient} configured using the provided set of
     * options.
     *
     * @param options
     *            The client options (never {@code null}).
     * @return A new {@link HttpClient} configured using the provided set of
     *         options.
     * @throws HttpApplicationException
     *             If the client implementation could not be configured using
     *             the provided set of options.
     */
    HttpClient newHttpClient(Options options) throws HttpApplicationException;
}
