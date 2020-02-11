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
import static org.forgerock.caf.authn.test.modules.SessionAuthModule.*;
import static org.hamcrest.Matchers.*;

import java.util.List;
import java.util.Map;

import org.assertj.core.api.Condition;
import org.assertj.core.data.MapEntry;
import org.forgerock.caf.authn.test.modules.SessionAuthModule;
import org.forgerock.caf.authn.test.runtime.GuiceModule;
import org.forgerock.guice.core.GuiceModules;
import org.forgerock.json.JsonPointer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Functional tests for the JASPI runtime when configured with just a "Session" auth module.
 *
 * @since 1.5.0
 */
@Test(testName = "SessionModuleOnly")
@GuiceModules(GuiceModule.class)
public class SessionModuleOnlyIT extends HandlerHolder {

    private final Logger logger = LoggerFactory.getLogger(SessionModuleOnlyIT.class);

    /**
     * No Modules
     *
     * Set up:
     * * No Session Module configured
     * * No Auth Modules configured
     *
     *
     * Expected Result:
     * * HTTP 401 status
     * * Audit overall result as failure
//                 * * No state cookie on response
     * * Requested resource not called (resource will set header 'RESOURCE_CALLED:true' on response)
     *
     */
    @Test
    public void testValidUsageNoModules() throws Exception {
        sessionModuleOnlyValidUsage("No Modules", null, moduleArray(), 401, false, exceptionMatcher(401),
                auditParams("FAILED", "", false));
    }

    /**
     * Session Module Only - SEND_SUCCESS:AuthException
     *
     * Set up:
     * * Session Module configured
     * * No Auth Modules configured
     * * Session Module #validateRequest will return SEND_SUCCESS
     * * Session Module #secureResponse will throw AuthException (but should not be called)
     *
     *
     * Expected Result:
     * * HTTP 200 status
     * * Audit Session Module success
     * * Audit overall result as success
     * * Audit record contains principal
     * * Audit record does not contain session id
//                 * * No state cookie on response
     * * Requested resource not called (resource will set header 'RESOURCE_CALLED':true on response)
     *
     */
    @Test
    public void testValidUsageSessionModuleOnlySendSuccessAuthException() throws Exception {
        sessionModuleOnlyValidUsage("Session Module Only - SEND_SUCCESS:AuthException",
                moduleParams(SessionAuthModule.class, "SESSION", SEND_SUCCESS_AUTH_STATUS, null),
                moduleArray(), 200, false, noData(),
                auditParams("SUCCESSFUL", SESSION_MODULE_PRINCIPAL, false,
                       entry("SessionAuthModule", "SUCCESSFUL")));
    }

    /**
     * Session Module Only - SEND_FAILURE:AuthException
     *
     * Set up:
     * * Session Module configured
     * * No Auth Modules configured
     * * Session Module #validateRequest will return SEND_FAILURE
     * * Session Module #secureResponse will throw AuthException (but should not be called)
     *
     *
     * Expected Result:
     * * HTTP 401 status
     * * Audit Session Module failure
     * * Audit overall result as failure
     * * Audit record contains principal - only if set by module (optional)
     * * Audit record does not contain session id
//                 * * No state cookie on response
     * * Requested resource not called (resource will set header 'RESOURCE_CALLED':true on response)
     *
     */
    @Test
    public void testValidUsageSessionModuleOnlySendFailureAuthException() throws Exception {
        sessionModuleOnlyValidUsage("Session Module Only - SEND_FAILURE:AuthException",
                moduleParams(SessionAuthModule.class, "SESSION", SEND_FAILURE_AUTH_STATUS, null),
                moduleArray(), 401, false, exceptionMatcher(401),
                auditParams("FAILED", "", false, entry("SessionAuthModule", "FAILED")));
    }

    /**
     * Session Module Only - SEND_CONTINUE:AuthException
     *
     * Set up:
     * * Session Module configured
     * * No Auth Modules configured
     * * Session Module #validateRequest will return SEND_CONTINUE
     * * Session Module #secureResponse will throw AuthException (but should not be called)
     *
     *
     * Expected Result:
     * ** HTTP response requesting more information from the client (contents of response are out of scope)
     * ** No auditing to occur
//                 * ** State cookie on response
     * * Requested resource not called (resource will set header 'RESOURCE_CALLED':true on response)
     *
     */
    @Test
    public void testValidUsageSessionModuleOnlySendContinueAuthException() throws Exception {
        sessionModuleOnlyValidUsage("Session Module Only - SEND_CONTINUE:AuthException",
                moduleParams(SessionAuthModule.class, "SESSION", SEND_CONTINUE_AUTH_STATUS, null),
                moduleArray(), 200, false, noData(), null);
    }

    /**
     * Session Module Only - AuthException:SEND_SUCCESS
     *
     * Set up:
     * * Session Module configured
     * * No Auth Modules configured
     * * Session Module #validateRequest will throw AuthException
     * * Session Module #secureResponse will return SEND_SUCCESS (but should not be called)
     *
     *
     * Expected Result:
     * * HTTP 500 status
     * * HTTP response detailing the cause of the failure
     * * Audit Session Module failure
     * * Audit overall result as failure
     * * Audit record does not contain principal
     * * Audit record does not contain session id
//                 * * No state cookie on response
     * * Requested resource not called (resource will set header 'RESOURCE_CALLED':true on response)
     *
     */
    @Test
    public void testValidUsageSessionModuleOnlyAuthExceptionSendSuccess() throws Exception {
        sessionModuleOnlyValidUsage("Session Module Only - AuthException:SEND_SUCCESS",
                moduleParams(SessionAuthModule.class, "SESSION", null, SEND_SUCCESS_AUTH_STATUS),
                moduleArray(), 500, false, exceptionMatcher(500),
                    auditParams("FAILED", "", false, entry("SessionAuthModule", "FAILED",
                            MapEntry.entry("exception", SESSION_VALIDATE_REQUEST_HEADER_NAME
                                    + " header not set, so throwing AuthException."))));
    }

    /**
     * Session Module Only - SUCCESS:SEND_SUCCESS
     *
     * Set up:
     * * Session Module configured
     * * No Auth Modules configured
     * * Session Module #validateRequest will return SUCCESS
     * * Response will not be committed after calling resource
     * * Session Module #secureResponse will return SEND_SUCCESS
     *
     *
     * Expected Result:
     * * HTTP 200 status
     * * HTTP response from resource
     * * Audit Session Module success
     * * Audit overall result as success
     * * Audit record contains principal
     * * Audit record contains session id
//                 * * No state cookie on response
     * * Requested resource called (resource will set header 'RESOURCE_CALLED':true on response)
     *
     */
    @Test
    public void testValidUsageSessionModuleOnlySuccessSendSuccess() throws Exception {
        sessionModuleOnlyValidUsage("Session Module Only - SUCCESS:SEND_SUCCESS",
                moduleParams(SessionAuthModule.class, "SESSION", SUCCESS_AUTH_STATUS, SEND_SUCCESS_AUTH_STATUS),
                moduleArray(), 200, true, resourceMatcher(SESSION_MODULE_PRINCIPAL, SESSION_MODULE_CONTEXT_ENTRY),
                auditParams("SUCCESSFUL", SESSION_MODULE_PRINCIPAL, true,
                        entry("SessionAuthModule", "SUCCESSFUL")));
    }

    /**
     * Session Module Only - SUCCESS:SEND_FAILURE
     *
     * Set up:
     * * Session Module configured
     * * No Auth Modules configured
     * * Session Module #validateRequest will return SUCCESS
     * * Response will not be committed after calling resource
     * * Session Module #secureResponse will return SEND_FAILURE
     *
     *
     * Expected Result:
     * * HTTP 500 status
     * * HTTP response from resource
     * * Audit Session Module success
     * * Audit overall result as success
     * * Audit record contains principal
     * * Audit record does not contain session id
//                 * * No state cookie on response
     * * Requested resource called (resource will set header 'RESOURCE_CALLED':true on response)
     *
     */
    @Test
    public void testValidUsageSessionModuleOnlySuccessSendFailure() throws Exception {
        sessionModuleOnlyValidUsage("Session Module Only - SUCCESS:SEND_FAILURE",
                moduleParams(SessionAuthModule.class, "SESSION", SUCCESS_AUTH_STATUS, SEND_FAILURE_AUTH_STATUS),
                moduleArray(), 500, true,
                resourceMatcher(SESSION_MODULE_PRINCIPAL, SESSION_MODULE_CONTEXT_ENTRY),
                auditParams("SUCCESSFUL", SESSION_MODULE_PRINCIPAL, false,
                        entry("SessionAuthModule", "SUCCESSFUL")));
    }

    /**
     * Session Module Only - SUCCESS:AuthException
     *
     * Set up:
     * * Session Module configured
     * * No Auth Modules configured
     * * Session Module #validateRequest will return SUCCESS
     * * Response will not be committed after calling resource
     * * Session Module #secureResponse will throw AuthException
     *
     *
     * Expected Result:
     * * HTTP 500 status
     * * HTTP response detailing the cause of the failure
     * * Audit Session Module success
     * * Audit overall result as success
     * * Audit record contains principal
     * * Audit record does not contain session id
//                 * * No state cookie on response
     * * Requested resource called (resource will set header 'RESOURCE_CALLED':true on response)
     *
     */
    @Test
    public void testValidUsageSessionModuleOnlySuccessAuthException() throws Exception {
        sessionModuleOnlyValidUsage("Session Module Only - SUCCESS:AuthException",
                moduleParams(SessionAuthModule.class, "SESSION", SUCCESS_AUTH_STATUS, null),
                moduleArray(), 500, true,
                exceptionMatcher(500, containsString("X-JASPI-SESSION-SECURE-RESPONSE header not set, so throwing"
                        + " AuthException")),
                auditParams("SUCCESSFUL", SESSION_MODULE_PRINCIPAL, false, entry("SessionAuthModule", "SUCCESSFUL")));
    }

    @DataProvider(name = "invalidUsage")
    private Object[][] invalidUsage() {
        return new Object[][]{
            /**
             * Session Module Only - FAILURE:SEND_SUCCESS
             *
             * Set up:
             * * Session Module configured
             * * No Auth Modules configured
             * * Session Module #validateRequest will return FAILURE
             * * Session Module #secureResponse will return SEND_SUCCESS (but should not be called)
             *
             *
             * Expected Result:
             * * HTTP 500 status
             * * HTTP response detailing the cause of the failure
             * * Audit Session Module failure
             * * Audit overall result as failure
             * * Audit record does not contain principal
             * * Audit record does not contain session id
//                 * * No state cookie on response
             * * Requested resource not called (resource will set header 'RESOURCE_CALLED':true on response)
             *
             */
            {"Session Module Only - FAILURE:SEND_SUCCESS",
                moduleParams(SessionAuthModule.class, "SESSION", FAILURE_AUTH_STATUS, SEND_SUCCESS_AUTH_STATUS),
                moduleArray(), 500, false,
                exceptionMatcher(500, containsString("Invalid AuthStatus returned from validateRequest, FAILURE")),
                auditParams("FAILED", "", false, entry("SessionAuthModule", "FAILED",
                        MapEntry.entry("message", "Invalid AuthStatus returned from validateRequest, FAILURE")))
            },
            /**
             * Session Module Only - null:SEND_SUCCESS
             *
             * Set up:
             * * Session Module configured
             * * No Auth Modules configured
             * * Session Module #validateRequest will return {@code null}
             * * Session Module #secureResponse will return SEND_SUCCESS (but should not be called)
             *
             *
             * Expected Result:
             * * HTTP 500 status
             * * HTTP response detailing the cause of the failure
             * * Audit Session Module failure
             * * Audit overall result as failure
             * * Audit record does not contain principal
             * * Audit record does not contain session id
//                 * * No state cookie on response
             * * Requested resource not called (resource will set header 'RESOURCE_CALLED':true on response)
             *
             */
            {"Session Module Only - null:SEND_SUCCESS",
                moduleParams(SessionAuthModule.class, "SESSION", NULL_AUTH_STATUS, SEND_SUCCESS_AUTH_STATUS),
                moduleArray(), 500, false,
                exceptionMatcher(500, containsString("Invalid AuthStatus returned from validateRequest, null")),
                auditParams("FAILED", "", false, entry("SessionAuthModule", "FAILED",
                        MapEntry.entry("message", "Invalid AuthStatus returned from validateRequest, null")))
            },
            /**
             * Session Module Only - SUCCESS:SEND_CONTINUE
             *
             * Set up:
             * * Session Module configured
             * * No Auth Modules configured
             * * Session Module #validateRequest will return SUCCESS
             * * Response will not be committed after calling resource
             * * Session Module #secureResponse will return SEND_CONTINUE
             *
             *
             * Expected Result:
             * * HTTP 200 status
             * * HTTP response from resource
             * * Audit Session Module success
             * * Audit overall result as success
             * * Audit record contains principal
             * * Audit record does not contain session id
//                 * * No state cookie on response
             * * Requested resource called (resource will set header 'RESOURCE_CALLED':true on response)
             *
             */
            {"Session Module Only - SUCCESS:SEND_CONTINUE",
                moduleParams(SessionAuthModule.class, "SESSION", SUCCESS_AUTH_STATUS, SEND_CONTINUE_AUTH_STATUS),
                moduleArray(), 200, true, resourceMatcher(SESSION_MODULE_PRINCIPAL, SESSION_MODULE_CONTEXT_ENTRY),
                auditParams("SUCCESSFUL", SESSION_MODULE_PRINCIPAL, false,
                        entry("SessionAuthModule", "SUCCESSFUL"))
            },
            /**
             * Session Module Only - SUCCESS:SUCCESS
             *
             * Set up:
             * * Session Module configured
             * * No Auth Modules configured
             * * Session Module #validateRequest will return SUCCESS
             * * Response will not be committed after calling resource
             * * Session Module #secureResponse will return SUCCESS
             *
             *
             * Expected Result:
             * * HTTP 500 status
             * * HTTP response detailing the cause of the failure
             * * Audit Session Module success
             * * Audit overall result as success
             * * Audit record contains principal
             * * Audit record does not contain session id
//                 * * No state cookie on response
             * * Requested resource called (resource will set header 'RESOURCE_CALLED':true on response)
             *
             */
            {"Session Module Only - SUCCESS:SUCCESS",
                moduleParams(SessionAuthModule.class, "SESSION", SUCCESS_AUTH_STATUS, SUCCESS_AUTH_STATUS),
                moduleArray(), 500, true,
                exceptionMatcher(500, containsString("Invalid AuthStatus returned from secureResponse, SUCCESS")),
                auditParams("SUCCESSFUL", SESSION_MODULE_PRINCIPAL, false,
                        entry("SessionAuthModule", "SUCCESSFUL"))
            },
            /**
             * Session Module Only - SUCCESS:FAILURE
             * Set up:
             * * Session Module configured
             * * No Auth Modules configured
             * * Session Module #validateRequest will return SUCCESS
             * * Response will not be committed after calling resource
             * * Session Module #secureResponse will return FAILURE
             *
             *
             * Expected Result:
             * * HTTP 500 status
             * * HTTP response detailing the cause of the failure
             * * Audit Session Module success
             * * Audit overall result as success
             * * Audit record contains principal
             * * Audit record does not contain session id
//                 * * No state cookie on response
             * * Requested resource called (resource will set header 'RESOURCE_CALLED':true on response)
             *
             */
            {"Session Module Only - SUCCESS:FAILURE",
                moduleParams(SessionAuthModule.class, "SESSION", SUCCESS_AUTH_STATUS, FAILURE_AUTH_STATUS),
                moduleArray(), 500, true,
                exceptionMatcher(500, containsString("Invalid AuthStatus returned from secureResponse, FAILURE")),
                auditParams("SUCCESSFUL", SESSION_MODULE_PRINCIPAL, false,
                        entry("SessionAuthModule", "SUCCESSFUL"))
            },
            /**
             * Session Module Only - SUCCESS:null
             *
             * Set up:
             * * Session Module configured
             * * No Auth Modules configured
             * * Session Module #validateRequest will return SUCCESS
             * * Response will not be committed after calling resource
             * * Session Module #secureResponse will return {@code null}
             *
             *
             * Expected Result:
             * * HTTP 500 status
             * * HTTP response detailing the cause of the failure
             * * Audit Session Module success
             * * Audit overall result as success
             * * Audit record contains principal
             * * Audit record does not contain session id
//                 * * No state cookie on response
             * * Requested resource called (resource will set header 'RESOURCE_CALLED':true on response)
             *
             */
            {"Session Module Only - SUCCESS:null",
                moduleParams(SessionAuthModule.class, "SESSION", SUCCESS_AUTH_STATUS, NULL_AUTH_STATUS),
                moduleArray(), 500, true,
                exceptionMatcher(500, containsString("Invalid AuthStatus returned from secureResponse, null")),
                auditParams("SUCCESSFUL", SESSION_MODULE_PRINCIPAL, false,
                        entry("SessionAuthModule", "SUCCESSFUL"))
            },
        };
    }

    private void sessionModuleOnlyValidUsage(String dataName, AuthModuleParameters sessionModuleParams,
            List<AuthModuleParameters> authModuleParametersList, int expectedResponseStatus,
            boolean expectResourceToBeCalled, Map<JsonPointer, Condition<?>> expectedBody, AuditParameters auditParams)
            throws Exception {
        logger.info("Running sessionModuleOnlyValidUsage test with data set: " + dataName);
        runTest(handler, "/protected/resource", sessionModuleParams, authModuleParametersList, expectedResponseStatus,
                expectResourceToBeCalled, expectedBody, auditParams);
    }

    @Test (dataProvider = "invalidUsage")
    public void sessionModuleOnlyInvalidUsage(String dataName, AuthModuleParameters sessionModuleParams,
            List<AuthModuleParameters> authModuleParametersList, int expectedResponseStatus,
            boolean expectResourceToBeCalled, Map<JsonPointer, Condition<?>> expectedBody, AuditParameters auditParams)
            throws Exception {
        logger.info("Running sessionModuleOnlyInvalidUsage test with data set: " + dataName);
        runTest(handler, "/protected/resource", sessionModuleParams, authModuleParametersList, expectedResponseStatus,
                expectResourceToBeCalled, expectedBody, auditParams);
    }
}
