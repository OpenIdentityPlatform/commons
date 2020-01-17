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
 */
package org.forgerock.selfservice.core;

import org.forgerock.json.JsonValue;
import org.forgerock.services.context.AbstractContext;
import org.forgerock.services.context.Context;

/**
 * A Context that indicates the request came from Self-Service.
 *
 * @since 0.2.0
 */
public final class SelfServiceContext extends AbstractContext {

    /**
     * Constructs a new SelfServiceContext.
     * @param parent The parent context.
     */
    public SelfServiceContext(Context parent) {
        super(parent, "selfservice");
    }

    /**
     * Constructs a new SelfServiceContext.
     * @param savedContext
     *            The JSON representation from which this context's attributes
     *            should be parsed.
     * @param classLoader
     *            The ClassLoader which can properly resolve the persisted class-name.
     */
    public SelfServiceContext(JsonValue savedContext, ClassLoader classLoader) {
        super(savedContext, classLoader);
    }
}
