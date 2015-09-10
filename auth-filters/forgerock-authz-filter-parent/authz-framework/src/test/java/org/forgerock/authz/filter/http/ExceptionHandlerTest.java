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
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import org.forgerock.http.protocol.Response;
import org.forgerock.http.protocol.Status;
import org.forgerock.json.JsonValue;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class ExceptionHandlerTest {

    private ExceptionHandler exceptionHandler;

    private ResponseHandler responseHandler;

    @BeforeMethod
    public void setUp() {
        responseHandler = mock(ResponseHandler.class);

        exceptionHandler = new ExceptionHandler(responseHandler);
    }

    @Test
    public void shouldWriteToResponseWithReason() throws Exception {

        //Given
        Exception exception = mock(Exception.class);
        JsonValue jsonResponse = json(object());

        given(exception.getMessage()).willReturn("EXCEPTION_MESSAGE");
        given(responseHandler.getJsonErrorResponse("EXCEPTION_MESSAGE", null)).willReturn(jsonResponse);

        //When
        Response response = exceptionHandler.apply(exception).getOrThrowUninterruptibly();

        //Then
        assertThat(response.getStatus()).isEqualTo(Status.INTERNAL_SERVER_ERROR);
        assertThat(response.getEntity().getJson()).isEqualTo(jsonResponse.getObject());
    }
}
