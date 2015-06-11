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

package org.forgerock.http;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

import org.forgerock.util.Reject;

/**
 * Represents extrinsic state associated with the processing of a request in a
 * server.
 */
public final class HttpContext extends AbstractContext {

    /*
     * TODO: add connection information such as IP address, SSL parameters, etc.
     */

    /** The principal associated with the request, or {@code null} if unknown. */
    private Principal principal;

    /** Session information associated with the remote client. */
    private Session session;

    private final Map<String, Object> attributes = new HashMap<>();

    /**
     * Constructs a new {@code HttpContext}.
     *
     * @param parent The parent {@code Context}.
     * @param session The HTTP {@code Session}.
     */
    public HttpContext(Context parent, Session session) {
        super(parent, "httpRequest");
        Reject.ifNull(session, "Session cannot be null.");
        this.session = session;
    }

    /**
     * Returns the {@code Principal} of this request.
     *
     * @return The principal.
     */
    public Principal getPrincipal() {
        return principal;
    }

    /**
     * Sets the {@code Principal} of this request.
     *
     * @param principal The principal.
     * @return The {@code HttpContext}.
     */
    public HttpContext setPrincipal(Principal principal) {
        this.principal = principal;
        return this;
    }

    /**
     * Returns the {@code Session} associated with this request.
     *
     * @return The session.
     */
    public Session getSession() {
        return session;
    }

    /**
     * Sets the {@code Session} associated with this request.
     *
     * @param session The session.
     * @return The {@code HttpContext}
     */
    public HttpContext setSession(Session session) {
        this.session = session;
        return this;
    }

    /**
     * Returns the attributes associated with this request.
     *
     * @return The request attributes.
     */
    public Map<String, Object> getAttributes() {
        return attributes;
    }
}
