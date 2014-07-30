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
 * Copyright 2014 ForgeRock AS.
 */

package org.forgerock.caf.authn;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.config.EncoderConfig;
import com.jayway.restassured.config.RestAssuredConfig;
import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.parsing.Parser;
import com.jayway.restassured.specification.RequestSpecification;
import com.jayway.restassured.specification.ResponseSpecification;
import org.codehaus.jackson.map.ObjectMapper;
import org.forgerock.json.fluent.JsonValue;
import org.hamcrest.Matcher;

import javax.security.auth.message.module.ServerAuthModule;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.forgerock.caf.authn.test.ProtectedResource.RESOURCE_CALLED_HEADER;
import static org.forgerock.json.fluent.JsonValue.array;
import static org.forgerock.json.fluent.JsonValue.field;
import static org.forgerock.json.fluent.JsonValue.json;
import static org.forgerock.json.fluent.JsonValue.object;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

/**
 * Test framework for running tests and verifing results against the JASPI runtime.
 *
 * @since 1.5.0
 */
class TestFramework {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * Sets up RestAssured with the correct settings for connecting to the JASPI Test Server.
     */
    static void setUpConnection() {
        RestAssured.port = Integer.parseInt(System.getProperty("HTTP_PORT"));
        RestAssured.baseURI = "http://" + System.getProperty("HOSTNAME");
        RestAssured.basePath = System.getProperty("CONTEXT_URI");

        RestAssured.defaultParser = Parser.JSON;
        RestAssured.config = RestAssuredConfig.newConfig()
                .encoderConfig(EncoderConfig.encoderConfig().defaultContentCharset("UTF-8"));
    }

    /**
     * <p>Configures the runtime with the provided "Session" auth module and auth modules array.</p>
     *
     * <p>Pass {@code null} in as the first parameter for no "Session" auth module.</p>
     *
     * @param sessionModule The "Session" auth module class.
     * @param authModules An array of auth module classes.
     */
    private static void configureRuntime(Class<? extends ServerAuthModule> sessionModule,
            List<Class<? extends ServerAuthModule>> authModules) {

        JsonValue config = json(object());
        JsonValue configuration = json(object(field("serverAuthContext", config)));

        if (sessionModule != null) {
            config.put("sessionModule", object(field("className", sessionModule.getName())));
        }

        if (authModules != null) {
            config.put("authModules", array());
            for (Class<? extends ServerAuthModule> authModule : authModules) {
                config.get("authModules").add(object(field("className", authModule.getName())));
            }
        }

        RequestSpecification given = com.jayway.restassured.RestAssured.given()
                .contentType(ContentType.JSON)
                .body(configuration.toString());

        ResponseSpecification expect = given.expect()
                .statusCode(200);

        expect.when()
                .put("/configuration");
    }

    /**
     * Reads and clears the audit records from the server.
     *
     * @return A {@code List} of the audit records which are represented as {@code Map}s containing the audit
     * information.
     */
    @SuppressWarnings("unchecked")
    private static List<Map<String, String>> getAuditRecords() {

        RequestSpecification given = com.jayway.restassured.RestAssured.given()
                .contentType(ContentType.JSON)
                .body("{}");

        ResponseSpecification expect = given.expect()
                .statusCode(200);

        try {
            return OBJECT_MAPPER.readValue(expect.when()
                    .post("/auditrecords?_action=readAndClear")
                    .getBody().asString(), List.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Configures the JASPI runtime with the given modules and sets request headers to configure what {@code AuthStatus}
     * each of the modules will return for #validateRequest and #secureResponse.
     *
     * @param sessionModuleParams The configuration and parameters for the "Session" auth module.
     * @param authModuleParametersList A {@code List} of configuration and parameters for the auth modules.
     * @return A RestAssured RequestSpecification instance.
     */
    private static RequestSpecification given(AuthModuleParameters sessionModuleParams,
            List<AuthModuleParameters> authModuleParametersList) {
        Class<? extends ServerAuthModule> sessionModuleClass = null;
        List<Class<? extends ServerAuthModule>> authModuleClasses = new ArrayList<Class<? extends ServerAuthModule>>();

        RequestSpecification given = com.jayway.restassured.RestAssured.given();
        if (sessionModuleParams != null) {
            sessionModuleClass = sessionModuleParams.getModuleClass();
            if (sessionModuleParams.validateRequestReturnValue() != null) {
                given.header("X-JASPI-" + sessionModuleParams.getModuleName() + "-VALIDATE-REQUEST",
                        sessionModuleParams.validateRequestReturnValue());
            }
            if (sessionModuleParams.secureResponseReturnValue() != null) {
                given.header("X-JASPI-" + sessionModuleParams.getModuleName() + "-SECURE-RESPONSE",
                        sessionModuleParams.secureResponseReturnValue());
            }
        }

        if (authModuleParametersList != null) {
            for (AuthModuleParameters authModuleParams : authModuleParametersList) {
                authModuleClasses.add(authModuleParams.getModuleClass());
                if (authModuleParams.validateRequestReturnValue() != null) {
                    given.header("X-JASPI-" + authModuleParams.getModuleName() + "-VALIDATE-REQUEST",
                            authModuleParams.validateRequestReturnValue());
                }
                if (authModuleParams.secureResponseReturnValue() != null) {
                    given.header("X-JASPI-" + authModuleParams.getModuleName() + "-SECURE-RESPONSE",
                            authModuleParams.secureResponseReturnValue());
                }
            }
        }
        configureRuntime(sessionModuleClass, authModuleClasses);
        return given;
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
     * @param auditParams The expected audit operations to have occured.
     */
    static void runTest(String resourceName, AuthModuleParameters sessionModuleParams,
            List<AuthModuleParameters> authModuleParametersList, int expectedResponseStatus,
            boolean expectResourceToBeCalled, Map<String, Matcher<?>> expectedBody, AuditParameters auditParams) {

        /* Ensure audit records are cleared before running test. */
        getAuditRecords();

        RequestSpecification given = given(sessionModuleParams, authModuleParametersList);

        ResponseSpecification expect = given.expect()
                .statusCode(expectedResponseStatus);
        if (expectResourceToBeCalled) {
            expect.header(RESOURCE_CALLED_HEADER, equalTo("true"));
        } else {
            expect.header(RESOURCE_CALLED_HEADER, nullValue());
        }
        for (Map.Entry<String, Matcher<?>> bodyMatcher : expectedBody.entrySet()) {
            if (bodyMatcher.getKey() == null) {
                expect.body(bodyMatcher.getValue());
            } else {
                expect.body(bodyMatcher.getKey(), bodyMatcher.getValue());
            }
        }

        expect.when()
                .get(resourceName);

        List<Map<String, String>> auditRecords = getAuditRecords();
        if (auditParams != null) {
            assertThat(auditRecords).hasSize(1);
            assertThat(auditRecords.get(0)).contains(entry("outcome", auditParams.expectedOutcome()));
        } else {
            assertThat(auditRecords).isEmpty();
        }
    }
}
