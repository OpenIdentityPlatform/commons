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

package org.forgerock.caf.authentication.framework;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.forgerock.json.fluent.JsonValue.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.forgerock.caf.authentication.api.AuthenticationException;
import org.forgerock.caf.authentication.api.MessageContext;
import org.forgerock.guava.common.net.MediaType;
import org.forgerock.http.header.ContentTypeHeader;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.json.resource.ResourceException;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class ResponseHandlerTest {

    private static final String APPLICATION_XML = "application/xml";
    private static final String APPLICATION_JSON = "application/json; charset=UTF-8";
    private static final String TEXT_XML = "text/xml";
    private static final String TEXT_PLAIN = "text/plain";

    private ResponseHandler handler;

    @BeforeMethod
    public void setup() throws Exception {
        this.handler = new ResponseHandler();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void jsonHandlerShouldRenderAuthenticationFailedException() throws Exception {

        //Given
        MessageContext context = mock(MessageContext.class);
        Response response = new Response();
        AuditTrail auditTrail = mock(AuditTrail.class);
        AuthenticationException exception = new AuthenticationFailedException();

        given(context.getResponse()).willReturn(response);
        given(context.getAuditTrail()).willReturn(auditTrail);

        //When
        new ResponseHandler.JsonResponseWriter().write(context, exception);

        //Then
        Map<String, Object> responseBody = (Map<String, Object>) response.getEntity().getJson();
        assertThat(response.getHeaders().getFirst(ContentTypeHeader.NAME)).isEqualTo("application/json; charset=UTF-8");
        assertThat(responseBody).contains(
                entry("code", 401),
                entry("reason", "Unauthorized"),
                entry("message", "Access Denied"));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void jsonHandlerShouldRenderAuthenticationException() throws Exception {

        //Given
        MessageContext context = mock(MessageContext.class);
        Response response = new Response();
        AuditTrail auditTrail = mock(AuditTrail.class);
        AuthenticationException exception = new AuthenticationException("ERROR");

        given(context.getResponse()).willReturn(response);
        given(context.getAuditTrail()).willReturn(auditTrail);

        //When
        new ResponseHandler.JsonResponseWriter().write(context, exception);

        //Then
        Map<String, Object> responseBody = (Map<String, Object>) response.getEntity().getJson();
        assertThat(response.getHeaders().getFirst(ContentTypeHeader.NAME)).isEqualTo("application/json; charset=UTF-8");
        assertThat(responseBody).contains(
                entry("code", 500),
                entry("reason", "Internal Server Error"),
                entry("message", "ERROR"));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void jsonHandlerShouldRenderException() throws Exception {

        //Given
        MessageContext context = mock(MessageContext.class);
        Response response = new Response();
        AuditTrail auditTrail = mock(AuditTrail.class);
        List<Map<String, Object>> failureReasons = new ArrayList<>();
        ResourceException jre = ResourceException.getException(ResourceException.BAD_REQUEST, "BAD_REQUEST");
        AuthenticationException exception = new AuthenticationException(jre);

        given(context.getResponse()).willReturn(response);
        given(context.getAuditTrail()).willReturn(auditTrail);
        given(auditTrail.getFailureReasons()).willReturn(failureReasons);
        failureReasons.add(Collections.<String, Object>singletonMap("DETAIL_KEY", "DETAIL_VALUE"));

        //When
        new ResponseHandler.JsonResponseWriter().write(context, exception);

        //Then
        Map<String, Object> responseBody = (Map<String, Object>) response.getEntity().getJson();
        assertThat(response.getHeaders().getFirst(ContentTypeHeader.NAME)).isEqualTo("application/json; charset=UTF-8");
        assertThat(responseBody).containsKey("detail").contains(
                entry("code", 400),
                entry("reason", "Bad Request"),
                entry("message", "BAD_REQUEST"));
        Map<String, Object> detail = (Map<String, Object>) responseBody.get("detail");
        assertThat(detail).hasSize(1);
        List<Map<String, Object>> detailEntry = (List<Map<String, Object>>) detail.get("failureReasons");
        assertThat(detailEntry).contains(Collections.<String, Object>singletonMap("DETAIL_KEY", "DETAIL_VALUE"));
    }

    @DataProvider(name = "content-types")
    public static Object[][] contentTypes() {
        return new Object[][] {
            { APPLICATION_XML, APPLICATION_XML },
            { "application/svg+xml", APPLICATION_JSON },
            { TEXT_XML, APPLICATION_XML },
            { TEXT_PLAIN, APPLICATION_JSON },
            { "application/json; q=0.8, application/xml", APPLICATION_XML },
            { "application/json; q=0.6, application/xml; q=0.8", APPLICATION_XML },
            { "application/json; q=0.9, application/xml; q=0.8", APPLICATION_JSON },
            { "*/*; q=1.0, application/json; q=0.6, application/xml; q=0.8", APPLICATION_JSON },
            { "*/*; q=1.0, application/json; q=0.6, application/xml; q=1.0", APPLICATION_XML },
            { null, APPLICATION_JSON }
        };
    }

    @Test(dataProvider = "content-types")
    public void shouldUseExceptionHandlerIfAvailable(String accept, String expected) throws Exception {

        //Given
        Request request = new Request();
        Response response = new Response();
        AuditTrail auditTrail = mock(AuditTrail.class);
        MessageContext context = mock(MessageContext.class);
        ResourceException jre = ResourceException.getException(ResourceException.BAD_REQUEST, "BAD_REQUEST");
        AuthenticationException exception = new AuthenticationException(jre);

        request.getHeaders().putSingle("Accept", accept);
        given(context.getRequest()).willReturn(request);
        given(context.getResponse()).willReturn(response);
        given(context.getAuditTrail()).willReturn(auditTrail);
        jre.setDetail(json(object(field("DETAIL_KEY", "DETAIL_VALUE"))));

        handler.addResponseWriter(new TestExceptionWriter());

        //When
        handler.handle(context, exception);

        //Then
        assertThat(response.getHeaders().getFirst(ContentTypeHeader.NAME)).isEqualTo(expected);
    }

    public static final class TestExceptionWriter implements ResponseWriter {

        public List<MediaType> handles() {
            return Arrays.asList(MediaType.parse(APPLICATION_XML), MediaType.parse(TEXT_XML));
        }

        public void write(MessageContext context, AuthenticationException exception) {
            Response response = context.getResponse();
            response.getHeaders().putSingle(ContentTypeHeader.valueOf(APPLICATION_XML));
            ResourceException jre = (ResourceException) exception.getCause();
            response.setEntity("Hello " + jre.getCode());
        }
    }
}
