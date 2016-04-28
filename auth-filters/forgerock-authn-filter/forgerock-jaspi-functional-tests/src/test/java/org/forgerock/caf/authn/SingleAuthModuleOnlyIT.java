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
import static org.forgerock.caf.authn.test.modules.SessionAuthModule.*;
import static org.hamcrest.Matchers.*;

import java.util.List;
import java.util.Map;

import org.assertj.core.api.Condition;
import org.assertj.core.data.MapEntry;
import org.forgerock.caf.authn.test.modules.AuthModuleOne;
import org.forgerock.caf.authn.test.runtime.GuiceModule;
import org.forgerock.guice.core.GuiceModules;
import org.forgerock.json.JsonPointer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Functional tests for the JASPI runtime when configured with just a single auth module.
 *
 * @since 1.5.0
 */
@Test(testName = "SingleAuthModuleOnly")
@GuiceModules(GuiceModule.class)
public class SingleAuthModuleOnlyIT extends HandlerHolder {

    private final Logger logger = LoggerFactory.getLogger(SingleAuthModuleOnlyIT.class);

    @DataProvider(name = "validUsage")
    private Object[][] validUsage() {
        return new Object[][]{
            /**
             * Single Auth Module Only - SEND_SUCCESS:AuthException
             *
             * Set up:
             * * No Session Module configured
             * * Single Auth Module configured
             * * Auth Module #validateRequest will return SEND_SUCCESS
             * * Auth Module #secureResponse will throw AuthException (but should not be called)
             *
             *
             * Expected Result:
             * * HTTP 200 status
             * * Audit Auth Module success
             * * Audit overall result as success
             * * Audit record contains principal
             * * Audit record does not contain session id
//                 * * No state cookie on response
             * * Requested resource not called (resource will set header 'RESOURCE_CALLED':true on response)
             *
             */
            {"Single Auth Module Only - SEND_SUCCESS:AuthException",
                null, moduleArray(
                    moduleParams(AuthModuleOne.class, "AUTH-MODULE-ONE", SEND_SUCCESS_AUTH_STATUS, null)),
                200, false, noData(),
                auditParams("SUCCESSFUL", AUTH_MODULE_ONE_PRINCIPAL, false,
                        entry("AuthModuleOne", "SUCCESSFUL"))
            },
            /**
             * Single Auth Module Only - SEND_FAILURE:AuthException
             *
             * Set up:
             * * No Session Module configured
             * * Single Auth Module configured
             * * Auth Module #validateRequest will return SEND_FAILURE
             * * Auth Module #secureResponse will throw AuthException (but should not be called)
             *
             *
             * Expected Result:
             * * HTTP 401 status
             * * Audit Auth Module failure
             * * Audit overall result as failure
             * * Audit record contains principal
             * * Audit record does not contain session id
//                 * * No state cookie on response
             * * Requested resource not called (resource will set header 'RESOURCE_CALLED':true on response)
             *
             */
            {"Single Auth Module Only - SEND_FAILURE:AuthException",
                null, moduleArray(
                    moduleParams(AuthModuleOne.class, "AUTH-MODULE-ONE", SEND_FAILURE_AUTH_STATUS, null)),
                401, false, exceptionMatcher(401),
                auditParams("FAILED", "", false, entry("AuthModuleOne", "FAILED"))
            },
            /**
             * Single Auth Module Only - SEND_CONTINUE:AuthException
             *
             * Set up:
             * * No Session Module configured
             * * Single Auth Module configured
             * * Auth Module #validateRequest will return SEND_CONTINUE
//                 * * Auth Module will set HTTP 100 status
             * * Auth Module #secureResponse will throw AuthException (but should not be called)
             *
             *
             * Expected Result:
//                * ** HTTP response requesting more information from the client (contents of response are out of scope)
             * ** No auditing to occur
//                 * ** State cookie on response
             * * Requested resource not called (resource will set header 'RESOURCE_CALLED':true on response)
             *
             */
            {"Single Auth Module Only - SEND_CONTINUE:AuthException",
                null, moduleArray(
                    moduleParams(AuthModuleOne.class, "AUTH-MODULE-ONE", SEND_CONTINUE_AUTH_STATUS, null)),
                200, false, noData(), null
            },
            /**
             * Single Auth Module Only - AuthException:SEND_SUCCESS
             *
             * Set up:
             * * No Session Module configured
             * * Single Auth Module configured
             * * Auth Module #validateRequest will throw AuthException
             * * Auth Module #secureResponse will return SEND_SUCCESS (but should not be called)
             *
             *
             * Expected Result:
             * * HTTP 500 status
             * * HTTP response detailing the cause of the failure
             * * Audit Auth Module failure
             * * Audit overall result as failure
             * * Audit record does not contain principal
             * * Audit record does not contain session id
//                 * * No state cookie on response
             * * Requested resource not called (resource will set header 'RESOURCE_CALLED':true on response)
             *
             */
            {"Single Auth Module Only - AuthException:SEND_SUCCESS",
                null, moduleArray(
                    moduleParams(AuthModuleOne.class, "AUTH-MODULE-ONE", null, SEND_SUCCESS_AUTH_STATUS)),
                500, false, exceptionMatcher(500),
                auditParams("FAILED", "", false, entry("AuthModuleOne", "FAILED",
                        MapEntry.entry("exception", AUTH_MODULE_ONE_VALIDATE_REQUEST_HEADER_NAME
                                + " header not set, so throwing AuthException.")))
            },
            /**
             * Single Auth Module Only - SUCCESS:SEND_SUCCESS
             *
             * Set up:
             * * No Session Module configured
             * * Single Auth Module configured
             * * Auth Module #validateRequest will return SUCCESS
             * * Response will not be committed after calling resource
             * * Auth Module #secureResponse will return SEND_SUCCESS
             *
             *
             * Expected Result:
             * * HTTP 200 status
             * * HTTP response from resource
             * * Audit Auth Module success
             * * Audit overall result as success
             * * Audit record contains principal
             * * Audit record does not contain session id
//                 * * No state cookie on response
             * * Requested resource called (resource will set header 'RESOURCE_CALLED':true on response)
             *
             */
            {"Single Auth Module Only - SUCCESS:SEND_SUCCESS",
                null, moduleArray(
                    moduleParams(AuthModuleOne.class, "AUTH-MODULE-ONE", SUCCESS_AUTH_STATUS,
                        SEND_SUCCESS_AUTH_STATUS)),
                200, true, resourceMatcher(AUTH_MODULE_ONE_PRINCIPAL, AUTH_MODULE_ONE_CONTEXT_ENTRY),
                auditParams("SUCCESSFUL", AUTH_MODULE_ONE_PRINCIPAL, false,
                        entry("AuthModuleOne", "SUCCESSFUL"))
            },
            /**
             * Single Auth Module Only - SUCCESS:SEND_FAILURE
             *
             * Set up:
             * * No Session Module configured
             * * Single Auth Module configured
             * * Auth Module #validateRequest will return SUCCESS
             * * Response will not be committed after calling resource
             * * Auth Module #secureResponse will return SEND_FAILURE
             *
             *
             * Expected Result:
             * * HTTP 500 status
             * * HTTP response from resource
             * * Audit Auth Module success
             * * Audit overall result as success
             * * Audit record contains principal
             * * Audit record does not contain session id
//                 * * No state cookie on response
             * * Requested resource called (resource will set header 'RESOURCE_CALLED':true on response)
             *
             */
            {"Single Auth Module Only - SUCCESS:SEND_FAILURE",
                null, moduleArray(
                    moduleParams(AuthModuleOne.class, "AUTH-MODULE-ONE", SUCCESS_AUTH_STATUS,
                        SEND_FAILURE_AUTH_STATUS)),
                500, true,
                resourceMatcher(AUTH_MODULE_ONE_PRINCIPAL, AUTH_MODULE_ONE_CONTEXT_ENTRY),
                auditParams("SUCCESSFUL", AUTH_MODULE_ONE_PRINCIPAL, false,
                        entry("AuthModuleOne", "SUCCESSFUL"))
            },
            /**
             * Single Auth Module Only - SUCCESS:AuthException
             *
             * Set up:
             * * No Session Module configured
             * * Single Auth Module configured
             * * Auth Module #validateRequest will return SUCCESS
             * * Response will not be committed after calling resource
             * * Auth Module #secureResponse will throw AuthException
             *
             *
             * Expected Result:
             * * HTTP 500 status
             * * HTTP response detailing the cause of the failure
             * * Audit Auth Module success
             * * Audit overall result as success
             * * Audit record contains principal
             * * Audit record does not contain session id
//                 * * No state cookie on response
             * * Requested resource called (resource will set header 'RESOURCE_CALLED':true on response)
             *
             */
            {"Single Auth Module Only - SUCCESS:AuthException",
                null, moduleArray(
                    moduleParams(AuthModuleOne.class, "AUTH-MODULE-ONE", SUCCESS_AUTH_STATUS, null)), 500, true,
                exceptionMatcher(500, containsString("X-JASPI-AUTH-MODULE-ONE-SECURE-RESPONSE header not set, so"
                        + " throwing AuthException.")),
                auditParams("SUCCESSFUL", AUTH_MODULE_ONE_PRINCIPAL, false,
                        entry("AuthModuleOne", "SUCCESSFUL"))
            },
        };
    }

    @DataProvider(name = "invalidUsage")
    private Object[][] invalidUsage() {
        return new Object[][]{
            /**
             * Single Auth Module Only - FAILURE:SEND_SUCCESS
             *
             * Set up:
             * * No Session Module configured
             * * Single Auth Module configured
             * * Auth Module #validateRequest will return FAILURE
             * * Auth Module #secureResponse will return SEND_SUCCESS (but should not be called)
             *
             *
             * Expected Result:
             * * HTTP 500 status
             * * HTTP response detailing the cause of the failure
             * * Audit Auth Module failure
             * * Audit overall result as failure
             * * Audit record does not contain principal
             * * Audit record does not contain session id
//                 * * No state cookie on response
             * * Requested resource not called (resource will set header 'RESOURCE_CALLED':true on response)
             *
             */
            {"Single Auth Module Only - FAILURE:SEND_SUCCESS",
                null, moduleArray(
                    moduleParams(AuthModuleOne.class, "AUTH-MODULE-ONE", FAILURE_AUTH_STATUS,
                        SEND_SUCCESS_AUTH_STATUS)), 500, false,
                exceptionMatcher(500, containsString("Invalid AuthStatus returned from validateRequest, FAILURE")),
                auditParams("FAILED", "", false, entry("AuthModuleOne", "FAILED",
                        MapEntry.entry("message", "Invalid AuthStatus returned from validateRequest, FAILURE")))
            },
            /**
             * Single Auth Module Only - null:SEND_SUCCESS
             *
             * Set up:
             * * No Session Module configured
             * * Single Auth Module configured
             * * Auth Module #validateRequest will return {@code null}
             * * Auth Module #secureResponse will return SEND_SUCCESS (but should not be called)
             *
             *
             * Expected Result:
             * * HTTP 500 status
             * * HTTP response detailing the cause of the failure
             * * Audit Auth Module failure
             * * Audit overall result as failure
             * * Audit record does not contain principal
             * * Audit record does not contain session id
//                 * * No state cookie on response
             * * Requested resource not called (resource will set header 'RESOURCE_CALLED':true on response)
             *
             */
            {"Single Auth Module Only - null:SEND_SUCCESS",
                null, moduleArray(
                    moduleParams(AuthModuleOne.class, "AUTH-MODULE-ONE", NULL_AUTH_STATUS,
                        SEND_SUCCESS_AUTH_STATUS)), 500, false,
                exceptionMatcher(500, containsString("Invalid AuthStatus returned from validateRequest, null")),
                auditParams("FAILED", "", false, entry("AuthModuleOne", "FAILED",
                        MapEntry.entry("message", "Invalid AuthStatus returned from validateRequest, null")))
            },
            /**
             * Single Auth Module Only - SUCCESS:SEND_CONTINUE
             *
             * Set up:
             * * No Session Module configured
             * * Single Auth Module configured
             * * Auth Module #validateRequest will return SUCCESS
             * * Response will not be committed after calling resource
             * * Auth Module #secureResponse will return SEND_CONTINUE
             *
             *
             * Expected Result:
             * * HTTP 200 status
             * * HTTP response from resource
             * * HTTP response detailing the cause of the failure
             * * Audit Auth Module success
             * * Audit overall result as success
             * * Audit record contains principal
             * * Audit record does not contain session id
//                 * * No state cookie on response
             * * Requested resource called (resource will set header 'RESOURCE_CALLED':true on response)
             *
             */
            {"Single Auth Module Only - SUCCESS:SEND_CONTINUE",
                null, moduleArray(
                    moduleParams(AuthModuleOne.class, "AUTH-MODULE-ONE", SUCCESS_AUTH_STATUS,
                        SEND_CONTINUE_AUTH_STATUS)),
                200, true, resourceMatcher(AUTH_MODULE_ONE_PRINCIPAL, AUTH_MODULE_ONE_CONTEXT_ENTRY),
                auditParams("SUCCESSFUL", AUTH_MODULE_ONE_PRINCIPAL, false,
                        entry("AuthModuleOne", "SUCCESSFUL"))
            },
            /**
             * Single Auth Module Only - SUCCESS:SUCCESS
             *
             * Set up:
             * * No Session Module configured
             * * Single Auth Module configured
             * * Auth Module #validateRequest will return SUCCESS
             * * Response will not be committed after calling resource
             * * Auth Module #secureResponse will return SUCCESS
             *
             *
             * Expected Result:
             * * HTTP 500 status
             * * HTTP response detailing the cause of the failure
             * * Audit Auth Module success
             * * Audit overall result as success
             * * Audit record contains principal
             * * Audit record does not contain session id
//                 * * No state cookie on response
             * * Requested resource called (resource will set header 'RESOURCE_CALLED':true on response)
             *
             */
            {"Single Auth Module Only - SUCCESS:SUCCESS",
                null, moduleArray(
                    moduleParams(AuthModuleOne.class, "AUTH-MODULE-ONE", SUCCESS_AUTH_STATUS, SUCCESS_AUTH_STATUS)),
                500, true,
                exceptionMatcher(500, containsString("Invalid AuthStatus returned from secureResponse, SUCCESS")),
                auditParams("SUCCESSFUL", AUTH_MODULE_ONE_PRINCIPAL, false,
                        entry("AuthModuleOne", "SUCCESSFUL"))
            },
            /**
             * Single Auth Module Only - SUCCESS:FAILURE
             * Set up:
             * * No Session Module configured
             * * Single Auth Module configured
             * * Auth Module #validateRequest will return SUCCESS
             * * Response will not be committed after calling resource
             * * Auth Module #secureResponse will return FAILURE
             *
             *
             * Expected Result:
             * * HTTP 500 status
             * * HTTP response detailing the cause of the failure
             * * Audit Auth Module success
             * * Audit overall result as success
             * * Audit record contains principal
             * * Audit record does not contain session id
//                 * * No state cookie on response
             * * Requested resource called (resource will set header 'RESOURCE_CALLED':true on response)
             *
             */
            {"Single Auth Module Only - SUCCESS:FAILURE",
                null, moduleArray(
                    moduleParams(AuthModuleOne.class, "AUTH-MODULE-ONE", SUCCESS_AUTH_STATUS, FAILURE_AUTH_STATUS)),
                500, true,
                exceptionMatcher(500, containsString("Invalid AuthStatus returned from secureResponse, FAILURE")),
                auditParams("SUCCESSFUL", AUTH_MODULE_ONE_PRINCIPAL, false,
                        entry("AuthModuleOne", "SUCCESSFUL"))
            },
            /**
             * Single Auth Module Only - SUCCESS:null
             *
             * Set up:
             * * No Session Module configured
             * * Single Auth Module configured
             * * Auth Module #validateRequest will return SUCCESS
             * * Response will not be committed after calling resource
             * * Auth Module #secureResponse will return {@code null}
             *
             *
             * Expected Result:
             * * HTTP 500 status
             * * HTTP response detailing the cause of the failure
             * * Audit Auth Module success
             * * Audit overall result as success
             * * Audit record contains principal
             * * Audit record does not contain session id
//                 * * No state cookie on response
             * * Requested resource called (resource will set header 'RESOURCE_CALLED':true on response)
             *
             */
            {"Single Auth Module Only - SUCCESS:null",
                null, moduleArray(
                    moduleParams(AuthModuleOne.class, "AUTH-MODULE-ONE", SUCCESS_AUTH_STATUS, NULL_AUTH_STATUS)),
                500, true,
                exceptionMatcher(500, containsString("Invalid AuthStatus returned from secureResponse, null")),
                auditParams("SUCCESSFUL", AUTH_MODULE_ONE_PRINCIPAL, false,
                        entry("AuthModuleOne", "SUCCESSFUL"))
            },
        };
    }

    @Test (dataProvider = "validUsage")
    public void singleAuthModuleOnlyValidUsage(String dataName, AuthModuleParameters sessionModuleParams,
            List<AuthModuleParameters> authModuleParametersList, int expectedResponseStatus,
            boolean expectResourceToBeCalled, Map<JsonPointer, Condition<?>> expectedBody, AuditParameters auditParams)
            throws Exception {
        logger.info("Running singleAuthModuleOnlyValidUsage test with data set: " + dataName);
        runTest(handler, "/protected/resource", sessionModuleParams, authModuleParametersList, expectedResponseStatus,
                expectResourceToBeCalled, expectedBody, auditParams);
    }

    @Test (dataProvider = "invalidUsage")
    public void singleAuthModuleOnlyInvalidUsage(String dataName, AuthModuleParameters sessionModuleParams,
            List<AuthModuleParameters> authModuleParametersList, int expectedResponseStatus,
            boolean expectResourceToBeCalled, Map<JsonPointer, Condition<?>> expectedBody, AuditParameters auditParams)
            throws Exception {
        logger.info("Running singleAuthModuleOnlyInvalidUsage test with data set: " + dataName);
        runTest(handler, "/protected/resource", sessionModuleParams, authModuleParametersList, expectedResponseStatus,
                expectResourceToBeCalled, expectedBody, auditParams);
    }
}
