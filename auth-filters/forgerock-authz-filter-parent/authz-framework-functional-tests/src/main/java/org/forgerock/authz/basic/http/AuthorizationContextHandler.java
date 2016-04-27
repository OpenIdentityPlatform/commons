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
 * Copyright 2014 ForgeRock, AS.
 */

package org.forgerock.authz.basic.http;

import static org.forgerock.util.promise.Promises.newResultPromise;

import org.forgerock.authz.filter.api.AuthorizationContext;
import org.forgerock.authz.filter.http.api.HttpAuthorizationContext;
import org.forgerock.services.context.Context;
import org.forgerock.http.Handler;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.http.protocol.Status;
import org.forgerock.util.promise.NeverThrowsException;
import org.forgerock.util.promise.Promise;

/**
 * A simple CHF Handler that can be used to test authorization filters. The
 * handler returns any authorization context attributes that have been set on
 * the session as a JSON object.
 *
 * @since 1.4.0
 */
public class AuthorizationContextHandler implements Handler {

    @Override
    public Promise<Response, NeverThrowsException> handle(Context context, Request request) {
        AuthorizationContext authzContext = HttpAuthorizationContext.forRequest(context);
        return newResultPromise(new Response(Status.OK).setEntity(authzContext.getAttributes()));
    }
}
