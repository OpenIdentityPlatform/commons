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

package org.forgerock.http.header;

import static org.assertj.core.api.Assertions.assertThat;

import org.forgerock.http.protocol.Request;
import org.testng.annotations.Test;

@SuppressWarnings("javadoc")
public class TransactionIdHeaderTest {

    @Test
    public void shouldCreateTransactionIdFromRequest() throws MalformedHeaderException {
        Request request = new Request();
        request.getHeaders().put(TransactionIdHeader.NAME, "foo-bar-quix");

        TransactionIdHeader header = TransactionIdHeader.valueOf(request);

        assertThat(header.getTransactionId().getValue()).isEqualTo("foo-bar-quix");
    }

    @Test
    public void shouldCreateTransactionIdFromString() throws MalformedHeaderException {
        TransactionIdHeader header = TransactionIdHeader.valueOf("foo-bar-quix");

        assertThat(header.getTransactionId().getValue()).isEqualTo("foo-bar-quix");
    }

    @Test(expectedExceptions = MalformedHeaderException.class)
    public void shouldFailWhenNullValue() throws Exception {
        TransactionIdHeader.valueOf((String) null);
    }

    @Test(expectedExceptions = MalformedHeaderException.class)
    public void shouldFailWhenEmptyValue() throws Exception {
        TransactionIdHeader.valueOf("").getTransactionId();
    }

}