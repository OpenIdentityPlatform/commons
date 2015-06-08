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
 * Copyright 2013-2015 ForgeRock AS.
 */

package org.forgerock.json.resource.http;

import static org.forgerock.http.test.HttpTest.newRequest;
import static org.forgerock.json.fluent.JsonValue.*;
import static org.forgerock.util.promise.Promises.newExceptionPromise;
import static org.forgerock.util.promise.Promises.newResultPromise;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;
import static org.testng.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.forgerock.http.Context;
import org.forgerock.http.protocol.Response;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.Connection;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResult;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.util.promise.Promise;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.testng.annotations.Test;

@SuppressWarnings("javadoc")
public class RequestRunnerTest {

    private static final ResourceException EXCEPTION = ResourceException.getException(ResourceException.NOT_FOUND);
    private static final Promise<QueryResult, ResourceException> QUERY_RESULT = newResultPromise(new QueryResult());
    private static final Promise<QueryResult, ResourceException> RESOURCE_EXCEPTION = newExceptionPromise(EXCEPTION);

    @Test
    public void testHandleResultAnonymousQueryResourceHandlerInVisitQueryAsync() throws Exception {
        Response response = getAnonymousQueryResourceHandler(QUERY_RESULT);
        assertEquals(getResponseContent(response), "{" + "\"result\":[],"
                + "\"resultCount\":0,\"pagedResultsCookie\":null,\"remainingPagedResults\":-1"
                + "}");
    }

    @Test
    public void testHandleResourceAnonymousQueryResourceHandlerInVisitQueryAsync() throws Exception {
        Response response = getAnonymousQueryResourceHandler(QUERY_RESULT,
                new Resource("id", "revision", new JsonValue("jsonValue")));
        assertEquals(getResponseContent(response), "{" + "\"result\":[\"jsonValue\"],"
                + "\"resultCount\":1,\"pagedResultsCookie\":null,\"remainingPagedResults\":-1"
                + "}");
    }

    @Test
    public void testHandleResourceTwoAnonymousQueryResourceHandlerInVisitQueryAsync()
            throws Exception {
        Response response = getAnonymousQueryResourceHandler(QUERY_RESULT,
                new Resource("id", "revision",
                        json(object(field("intField", 42), field("stringField", "stringValue")))),
                new Resource("id", "revision",
                        json(object(field("intField", 43), field("stringField", "otherString")))));
        assertEquals(getResponseContent(response), "{" + "\"result\":["
                + "{\"intField\":42,\"stringField\":\"stringValue\"},"
                + "{\"intField\":43,\"stringField\":\"otherString\"}" + "],"
                + "\"resultCount\":2,\"pagedResultsCookie\":null,\"remainingPagedResults\":-1"
                + "}");
    }

    @Test
    public void testHandleErrorAnonymousQueryResourceHandlerInVisitQueryAsync() throws Exception {
        Response response = getAnonymousQueryResourceHandler(RESOURCE_EXCEPTION);
        assertEquals(getResponseContent(response), "{\"code\":404,\"reason\":\"Not Found\",\"message\":\"Not Found\"}");
    }

    @Test
    public void testHandleResourceThenErrorAnonymousQueryResourceHandlerInVisitQueryAsync()
            throws Exception {
        Response response = getAnonymousQueryResourceHandler(RESOURCE_EXCEPTION,
                new Resource("id", "revision",
                        json(object(field("intField", 42), field("stringField", "stringValue")))));
        assertEquals(getResponseContent(response), "{\"code\":404,\"reason\":\"Not Found\",\"message\":\"Not Found\"}");
    }

    private String getResponseContent(Response response) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        response.getEntity().copyDecodedContentTo(outputStream);
        return new String(outputStream.toByteArray());
    }

    private Response getAnonymousQueryResourceHandler(final Promise<QueryResult, ResourceException> queryPromise,
            final Resource... resources) throws Exception {
        // mock everything
        Context context = mock(Context.class);
        QueryRequest request = Requests.newQueryRequest("");
        Response httpResponse = new Response();
        org.forgerock.http.protocol.Request httpRequest = newRequest();
        Connection connection = mock(Connection.class);

        // set the expectations
        when(connection.queryAsync(eq(context), eq(request), any(QueryResourceHandler.class)))
                .thenAnswer(new Answer<Promise<QueryResult, ResourceException>>() {
                    @Override
                    public Promise<QueryResult, ResourceException> answer(InvocationOnMock invocationOnMock) {
                        QueryResourceHandler handler = (QueryResourceHandler) invocationOnMock.getArguments()[2];
                        for (Resource resource : resources) {
                            handler.handleResource(resource);
                        }
                        return queryPromise;
                    }
                });

        // run the code to access the anonymous class
        RequestRunner requestRunner = new RequestRunner(context, request, httpRequest, httpResponse);
        return requestRunner.handleResult(connection).getOrThrowUninterruptibly();
    }
}
