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
 * Copyright 2015 ForgeRock AS.
 *
 */

package org.forgerock.http.session;

import org.forgerock.json.JsonValue;
import org.forgerock.services.context.AbstractContext;
import org.forgerock.services.context.Context;
import org.forgerock.util.Reject;

/**
 * A {@code SessionContext} is a mechanism for maintaining state between components when processing a successive
 * requests from the same logical client or end-user. For example, a filter may store information about the end-user
 * in the {@code SessionContext} which can then be accessed in subsequent filters and handlers in order to perform
 * access control decisions, routing decisions, etc.
 * <p>
 * Unlike an {@link org.forgerock.services.context.AttributesContext AttributesContext}, a {@code SessionContext} has
 * a life-cycle that spans successive requests from the same client, although its content may be lost after periods
 * of inactivity. The exact details of how a "session" is associated with a client, how it is persisted between
 * requests, and if and when it is expired are the responsibility of the {@link Session} and
 * {@link SessionManager SessionManager} implementation.
 * <p>
 * Use an {@link org.forgerock.services.context.AttributesContext AttributesContext} for transferring transient
 * state between components when processing a single request.
 */
public final class SessionContext extends AbstractContext {

    /**
     * Session information associated with the remote client. This field is not serialized in the {@link JsonValue}
     * representation of this context.
     */
    private Session session;

    /**
     * Constructs a new {@code SessionContext}.
     *
     * @param parent
     *         The parent {@code Context}.
     * @param session
     *         The HTTP {@code Session}.
     */
    public SessionContext(Context parent, Session session) {
        super(parent, "session");
        Reject.ifNull(session, "Session cannot be null.");
        this.session = session;
    }

    /**
     * Restore from JSON representation.
     *
     * @param savedContext
     *         The JSON representation from which this context's attributes should be parsed.
     * @param classLoader
     *         The ClassLoader which can properly resolve the persisted class-name.
     */
    public SessionContext(final JsonValue savedContext, final ClassLoader classLoader) {
        super(savedContext, classLoader);
    }

    /**
     * Returns the {@code Session} associated with the remote client.
     *
     * @return The {@code Session} associated with the remote client.
     */
    public Session getSession() {
        return session;
    }

    /**
     * Sets the {@code Session} associated with the remote client.
     *
     * @param session
     *         The session.
     * @return This {@code SessionContext}.
     */
    public SessionContext setSession(Session session) {
        this.session = session;
        return this;
    }
}
