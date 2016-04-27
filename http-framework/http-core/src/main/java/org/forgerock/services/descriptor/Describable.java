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

import org.forgerock.services.context.ApiContext;

/**
 * A routing component (a CHF {@link org.forgerock.http.Handler} or CREST {@code RequestHandler}) can describe its API
 * by implementing this interface.
 * @param <T> The type of API Descriptor object that will be the result of the description. For example, for CREST this
 *           would be the {@code ApiDescription} class from the api-descriptor module.
 */
public interface Describable<T> {
    /**
     * Provide the API description for the component.
     * @param context The API context that provides information about the
     * @return The description object.
     */
    T api(ApiContext<T> context);
}
