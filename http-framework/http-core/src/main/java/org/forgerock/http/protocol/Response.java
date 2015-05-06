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
 * Copyright 2009 Sun Microsystems Inc.
 * Portions Copyright 2010–2011 ApexIdentity Inc.
 * Portions Copyright 2011-2015 ForgeRock AS.
 */

package org.forgerock.http.protocol;


/**
 * A response message.
 */
public final class Response extends MessageImpl<Response> {
    /** The response status. */
    private Status status;

    /**
     * Creates a new response.
     */
    public Response() {
        // Nothing to do.
    }

    /**
     * Creates a new response with a default status.
     *
     * @param status The status to use for the Reponse.
     */
    public Response(Status status) {
        this.status = status;
    }

    /**
     * Returns the response status.
     *
     * @return The response status.
     */
    public Status getStatus() {
        return status;
    }

    @Override
    public Response setEntity(Object o) {
        setEntity0(o);
        return this;
    }

    /**
     * Sets the response status code.
     *
     * @param status
     *            The response status code.
     * @return This response.
     */
    public Response setStatus(final Status status) {
        this.status = status;
        return this;
    }

    @Override
    public Response setVersion(String version) {
        setVersion0(version);
        return this;
    };
}
