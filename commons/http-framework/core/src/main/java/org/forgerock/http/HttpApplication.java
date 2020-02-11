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
 * Copyright 2014-2016 ForgeRock AS.
 */
package org.forgerock.http;

import org.forgerock.http.io.Buffer;
import org.forgerock.util.Factory;

/**
 * Configuration class to configure the {@code HttpApplication} instance.
 * <p>
 *     If the application should expose OpenAPI API Descriptors, the {@link DescribedHttpApplication} interface should
 *     be used instead of this one.
 * </p>
 * <p>
 *     The implementation of this class will be loaded using the {@link java.util.ServiceLoader} framework.
 * </p>
 */
public interface HttpApplication {

    /**
     * Gets the root {@link Handler} that will handle all HTTP requests.
     *
     * <p>The {@code Handler} returned from this method MUST be a singleton.</p>
     *
     * @return The {@code Handler} to handle HTTP requests.
     * @throws HttpApplicationException If there is a problem constructing the
     * root application {@code Handler}.
     */
    Handler start() throws HttpApplicationException;

    /**
     * Gets the {@link Factory} that will create temporary storage
     * {@link Buffer}s to handle the processing of requests.
     *
     * <p>May return {@code null} indicating that the container should provide
     * a default buffer factory.</p>
     *
     * @return A {@code Buffer} {@code Factory} or {@code null}.
     */
    Factory<Buffer> getBufferFactory();

    /**
     * Called when HTTP application is shutdown.
     *
     * <p>Implementation should use this method to clear up all remaining
     * resources.</p>
     */
    void stop();

}
