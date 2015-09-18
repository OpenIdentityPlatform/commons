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
 * Copyright 2014-2015 ForgeRock AS.
 */

package org.forgerock.authz.filter.http;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.authz.filter.api.AuthorizationResult.accessDenied;
import static org.forgerock.authz.filter.api.AuthorizationResult.accessPermitted;
import static org.forgerock.json.JsonValue.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

import org.forgerock.authz.filter.api.AuthorizationResult;
import org.forgerock.services.context.Context;
import org.forgerock.http.Handler;
import org.forgerock.services.context.RootContext;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.http.protocol.Status;
import org.forgerock.json.JsonValue;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class ResultHandlerTest {

    private ResultHandler resultHandler;

    private ResponseHandler responseHandler;
    private Context context;
    private Request request;
    private Handler next;

    @BeforeMethod
    public void setUp() {
        responseHandler = mock(ResponseHandler.class);
        context = new RootContext();
        request = new Request();
        next = mock(Handler.class);

        resultHandler = new ResultHandler(responseHandler, context, request, next);
    }

    @Test
    public void shouldCallDoFilter() throws Exception {

        //Given
        AuthorizationResult result = accessPermitted();

        //When
        resultHandler.apply(result);

        //Then
        verify(next).handle(context, request);
    }

    @Test
    public void shouldNotCallDoFilterIfNotAuthorizedAndRespondWithReasonAndDetail() throws Exception {

        //Given
        JsonValue detail = json(object(field("INTERNAL", "VALUE")));
        AuthorizationResult result = accessDenied("REASON", detail);
        JsonValue jsonResponse = json(object());

        given(responseHandler.getJsonForbiddenResponse("REASON", detail)).willReturn(jsonResponse);

        //When
        Response response = resultHandler.apply(result).getOrThrowUninterruptibly();

        //Then
        verifyZeroInteractions(next);
        assertThat(response.getStatus()).isEqualTo(Status.FORBIDDEN);
        assertThat(response.getEntity().getJson()).isEqualTo(jsonResponse.getObject());
    }
}
