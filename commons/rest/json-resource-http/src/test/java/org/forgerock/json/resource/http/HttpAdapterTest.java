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
 * Portions Copyright 2026 3A Systems, LLC.
 */

package org.forgerock.json.resource.http;

import static org.forgerock.api.models.ApiDescription.*;
import static org.forgerock.json.resource.Applications.simpleCrestApplication;
import static org.forgerock.json.resource.Responses.newResourceResponse;
import static org.forgerock.util.promise.Promises.*;
import static org.forgerock.util.test.assertj.AssertJPromiseAssert.assertThat;
import static org.forgerock.json.test.assertj.AssertJJsonValueAssert.assertThat;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.any;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

import java.util.Collections;
import java.util.List;

import org.forgerock.api.models.ApiDescription;
import org.forgerock.http.header.ContentTypeHeader;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.http.protocol.Status;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.AdviceContext;
import org.forgerock.json.resource.Connection;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.services.context.AttributesContext;
import org.forgerock.services.context.Context;
import org.forgerock.services.context.RootContext;
import org.forgerock.services.descriptor.Describable;
import org.forgerock.util.promise.NeverThrowsException;
import org.forgerock.util.promise.Promise;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class HttpAdapterTest {

    @Mock
    private DescribableConnection connection;
    private HttpAdapter adapter;

    @BeforeMethod
    public void setup() {
        MockitoAnnotations.initMocks(this);
        adapter = new HttpAdapter(simpleCrestApplication(new ConnectionFactory() {
            @Override
            public void close() {

            }

            @Override
            public Connection getConnection() throws ResourceException {
                return connection;
            }

            @Override
            public Promise<Connection, ResourceException> getConnectionAsync() {
                return newResultPromise((Connection) connection);
            }
        }, "frapi:test", "1.0"), null);
    }

    @Test
    public void testHandleApiRequest() throws Exception {
        // Given
        given(connection.handleApiRequest(any(Context.class), any(org.forgerock.json.resource.Request.class)))
                .willReturn(apiDescription().id("test:descriptor").version("1.0").build());
        Request request = new Request().setMethod("GET").setUri("/test?_crestapi");
        AttributesContext context = new AttributesContext(new RootContext());

        // When
        Promise<Response, NeverThrowsException> result = adapter.handle(context, request);

        // Then
        assertThat(result).succeeded();
        Object json = result.get().getEntity().getJson();
        assertThat(JsonValue.json(json)).isObject().stringAt("id").isEqualTo("test:descriptor");
    }

    @Test
    public void testReadBinaryFieldAsDownload() throws Exception {
        JsonValue content = JsonValue.json(Collections.singletonMap("file", "AQID"));
        given(connection.readAsync(any(Context.class), any(ReadRequest.class)))
                .willReturn(newResultPromise(newResourceResponse("test", "1", content)));
        Request request = new Request().setMethod("GET")
                .setUri("/test?_fields=file&_mimeType=application/example");
        AttributesContext context = new AttributesContext(new RootContext());

        Response response = adapter.handle(context, request).getOrThrowUninterruptibly();

        assertEquals(response.getStatus(), Status.OK);
        assertEquals(ContentTypeHeader.valueOf(response).getType(), "application/octet-stream");
        assertEquals(response.getHeaders().getFirst("Content-Disposition"), "attachment; filename=\"download.bin\"");
        assertEquals(response.getHeaders().getFirst("Content-Security-Policy"), "sandbox");
        assertEquals(response.getEntity().getBytes(), new byte[] { 1, 2, 3 });
    }

    @Test
    public void testReadAddsSandboxAlongsideResourceContentSecurityPolicy() throws Exception {
        JsonValue content = JsonValue.json(Collections.singletonMap("file", "example"));
        given(connection.readAsync(any(Context.class), any(ReadRequest.class))).willAnswer(invocation -> {
            Context readContext = invocation.getArgument(0);
            readContext.asContext(AdviceContext.class).putAdvice("Content-Security-Policy", "default-src 'none'");
            return newResultPromise(newResourceResponse("test", "1", content));
        });
        Request request = new Request().setMethod("GET")
                .setUri("/test?_fields=file&_mimeType=text/plain");
        AttributesContext context = new AttributesContext(new RootContext());

        Response response = adapter.handle(context, request).getOrThrowUninterruptibly();

        assertEquals(response.getStatus(), Status.OK);
        assertEquals(response.getHeaders().get("Content-Security-Policy").getValues(),
                List.of("default-src 'none'", "sandbox"));
    }

    @Test
    public void testReadWithMalformedMimeTypeReturnsJsonBadRequest() throws Exception {
        Request request = new Request().setMethod("GET")
                .setUri("/test?_fields=file&_mimeType=invalid");
        AttributesContext context = new AttributesContext(new RootContext());

        Response response = adapter.handle(context, request).getOrThrowUninterruptibly();

        assertEquals(response.getStatus(), Status.BAD_REQUEST);
        assertEquals(ContentTypeHeader.valueOf(response).getType(), "application/json");
        assertEquals(response.getHeaders().getFirst("X-Content-Type-Options"), "nosniff");
        assertEquals(response.getHeaders().getFirst("Content-Security-Policy"), "sandbox");
        assertNull(response.getHeaders().getFirst("Content-Disposition"));
        assertEquals(JsonValue.json(response.getEntity().getJson()).get("code").asInteger(), Integer.valueOf(400));
    }

    private interface DescribableConnection extends Connection,
            Describable<ApiDescription, org.forgerock.json.resource.Request> {
        // for mocking
    }

}