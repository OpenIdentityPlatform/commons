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

package org.forgerock.http.filter;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

import org.forgerock.http.Context;
import org.forgerock.http.Filter;
import org.forgerock.http.Handler;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.util.promise.NeverThrowsException;
import org.forgerock.util.promise.Promise;
import org.mockito.InOrder;
import org.testng.annotations.Test;

public class FiltersTest {

    @Test
    public void shouldCreateChainOfFilters() {

        //Given
        Filter filterOne = mockFilter();
        Filter filterTwo = mockFilter();
        Filter filterThree = mockFilter();

        //When
        Filter filter = Filters.chainOf(filterOne, filterTwo, filterThree);

        //Then
        Context context = mock(Context.class);
        Request request = new Request();
        Handler handler = mock(Handler.class);
        filter.filter(context, request, handler);

        InOrder inOrder = inOrder(filterOne, filterTwo, filterThree, handler);
        inOrder.verify(filterOne).filter(any(Context.class), any(Request.class), any(Handler.class));
        inOrder.verify(filterTwo).filter(any(Context.class), any(Request.class), any(Handler.class));
        inOrder.verify(filterThree).filter(any(Context.class), any(Request.class), any(Handler.class));
        inOrder.verify(handler).handle(any(Context.class), any(Request.class));
    }

    private Filter mockFilter() {
        return spy(new Filter() {
            @Override
            public Promise<Response, NeverThrowsException> filter(Context context, Request request, Handler next) {
                return next.handle(context, request);
            }
        });
    }
}
