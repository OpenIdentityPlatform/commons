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
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.forgerock.http.Handler;
import org.forgerock.http.header.TransactionIdHeader;
import org.forgerock.http.protocol.Headers;
import org.forgerock.http.protocol.Request;
import org.forgerock.services.context.Context;
import org.forgerock.services.context.RootContext;
import org.forgerock.services.TransactionId;
import org.forgerock.services.context.TransactionIdContext;
import org.mockito.ArgumentCaptor;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;
import org.testng.annotations.AfterTest;

import static org.forgerock.http.filter.TransactionIdInboundFilter.SYSPROP_TRUST_TRANSACTION_HEADER;


public class TransactionIdInboundFilterTest {

    Headers headers;

    String previousPropertyValue;

    @BeforeTest
    public void beforeTest() {
        previousPropertyValue = System.setProperty(SYSPROP_TRUST_TRANSACTION_HEADER, Boolean.TRUE.toString());
    }

    @AfterTest
    public void afterTest() {
        if (previousPropertyValue != null) {
            System.setProperty(SYSPROP_TRUST_TRANSACTION_HEADER, previousPropertyValue);
        }
    }

    @BeforeMethod
    public void setup() {
        headers = new Headers();
    }

    @Test
    public void shouldCreateTransactionIdFromTheHeaderValue() {
        headers.put(TransactionIdHeader.NAME, "txId");

        TransactionId txId = TransactionIdInboundFilter.createTransactionId(headers);

        assertThat(txId.getValue()).isEqualTo("txId");
    }

    @Test
    public void shouldCreateTransactionIdWhenTheHeaderValueIsEmpty() {
        headers.put(TransactionIdHeader.NAME, "");

        TransactionId txId = TransactionIdInboundFilter.createTransactionId(headers);

        assertThat(txId.getValue()).isNotNull().isNotEmpty();
    }

    @Test
    public void shouldCreateTransactionIdWhenNoHeader() {
        TransactionId txId = TransactionIdInboundFilter.createTransactionId(headers);

        assertThat(txId.getValue()).isNotNull().isNotEmpty();
    }

    @Test
    public void shouldCreateTransactionIdContext() {
        TransactionIdInboundFilter filter = new TransactionIdInboundFilter();
        final Handler handler = mock(Handler.class);
        final RootContext rootContext = new RootContext();
        Request request = new Request();
        request.getHeaders().put(TransactionIdHeader.NAME, "txId");

        filter.filter(rootContext, request, handler);

        ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
        verify(handler).handle(contextCaptor.capture(), any(Request.class));
        final Context capturedContext = contextCaptor.getValue();
        assertThat(capturedContext.getParent()).isSameAs(rootContext);
        assertThat(((TransactionIdContext) capturedContext).getTransactionId().getValue()).isEqualTo("txId");
    }

}
