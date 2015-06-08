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

package org.forgerock.json.resource.jcite;

import static org.forgerock.util.promise.Promises.newExceptionPromise;

import org.forgerock.http.ServerContext;
import org.forgerock.json.resource.Filter;
import org.forgerock.json.resource.ForbiddenException;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Request;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.util.AsyncFunction;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.promise.Promises;

public abstract class AuthzFilter implements Filter {

    public Promise<Resource, ResourceException> filterRead(final ServerContext context,
            final ReadRequest request, final RequestHandler next) {
        /*
         * Only forward the request if the request is allowed.
         */
        if (isAuthorized(context, request)) {
            /*
             * Continue processing the request since it is allowed. Chain the
             * promise so that we can filter the returned resource.
             */
            return next.handleRead(context, request)
                    .thenAsync(new AsyncFunction<Resource, Resource, ResourceException>() {
                        @Override
                        public Promise<Resource, ResourceException> apply(Resource result) {
                            /*
                             * Filter the resource and its attributes.
                             */
                            if (isAuthorized(context, result)) {
                                return Promises.newResultPromise(filterResource(context, result));
                            } else {
                                return newExceptionPromise(ResourceException.newNotFoundException());
                            }
                        }
                    }, new AsyncFunction<ResourceException, Resource, ResourceException>() {
                        @Override
                        public Promise<Resource, ResourceException> apply(ResourceException error) {
                            // Forward - assumes no authorization is required.
                            return newExceptionPromise(error);
                        }
                    });
        } else {
            /*
             * Stop processing the request since it is not allowed.
             */
            ResourceException exception = new ForbiddenException();
            return newExceptionPromise(exception);
        }
    }

    // Remaining filterXXX methods...

    abstract boolean isAuthorized(ServerContext context, Request request);

    abstract boolean isAuthorized(ServerContext context, Resource request);

    abstract Resource filterResource(ServerContext context, Resource resource);
}
