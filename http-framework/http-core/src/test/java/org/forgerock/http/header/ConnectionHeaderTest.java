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
 * information: "Portions Copyright [year] [name of copyright owner]".
 *
 * Copyright 2014-2015 ForgeRock AS.
 */
package org.forgerock.http.header;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.http.header.ConnectionHeader.NAME;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;

import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Unit tests for the connection header class.
 * Header field example :<pre>
 * Connection: close
 * </pre>
 */
@SuppressWarnings("javadoc")
public class ConnectionHeaderTest {

    private static final String ESCAPED_KEEP_ALIVE_VALUE = "\\\"Keep-Alive\\\"";
    private static final String QUOTED_KEEP_ALIVE_VALUE = "'Keep-Alive'";

    @DataProvider
    private Object[][] connectionHeaders() {
        return new Object[][] {
            { "Keep-Alive" },
            { "close" } };
    }

    @Test(dataProvider = "nullOrEmptyDataProvider", dataProviderClass = StaticProvider.class)
    public void testConnectionHeaderAllowsNullOrEmptyString(final String cheader) {
        final ConnectionHeader ch = ConnectionHeader.valueOf(cheader);
        assertEquals(ch.getTokens().size(), 0);
    }

    @Test(dataProvider = "connectionHeaders")
    public void testConnectionHeaderFromMessageResponse(final String cheader) throws Exception {
        final Response response = new Response();
        assertNull(response.getHeaders().get(NAME));
        response.getHeaders().put(NAME, cheader);
        assertNotNull(response.getHeaders().get(NAME));

        final ConnectionHeader ch = ConnectionHeader.valueOf(response);
        assertThat(ch.getName()).isEqualTo(NAME);
        assertEquals(ch.getTokens().size(), 1);
        assertEquals(ch.getTokens().get(0), cheader);
    }

    @Test(dataProvider = "connectionHeaders")
    public void testConnectionHeaderFromMessageRequest(final String cheader) throws Exception {
        final Request request = new Request();
        assertNull(request.getHeaders().get(NAME));
        request.getHeaders().put(NAME, cheader);
        assertNotNull(request.getHeaders().get(NAME));

        final ConnectionHeader ch = ConnectionHeader.valueOf(request);
        assertEquals(ch.getTokens().size(), 1);
        assertEquals(ch.getTokens().get(0), cheader);
    }

    @Test
    public void testConnectionHeaderFromEmptyMessage() {
        final Response response = new Response();
        assertNull(response.getHeaders().get(NAME));

        final ConnectionHeader ch = ConnectionHeader.valueOf(response);
        assertEquals(ch.getTokens().size(), 0);
    }

    @Test(dataProvider = "connectionHeaders")
    public void testConnectionHeaderFromString(final String connectionHeader) throws Exception {
        final ConnectionHeader ch = ConnectionHeader.valueOf(connectionHeader);
        assertEquals(ch.getTokens().size(), 1);
        assertEquals(ch.getTokens().get(0), connectionHeader);
        assertEquals(ch.getName(), NAME);
    }

    @Test
    public void testConnectionHeaderFromEscapedString() throws Exception {
        final ConnectionHeader ch = ConnectionHeader.valueOf(ESCAPED_KEEP_ALIVE_VALUE);
        assertEquals(ch.getTokens().size(), 1);
        assertEquals(ch.getTokens().get(0), ESCAPED_KEEP_ALIVE_VALUE);
        assertEquals(ch.getName(), NAME);
    }

    @Test
    public void testConnectionHeaderFromQuotedString() throws Exception {
        final ConnectionHeader ch = ConnectionHeader.valueOf(QUOTED_KEEP_ALIVE_VALUE);
        assertEquals(ch.getTokens().size(), 1);
        assertEquals(ch.getTokens().get(0), QUOTED_KEEP_ALIVE_VALUE);
        assertEquals(ch.getName(), NAME);
    }

    @Test(dataProvider = "connectionHeaders")
    public void testConnectionHeaderToMessageRequest(final String connectionHeader) {
        final Request request = new Request();
        assertNull(request.getHeaders().get(NAME));
        final ConnectionHeader ch = ConnectionHeader.valueOf(connectionHeader);
        request.getHeaders().put(ch);
        assertNotNull(request.getHeaders().get(NAME));
        assertThat(request.getHeaders().get(NAME).getValues()).containsOnly(connectionHeader);
    }

    @Test(dataProvider = "nullOrEmptyDataProvider", dataProviderClass = StaticProvider.class)
    public void testConnectionHeaderToMessageNullOrEmptyDoesNothing(final String cheader) {
        final Response response = new Response();
        assertNull(response.getHeaders().get(NAME));
        final ConnectionHeader ch = ConnectionHeader.valueOf(cheader);
        response.getHeaders().put(ch);
        assertNull(response.getHeaders().get(NAME));
    }

    @Test(dataProvider = "connectionHeaders")
    public void testConnectionHeaderToMessageResponse(final String connectionHeader) {
        final Response response = new Response();
        assertNull(response.getHeaders().get(NAME));
        final ConnectionHeader ch = ConnectionHeader.valueOf(connectionHeader);
        response.getHeaders().put(ch);
        assertNotNull(response.getHeaders().get(NAME));
        assertThat(response.getHeaders().get(NAME).getValues()).containsOnly(connectionHeader);
    }
}
