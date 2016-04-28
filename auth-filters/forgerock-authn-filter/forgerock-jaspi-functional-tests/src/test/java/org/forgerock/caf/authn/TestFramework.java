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
 * Copyright 2014-2016 ForgeRock AS.
 */

package org.forgerock.caf.authn;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.*;
import static org.forgerock.caf.authn.test.ProtectedResource.*;
import static org.forgerock.json.JsonValue.*;
import static org.forgerock.util.test.assertj.AssertJPromiseAssert.assertThat;
import static org.testng.Assert.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.assertj.core.api.Condition;
import org.forgerock.caf.authentication.api.AsyncServerAuthModule;
import org.forgerock.http.Handler;
import org.forgerock.http.header.ContentTypeHeader;
import org.forgerock.http.protocol.Headers;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.http.protocol.Status;
import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.services.context.AttributesContext;
import org.forgerock.services.context.RootContext;
import org.forgerock.util.promise.NeverThrowsException;
import org.forgerock.util.promise.Promise;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Test framework for running tests and verifing results against the JASPI runtime.
 *
 * @since 1.5.0
 */
class TestFramework {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * <p>Configures the runtime with the provided "Session" auth module and auth modules array.</p>
     *
     * <p>Pass {@code null} in as the first parameter for no "Session" auth module.</p>
     *
     * @param sessionModule The "Session" auth module class.
     * @param authModules An array of auth module classes.
     */
    private static void configureRuntime(Handler handler, Class<? extends AsyncServerAuthModule> sessionModule,
            List<Class<? extends AsyncServerAuthModule>> authModules) throws Exception {

        JsonValue config = json(object());
        JsonValue configuration = json(object(field("serverAuthContext", config.getObject())));

        if (sessionModule != null) {
            config.put("sessionModule", object(field("className", sessionModule.getName())));
        }

        if (authModules != null) {
            config.put("authModules", array());
            for (Class<? extends AsyncServerAuthModule> authModule : authModules) {
                config.get("authModules").add(object(field("className", authModule.getName())));
            }
        }

        final Request request = new Request().setUri("/configuration")
                .setEntity(configuration.getObject()).setMethod("PUT");
        request.getHeaders().add(ContentTypeHeader.valueOf("application/json; charset=UTF-8"));
        request.getHeaders().add("If-Match", "*");
        Promise<Response, NeverThrowsException> result = handler.handle(new AttributesContext(new RootContext()),
                request);
        assertThat(result).succeeded();
        assertThat(result.get().getStatus()).isEqualTo(Status.OK);
    }

    /**
     * Reads and clears the audit records from the server.
     *
     * @return A {@code List} of the audit records which are represented as {@code Map}s containing the audit
     * information.
     */
    @SuppressWarnings("unchecked")
    static JsonValue getAuditRecords(Handler handler) throws Exception {
        final Request request = new Request().setUri("/auditrecords?_action=readAndClear")
                .setEntity("{}").setMethod("POST");
        request.getHeaders().add(ContentTypeHeader.valueOf("application/json; charset=UTF-8"));
        Promise<Response, NeverThrowsException> result = handler.handle(new AttributesContext(new RootContext()),
                request);
        assertThat(result).succeeded();
        assertThat(result.get().getStatus()).isEqualTo(Status.OK);
        return json(result.get().getEntity().getJson());
    }

    /**
     * Configures the JASPI runtime with the given modules and sets request headers to configure what {@code AuthStatus}
     * each of the modules will return for #validateRequest and #secureResponse.
     *
     * @param sessionModuleParams The configuration and parameters for the "Session" auth module.
     * @param authModuleParametersList A {@code List} of configuration and parameters for the auth modules.
     * @return A RestAssured RequestSpecification instance.
     */
    static Request given(Handler handler, AuthModuleParameters sessionModuleParams,
            List<AuthModuleParameters> authModuleParametersList) throws Exception {
        Request request = new Request();
        Headers headers = request.getHeaders();

        Class<? extends AsyncServerAuthModule> sessionModuleClass = null;
        List<Class<? extends AsyncServerAuthModule>> authModuleClasses = new ArrayList<>();

        if (sessionModuleParams != null) {
            sessionModuleClass = sessionModuleParams.getModuleClass();
            if (sessionModuleParams.validateRequestReturnValue() != null) {
                headers.add("X-JASPI-" + sessionModuleParams.getModuleName() + "-VALIDATE-REQUEST",
                        sessionModuleParams.validateRequestReturnValue());
            }
            if (sessionModuleParams.secureResponseReturnValue() != null) {
                headers.add("X-JASPI-" + sessionModuleParams.getModuleName() + "-SECURE-RESPONSE",
                        sessionModuleParams.secureResponseReturnValue());
            }
        }

        if (authModuleParametersList != null) {
            for (AuthModuleParameters authModuleParams : authModuleParametersList) {
                authModuleClasses.add(authModuleParams.getModuleClass());
                if (authModuleParams.validateRequestReturnValue() != null) {
                    headers.add("X-JASPI-" + authModuleParams.getModuleName() + "-VALIDATE-REQUEST",
                            authModuleParams.validateRequestReturnValue());
                }
                if (authModuleParams.secureResponseReturnValue() != null) {
                    headers.add("X-JASPI-" + authModuleParams.getModuleName() + "-SECURE-RESPONSE",
                            authModuleParams.secureResponseReturnValue());
                }
            }
        }
        configureRuntime(handler, sessionModuleClass, authModuleClasses);
        return request;
    }

    /**
     * Runs the test, configuring the JASPI runtime with the given modules and then asserting the response and audit
     * records after the request has completed.
     *
     * @param resourceName The name of the protected resource to attempt to access.
     * @param sessionModuleParams The configuration and parameters for the "Session" auth module.
     * @param authModuleParametersList A {@code List} of configuration and parameters for the auth modules.
     * @param expectedResponseStatus The expected response status.
     * @param expectResourceToBeCalled Whether it is expected that the resource have been accessed.
     * @param expectedBody A {@code Map} of the JSON path and {@code Matcher}s to verify the response body.
     * @param auditParams The expected audit operations to have occurred.
     */
    static void runTest(Handler handler, String resourceName, AuthModuleParameters sessionModuleParams,
            List<AuthModuleParameters> authModuleParametersList, int expectedResponseStatus,
            boolean expectResourceToBeCalled, Map<JsonPointer, Condition<?>> expectedBody, AuditParameters auditParams)
            throws Exception {

        /* Ensure audit records are cleared before running test. */
        getAuditRecords(handler);

        Request request = given(handler, sessionModuleParams, authModuleParametersList)
                .setUri(resourceName);

        Promise<Response, NeverThrowsException> result = handler.handle(new AttributesContext(new RootContext()),
                request);
        assertThat(result).succeeded();

        Response response = result.get();

        if (expectResourceToBeCalled) {
            assertThat(response.getHeaders().getFirst(RESOURCE_CALLED_HEADER)).isEqualTo("true");
        } else {
            assertThat(response.getHeaders().getFirst(RESOURCE_CALLED_HEADER)).isNull();
        }
        for (Map.Entry<JsonPointer, Condition<?>> bodyMatcher : expectedBody.entrySet()) {
            if (bodyMatcher.getKey() == null) {
                assertThat(response.getEntity().getString()).is((Condition<? super String>) bodyMatcher.getValue());
            } else {
                final JsonValue json = json(response.getEntity().getJson());
                final JsonValue jsonValue = json.get(bodyMatcher.getKey());
                assertThat(jsonValue == null ? null : jsonValue.getObject()).is(
                        (Condition<? super Object>) bodyMatcher.getValue());
            }
        }

        JsonValue auditRecords = getAuditRecords(handler);
        if (auditParams != null) {
            assertThat(auditRecords).hasSize(1);

            JsonValue auditRecord = auditRecords.get(0);
            assertThat(auditRecord.get("result").asString()).isEqualTo(auditParams.result());
            assertThat(auditRecord.get("requestId").asString()).isNotNull();
            if (auditParams.principal() == null) {
                assertThat(auditRecord.get("principal").asList(String.class)).isNull();
            } else if (auditParams.principal().isEmpty()) {
                assertThat(auditRecord.get("principal").asList(String.class)).isEmpty();
            } else {
                assertThat(auditRecord.get("principal").asList(String.class)).containsExactly(auditParams.principal());
            }
            if (auditParams.sessionPresent()) {
                assertThat(auditRecord.get("sessionId").getObject()).isNotNull();
            } else {
                assertThat(auditRecord.get("sessionId").getObject()).isNull();
            }
            assertThat(auditRecord.get("entries")).hasSize(auditParams.entries().size());

            for (int i = 0; i < auditParams.entries().size(); i++) {
                AuditParameters.Entry entry = auditParams.entries().get(i);
                Map<String, Object> entries = auditRecord.get("entries").get(i).asMap();
                assertThat(entries).contains(
                        entry("moduleId", entry.getModuleId()), entry("result", entry.getResult()));
                Map<String, Object> reason = (Map<String, Object>) entries.get("reason");
                if (reason != null) {
                    assertThat(reason).containsOnly(entry.getReasonMatchers());
                } else if (entry.getReasonMatchers().length != 0) {
                    fail("Module reason is unexpectedly null!");
                }
            }
        } else {
            assertThat(auditRecords).isEmpty();
        }
    }
}
