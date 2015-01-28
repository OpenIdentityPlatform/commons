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
 * Copyright 2013-2014 ForgeRock AS.
 */

package org.forgerock.json.resource.http;

import static org.forgerock.http.test.HttpTest.newRequest;
import static org.forgerock.json.fluent.JsonValue.*;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;
import static org.testng.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.forgerock.http.Response;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.Connection;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResult;
import org.forgerock.json.resource.QueryResultHandler;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.resource.core.Context;
import org.forgerock.util.promise.Promises;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.testng.annotations.Test;

@SuppressWarnings("javadoc")
public class RequestRunnerTest {

    private static final ResourceException EXCEPTION = ResourceException
            .getException(ResourceException.NOT_FOUND);

    @Test
    public void testHandleResultAnonymousQueryResultHandlerInVisitQueryAsync() throws Exception {
        Response response = new Response();
        QueryResultHandler resultHandler = getAnonymousQueryResultHandler(response);
        resultHandler.handleResult(new QueryResult());
        assertEquals(getResponseContent(response), "{" + "\"result\":[],"
                + "\"resultCount\":0,\"pagedResultsCookie\":null,\"remainingPagedResults\":-1"
                + "}");
    }

    @Test
    public void testHandleResourceAnonymousQueryResultHandlerInVisitQueryAsync() throws Exception {
        Response response = new Response();
        QueryResultHandler resultHandler = getAnonymousQueryResultHandler(response);
        resultHandler.handleResource(new Resource("id", "revision", new JsonValue("jsonValue")));
        resultHandler.handleResult(new QueryResult());
        assertEquals(getResponseContent(response), "{" + "\"result\":[\"jsonValue\"],"
                + "\"resultCount\":1,\"pagedResultsCookie\":null,\"remainingPagedResults\":-1"
                + "}");
    }

    @Test
    public void testHandleResourceTwoAnonymousQueryResultHandlerInVisitQueryAsync()
            throws Exception {
        Response response = new Response();
        QueryResultHandler resultHandler = getAnonymousQueryResultHandler(response);
        resultHandler.handleResource(new Resource("id", "revision", json(object(field("intField",
                42), field("stringField", "stringValue")))));
        resultHandler.handleResource(new Resource("id", "revision", json(object(field("intField",
                43), field("stringField", "otherString")))));
        resultHandler.handleResult(new QueryResult());
        assertEquals(getResponseContent(response), "{" + "\"result\":["
                + "{\"intField\":42,\"stringField\":\"stringValue\"},"
                + "{\"intField\":43,\"stringField\":\"otherString\"}" + "],"
                + "\"resultCount\":2,\"pagedResultsCookie\":null,\"remainingPagedResults\":-1"
                + "}");
    }

    @Test
    public void testHandleErrorAnonymousQueryResultHandlerInVisitQueryAsync() throws Exception {
        Response response = new Response();
        QueryResultHandler resultHandler = getAnonymousQueryResultHandler(response);
        resultHandler.handleError(EXCEPTION);
        assertEquals(getResponseContent(response), "");
    }

    @Test
    public void testHandleResourceThenErrorAnonymousQueryResultHandlerInVisitQueryAsync()
            throws Exception {
        Response response = new Response();
        QueryResultHandler resultHandler = getAnonymousQueryResultHandler(response);
        resultHandler.handleResource(new Resource("id", "revision", json(object(field("intField",
                42), field("stringField", "stringValue")))));
        resultHandler.handleError(EXCEPTION);
        assertEquals(getResponseContent(response), "{" + "\"result\":["
                + "{\"intField\":42,\"stringField\":\"stringValue\"}" + "]," + "\"resultCount\":1,"
                + "\"error\":{\"code\":404,\"reason\":\"Not Found\",\"message\":\"Not Found\"}"
                + "}");
    }

    private String getResponseContent(Response response) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        response.getEntity().copyDecodedContentTo(outputStream);
        return new String(outputStream.toByteArray());
    }

    private QueryResultHandler getAnonymousQueryResultHandler(Response httpResponse) throws Exception {
        // mock everything
        Context context = mock(Context.class);
        QueryRequest request = Requests.newQueryRequest("");
        org.forgerock.http.Request httpRequest = newRequest();
        Connection connection = mock(Connection.class);

        // set the expectations
        when(connection.queryAsync(eq(context), eq(request), Matchers.<QueryResultHandler>anyObject()))
                .thenReturn(Promises.<QueryResult, ResourceException>newSuccessfulPromise(null));

        // run the code to access the anonymous class
        RequestRunner requestRunner = new RequestRunner(context, request, httpRequest, httpResponse);
        requestRunner.handleResult(connection);

        // Retrieve the anonymous class (phewww!)
        ArgumentCaptor<QueryResultHandler> arg = ArgumentCaptor.forClass(QueryResultHandler.class);
        verify(connection).queryAsync(eq(context), eq(request), arg.capture());
        return arg.getValue();
    }
}
