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
 * Copyright 2013 ForgeRock AS.
 */
package org.forgerock.json.resource.servlet;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

import java.util.Collections;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.Connection;
import org.forgerock.json.resource.Context;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResult;
import org.forgerock.json.resource.QueryResultHandler;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResourceException;
import org.mockito.ArgumentCaptor;
import org.testng.annotations.Test;

@SuppressWarnings("javadoc")
public class RequestRunnerTest {

    private static final ResourceException EXCEPTION = ResourceException.getException(ResourceException.NOT_FOUND);

    final class MyPojo {

        private int intValue;
        private String stringValue;

        public MyPojo(int intValue, String stringValue) {
            this.intValue = intValue;
            this.stringValue = stringValue;
        }

        public int getintField() {
            return intValue;
        }

        public String getStringField() {
            return stringValue;
        }
    }

    @Test
    public void testHandleResultAnonymousQueryResultHandlerInVisitQueryAsync() throws Exception {
        StringBuilder output = new StringBuilder();
        QueryResultHandler resultHandler = getAnonymousQueryResultHandler(output);
        resultHandler.handleResult(new QueryResult());
        assertEquals(output.toString(), ""
                + "{"
                + "\"result\":[],"
                + "\"resultCount\":0,\"pagedResultsCookie\":null,\"remainingPagedResults\":-1"
                + "}");
    }

    @Test
    public void testHandleResourceAnonymousQueryResultHandlerInVisitQueryAsync() throws Exception {
        StringBuilder output = new StringBuilder();
        QueryResultHandler resultHandler = getAnonymousQueryResultHandler(output);
        resultHandler.handleResource(new Resource("id", "revision", new JsonValue("jsonValue")));
        resultHandler.handleResult(new QueryResult());
        assertEquals(output.toString(), ""
                + "{"
                + "\"result\":[\"jsonValue\"],"
                + "\"resultCount\":1,\"pagedResultsCookie\":null,\"remainingPagedResults\":-1"
                + "}");
    }

    @Test
    public void testHandleResourceTwoAnonymousQueryResultHandlerInVisitQueryAsync() throws Exception {
        StringBuilder output = new StringBuilder();
        QueryResultHandler resultHandler = getAnonymousQueryResultHandler(output);
        resultHandler.handleResource(new Resource("id", "revision", new JsonValue(new MyPojo(42, "stringValue"))));
        resultHandler.handleResource(new Resource("id", "revision", new JsonValue(new MyPojo(43, "otherString"))));
        resultHandler.handleResult(new QueryResult());
        assertEquals(output.toString(), ""
                + "{"
                + "\"result\":["
                + "{\"intField\":42,\"stringField\":\"stringValue\"},"
                + "{\"intField\":43,\"stringField\":\"otherString\"}"
                + "],"
                + "\"resultCount\":2,\"pagedResultsCookie\":null,\"remainingPagedResults\":-1"
                + "}");
    }

    @Test
    public void testHandleErrorAnonymousQueryResultHandlerInVisitQueryAsync() throws Exception {
        StringBuilder output = new StringBuilder();
        QueryResultHandler resultHandler = getAnonymousQueryResultHandler(output);
        resultHandler.handleError(EXCEPTION);
        assertEquals(output.toString(), "");
    }

    @Test
    public void testHandleResourceThenErrorAnonymousQueryResultHandlerInVisitQueryAsync() throws Exception {
        StringBuilder output = new StringBuilder();
        QueryResultHandler resultHandler = getAnonymousQueryResultHandler(output);
        resultHandler.handleResource(new Resource("id", "revision", new JsonValue(new MyPojo(42, "stringValue"))));
        resultHandler.handleError(EXCEPTION);
        assertEquals(output.toString(), ""
                + "{"
                + "\"result\":["
                + "{\"intField\":42,\"stringField\":\"stringValue\"}"
                + "],"
                + "\"resultCount\":1,"
                + "\"error\":{\"code\":404,\"reason\":\"Not Found\",\"message\":\"Not Found\"}"
                + "}");
    }

    private QueryResultHandler getAnonymousQueryResultHandler(StringBuilder output)
            throws Exception {
        // mock everything
        Context context = null;
        QueryRequest request = Requests.newQueryRequest("");
        HttpServletRequest httpRequest = mock(HttpServletRequest.class);
        HttpServletResponse httpResponse = mock(HttpServletResponse.class);
        ServletSynchronizer sync = mock(ServletSynchronizer.class);
        Connection connection = mock(Connection.class);

        // set the expectations
        when(httpResponse.getOutputStream()).thenReturn(new StringBuilderOutputStream(output));
        when(httpRequest.getParameterMap()).thenReturn(Collections.<String, String[]>emptyMap());

        // run the code to access the anonymous class
        RequestRunner requestRunner = new RequestRunner(context, request, httpRequest, httpResponse, sync);
        requestRunner.handleResult(connection);

        // Retrieve the anonymous class (phewww!)
        ArgumentCaptor<QueryResultHandler> arg = ArgumentCaptor.forClass(QueryResultHandler.class);
        verify(connection).queryAsync(eq(context), eq(request), arg.capture());
        return arg.getValue();
    }
}
