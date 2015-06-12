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

package org.forgerock.http;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.http.Http.newResponsePromise;
import static org.forgerock.util.Utils.joinAsString;
import static org.mockito.Matchers.notNull;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.http.protocol.Status;
import org.forgerock.util.promise.Promise;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Tests the Client class.
 */
@SuppressWarnings("javadoc")
public final class ClientTest {
    private static final String EOL = System.getProperty("line.separator");

    @Mock
    private Handler handler;

    private Client client;

    @BeforeMethod
    public void beforeMethod() {
        initMocks(this);
        when(handler.handle(notNull(RootContext.class), Matchers.notNull(Request.class)))
                .thenReturn(newResponsePromise(new Response(Status.OK)));
        client = new Client(handler);
    }

    @Test
    public void testSend() throws Exception {
        final Request request = new Request().setUri("http://example.com").setMethod("GET");
        final Response response = client.send(request).get();
        assertThat(response.getStatus()).isEqualTo(Status.OK);
    }

    // Disabled due to incompatibility with JDK7 Rhino.
    @Test(enabled = false)
    public void testSendFromJavaScript() throws Exception {
        final ScriptEngine engine = new ScriptEngineManager().getEngineByExtension("js");
        engine.put("http", client);
        final Object response = engine.eval(script(
                // @formatter:off
                "var Request = Java.type('org.forgerock.http.protocol.Request')",
                "",
                "var request = new Request()",
                "request.uri = 'http://example.com'",
                "request.method = 'GET'",
                "",
                "http.send(request).get()"
                // @formatter:on
        ));
        assertThat(((Response) response).getStatus()).isEqualTo(Status.OK);
    }

    // Disabled due to incompatibility with JDK7 Rhino.
    @Test(enabled = false)
    public void testSendFromJavaScriptAsync() throws Exception {
        final ScriptEngine engine = new ScriptEngineManager().getEngineByExtension("js");
        engine.put("http", client);
        final Object promise = engine.eval(script(
                // @formatter:off
                "var Request = Java.type('org.forgerock.http.protocol.Request')",
                "",
                "var request = new Request()",
                "request.uri = 'http://example.com'",
                "request.method = 'GET'",
                "",
                "http.send(request).then(function(response) { return response.status; })"
                // @formatter:on
        ));
        assertThat(((Promise<?, ?>) promise).get()).isEqualTo(Status.OK);
    }

    private String script(String... lines) {
        return joinAsString(EOL, (Object[]) lines);
    }
}
