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
 * Copyright 2016 ForgeRock AS.
 */

package org.forgerock.services.routing;

import org.forgerock.http.ApiProducer;
import org.forgerock.services.context.Context;

/**
 * A route matcher that delegates to a provided route matcher.
 * @param <R> The type or request being matched.
 */
public class DelegatingRouteMatcher<R> extends RouteMatcher<R> {

    private final RouteMatcher<R> delegate;

    /**
     * Create a new route matcher, delegating to the provided delegate.
     * @param delegate The delegate.
     */
    public DelegatingRouteMatcher(RouteMatcher<R> delegate) {
        this.delegate = delegate;
    }

    @Override
    public RouteMatch evaluate(Context context, R request) {
        return delegate.evaluate(context, request);
    }

    @Override
    public String toString() {
        return delegate.toString();
    }

    @Override
    public String idFragment() {
        return delegate.idFragment();
    }

    @Override
    public int hashCode() {
        return delegate.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        return delegate.equals(o);
    }

    @Override
    public <D> D transformApi(D descriptor, ApiProducer<D> producer) {
        return delegate.transformApi(descriptor, producer);
    }
}
