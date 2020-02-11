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

package org.forgerock.json.resource.http;

import static org.forgerock.api.models.ApiDescription.*;
import static org.forgerock.json.resource.Applications.simpleCrestApplication;
import static org.forgerock.util.promise.Promises.*;
import static org.forgerock.util.test.assertj.AssertJPromiseAssert.assertThat;
import static org.forgerock.json.test.assertj.AssertJJsonValueAssert.assertThat;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.any;

import org.forgerock.api.models.ApiDescription;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.Connection;
import org.forgerock.json.resource.ConnectionFactory;
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

    private interface DescribableConnection extends Connection,
            Describable<ApiDescription, org.forgerock.json.resource.Request> {
        // for mocking
    }

}