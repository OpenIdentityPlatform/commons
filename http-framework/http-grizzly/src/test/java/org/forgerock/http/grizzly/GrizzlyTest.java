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
 * Copyright 2016 ForgeRock AS.
 */
package org.forgerock.http.grizzly;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.forgerock.http.grizzly.GrizzlySupport.newGrizzlyHttpHandler;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.List;

import org.assertj.core.api.SoftAssertionError;
import org.assertj.core.api.SoftAssertions;
import org.forgerock.http.Client;
import org.forgerock.http.Handler;
import org.forgerock.http.HttpApplication;
import org.forgerock.http.HttpApplicationException;
import org.forgerock.http.handler.HttpClientHandler;
import org.forgerock.http.header.CookieHeader;
import org.forgerock.http.header.SetCookieHeader;
import org.forgerock.http.protocol.Cookie;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.http.protocol.Status;
import org.forgerock.http.routing.UriRouterContext;
import org.forgerock.http.session.Session;
import org.forgerock.http.session.SessionContext;
import org.forgerock.services.context.Context;
import org.forgerock.util.promise.NeverThrowsException;
import org.forgerock.util.promise.Promise;
import org.glassfish.grizzly.PortRange;
import org.glassfish.grizzly.http.server.HttpServer;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class GrizzlyTest {

    private HttpServer server;

    @BeforeMethod
    public void setUp() {
        server = HttpServer.createSimpleServer(null, new PortRange(6000, 7000));
    }

    @AfterMethod
    public void tearDown() {
        server.shutdownNow();
    }

    @Test
    public void testHttpApplicationLifecycle() throws IOException, HttpApplicationException {
        final HttpApplication application = mock(HttpApplication.class);
        server.getServerConfiguration().addHttpHandler(newGrizzlyHttpHandler(application));

        server.start();
        verify(application).start();

        server.shutdownNow();
        verify(application).stop();
    }

    @Test
    public void testAnswerWith500IfHttpApplicationFailedToStart() throws Exception {
        final HttpApplication application = mock(HttpApplication.class);
        server.getServerConfiguration().addHttpHandler(newGrizzlyHttpHandler(application));

        when(application.start()).thenThrow(new HttpApplicationException("Unable to start the HttpApplication"));
        server.start();

        try (final HttpClientHandler handler = new HttpClientHandler()) {
            final Client client = new Client(handler);
            final Request request = new Request()
                    .setMethod("GET")
                    .setUri(format("http://localhost:%d/test", server.getListeners().iterator().next().getPort()));
            final Response response = client.send(request).get();
            assertThat(response.getStatus()).isEqualTo(Status.INTERNAL_SERVER_ERROR);
        }
    }

    @Test
    public void testRequest() throws Exception {
        server.getServerConfiguration().addHttpHandler(newGrizzlyHttpHandler(new TestHandler(), null));
        server.start();

        try (final HttpClientHandler handler = new HttpClientHandler()) {
            final Client client = new Client(handler);
            final Request request = new Request()
                .setMethod("POST")
                .setUri(format("http://localhost:%d/test", server.getListeners().iterator().next().getPort()));
            request.getHeaders().add("X-WhateverHeader", "Whatever Value");
            request.getEntity().setString("Hello");

            final Response response = client.send(request).get();
            assertThat(response.getEntity().toString()).isEqualTo("HELLO");
            assertThat(response.getHeaders().get("X-WhateverHeader").getFirstValue()).isEqualTo("Whatever Value");
        }
    }

    @Test
    public void testSession() throws Exception {
        server.getServerConfiguration().addHttpHandler(newGrizzlyHttpHandler(new TestSessionHandler(), null));
        server.start();

        try (final HttpClientHandler handler = new HttpClientHandler()) {
            final Client client = new Client(handler);
            final Request populate = new Request()
                .setMethod("POST")
                .setUri(format("http://localhost:%d/populate", server.getListeners().iterator().next().getPort()));

            Response response = client.send(populate).get();
            assertThat(response.getStatus()).isEqualTo(Status.OK);
            final List<Cookie> sessionCookie = response.getHeaders().get(SetCookieHeader.class).getCookies();

            final Request check = new Request()
                    .setMethod("POST")
                    .setUri(format("http://localhost:%d/check", server.getListeners().iterator().next().getPort()));
            check.getHeaders().put(new CookieHeader(sessionCookie));

            response = client.send(check).get();
            assertThat(response.getEntity().toString()).isEqualTo("OK");
        }
    }

    private final class TestHandler implements Handler {
        @Override
        public Promise<Response, NeverThrowsException> handle(Context context, Request request) {
            final int httpServerPort = server.getListeners().iterator().next().getPort();
            final SoftAssertions softly = new SoftAssertions();
            try {
                softly.assertThat(request.getMethod()).isEqualTo("POST");
                softly.assertThat(request.getUri().getPath()).isEqualTo("/test");
                softly.assertThat(request.getEntity().toString()).isEqualTo("Hello");
                softly.assertThat(request.getHeaders().get("X-WhateverHeader").getFirstValue())
                    .isEqualTo("Whatever Value");
                softly.assertThat(context.asContext(UriRouterContext.class)).isNotNull();
                softly.assertThat(context.asContext(UriRouterContext.class).getMatchedUri()).isEmpty();
                softly.assertThat(context.asContext(UriRouterContext.class).getOriginalUri().toString())
                    .isEqualTo(format("http://localhost:%d/test", httpServerPort));
                softly.assertThat(context.asContext(SessionContext.class)).isNotNull();
                softly.assertThat(context.asContext(SessionContext.class).getSession()).isNotNull();
                softly.assertThat(context.asContext(org.forgerock.services.context.ClientContext.class)).isNotNull();
                softly.assertThat(context.asContext(org.forgerock.services.context.ClientContext.class).getLocalPort())
                    .isEqualTo(httpServerPort);
                softly.assertAll();

                final Response response = new Response(Status.OK);
                response.getHeaders().addAll(request.getHeaders().asMapOfHeaders());
                response.setEntity(request.getEntity().toString().toUpperCase());
                return Response.newResponsePromise(response);
            } catch (SoftAssertionError e) {
                return Response
                        .newResponsePromise(new Response(Status.INTERNAL_SERVER_ERROR)
                                .setEntity(e.getMessage()).setCause(new Exception(e)));
            }
        }
    }

    private final class TestSessionHandler implements Handler {
        @Override
        public Promise<Response, NeverThrowsException> handle(Context context, Request request) {
            final Session session = context.asContext(SessionContext.class).getSession();
            try {
                if (request.getUri().toASCIIString().endsWith("/populate")) {
                    assertThat(session.isEmpty()).isTrue();
                    assertThat(session.size()).isEqualTo(0);
                    assertThat(session.containsKey("sessionKey")).isFalse();
                    assertThat(session.containsValue("sessionValue")).isFalse();
                    assertThat(session.put("sessionKey", "sessionValue")).isNull();
                } else if (request.getUri().toASCIIString().endsWith("/check")) {
                    assertThat(session.get("sessionKey")).isEqualTo("sessionValue");
                    assertThat(session.isEmpty()).isFalse();
                    assertThat(session.size()).isEqualTo(1);
                    assertThat(session.containsKey("sessionKey")).isTrue();
                    assertThat(session.containsValue("sessionValue")).isTrue();
                } else {
                    fail("Unsupported URI: " + request.getUri().toString());
                }

                final Response response = new Response(Status.OK);
                response.setEntity("OK");
                return Response.newResponsePromise(response);
            } catch (AssertionError e) {
                return Response
                        .newResponsePromise(new Response(Status.INTERNAL_SERVER_ERROR)
                                .setEntity(e.getMessage()).setCause(new Exception(e)));
            }
        }
    }
}
