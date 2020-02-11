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

import static org.forgerock.caf.authn.AuditParameters.Entry.*;
import static org.forgerock.caf.authn.AuditParameters.*;
import static org.forgerock.caf.authn.AuthModuleParameters.*;
import static org.forgerock.caf.authn.BodyMatcher.*;
import static org.forgerock.caf.authn.TestFramework.*;
import static org.forgerock.caf.authn.test.modules.AuthModuleOne.*;
import static org.forgerock.caf.authn.test.modules.AuthModuleTwo.*;
import static org.forgerock.caf.authn.test.modules.SessionAuthModule.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.assertj.core.api.Condition;
import org.forgerock.caf.authn.test.modules.AuthModuleOne;
import org.forgerock.caf.authn.test.modules.AuthModuleTwo;
import org.forgerock.caf.authn.test.modules.SessionAuthModule;
import org.forgerock.caf.authn.test.runtime.GuiceModule;
import org.forgerock.guice.core.GuiceModules;
import org.forgerock.json.JsonPointer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Functional tests for the JASPI runtime to test that SEND_CONTINUE (multi-stage module) support works.
 *
 * @since 1.5.0
 */
@Test(testName = "SendContinueSupport")
@GuiceModules(GuiceModule.class)
public class SendContinueSupportIT extends HandlerHolder {

    private final Logger logger = LoggerFactory.getLogger(SendContinueSupportIT.class);

    @DataProvider(name = "sendContinueSupportData")
    private Object[][] sendContinueSupportData() {
        return new Object[][]{
            /**
             * Session Module Only - SEND_CONTINUE->SEND_SUCCESS:AuthException
             *
             * Set up:
             * * Session Module configured
             * * No Auth Modules configured
             * * Session Module #validateRequest will return SEND_CONTINUE for first request and will return
             * SEND_SUCCESS for subsequent requests
             * * Session Module #secureResponse will throw AuthException (but should not be called)
             *
             *
             * Expected Result:
             * * First request:
             * ** HTTP response requesting more information from the client (contents of response are out of scope)
             * ** No auditing to occur
//                 * ** State cookie on response
             * ** Requested resource not called
             * *-------------------------------
             * * Second request:
             * ** HTTP 200 status
             * * Audit Session Module success
             * * Audit overall result as success
             * * Audit record contains Session Module principal
             * * Audit record does not contain session id
//                 * ** No state cookie on response
             * * Requested resource not called (resource will set header 'RESOURCE_CALLED':true on response)
             *
             */
            {"Session Module Only - SEND_CONTINUE->SEND_SUCCESS:AuthException", requests(
                request(moduleParams(SessionAuthModule.class, "SESSION", SEND_CONTINUE_AUTH_STATUS, null),
                    moduleArray(), 200, false, noData(), null),
                request(moduleParams(SessionAuthModule.class, "SESSION", SEND_SUCCESS_AUTH_STATUS, null),
                        moduleArray(), 200, false, noData(),
                    auditParams("SUCCESSFUL", SESSION_MODULE_PRINCIPAL, false,
                            entry("SessionAuthModule", "SUCCESSFUL"))))
            },
            /**
             * Single Auth Module Only - SEND_CONTINUE->SEND_SUCCESS:AuthException
             *
             * Set up:
             * * No Session Module configured
             * * Single Auth Module configured
             * * Auth Module #validateRequest will return SEND_CONTINUE for first request and will return
             * SEND_SUCCESS for subsequent requests
             * * Auth Module #secureResponse will throw AuthException (but should not be called)
             *
             *
             * Expected Result:
             * * First request:
             * ** HTTP response requesting more information from the client (contents of response are out of scope)
             * ** No auditing to occur
//                 * ** State cookie on response
             * ** Requested resource not called
             * *-------------------------------
             * * Second request:
             * ** HTTP 200 status
             * ** Audit Auth Module success
             * ** Audit overall result as success
             * * Audit record contains Auth Module One principal
             * * Audit record does not contain session id
//                 * ** No state cookie on response
             * * Requested resource not called (resource will set header 'RESOURCE_CALLED':true on response)
             *
             */
            {"Single Auth Module Only - SEND_CONTINUE->SEND_SUCCESS:AuthException", requests(
                request(null, moduleArray(
                        moduleParams(AuthModuleOne.class, "AUTH-MODULE-ONE", SEND_CONTINUE_AUTH_STATUS, null)),
                    200, false, noData(), null),
                request(null, moduleArray(
                        moduleParams(AuthModuleOne.class, "AUTH-MODULE-ONE", SEND_SUCCESS_AUTH_STATUS, null)),
                    200, false, noData(),
                    auditParams("SUCCESSFUL", AUTH_MODULE_ONE_PRINCIPAL, false,
                            entry("AuthModuleOne", "SUCCESSFUL"))))
            },
            /**
             * Session Module and Two Auth Modules - SEND_CONTINUE->SEND_SUCCESS:AuthException &
             * AuthException:AuthException & AuthException:AuthException
             *
             * Set up:
             * * Session Module configured
             * * Two Auth Modules configured
             * * Session Module #validateRequest will return SEND_CONTINUE for first request and will return
             * SEND_SUCCESS for subsequent requests
             * * Session Module #secureResponse will throw AuthException (but should not be called)
             * * Auth Module One #validateRequest will throw AuthException (but should not be called)
             * * Auth Module One #secureResponse will throw AuthException (but should not be called)
             * * Auth Module Two #validateRequest will throw AuthException (but should not be called)
             * * Auth Module Two #secureResponse will throw AuthException (but should not be called)
             *
             *
             * Expected Result:
             * * First request:
             * ** HTTP response requesting more information from the client (contents of response are out of scope)
             * ** No auditing to occur
//                 * ** State cookie on response
             * ** Requested resource not called
             * *-------------------------------
             * * Second request:
             * ** HTTP 200 status
             * ** Audit Session Module success
             * ** Audit overall result as success
             * * Audit record contains Session Module principal
             * * Audit record does not contain session id
//                 * ** No state cookie on response
             * * Requested resource not called (resource will set header 'RESOURCE_CALLED':true on response)
             *
             */
            {"Session Module and Two Auth Modules - SC->SS:AE & AE:AE & AE:AE", requests(
                request(moduleParams(SessionAuthModule.class, "SESSION", SEND_CONTINUE_AUTH_STATUS, null),
                    moduleArray(
                        moduleParams(AuthModuleOne.class, "AUTH-MODULE-ONE", null, null),
                        moduleParams(AuthModuleTwo.class, "AUTH-MODULE-TWO", null, null)), 200, false,
                    noData(), null),
                request(moduleParams(SessionAuthModule.class, "SESSION", SEND_SUCCESS_AUTH_STATUS, null),
                    moduleArray(
                        moduleParams(AuthModuleOne.class, "AUTH-MODULE-ONE", null, null),
                        moduleParams(AuthModuleTwo.class, "AUTH-MODULE-TWO", null, null)), 200, false,
                    noData(),
                    auditParams("SUCCESSFUL", SESSION_MODULE_PRINCIPAL, false,
                            entry("SessionAuthModule", "SUCCESSFUL"))))
            },
            /**
             * Session Module and Two Auth Modules - SEND_FAILURE:AuthException &
             * SEND_CONTINUE->SEND_SUCCESS:AuthException & AuthException:AuthException
             *
             * Set up:
             * * Session Module configured
             * * Two Auth Modules configured
             * * Session Module #validateRequest will return SEND_FAILURE
             * * Session Module #secureResponse will throw AuthException (but should not be called)
             * * Auth Module One #validateRequest will return SEND_CONTINUE for first request and will return
             * SEND_SUCCESS for subsequent requests
             * * Auth Module One #secureResponse will throw AuthException (but should not be called)
             * * Auth Module Two #validateRequest will throw AuthException (but should not be called)
             * * Auth Module Two #secureResponse will throw AuthException (but should not be called)
             *
             *
             * Expected Result:
             * * First request:
             * ** HTTP response requesting more information from the client (contents of response are out of scope)
             * ** No auditing to occur
//                 * ** State cookie on response
             * ** Requested resource not called
             * *-------------------------------
             * * Second request:
             * ** HTTP 200 status
             * ** Audit Session Module success
             * ** Audit Auth Module One success
             * ** Audit overall result as success
             * * Audit record contains Auth Module One principal
             * * Audit record does not contain session id
//                 * ** No state cookie on response
             * * Requested resource not called (resource will set header 'RESOURCE_CALLED':true on response)
             *
             */
            {"Session Module and Two Auth Modules - SF:AE & SC->SS:AE & AE:AE", requests(
                request(moduleParams(SessionAuthModule.class, "SESSION", SEND_FAILURE_AUTH_STATUS, null),
                    moduleArray(
                        moduleParams(AuthModuleOne.class, "AUTH-MODULE-ONE", SEND_CONTINUE_AUTH_STATUS, null),
                        moduleParams(AuthModuleTwo.class, "AUTH-MODULE-TWO", null, null)), 200, false,
                    noData(), null),
                request(moduleParams(SessionAuthModule.class, "SESSION", SEND_FAILURE_AUTH_STATUS, null),
                    moduleArray(
                        moduleParams(AuthModuleOne.class, "AUTH-MODULE-ONE", SEND_SUCCESS_AUTH_STATUS, null),
                        moduleParams(AuthModuleTwo.class, "AUTH-MODULE-TWO", null, null)), 200, false,
                    noData(),
                    auditParams("SUCCESSFUL", AUTH_MODULE_ONE_PRINCIPAL, false,
                            entry("SessionAuthModule", "FAILED"),
                            entry("AuthModuleOne", "SUCCESSFUL"))))
            },
            /**
             * Session Module and Two Auth Modules - SEND_FAILURE:AuthException & SEND_FAILURE:AuthException &
             * SEND_CONTINUE->SEND_SUCCESS:AuthException
             *
             * Set up:
             * * Session Module configured
             * * Two Auth Modules configured
             * * Session Module #validateRequest will return SEND_FAILURE
             * * Session Module #secureResponse will throw AuthException (but should not be called)
             * * Auth Module One #validateRequest will SEND_FAILURE
             * * Auth Module One #secureResponse will throw AuthException (but should not be called)
             * * Auth Module Two #validateRequest will return SEND_CONTINUE for first request and will return
             * SEND_SUCCESS for subsequent requests
             * * Auth Module Two #secureResponse will throw AuthException (but should not be called)
             *
             *
             * Expected Result:
             * * First request:
             * ** HTTP response requesting more information from the client (contents of response are out of scope)
             * ** No auditing to occur
//                 * ** State cookie on response
             * ** Requested resource not called
             * *-------------------------------
             * * Second request:
             * ** HTTP 200 status
             * ** Audit Session Module success
             * ** Audit Auth Module One failure
             * ** Audit Auth Module Two success
             * ** Audit overall result as success
             * * Audit record contains Auth Module Two principal
             * * Audit record does not contain session id
//                 * ** No state cookie on response
             * * Requested resource not called (resource will set header 'RESOURCE_CALLED':true on response)
             *
             */
            {"Session Module and Two Auth Modules - SF:AE & SF:AE & SC->SS:AE", requests(
                request(moduleParams(SessionAuthModule.class, "SESSION", SEND_FAILURE_AUTH_STATUS, null),
                    moduleArray(
                        moduleParams(AuthModuleOne.class, "AUTH-MODULE-ONE", SEND_FAILURE_AUTH_STATUS, null),
                        moduleParams(AuthModuleTwo.class, "AUTH-MODULE-TWO", SEND_CONTINUE_AUTH_STATUS, null)), 200,
                    false, noData(), null),
                request(moduleParams(SessionAuthModule.class, "SESSION", SEND_FAILURE_AUTH_STATUS, null),
                    moduleArray(
                        moduleParams(AuthModuleOne.class, "AUTH-MODULE-ONE", SEND_FAILURE_AUTH_STATUS, null),
                        moduleParams(AuthModuleTwo.class, "AUTH-MODULE-TWO", SEND_SUCCESS_AUTH_STATUS, null)), 200,
                    false, noData(),
                    auditParams("SUCCESSFUL", AUTH_MODULE_TWO_PRINCIPAL, false,
                            entry("SessionAuthModule", "FAILED"), entry("AuthModuleOne", "FAILED"),
                            entry("AuthModuleTwo", "SUCCESSFUL"))))
            },
        };
    }

    static List<RequestParameters> requests(RequestParameters... requestParametersList) {
        return Arrays.asList(requestParametersList);
    }

    static RequestParameters request(AuthModuleParameters sessionModuleParams,
            List<AuthModuleParameters> authModuleParametersList, int expectedResponseStatus,
            boolean expectResourceToBeCalled, Map<JsonPointer, Condition<?>> expectedBody,
            AuditParameters auditParams) {
        return new RequestParameters(sessionModuleParams, authModuleParametersList, expectedResponseStatus,
                expectResourceToBeCalled, expectedBody, auditParams);
    }

    private static class RequestParameters {

        private final AuthModuleParameters sessionModuleParams;
        private final List<AuthModuleParameters> authModuleParametersList;
        private final int expectedResponseStatus;
        private final boolean expectResourceToBeCalled;
        private final Map<JsonPointer, Condition<?>> expectedBody;
        private final AuditParameters auditParams;

        private RequestParameters(AuthModuleParameters sessionModuleParams,
                List<AuthModuleParameters> authModuleParametersList, int expectedResponseStatus,
                boolean expectResourceToBeCalled, Map<JsonPointer, Condition<?>> expectedBody,
                AuditParameters auditParams) {
            this.sessionModuleParams = sessionModuleParams;
            this.authModuleParametersList = authModuleParametersList;
            this.expectedResponseStatus = expectedResponseStatus;
            this.expectResourceToBeCalled = expectResourceToBeCalled;
            this.expectedBody = expectedBody;
            this.auditParams = auditParams;
        }

        public AuthModuleParameters getSessionModuleParams() {
            return sessionModuleParams;
        }

        public List<AuthModuleParameters> getAuthModuleParametersList() {
            return authModuleParametersList;
        }

        public int getExpectedResponseStatus() {
            return expectedResponseStatus;
        }

        public boolean isExpectResourceToBeCalled() {
            return expectResourceToBeCalled;
        }

        public Map<JsonPointer, Condition<?>> getExpectedBody() {
            return expectedBody;
        }

        public AuditParameters getAuditParams() {
            return auditParams;
        }
    }

    @Test (dataProvider = "sendContinueSupportData")
    public void sendContinueSupport(String dataName, List<RequestParameters> requestParametersList) throws Exception {
        logger.info("Running sendContinueSupport test with data set: " + dataName);
        for (RequestParameters requestParameters : requestParametersList) {
            runRequest(requestParameters.getSessionModuleParams(), requestParameters.getAuthModuleParametersList(),
                    requestParameters.getExpectedResponseStatus(), requestParameters.isExpectResourceToBeCalled(),
                    requestParameters.getExpectedBody(), requestParameters.getAuditParams());
        }

    }

    private void runRequest(AuthModuleParameters sessionModuleParams,
            List<AuthModuleParameters> authModuleParametersList, int expectedResponseStatus,
            boolean expectResourceToBeCalled, Map<JsonPointer, Condition<?>> expectedBody, AuditParameters auditParams)
            throws Exception {
        runTest(handler, "/protected/resource", sessionModuleParams, authModuleParametersList, expectedResponseStatus,
                expectResourceToBeCalled, expectedBody, auditParams);
    }
}
