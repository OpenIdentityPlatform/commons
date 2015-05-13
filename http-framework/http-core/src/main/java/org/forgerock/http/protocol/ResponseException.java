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
package org.forgerock.http.protocol;

/**
 * An HTTP Framework Exception that can be used by filters/handlers to simplify
 * control-flow inside async call-backs.
 * <p>
 * As a developer, it's still useful to be able to use try-catch blocks, even if the catch block converts
 * the {@link ResponseException} to a {@code Promise<Response, ...>}.
 * <p>
 * Note that this is a convenience class offered by the HTTP Framework, there is no requirement to use it in Filter
 * or Handler implementations:
 * <ul>
 *     <li>Ease control-flow inside async call-backs or in synchronous code</li>
 *     <li>Contains a {@link Response} that may be used to forward an error message without losing detailed
 *     information about the cause of the failure</li>
 * </ul>
 */
public class ResponseException extends Exception {
    private static final long serialVersionUID = 7012424171155584261L;

    private final Response response;

    /**
     * Constructs a ResponseException using the given {@code message}.
     *
     * @param message Error message
     */
    public ResponseException(String message) {
        this(new Response(Status.INTERNAL_SERVER_ERROR).setEntity(message),
             message,
             null);
    }

    /**
     * Constructs a ResponseException using the given {@code message} and parent {@code cause}.
     *
     * @param message Error message
     * @param cause Error cause
     */
    public ResponseException(String message, Throwable cause) {
        this(new Response(Status.INTERNAL_SERVER_ERROR).setEntity(message),
             message,
             cause);
    }

    /**
     * Constructs a ResponseException using the given {@code response}, {@code message} and parent {@code cause}.
     *
     * @param response response object
     * @param message Error message
     * @param cause Error cause
     */
    public ResponseException(Response response, String message, Throwable cause) {
        super(message, cause);
        this.response = response;
    }

    /**
     * Returns the response linked to this exception.
     * It is intended to be used when propagating a specific Response message (constructed at the point where the
     * original error occurred) in try-catch blocks:
     *
     * <pre>
     *     {@code try {
     *         doSomeStuff(request);
     *       } catch (ResponseException e) {
     *         return Promises.newResultPromise(e.getResponse());
     *       }}
     * </pre>
     *
     * <p>
     * It can also be used as an informal pointer to the message that caused the exception (Client API usage)
     *
     * @return the response linked to this exception.
     */
    public Response getResponse() {
        return response;
    }
}
