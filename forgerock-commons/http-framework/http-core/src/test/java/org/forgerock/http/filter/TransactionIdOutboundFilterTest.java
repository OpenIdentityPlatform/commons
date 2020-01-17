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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.forgerock.http.Handler;
import org.forgerock.http.header.TransactionIdHeader;
import org.forgerock.http.protocol.Request;
import org.forgerock.services.TransactionId;
import org.forgerock.services.context.Context;
import org.forgerock.services.context.RootContext;
import org.forgerock.services.context.TransactionIdContext;
import org.testng.annotations.Test;

public class TransactionIdOutboundFilterTest {

    @Test
    public void shouldNotModifyRequestHeadersWhenNoTransactionIdContext() {
        TransactionIdOutboundFilter filter = new TransactionIdOutboundFilter();

        Handler handler = mock(Handler.class);
        final Request request = new Request();
        final Context context = new RootContext();

        filter.filter(context, request, handler);

        verify(handler).handle(same(context), same(request));
        assertThat(request.getHeaders()).isEmpty();
    }

    @Test
    public void shouldAddRequestHeaderWhenTransactionIdContext() {
        TransactionIdOutboundFilter filter = new TransactionIdOutboundFilter();

        Handler handler = mock(Handler.class);
        Request request = new Request();
        TransactionIdContext context = new TransactionIdContext(new RootContext(), new TransactionId("txId"));

        filter.filter(context, request, handler);

        verify(handler).handle(same(context), same(request));
        assertThat(request.getHeaders().getFirst(TransactionIdHeader.class)).isEqualTo("txId/0");
    }

    @Test
    public void shouldReplaceRequestHeaderWhenTransactionIdContext() throws Exception {
        TransactionIdOutboundFilter filter = new TransactionIdOutboundFilter();

        Handler handler = mock(Handler.class);
        Request request = new Request();
        request.getHeaders().put(new TransactionIdHeader("foo-bar-quix"));
        TransactionIdContext context = new TransactionIdContext(new RootContext(), new TransactionId("txId"));

        filter.filter(context, request, handler);

        verify(handler).handle(same(context), same(request));
        final TransactionIdHeader header = request.getHeaders().get(TransactionIdHeader.class);
        assertThat(header.getValues()).hasSize(1).containsExactly("txId/0");
    }

}
