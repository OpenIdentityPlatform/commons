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

package org.forgerock.http.bindings;

import static io.swagger.models.Scheme.HTTP;
import static io.swagger.models.Scheme.HTTPS;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.forgerock.http.Applications.describedHttpApplication;
import static org.forgerock.http.Applications.simpleHttpApplication;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.test.assertj.AssertJJsonValueAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.assertj.core.api.SoftAssertionError;
import org.assertj.core.api.SoftAssertions;
import org.forgerock.http.ApiProducer;
import org.forgerock.http.Client;
import org.forgerock.http.DescribedHttpApplication;
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
import org.forgerock.http.swagger.SwaggerApiProducer;
import org.forgerock.services.context.ClientContext;
import org.forgerock.services.context.Context;
import org.forgerock.services.descriptor.Describable;
import org.forgerock.util.promise.NeverThrowsException;
import org.forgerock.util.promise.Promise;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import io.swagger.models.Info;
import io.swagger.models.Operation;
import io.swagger.models.Path;
import io.swagger.models.Swagger;

/**
 * A test class for CHF bindings.
 */
public abstract class BindingTest {

    private int port;

    /**
     * Create a server to bind a CHF application to in tests.
     */
    protected abstract void createServer();

    /**
     * Stop the server.
     * @throws Exception In case of error.
     */
    protected abstract void stopServer() throws Exception;

    /**
     * Start the server.
     * @throws Exception In case of error.
     * @return The port number the server is listening on.
     */
    protected abstract int startServer() throws Exception;

    /**
     * Add an application to the server. The application should be added to the root path.
     * @param application The application.
     * @throws Exception In case of failure.
     */
    protected abstract void addApplication(HttpApplication application) throws Exception;

    /**
     * Set up for tests.
     * @throws Exception In case of failure.
     */
    @BeforeMethod
    public final void setUp() throws Exception {
        createServer();
    }

    /**
     * Tear down after tests.
     * @throws Exception In case of failure.
     */
    @AfterMethod
    public final void tearDown() throws Exception {
        stopServer();
        port = 0;
    }

    /**
     * Test the application lifecycle for a described application.
     * @throws Exception In case of failure.
     */
    @Test
    public void testDescribedHttpApplicationLifecycle() throws Exception {
        final DescribedHttpApplication application = mock(DescribedHttpApplication.class);
        when(application.start()).thenReturn(mock(DescribableHandler.class));
        addApplication(application);
        port = startServer();
        verify(application).getBufferFactory();
        verify(application).start();
        verify(application).getApiProducer();
        verifyNoMoreInteractions(application);

        stopServer();
        verify(application).stop();
        verifyNoMoreInteractions(application);
    }

    /**
     * Test the application lifecycle.
     * @throws Exception In case of failure.
     */
    @Test
    public void testHttpApplicationLifecycle() throws Exception {
        final HttpApplication application = mock(HttpApplication.class);
        addApplication(application);
        port = startServer();
        verify(application).getBufferFactory();
        verify(application).start();
        verifyNoMoreInteractions(application);

        stopServer();
        verify(application).stop();
        verifyNoMoreInteractions(application);
    }

    /**
     * Test 500 errors are returned if the application doesn't start correctly.
     * @throws Exception In case of failure.
     */
    @Test
    public void testAnswerWith500IfHttpApplicationFailedToStart() throws Exception {
        final HttpApplication application = mock(HttpApplication.class);
        addApplication(application);

        when(application.start()).thenThrow(new HttpApplicationException("Unable to start the HttpApplication"));
        port = startServer();

        try (final HttpClientHandler handler = new HttpClientHandler()) {
            final Client client = new Client(handler);
            final Request request = new Request()
                    .setMethod("GET")
                    .setUri(format("http://localhost:%d/test", port));
            final Response response = client.send(request).get();
            assertThat(response.getStatus()).isEqualTo(Status.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Test a request.
     * @throws Exception In case of failure.
     */
    @Test
    public void testRequest() throws Exception {
        HttpApplication application = simpleHttpApplication(new TestHandler(), null);
        addApplication(application);
        port = startServer();

        try (final HttpClientHandler handler = new HttpClientHandler()) {
            final Client client = new Client(handler);
            final Request request = new Request()
                    .setMethod("POST")
                    .setUri(format("http://localhost:%d/test", port));
            request.getHeaders().add("X-WhateverHeader", "Whatever Value");
            request.getEntity().setString("Hello");

            final Response response = client.send(request).get();
            assertThat(response.getEntity().toString()).isEqualTo("HELLO");
            assertThat(response.getHeaders().get("X-WhateverHeader").getFirstValue()).isEqualTo("Whatever Value");
        }
    }

    /**
     * Test an API request.
     * @throws Exception In case of failure.
     */
//    @Test
//    public void testRequestApi() throws Exception {
//        HttpApplication application = describedHttpApplication(new TestHandler(), null,
//                new SwaggerApiProducer(new Info(), "", "", asList(HTTP, HTTPS)));
//        addApplication(application);
//
//        port = startServer();
//
//        try (final HttpClientHandler handler = new HttpClientHandler()) {
//            final Client client = new Client(handler);
//            final Request request = new Request()
//                    .setMethod("GET")
//                    .setUri(format("http://localhost:%d/test?_api", port));
//            request.getHeaders().add("X-WhateverHeader", "Whatever Value");
//
//            final Response response = client.send(request).get();
//            assertThat(json(response.getEntity().getJson())).isObject()
//                    .hasObject("paths")
//                    .hasObject("test")
//                    .hasObject("post")
//                    .hasArray("produces")
//                    .containsExactly("text/plain");
//        }
//    }

    /**
     * Test the session.
     * @throws Exception In case of failure.
     */
    @Test
    public void testSession() throws Exception {
        HttpApplication application = simpleHttpApplication(new TestSessionHandler(), null);
        addApplication(application);
        port = startServer();

        try (final HttpClientHandler handler = new HttpClientHandler()) {
            final Client client = new Client(handler);
            final Request populate = new Request()
                    .setMethod("POST")
                    .setUri(format("http://localhost:%d/populate", port));

            Response response = client.send(populate).get();
            assertThat(response.getStatus()).isEqualTo(Status.OK);
            final List<Cookie> sessionCookie = response.getHeaders().get(SetCookieHeader.class).getCookies();

            final Request check = new Request()
                    .setMethod("POST")
                    .setUri(format("http://localhost:%d/check", port));
            check.getHeaders().put(new CookieHeader(sessionCookie));

            response = client.send(check).get();
            assertThat(response.getEntity().toString()).isEqualTo("OK");
        }
    }

    private final class TestHandler implements Handler, Describable<Swagger, Request> {

        @Override
        public Promise<Response, NeverThrowsException> handle(Context context, Request request) {
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
                        .isEqualTo(format("http://localhost:%d/test", port));
                softly.assertThat(context.asContext(SessionContext.class)).isNotNull();
                softly.assertThat(context.asContext(SessionContext.class).getSession()).isNotNull();
                softly.assertThat(context.asContext(ClientContext.class)).isNotNull();
                softly.assertThat(context.asContext(ClientContext.class).getLocalPort())
                        .isEqualTo(port);
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

        @Override
        public Swagger api(ApiProducer<Swagger> producer) {
            return null;
        }

        @Override
        public Swagger handleApiRequest(Context context, Request request) {
            return new Swagger().path("test", new Path().post(new Operation().produces("text/plain")));
        }

        @Override
        public void addDescriptorListener(Listener listener) {

        }

        @Override
        public void removeDescriptorListener(Listener listener) {

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

    interface DescribableHandler extends Handler, Describable<Swagger, Request> {
        // for mocking
    }
}
