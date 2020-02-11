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
 * Copyright 2016 ForgeRock AS.
 */

package org.forgerock.services.descriptor;

import org.forgerock.http.ApiProducer;
import org.forgerock.services.context.Context;

/**
 * A routing component (a CHF {@link org.forgerock.http.Handler} or CREST {@code RequestHandler}) can describe its API
 * by implementing this interface.
 * @param <D> The type of API Descriptor object that will be the result of the description. For example, for CREST this
 *           would be the {@code ApiDescription} class from the api-descriptor module.
 * @param <R> The type of request that will be presented to get API descriptions.
 */
public interface Describable<D, R> {
    /**
     * Provide the API description for the component. This method should perform the heavy-lifting of computing the
     * API descriptor, and should be expected to be called rarely. Upstream handlers should call this method in order to
     * compose all of their downstream API Descriptors into a single descriptor.
     *
     * @param producer The API producer that provides general information to be built into the descriptor.
     * @return The description object.
     */
    D api(ApiProducer<D> producer);

    /**
     * Handle a request for the API Descriptor. This method should not do any computation, but should return the
     * already computed descriptor.
     * @param context The request context.
     * @param request The request.
     * @return The descriptor.
     * @throws IllegalStateException When the request cannot be routed to an acceptable handler.
     * @throws UnsupportedOperationException When there is no API Descriptor available for the request.
     */
    D handleApiRequest(Context context, R request);

    /**
     * Add a listener for API Descriptor changes. The described object should call all the listeners.
     * @param listener The listener.
     */
    void addDescriptorListener(Listener listener);

    /**
     * Remove a listener from API Descriptor changes.
     * @param listener The listener.
     */
    void removeDescriptorListener(Listener listener);

    /**
     * Interface for listener instances. Any object implementing {@link Describable} should call the
     * {@link #notifyDescriptorChange()} method for all listeners once every time the API descriptor that it returns
     * is changed.
     */
    interface Listener {
        /** Implement this method to handle changes to API Descriptors. */
        void notifyDescriptorChange();
    }
}
