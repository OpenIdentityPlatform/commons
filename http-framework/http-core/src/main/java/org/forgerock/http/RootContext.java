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
 * Copyright 2012-2015 ForgeRock AS.
 */

package org.forgerock.http;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A {@link Context} which has an a globally unique ID but no parent. All request context
 * chains are terminated by a {@link RootContext} as the top-most context.
 *
 * We're assuming here that we only have requirement for IDs to be globally unique and non-repeating but not "secure"
 * (unguessable).
 */
public final class RootContext extends AbstractContext {

    /**
     * This ensures a globally unique key (even in a clustered environment).
     * The UUID is a JVM-wide prefix for the context ID, suffixed with an {@link AtomicLong}.
     */
    private static final String BASE = UUID.randomUUID().toString();
    private static final AtomicLong SEQUENCE = new AtomicLong();

    /**
     * Returns a new globally unique identifier.
     */
    private static String getNextID() {
        return BASE + SEQUENCE.getAndIncrement();
    }

    /**
     * Construct a new {@link RootContext} with a generated (unique) identifier.
     */
    public RootContext() {
        this(getNextID());
    }

    /**
     * Construct a new {@link RootContext} with the given {@code id} (uniqueness is not verified).
     * @param id context identifier (uniqueness is not verified)
     */
    public RootContext(String id) {
        // No parent
        super(id, "root", null);
    }
}
