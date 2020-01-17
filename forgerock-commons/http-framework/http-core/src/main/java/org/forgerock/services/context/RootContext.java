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

package org.forgerock.services.context;


import static org.forgerock.util.Reject.checkNotNull;

import org.forgerock.json.JsonValue;
import org.forgerock.util.generator.IdGenerator;

/**
 * A {@link Context} which has an a globally unique ID but no parent. All request context
 * chains are terminated by a {@link RootContext} as the top-most context.
 */
public final class RootContext extends AbstractContext {

    /**
     * Construct a new {@link RootContext} with the default {@code IdGenerator}.
     *
     * @see IdGenerator#DEFAULT
     */
    public RootContext() {
        this(IdGenerator.DEFAULT.generate());
    }

    /**
     * Construct a new {@link RootContext} with the given {@code id} (uniqueness is not verified).
     * @param id context identifier (uniqueness is not verified, cannot be {@code null})
     */
    public RootContext(String id) {
        // No parent
        super(checkNotNull(id, "The identifier can't be null."), "root", null);
    }

    /**
     * Restore from JSON representation.
     *
     * @param savedContext
     *            The JSON representation from which this context's attributes
     *            should be parsed.
     * @param classLoader
     *            The ClassLoader which can properly resolve the persisted class-name.
     */
    public RootContext(final JsonValue savedContext, final ClassLoader classLoader) {
        super(savedContext, classLoader);
    }

}
