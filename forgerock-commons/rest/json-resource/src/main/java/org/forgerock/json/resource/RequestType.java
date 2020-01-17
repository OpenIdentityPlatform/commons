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
 * Copyright 2013-2016 ForgeRock AS.
 */

package org.forgerock.json.resource;

/**
 * An enumeration whose values represent the different types of request.
 */
public enum RequestType {

    /**
     * An action request.
     *
     * @see ActionRequest
     */
    ACTION,

    /**
     * A create request.
     *
     * @see CreateRequest
     */
    CREATE,

    /**
     * A delete request.
     *
     * @see DeleteRequest
     */
    DELETE,

    /**
     * A patch request.
     *
     * @see PatchRequest
     */
    PATCH,

    /**
     * A query request.
     *
     * @see QueryRequest
     */
    QUERY,

    /**
     * A read request.
     *
     * @see ReadRequest
     */
    READ,

    /**
     * An update request.
     *
     * @see UpdateRequest
     */
    UPDATE,

    /**
     * An API Descriptor request.
     *
     * @see org.forgerock.services.descriptor.Describable
     */
    API
}
