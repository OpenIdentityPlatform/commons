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

import org.forgerock.caf.authn.test.modules.SessionAuthModule;
import org.hamcrest.Matcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;

import static org.forgerock.caf.authn.AuditParameters.auditParams;
import static org.forgerock.caf.authn.AuthModuleParameters.moduleArray;
import static org.forgerock.caf.authn.AuthModuleParameters.moduleParams;
import static org.forgerock.caf.authn.BodyMatcher.exceptionMatcher;
import static org.forgerock.caf.authn.BodyMatcher.noData;
import static org.forgerock.caf.authn.BodyMatcher.resourceMatcher;
import static org.forgerock.caf.authn.TestFramework.runTest;
import static org.forgerock.caf.authn.TestFramework.setUpConnection;
import static org.forgerock.caf.authn.test.modules.SessionAuthModule.*;
import static org.hamcrest.Matchers.containsString;

/**
 * Functional tests for the JASPI runtime when configured with just a "Session" auth module.
 *
 * @since 1.5.0
 */
@Test(testName = "SessionModuleOnly")
public class SessionModuleOnlyIT {

    private final Logger logger = LoggerFactory.getLogger(SessionModuleOnlyIT.class);

    @BeforeClass
    public void setUp() {
        setUpConnection();
    }

    @DataProvider(name = "validUsage")
    private Object[][] validUsage() {
        return new Object[][]{
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
            {
                "No Modules", null, moduleArray(), 401, false, exceptionMatcher(401), auditParams("FAILURE")
            },
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
//                 * * Does not audit Session Module success
             * * Does not audit overall result as success
//                 * * No state cookie on response
             * * Requested resource not called (resource will set header 'RESOURCE_CALLED':true on response)
             *
             */
            {"Session Module Only - SEND_SUCCESS:AuthException",
                moduleParams(SessionAuthModule.class, "SESSION", SEND_SUCCESS_AUTH_STATUS, null),
                moduleArray(), 200, false, noData(), null
            },
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
//                 * * Audit Session Module failure
             * * Audit overall result as failure
//                 * * No state cookie on response
             * * Requested resource not called (resource will set header 'RESOURCE_CALLED':true on response)
             *
             */
            {"Session Module Only - SEND_FAILURE:AuthException",
                moduleParams(SessionAuthModule.class, "SESSION", SEND_FAILURE_AUTH_STATUS, null),
                moduleArray(), 401, false, exceptionMatcher(401), auditParams("FAILURE")
            },
            /**
             * Session Module Only - SEND_CONTINUE:AuthException
             *
             * Set up:
             * * Session Module configured
             * * No Auth Modules configured
             * * Session Module #validateRequest will return SEND_CONTINUE
//                 * * Session Module will set HTTP 100 status
             * * Session Module #secureResponse will throw AuthException (but should not be called)
             *
             *
             * Expected Result:
//                * ** HTTP response requesting more information from the client (contents of response are out of scope)
             * ** No auditing to occur
//                 * ** State cookie on response
             * * Requested resource not called (resource will set header 'RESOURCE_CALLED':true on response)
             *
             */
            {"Session Module Only - SEND_CONTINUE:AuthException",
                moduleParams(SessionAuthModule.class, "SESSION", SEND_CONTINUE_AUTH_STATUS, null),
                moduleArray(), 200, false, noData(), null
            },
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
//                 * * Audit Session Module failure
             * * Audit overall result as failure
//                 * * No state cookie on response
             * * Requested resource not called (resource will set header 'RESOURCE_CALLED':true on response)
             *
             */
            {"Session Module Only - AuthException:SEND_SUCCESS",
                moduleParams(SessionAuthModule.class, "SESSION", null, SEND_SUCCESS_AUTH_STATUS),
                moduleArray(), 500, false, exceptionMatcher(500), auditParams("FAILURE")
            },
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
//                 * * Does not audit Session Module success
             * * Does not audit overall result as success
//                 * * No state cookie on response
             * * Requested resource called (resource will set header 'RESOURCE_CALLED':true on response)
             *
             */
            {"Session Module Only - SUCCESS:SEND_SUCCESS",
                moduleParams(SessionAuthModule.class, "SESSION", SUCCESS_AUTH_STATUS, SEND_SUCCESS_AUTH_STATUS),
                moduleArray(), 200, true, resourceMatcher(SESSION_MODULE_PRINCIPAL, SESSION_MODULE_CONTEXT_ENTRY),
                null
            },
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
//                 * * Does not audit Session Module success
             * * Does not audit overall result as success
//                 * * No state cookie on response
             * * Requested resource called (resource will set header 'RESOURCE_CALLED':true on response)
             *
             */
            {"Session Module Only - SUCCESS:SEND_FAILURE",
                moduleParams(SessionAuthModule.class, "SESSION", SUCCESS_AUTH_STATUS, SEND_FAILURE_AUTH_STATUS),
                moduleArray(), 500, true,
                resourceMatcher(SESSION_MODULE_PRINCIPAL, SESSION_MODULE_CONTEXT_ENTRY),
                null
            },
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
             * * HTTP response from resource
//                 * * Does not audit Session Module success
             * * Does not audit overall result as success
//                 * * No state cookie on response
             * * Requested resource called (resource will set header 'RESOURCE_CALLED':true on response)
             *
             */
            {"Session Module Only - SUCCESS:AuthException",
                moduleParams(SessionAuthModule.class, "SESSION", SUCCESS_AUTH_STATUS, null),
                moduleArray(), 500, true,
                resourceMatcher(SESSION_MODULE_PRINCIPAL, SESSION_MODULE_CONTEXT_ENTRY),
                null
            },
        };
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
//                 * * Does not audit Session Module failure
             * * Does not audit overall result as failure
//                 * * No state cookie on response
             * * Requested resource not called (resource will set header 'RESOURCE_CALLED':true on response)
             *
             */
            {"Session Module Only - FAILURE:SEND_SUCCESS",
                moduleParams(SessionAuthModule.class, "SESSION", FAILURE_AUTH_STATUS, SEND_SUCCESS_AUTH_STATUS),
                moduleArray(), 500, false,
                exceptionMatcher(500, containsString("Invalid AuthStatus returned from validateRequest, FAILURE")),
                null
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
//                 * * Does not audit Session Module failure
             * * Does not audit overall result as failure
//                 * * No state cookie on response
             * * Requested resource not called (resource will set header 'RESOURCE_CALLED':true on response)
             *
             */
            {"Session Module Only - null:SEND_SUCCESS",
                moduleParams(SessionAuthModule.class, "SESSION", NULL_AUTH_STATUS, SEND_SUCCESS_AUTH_STATUS),
                moduleArray(), 500, false,
                exceptionMatcher(500, containsString("Invalid AuthStatus returned from validateRequest, null")),
                null
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
//                 * * Does not audit Session Module success
             * * Does not audit overall result as success
//                 * * No state cookie on response
             * * Requested resource called (resource will set header 'RESOURCE_CALLED':true on response)
             *
             */
            {"Session Module Only - SUCCESS:SEND_CONTINUE",
                moduleParams(SessionAuthModule.class, "SESSION", SUCCESS_AUTH_STATUS, SEND_CONTINUE_AUTH_STATUS),
                moduleArray(), 200, true, resourceMatcher(SESSION_MODULE_PRINCIPAL, SESSION_MODULE_CONTEXT_ENTRY), null
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
             * * HTTP response from resource
//                 * * Does not audit Session Module success
             * * Does not audit overall result as success
//                 * * No state cookie on response
             * * Requested resource called (resource will set header 'RESOURCE_CALLED':true on response)
             *
             */
            {"Session Module Only - SUCCESS:SUCCESS",
                moduleParams(SessionAuthModule.class, "SESSION", SUCCESS_AUTH_STATUS, SUCCESS_AUTH_STATUS),
                moduleArray(), 500, true,
                resourceMatcher(SESSION_MODULE_PRINCIPAL, SESSION_MODULE_CONTEXT_ENTRY),
                null
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
             * * HTTP response from resource
//                 * * Does not audit Session Module success
             * * Does not audit overall result as success
//                 * * No state cookie on response
             * * Requested resource called (resource will set header 'RESOURCE_CALLED':true on response)
             *
             */
            {"Session Module Only - SUCCESS:FAILURE",
                moduleParams(SessionAuthModule.class, "SESSION", SUCCESS_AUTH_STATUS, FAILURE_AUTH_STATUS),
                moduleArray(), 500, true,
                resourceMatcher(SESSION_MODULE_PRINCIPAL, SESSION_MODULE_CONTEXT_ENTRY),
                null
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
             * * HTTP response from resource
//                 * * Does not audit Session Module success
             * * Does not audit overall result as success
//                 * * No state cookie on response
             * * Requested resource called (resource will set header 'RESOURCE_CALLED':true on response)
             *
             */
            {"Session Module Only - SUCCESS:null",
                moduleParams(SessionAuthModule.class, "SESSION", SUCCESS_AUTH_STATUS, NULL_AUTH_STATUS),
                moduleArray(), 500, true,
                resourceMatcher(SESSION_MODULE_PRINCIPAL, SESSION_MODULE_CONTEXT_ENTRY),
                null
            },
        };
    }

    @Test (dataProvider = "validUsage")
    public void sessionModuleOnlyValidUsage(String dataName, AuthModuleParameters sessionModuleParams,
            List<AuthModuleParameters> authModuleParametersList, int expectedResponseStatus,
            boolean expectResourceToBeCalled, Map<String, Matcher<?>> expectedBody, AuditParameters auditParams) {
        logger.info("Running sessionModuleOnlyValidUsage test with data set: " + dataName);
        runTest("/protected/resource", sessionModuleParams, authModuleParametersList, expectedResponseStatus,
                expectResourceToBeCalled, expectedBody, auditParams);
    }

    @Test (dataProvider = "invalidUsage")
    public void sessionModuleOnlyInvalidUsage(String dataName, AuthModuleParameters sessionModuleParams,
            List<AuthModuleParameters> authModuleParametersList, int expectedResponseStatus,
            boolean expectResourceToBeCalled, Map<String, Matcher<?>> expectedBody, AuditParameters auditParams) {
        logger.info("Running sessionModuleOnlyInvalidUsage test with data set: " + dataName);
        runTest("/protected/resource", sessionModuleParams, authModuleParametersList, expectedResponseStatus,
                expectResourceToBeCalled, expectedBody, auditParams);
    }
}
