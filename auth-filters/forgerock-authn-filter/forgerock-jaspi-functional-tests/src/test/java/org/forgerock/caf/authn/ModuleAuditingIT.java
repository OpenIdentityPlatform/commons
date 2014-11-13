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

import com.jayway.restassured.specification.RequestSpecification;
import org.assertj.core.api.Assertions;
import org.assertj.core.data.MapEntry;
import org.forgerock.caf.authn.test.modules.AuditingAuthModule;
import org.forgerock.caf.authn.test.modules.AuditingSessionAuthModule;
import org.forgerock.caf.authn.test.modules.FailureAuditingAuthModule;
import org.forgerock.json.fluent.JsonValue;
import org.hamcrest.Matcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.MapEntry.entry;
import static org.forgerock.caf.authn.AuthModuleParameters.moduleArray;
import static org.forgerock.caf.authn.AuthModuleParameters.moduleParams;
import static org.forgerock.caf.authn.TestFramework.*;
import static org.forgerock.caf.authn.test.modules.SessionAuthModule.SEND_FAILURE_AUTH_STATUS;
import static org.forgerock.caf.authn.test.modules.SessionAuthModule.SEND_SUCCESS_AUTH_STATUS;
import static org.forgerock.caf.authn.test.modules.SessionAuthModule.SUCCESS_AUTH_STATUS;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.testng.Assert.fail;

/**
 * Functional tests for auditing in the JASPI runtime.
 *
 * @since 1.5.0
 */
@Test(testName = "ModuleAuditing")
public class ModuleAuditingIT {

    private final Logger logger = LoggerFactory.getLogger(CommittedResponseIT.class);

    @BeforeClass
    public void setUp() {
        setUpConnection();
    }

    @DataProvider(name = "auditing")
    private Object[][] validUsage() {
        return new Object[][]{
            /**
             * Auditing Session Module
             *
             * Set up:
             * * Auditing Session Module configured
             * * No Auth Modules configured
             * * Session Module #validateRequest will add additional to audit info
             * * Session Module #secureResponse will add session id
             *
             * Expected Results:
             * * HTTP 200 status
             * * Audit Session Module success
             * * Audit overall result as success
             * * Session Module audit entry to contain additional audit info
             * * Audit record to contain session id
             *
             */
            {"Session Module Only",
                moduleParams(AuditingSessionAuthModule.class, "SESSION", SUCCESS_AUTH_STATUS,
                    SEND_SUCCESS_AUTH_STATUS),
                moduleArray(), 200, "SUCCESSFUL", true, "Session-AuditingSessionAuthModule",
                    Collections.singletonMap("AUDITING_SESSION_AUTH_MODULE_AUDIT_INFO", "AUDIT_INFO"), new MapEntry[0]
            },
            /**
             * Auditing Auth Module
             *
             * Set up:
             * * No Session Module configured
             * * Single Auditing Auth Module configured
             * * Auth Module #validateRequest will add additional to audit info
             * * Auth Module #secureResponse will add session id
             *
             * Expected Results:
             * * HTTP 200 status
             * * Audit Auth Module success
             * * Audit overall result as success
             * * Auth Module audit entry to contain additional audit info
             * * Audit record to not contain session id
             *
             */
            {"Single Auth Module Only",
                null, moduleArray(moduleParams(AuditingAuthModule.class, "AUTH-MODULE-ONE",
                    SUCCESS_AUTH_STATUS, SEND_SUCCESS_AUTH_STATUS)), 200, "SUCCESSFUL", false,
                "AuthModule-AuditingAuthModule-0",
                Collections.singletonMap("AUDITING_AUTH_MODULE_AUDIT_INFO", "AUDIT_INFO"), new MapEntry[0]
            },
            /**
             * Failing Auditing Session Auth Module
             *
             * Set up:
             * * Failing Auditing Session Module configured
             * * No Auth Modules configured
             * * Session Module #validateRequest will add failure reason
             *
             * Expected Results:
             * * HTTP 200 status
             * * Audit Session Module failure
             * * Audit overall result as failure
             * * Session Module audit entry to contain failure reason
             *
             */
            {"Failing Session Module Only",
                moduleParams(FailureAuditingAuthModule.class, "SESSION", SEND_FAILURE_AUTH_STATUS, null), moduleArray(),
                401, "FAILED", false, "Session-FailureAuditingAuthModule", Collections.emptyMap(),
                new MapEntry[]{entry("message", "FAILURE_REASON")}
            },
            /**
             * Failing Auditing Auth Module
             *
             * Set up:
             * * No Session Module configured
             * * Single Failing Auditing Auth Module configured
             * * Auth Module #validateRequest will add failure reason
             *
             * Expected Results:
             * * HTTP 200 status
             * * Audit Auth Module failure
             * * Audit overall result as failure
             * * Auth Module audit entry to contain failure reason
             *
             */
            {"Single Failing Auth Module Only",
                null, moduleArray(moduleParams(FailureAuditingAuthModule.class, "AUTH-MODULE-ONE",
                    SEND_FAILURE_AUTH_STATUS, null)), 401, "FAILED", false, "AuthModule-FailureAuditingAuthModule-0",
                    Collections.emptyMap(), new MapEntry[]{entry("message", "FAILURE_REASON")}
            },
        };
    }

    @Test (dataProvider = "auditing")
    public void auditing(String dataName, AuthModuleParameters sessionModuleParams,
            List<AuthModuleParameters> authModuleParametersList, int expectedResponseStatus, String auditResult,
            boolean sessionPresent, String moduleId, Map<String, Object> moduleAuditInfo,
            MapEntry[] auditReasonMatchers) {
        logger.info("Running ModuleAuditing test with data set: " + dataName);

        RequestSpecification given = given(sessionModuleParams, authModuleParametersList);

        given.expect()
                .statusCode(expectedResponseStatus);

        given.when()
                .get("/protected/resource");

        JsonValue auditRecords = getAuditRecords();

        assertThat(auditRecords).hasSize(1);

        assertThat(auditRecords.get(0).get("result").asString()).isEqualTo(auditResult);
        assertThat(auditRecords.get(0).get("requestId").asString()).isNotNull();
        if (sessionPresent) {
            assertThat(auditRecords.get(0).get("sessionId").asString())
                    .isEqualTo("AUDITING_SESSION_AUTH_MODULE_SESSION_ID");
        } else {
            assertThat(auditRecords.get(0).get("sessionId").getObject()).isNull();
        }
        assertThat(auditRecords.get(0).get("entries")).hasSize(1);

        Map<String, Object> entries = auditRecords.get(0).get("entries").get(0).asMap();
        assertThat(entries)
                .contains(Assertions.entry("moduleId", moduleId), Assertions.entry("result", auditResult),
                        Assertions.entry("info", moduleAuditInfo));
        Map<String, Object> reason = (Map<String, Object>) entries.get("reason");
        if (reason != null) {
            assertThat(reason).containsOnly(auditReasonMatchers);
        } else if (auditReasonMatchers.length != 0) {
            fail("Module reason is unexpectedly null!");
        }
    }
}
