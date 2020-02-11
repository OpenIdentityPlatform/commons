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

package org.forgerock.http.protocol;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.forgerock.http.protocol.Responses.internalServerError;
import static org.forgerock.http.protocol.Responses.newInternalServerError;
import static org.forgerock.http.protocol.Responses.newNotFound;
import static org.forgerock.http.protocol.Responses.onExceptionInternalServerError;
import static org.forgerock.http.protocol.Status.INTERNAL_SERVER_ERROR;
import static org.forgerock.http.protocol.Status.NOT_FOUND;

import org.forgerock.util.AsyncFunction;
import org.forgerock.util.Function;
import org.forgerock.util.promise.NeverThrowsException;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.promise.Promises;
import org.testng.annotations.Test;

@SuppressWarnings("javadoc")
public class ResponsesTest {

    private Exception exception = new Exception("boom");

    @Test
    public void shouldCreateInternalServerErrorResponse() throws Exception {
        assertThat(newInternalServerError().getStatus()).isEqualTo(INTERNAL_SERVER_ERROR);
    }

    @Test
    public void shouldCreateInternalServerErrorResponseWithCause() throws Exception {
        Response response = newInternalServerError(exception);
        assertThatResponseIsInternalServerErrorWithCause(response);
    }

    @Test
    public void shouldCreateNotFoundResponse() throws Exception {
        assertThat(newNotFound().getStatus()).isEqualTo(NOT_FOUND);
    }

    @Test
    public void shouldCreate500ErrorResponseOnException() throws Exception {
        Response response = Promises.<String, Exception>newExceptionPromise(exception)
                .then(new Function<String, Response, NeverThrowsException>() {
                    @Override
                    public Response apply(String value) {
                        fail("");
                        throw new IllegalStateException();
                    }
                }, onExceptionInternalServerError())
                .getOrThrow();

        assertThatResponseIsInternalServerErrorWithCause(response);
    }

    @Test
    public void shouldCreate500ErrorResponseOnExceptionAsync() throws Exception {
        Response response = Promises.<String, Exception>newExceptionPromise(exception)
                .thenAsync(new AsyncFunction<String, Response, NeverThrowsException>() {
                    @Override
                    public Promise<Response, NeverThrowsException> apply(String value) {
                        throw new IllegalStateException();
                    }
                }, internalServerError())
                .getOrThrow();

        assertThatResponseIsInternalServerErrorWithCause(response);
    }

    private void assertThatResponseIsInternalServerErrorWithCause(Response response) {
        assertThat(response.getStatus()).isEqualTo(INTERNAL_SERVER_ERROR);
        assertThat(response.getCause()).isSameAs(exception);
    }
}