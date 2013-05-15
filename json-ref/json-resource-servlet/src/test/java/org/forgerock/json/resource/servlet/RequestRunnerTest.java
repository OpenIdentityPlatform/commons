/*
 * CDDL HEADER START
 *
 * The contents of this file are subject to the terms of the
 * Common Development and Distribution License, Version 1.0 only
 * (the "License").  You may not use this file except in compliance
 * with the License.
 *
 * You can obtain a copy of the license at
 * trunk/opends/resource/legal-notices/OpenDS.LICENSE
 * or https://OpenDS.dev.java.net/OpenDS.LICENSE.
 * See the License for the specific language governing permissions
 * and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL HEADER in each
 * file and include the License file at
 * trunk/opends/resource/legal-notices/OpenDS.LICENSE.  If applicable,
 * add the following below this CDDL HEADER, with the fields enclosed
 * by brackets "[]" replaced with your own identifying information:
 *      Portions Copyright [yyyy] [name of copyright owner]
 *
 * CDDL HEADER END
 *
 *
 *      Copyright 2013 ForgeRock AS
 */
package org.forgerock.json.resource.servlet;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

import java.io.IOException;
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
public class RequestRunnerTest
{

  private static final ResourceException EXCEPTION = ResourceException
      .getException(ResourceException.NOT_FOUND);

  @Test
  public void testHandleResultAnonymousQueryResultHandlerInVisitQueryAsync()
      throws Exception
  {
    StringBuilder output = new StringBuilder();
    QueryResultHandler resultHandler = getAnonymousQueryResultHandler(output);
    resultHandler.handleResult(new QueryResult());
    assertEquals(
        output.toString(),
        "{\"result\":[],\"resultCount\":0,\"pagedResultsCookie\":null,\"remainingPagedResults\":-1}");
  }

  @Test
  public void testHandleResourceAnonymousQueryResultHandlerInVisitQueryAsync()
      throws Exception
  {
    StringBuilder output = new StringBuilder();
    QueryResultHandler resultHandler = getAnonymousQueryResultHandler(output);
    resultHandler.handleResource(new Resource("id", "revision", new JsonValue(
        "jsonValue")));
    resultHandler.handleResult(new QueryResult());
    assertEquals(
        output.toString(),
        "{\"result\":[\"jsonValue\"],\"resultCount\":1,\"pagedResultsCookie\":null,\"remainingPagedResults\":-1}");
  }

  @Test
  public void testHandleErrorAnonymousQueryResultHandlerInVisitQueryAsync()
      throws Exception
  {
    StringBuilder output = new StringBuilder();
    QueryResultHandler resultHandler = getAnonymousQueryResultHandler(output);
    resultHandler.handleError(EXCEPTION);
    assertEquals(output.toString(), "");
  }

  private QueryResultHandler getAnonymousQueryResultHandler(StringBuilder output)
      throws IOException, Exception
  {
    // mock everything
    Context context = null;
    QueryRequest request = Requests.newQueryRequest("");
    HttpServletRequest httpRequest = mock(HttpServletRequest.class);
    HttpServletResponse httpResponse = mock(HttpServletResponse.class);
    ServletSynchronizer sync = mock(ServletSynchronizer.class);
    Connection connection = mock(Connection.class);

    // set the expectations
    when(httpResponse.getOutputStream()).thenReturn(
        new StringBuilderOutputStream(output));
    when(httpRequest.getParameterMap()).thenReturn(Collections.EMPTY_MAP);

    // run the code to access the anonymous class
    RequestRunner requestRunner =
        new RequestRunner(context, request, httpRequest, httpResponse, sync);
    requestRunner.handleResult(connection);

    // Retrive the anonymous class (phewww!)
    ArgumentCaptor<QueryResultHandler> arg =
        ArgumentCaptor.forClass(QueryResultHandler.class);
    verify(connection).queryAsync(eq(context), eq(request), arg.capture());
    return arg.getValue();
  }
}
