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
import static org.forgerock.util.test.assertj.AssertJPromiseAssert.assertThat;
import static org.assertj.core.data.MapEntry.entry;
import static org.forgerock.caf.authn.AuthModuleParameters.moduleArray;
import static org.forgerock.caf.authn.AuthModuleParameters.moduleParams;
import static org.forgerock.caf.authn.TestFramework.*;
import static org.forgerock.caf.authn.test.modules.SessionAuthModule.*;
import static org.testng.Assert.fail;

import java.util.List;
import java.util.Map;

import org.assertj.core.api.Assertions;
import org.assertj.core.data.MapEntry;
import org.forgerock.caf.authn.test.modules.AuditingAuthModule;
import org.forgerock.caf.authn.test.modules.AuditingSessionAuthModule;
import org.forgerock.caf.authn.test.modules.FailureAuditingAuthModule;
import org.forgerock.caf.authn.test.runtime.GuiceModule;
import org.forgerock.guice.core.GuiceModules;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.json.JsonValue;
import org.forgerock.services.context.AttributesContext;
import org.forgerock.services.context.RootContext;
import org.forgerock.util.promise.NeverThrowsException;
import org.forgerock.util.promise.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Functional tests for auditing in the JASPI runtime.
 *
 * @since 1.5.0
 */
@Test(testName = "ModuleAuditing")
@GuiceModules(GuiceModule.class)
public class ModuleAuditingIT extends HandlerHolder {

    private final Logger logger = LoggerFactory.getLogger(CommittedResponseIT.class);

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
                moduleArray(), 200, "SUCCESSFUL", true, "AuditingSessionAuthModule", new MapEntry[]{
                    entry("AUDITING_SESSION_AUTH_MODULE_AUDIT_INFO", "AUDIT_INFO")}, new MapEntry[0]
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
                "AuditingAuthModule", new MapEntry[]{
                    entry("AUDITING_AUTH_MODULE_AUDIT_INFO", "AUDIT_INFO"),
                    entry("MORE_AUDITING_AUTH_MODULE_AUDIT_INFO", "AUDIT_INFO")}, new MapEntry[0],
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
                401, "FAILED", false, "FailureAuditingAuthModule", new MapEntry[0],
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
                SEND_FAILURE_AUTH_STATUS, null)), 401, "FAILED", false, "FailureAuditingAuthModule",
                new MapEntry[0], new MapEntry[]{entry("message", "FAILURE_REASON")}
            },
        };
    }

    @Test (dataProvider = "auditing")
    public void auditing(String dataName, AuthModuleParameters sessionModuleParams,
            List<AuthModuleParameters> authModuleParametersList, int expectedResponseStatus, String auditResult,
            boolean sessionPresent, String moduleId, MapEntry[] moduleAuditInfo,
            MapEntry[] auditReasonMatchers) throws Exception {
        logger.info("Running ModuleAuditing test with data set: " + dataName);

        Request request = given(handler, sessionModuleParams, authModuleParametersList)
                .setUri("http://localhost/protected/resource").setMethod("GET");

        Promise<Response, NeverThrowsException> result = handler.handle(new AttributesContext(new RootContext()),
                request);
        assertThat(result).succeeded();
        assertThat(result.get().getStatus().getCode()).isEqualTo(expectedResponseStatus);

        JsonValue auditRecords = getAuditRecords(handler);

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
                .contains(Assertions.entry("moduleId", moduleId), Assertions.entry("result", auditResult));
        assertThat(((Map<String, Object>) entries.get("info"))).contains(moduleAuditInfo);
        Map<String, Object> reason = (Map<String, Object>) entries.get("reason");
        if (reason != null) {
            assertThat(reason).containsOnly(auditReasonMatchers);
        } else if (auditReasonMatchers.length != 0) {
            fail("Module reason is unexpectedly null!");
        }
    }
}
