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

package org.forgerock.caf.authn;

import static org.forgerock.caf.authn.AuditParameters.Entry.*;
import static org.forgerock.caf.authn.AuditParameters.*;
import static org.forgerock.caf.authn.AuthModuleParameters.*;
import static org.forgerock.caf.authn.BodyMatcher.*;
import static org.forgerock.caf.authn.TestFramework.*;
import static org.forgerock.caf.authn.test.modules.AuthModuleOne.*;
import static org.forgerock.caf.authn.test.modules.AuthModuleTwo.*;
import static org.forgerock.caf.authn.test.modules.SessionAuthModule.*;

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
 * Functional tests for the JASPI runtime when configured with a "Session" auth module and two auth modules.
 *
 * @since 1.5.0
 */
@Test(testName = "SessionAndAuthModules")
@GuiceModules(GuiceModule.class)
public class SessionAndAuthModulesIT extends HandlerHolder {

    private final Logger logger = LoggerFactory.getLogger(SessionAndAuthModulesIT.class);

    @DataProvider(name = "validUsage")
    private Object[][] validUsage() {
        return new Object[][]{
            /**
             * Session Module and Two Auth Modules - SEND_SUCCESS:AuthException & AuthException:AuthException &
             * AuthException:AuthException
             *
             * Set up:
             * * Session Module configured
             * * Two Auth Modules configured
             * * Session Module #validateRequest will return SEND_SUCCESS
             * * Session Module #secureResponse will throw AuthException (but should not be called)
             * * Auth Module One #validateRequest will throw AuthException (but should not be called)
             * * Auth Module One #secureResponse will throw AuthException (but should not be called)
             * * Auth Module Two #validateRequest will throw AuthException (but should not be called)
             * * Auth Module Two #secureResponse will throw AuthException (but should not be called)
             *
             *
             * Expected Result:
             * * HTTP 200 status
             * * Audit Session Module success
             * * Audit overall result as success
             * * Audit record contains Session Module principal
             * * Audit record does not contain session id
//                 * * No state cookie on response
             * * Requested resource not called (resource will set header 'RESOURCE_CALLED':true on response)
             *
             */
            {"Session Module and Two Auth Modules - SS:AE & AE:AE & AE:AE",
                moduleParams(SessionAuthModule.class, "SESSION", SEND_SUCCESS_AUTH_STATUS, null),
                moduleArray(
                        moduleParams(AuthModuleOne.class, "AUTH-MODULE-ONE", null, null),
                        moduleParams(AuthModuleTwo.class, "AUTH-MODULE-TWO", null, null)),
                200, false, noData(),
                auditParams("SUCCESSFUL", SESSION_MODULE_PRINCIPAL, false,
                        entry("SessionAuthModule", "SUCCESSFUL"))
            },
            /**
             * Session Module and Two Auth Modules - SUCCESS:SEND_SUCCESS & AuthException:AuthException &
             * AuthException:AuthException
             *
             * Set up:
             * * Session Module configured
             * * Two Auth Modules configured
             * * Session Module #validateRequest will return SUCCESS
             * * Session Module #secureResponse will return SEND_SUCCESS
             * * Auth Module One #validateRequest will throw AuthException (but should not be called)
             * * Auth Module One #secureResponse will throw AuthException (but should not be called)
             * * Auth Module Two #validateRequest will throw AuthException (but should not be called)
             * * Auth Module Two #secureResponse will throw AuthException (but should not be called)
             *
             *
             * Expected Result:
             * * HTTP 200 status
             * * HTTP response from resource
             * * Audit Session Module success
             * * Audit overall result as success
             * * Audit record contains Session Module principal
             * * Audit record contains session id
//                 * * No state cookie on response
             * * Requested resource will be called (resource will set header 'RESOURCE_CALLED':true on response)
             *
             */
            {"Session Module and Two Auth Modules - S:SS & AE:AE & AE:AE",
                moduleParams(SessionAuthModule.class, "SESSION", SUCCESS_AUTH_STATUS, SEND_SUCCESS_AUTH_STATUS),
                moduleArray(
                        moduleParams(AuthModuleOne.class, "AUTH-MODULE-ONE", null, null),
                        moduleParams(AuthModuleTwo.class, "AUTH-MODULE-TWO", null, null)),
                200, true, resourceMatcher(SESSION_MODULE_PRINCIPAL, SESSION_MODULE_CONTEXT_ENTRY),
                auditParams("SUCCESSFUL", SESSION_MODULE_PRINCIPAL, true,
                        entry("SessionAuthModule", "SUCCESSFUL"))
            },
            /**
             * Session Module and Two Auth Modules - SEND_CONTINUE:AuthException &
             * AuthException:AuthException & AuthException:AuthException
             *
             * Set up:
             * * Session Module configured
             * * Two Auth Modules configured
             * * Session Module #validateRequest will return SEND_CONTINUE
             * * Session Module #secureResponse will throw AuthException (but should not be called)
             * * Auth Module One #validateRequest will throw AuthException (but should not be called)
             * * Auth Module One #secureResponse will throw AuthException (but should not be called)
             * * Auth Module Two #validateRequest will throw AuthException (but should not be called)
             * * Auth Module Two #secureResponse will throw AuthException (but should not be called)
             *
             *
             * Expected Result:
//                * ** HTTP response requesting more information from the client (contents of response are out of scope)
             * ** No auditing to occur
//                 * ** State cookie on response
             * * Requested resource not called (resource will set header 'RESOURCE_CALLED':true on response)
             *
             */
            {"Session Module and Two Auth Modules - SC:AE & AE:AE & AE:AE",
                moduleParams(SessionAuthModule.class, "SESSION", SEND_CONTINUE_AUTH_STATUS, null),
                moduleArray(
                    moduleParams(AuthModuleOne.class, "AUTH-MODULE-ONE", null, null),
                    moduleParams(AuthModuleTwo.class, "AUTH-MODULE-TWO", null, null)), 200, false,
                noData(), null
            },
            /**
             * Session Module and Two Auth Modules - SEND_FAILURE:AuthException & SEND_SUCCESS:AuthException &
             * AuthException:AuthException
             *
             * Set up:
             * * Session Module configured
             * * Two Auth Modules configured
             * * Session Module #validateRequest will return SEND_FAILURE
             * * Session Module #secureResponse will throw AuthException (but should not be called)
             * * Auth Module One #validateRequest will SEND_SUCCESS
             * * Auth Module One #secureResponse will throw AuthException (but should not be called)
             * * Auth Module Two #validateRequest will throw AuthException (but should not be called)
             * * Auth Module Two #secureResponse will throw AuthException (but should not be called)
             *
             *
             * Expected Result:
             * * HTTP 200 status
             * * Audit Session Module success
             * * Audit Auth Module One success
             * * Audit overall result as success
             * * Audit record contains Auth Module One principal
             * * Audit record does not contain session id
//                 * * No state cookie on response
             * * Requested resource not called (resource will set header 'RESOURCE_CALLED':true on response)
             *
             */
            {"Session Module and Two Auth Modules - SF:AE & SS:AE & AE:AE",
                moduleParams(SessionAuthModule.class, "SESSION", SEND_FAILURE_AUTH_STATUS, null),
                moduleArray(
                        moduleParams(AuthModuleOne.class, "AUTH-MODULE-ONE", SEND_SUCCESS_AUTH_STATUS, null),
                        moduleParams(AuthModuleTwo.class, "AUTH-MODULE-TWO", null, null)),
                200, false, noData(),
                auditParams("SUCCESSFUL", AUTH_MODULE_ONE_PRINCIPAL, false,
                        entry("SessionAuthModule", "FAILED"), entry("AuthModuleOne", "SUCCESSFUL"))
            },
            /**
             * Session Module and Two Auth Modules - SEND_FAILURE:AuthException & SEND_CONTINUE:AuthException &
             * AuthException:AuthException
             *
             * Set up:
             * * Session Module configured
             * * Two Auth Modules configured
             * * Session Module #validateRequest will return SEND_FAILURE
             * * Session Module #secureResponse will throw AuthException (but should not be called)
             * * Auth Module One #validateRequest will return SEND_CONTINUE
             * * Auth Module One #secureResponse will throw AuthException (but should not be called)
             * * Auth Module Two #validateRequest will throw AuthException (but should not be called)
             * * Auth Module Two #secureResponse will throw AuthException (but should not be called)
             *
             *
             * Expected Result:
//                * ** HTTP response requesting more information from the client (contents of response are out of scope)
             * ** No auditing to occur
//                 * ** State cookie on response
             * * Requested resource not called (resource will set header 'RESOURCE_CALLED':true on response)
             *
             */
            {"Session Module and Two Auth Modules - SF:AE & SC:AE & AE:AE",
                moduleParams(SessionAuthModule.class, "SESSION", SEND_CONTINUE_AUTH_STATUS, null),
                moduleArray(
                    moduleParams(AuthModuleOne.class, "AUTH-MODULE-ONE", null, null),
                    moduleParams(AuthModuleTwo.class, "AUTH-MODULE-TWO", null, null)), 200, false,
                noData(), null
            },
            /**
             * Session Module and Two Auth Modules - SEND_FAILURE:SEND_SUCCESS & SUCCESS:SEND_SUCCESS &
             * AuthException:AuthException
             *
             * Set up:
             * * Session Module configured
             * * Two Auth Modules configured
             * * Session Module #validateRequest will return SUCCESS
             * * Session Module #secureResponse will return SEND_SUCCESS
             * * Auth Module One #validateRequest will SUCCESS
             * * Auth Module One #secureResponse will return SEND_SUCCESS
             * * Auth Module Two #validateRequest will throw AuthException (but should not be called)
             * * Auth Module Two #secureResponse will throw AuthException (but should not be called)
             *
             *
             * Expected Result:
             * * HTTP 200 status
             * * Audit Session Module success
             * * Audit Auth Module One success
             * * Audit overall result as success
             * * Audit record contains Auth Module One principal
             * * Audit record contains session id
//                 * * No state cookie on response
             * * Requested resource will be called (resource will set header 'RESOURCE_CALLED':true on response)
             *
             */
            {"Session Module and Two Auth Modules - SF:SS & S:SS & AE:AE",
                moduleParams(SessionAuthModule.class, "SESSION", SEND_FAILURE_AUTH_STATUS,
                        SEND_SUCCESS_AUTH_STATUS),
                moduleArray(
                        moduleParams(AuthModuleOne.class, "AUTH-MODULE-ONE", SUCCESS_AUTH_STATUS,
                                SEND_SUCCESS_AUTH_STATUS),
                        moduleParams(AuthModuleTwo.class, "AUTH-MODULE-TWO", null, null)),
                200, true, resourceMatcher(AUTH_MODULE_ONE_PRINCIPAL, SESSION_MODULE_CONTEXT_ENTRY,
                    AUTH_MODULE_ONE_CONTEXT_ENTRY),
                auditParams("SUCCESSFUL", AUTH_MODULE_ONE_PRINCIPAL, true, entry("SessionAuthModule", "FAILED"),
                        entry("AuthModuleOne", "SUCCESSFUL"))
            },
            /**
             * Session Module and Two Auth Modules - SEND_FAILURE:AuthException & SEND_FAILURE:AuthException &
             * SEND_SUCCESS:AuthException
             *
             * Set up:
             * * Session Module configured
             * * Two Auth Modules configured
             * * Session Module #validateRequest will return SEND_FAILURE
             * * Session Module #secureResponse will throw AuthException (but should not be called)
             * * Auth Module One #validateRequest will SEND_FAILURE
             * * Auth Module One #secureResponse will throw AuthException (but should not be called)
             * * Auth Module Two #validateRequest will return SEND_SUCCESS
             * * Auth Module Two #secureResponse will throw AuthException (but should not be called)
             *
             *
             * Expected Result:
             * * HTTP 200 status
             * * Audit Session Module success
             * * Audit Auth Module One failure
             * * Audit Auth Module Two success
             * * Audit overall result as success
             * * Audit record contains Auth Module Two principal
             * * Audit record does not contain session id
//                 * * No state cookie on response
             * * Requested resource not called (resource will set header 'RESOURCE_CALLED':true on response)
             *
             */
            {"Session Module and Two Auth Modules - SF:AE & SF:AE & SS:AE",
                moduleParams(SessionAuthModule.class, "SESSION", SEND_FAILURE_AUTH_STATUS, null),
                moduleArray(
                        moduleParams(AuthModuleOne.class, "AUTH-MODULE-ONE", SEND_FAILURE_AUTH_STATUS, null),
                        moduleParams(AuthModuleTwo.class, "AUTH-MODULE-TWO", SEND_SUCCESS_AUTH_STATUS, null)),
                200, false, noData(),
                auditParams("SUCCESSFUL", AUTH_MODULE_TWO_PRINCIPAL, false,
                        entry("SessionAuthModule", "FAILED"), entry("AuthModuleOne", "FAILED"),
                        entry("AuthModuleTwo", "SUCCESSFUL"))
            },
            /**
             * Session Module and Two Auth Modules - SEND_FAILURE:AuthException & SEND_FAILURE:AuthException &
             * SEND_CONTINUE:AuthException
             *
             * Set up:
             * * Session Module configured
             * * Two Auth Modules configured
             * * Session Module #validateRequest will return SEND_FAILURE
             * * Session Module #secureResponse will throw AuthException (but should not be called)
             * * Auth Module One #validateRequest will SEND_FAILURE
             * * Auth Module One #secureResponse will throw AuthException (but should not be called)
             * * Auth Module Two #validateRequest will return SEND_CONTINUE
             * * Auth Module Two #secureResponse will throw AuthException (but should not be called)
             *
             *
             * Expected Result:
//                * ** HTTP response requesting more information from the client (contents of response are out of scope)
             * ** No auditing to occur
//                 * ** State cookie on response
             * * Requested resource not called (resource will set header 'RESOURCE_CALLED':true on response)
             *
             */
            {"Session Module and Two Auth Modules - SF:AE & SF:AE & SC:AE",
                moduleParams(SessionAuthModule.class, "SESSION", SEND_FAILURE_AUTH_STATUS, null),
                moduleArray(
                    moduleParams(AuthModuleOne.class, "AUTH-MODULE-ONE", SEND_FAILURE_AUTH_STATUS, null),
                    moduleParams(AuthModuleTwo.class, "AUTH-MODULE-TWO", SEND_CONTINUE_AUTH_STATUS, null)), 200,
                false, noData(), null
            },
            /**
             * Session Module and Two Auth Modules - SEND_FAILURE:AuthException & SEND_FAILURE:AuthException &
             * SEND_FAILURE:AuthException
             *
             * Set up:
             * * Session Module configured
             * * Two Auth Modules configured
             * * Session Module #validateRequest will return SEND_FAILURE
             * * Session Module #secureResponse will throw AuthException (but should not be called)
             * * Auth Module One #validateRequest will SEND_FAILURE
             * * Auth Module One #secureResponse will throw AuthException (but should not be called)
             * * Auth Module Two #validateRequest will return SEND_FAILURE
             * * Auth Module Two #secureResponse will throw AuthException (but should not be called)
             *
             *
             * Expected Result:
             * * HTTP 401 status
             * * Audit Session Module failure
             * * Audit Auth Module One failure
             * * Audit Auth Module Two failure
             * * Audit overall result as failure
             * * Audit record does not contain principal
             * * Audit record does not contain session id
//                 * * No state cookie on response
             * * Requested resource not called (resource will set header 'RESOURCE_CALLED':true on response)
             *
             */
            {"Session Module and Two Auth Modules - SF:AE & SF:AE & SF:AE",
                moduleParams(SessionAuthModule.class, "SESSION", SEND_FAILURE_AUTH_STATUS, null),
                moduleArray(
                        moduleParams(AuthModuleOne.class, "AUTH-MODULE-ONE", SEND_FAILURE_AUTH_STATUS, null),
                        moduleParams(AuthModuleTwo.class, "AUTH-MODULE-TWO", SEND_FAILURE_AUTH_STATUS, null)),
                401, false, exceptionMatcher(401),
                auditParams("FAILED", "", false, entry("SessionAuthModule", "FAILED"),
                        entry("AuthModuleOne", "FAILED"), entry("AuthModuleTwo", "FAILED"))
            },
            /**
             * Session Module and Two Auth Modules - SEND_FAILURE:AuthException & SEND_FAILURE:AuthException &
             * SUCCESS:SEND_FAILURE
             *
             * Set up:
             * * Session Module configured
             * * Two Auth Modules configured
             * * Session Module #validateRequest will return SEND_FAILURE
             * * Session Module #secureResponse will throw AuthException (but should not be called)
             * * Auth Module One #validateRequest will SEND_FAILURE
             * * Auth Module One #secureResponse will throw AuthException (but should not be called)
             * * Auth Module Two #validateRequest will return SUCCESS
             * * Auth Module Two #secureResponse will return SEND_FAILURE
             *
             *
             * Expected Result:
             * * HTTP 500 status
             * * Audit Session Module failure
             * * Audit Auth Module One failure
             * * Audit Auth Module Two success
             * * Audit overall result as success
             * * Audit record contains Auth Module Two principal
             * * Audit record does not contain session id
//                 * * No state cookie on response
             * * Requested resource will be called (resource will set header 'RESOURCE_CALLED':true on response)
             *
             */
            {"Session Module and Two Auth Modules - SF:AE & SF:AE & S:SF",
                moduleParams(SessionAuthModule.class, "SESSION", SEND_FAILURE_AUTH_STATUS, null),
                moduleArray(
                        moduleParams(AuthModuleOne.class, "AUTH-MODULE-ONE", SEND_FAILURE_AUTH_STATUS, null),
                        moduleParams(AuthModuleTwo.class, "AUTH-MODULE-TWO", SUCCESS_AUTH_STATUS,
                                SEND_FAILURE_AUTH_STATUS)),
                500, true, resourceMatcher(AUTH_MODULE_TWO_PRINCIPAL, SESSION_MODULE_CONTEXT_ENTRY,
                    AUTH_MODULE_TWO_CONTEXT_ENTRY),
                auditParams("SUCCESSFUL", AUTH_MODULE_TWO_PRINCIPAL, false,
                        entry("SessionAuthModule", "FAILED"), entry("AuthModuleOne", "FAILED"),
                        entry("AuthModuleTwo", "SUCCESSFUL"))
            },
            /**
             * Session Module and Two Auth Modules - SEND_FAILURE:SEND_FAILURE & SEND_FAILURE:AuthException &
             * SUCCESS:SEND_SUCCESS
             *
             * Set up:
             * * Session Module configured
             * * Two Auth Modules configured
             * * Session Module #validateRequest will return SEND_FAILURE
             * * Session Module #secureResponse will return SEND_FAILURE
             * * Auth Module One #validateRequest will SEND_FAILURE
             * * Auth Module One #secureResponse will throw AuthException (but should not be called)
             * * Auth Module Two #validateRequest will return SUCCESS
             * * Auth Module Two #secureResponse will return SEND_SUCCESS
             *
             *
             * Expected Result:
             * * HTTP 500 status
             * * Audit Session Module failure
             * * Audit Auth Module One failure
             * * Audit Auth Module Two success
             * * Audit overall result as success
             * * Audit record contains Auth Module Two principal
             * * Audit record does not contain session id
//                 * * No state cookie on response
             * * Requested resource will be called (resource will set header 'RESOURCE_CALLED':true on response)
             *
             */
            {"Session Module and Two Auth Modules - SF:SF & SF:AE & S:SS",
                moduleParams(SessionAuthModule.class, "SESSION", SEND_FAILURE_AUTH_STATUS,
                        SEND_FAILURE_AUTH_STATUS),
                moduleArray(
                        moduleParams(AuthModuleOne.class, "AUTH-MODULE-ONE", SEND_FAILURE_AUTH_STATUS, null),
                        moduleParams(AuthModuleTwo.class, "AUTH-MODULE-TWO", SUCCESS_AUTH_STATUS,
                                SEND_SUCCESS_AUTH_STATUS)),
                500, true, resourceMatcher(AUTH_MODULE_TWO_PRINCIPAL, SESSION_MODULE_CONTEXT_ENTRY,
                    AUTH_MODULE_TWO_CONTEXT_ENTRY),
                auditParams("SUCCESSFUL", AUTH_MODULE_TWO_PRINCIPAL, false,
                        entry("SessionAuthModule", "FAILED"), entry("AuthModuleOne", "FAILED"),
                        entry("AuthModuleTwo", "SUCCESSFUL"))
            },
            /**
             * Session Module and Two Auth Modules - SEND_FAILURE:SEND_SUCCESS & SEND_FAILURE:AuthException &
             * SUCCESS:SEND_SUCCESS
             *
             * Set up:
             * * Session Module configured
             * * Two Auth Modules configured
             * * Session Module #validateRequest will return SEND_FAILURE
             * * Session Module #secureResponse will return SEND_SUCCESS
             * * Auth Module One #validateRequest will SEND_FAILURE
             * * Auth Module One #secureResponse will throw AuthException (but should not be called)
             * * Auth Module Two #validateRequest will return SUCCESS
             * * Auth Module Two #secureResponse will return SEND_SUCCESS
             *
             *
             * Expected Result:
             * * HTTP 200 status
             * * Audit Session Module failure
             * * Audit Auth Module One failure
             * * Audit Auth Module Two success
             * * Audit overall result as success
             * * Audit record contains Auth Module Two principal
             * * Audit record contains session id
//                 * * No state cookie on response
             * * Requested resource will be called (resource will set header 'RESOURCE_CALLED':true on response)
             *
             */
            {"Session Module and Two Auth Modules - SF:SS & SF:AE & S:SS",
                moduleParams(SessionAuthModule.class, "SESSION", SEND_FAILURE_AUTH_STATUS,
                        SEND_SUCCESS_AUTH_STATUS),
                moduleArray(
                        moduleParams(AuthModuleOne.class, "AUTH-MODULE-ONE", SEND_FAILURE_AUTH_STATUS, null),
                        moduleParams(AuthModuleTwo.class, "AUTH-MODULE-TWO", SUCCESS_AUTH_STATUS,
                                SEND_SUCCESS_AUTH_STATUS)),
                200, true, resourceMatcher(AUTH_MODULE_TWO_PRINCIPAL, SESSION_MODULE_CONTEXT_ENTRY,
                    AUTH_MODULE_TWO_CONTEXT_ENTRY),
                auditParams("SUCCESSFUL", AUTH_MODULE_TWO_PRINCIPAL, true, entry("SessionAuthModule", "FAILED"),
                        entry("AuthModuleOne", "FAILED"),
                        entry("AuthModuleTwo", "SUCCESSFUL"))
            },
        };
    }

    @Test (dataProvider = "validUsage")
    public void sessionAndAuthModulesValidUsage(String dataName, AuthModuleParameters sessionModuleParams,
            List<AuthModuleParameters> authModuleParametersList, int expectedResponseStatus,
            boolean expectResourceToBeCalled, Map<JsonPointer, Condition<?>> expectedBody, AuditParameters auditParams)
            throws Exception {
        logger.info("Running sessionAndAuthModulesValidUsage test with data set: " + dataName);
        runTest(handler, "/protected/resource", sessionModuleParams, authModuleParametersList, expectedResponseStatus,
                expectResourceToBeCalled, expectedBody, auditParams);
    }
}
