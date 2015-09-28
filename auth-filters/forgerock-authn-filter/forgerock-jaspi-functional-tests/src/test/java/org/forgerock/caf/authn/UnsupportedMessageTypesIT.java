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

import static org.forgerock.caf.authn.AuditParameters.*;
import static org.forgerock.caf.authn.AuthModuleParameters.*;
import static org.forgerock.caf.authn.BodyMatcher.*;
import static org.forgerock.caf.authn.TestFramework.*;

import java.util.List;
import java.util.Map;

import org.assertj.core.api.Condition;
import org.forgerock.caf.authn.test.modules.AuthModuleUnsupportedMessageTypes;
import org.forgerock.caf.authn.test.runtime.GuiceModule;
import org.forgerock.guice.core.GuiceModules;
import org.forgerock.json.JsonPointer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Functional tests for the JASPI runtime when the configured auth modules do not support the required message types.
 *
 * @since 1.5.0
 */
@Test(testName = "UnsupportedMessageTypes")
@GuiceModules(GuiceModule.class)
public class UnsupportedMessageTypesIT extends HandlerHolder {

    private final Logger logger = LoggerFactory.getLogger(UnsupportedMessageTypesIT.class);

    @DataProvider(name = "unsupportedMessageTypesData")
    private Object[][] unsupportedMessageTypesData() {
        return new Object[][]{
            /**
             * Session Module Only - SUCCESS:SEND_SUCCESS
             *
             * Set up:
             * * Session Module configured
             * * No Auth Modules configured
             * * Session Module #validateRequest will return SUCCESS
             * * Response will be committed after calling resource
             * * Session Module #secureResponse will return SEND_SUCCESS
             * * Resource will set response status to 201, set header and cause response to be committed
             *
             *
             * Expected Result:
             * * HTTP 500 status
             * * HTTP response detailing the cause of the failure
//                 * * Audit Session Module failure
             * * Audit overall result as failure
//                 * * No state cookie on response
             * * Requested not resource called (resource will set header 'RESOURCE_CALLED':true on response)
             *
             */
            {"Session Module Only - Unsupported Message Types",
                moduleParams(AuthModuleUnsupportedMessageTypes.class, null, null, null),
                moduleArray(), 500, false, exceptionMatcher(500),
                auditParams(null, null, false)
            },
            /**
             * Single Auth Module Only - SUCCESS:SEND_FAILURE
             *
             * Set up:
             * * No Session Module configured
             * * Single Auth Module configured
             * * Auth Module #validateRequest will return SUCCESS
             * * Response will be committed after calling resource
             * * Auth Module #secureResponse will return SEND_SUCCESS
             * * Resource will set response status to 201, set header and cause response to be committed
             *
             *
             * Expected Result:
             * * HTTP 500 status
             * * HTTP response detailing the cause of the failure
//                 * * Audit Auth Module failure
             * * Audit overall result as failure
//                 * * No state cookie on response
             * * Requested not resource called (resource will set header 'RESOURCE_CALLED':true on response)
             *
             */
            {"Single Auth Module Only - Unsupported Message Types",
                null,
                moduleArray(moduleParams(AuthModuleUnsupportedMessageTypes.class, null, null, null)), 500,
                false,
                exceptionMatcher(500),
                auditParams(null, null, false)
            },
        };
    }

    @Test (enabled = false, dataProvider = "unsupportedMessageTypesData")
    public void unsupportedMessageTypes(String dataName,
            AuthModuleParameters sessionModuleParams, List<AuthModuleParameters> authModuleParametersList,
            int expectedResponseStatus, boolean expectResourceToBeCalled, Map<JsonPointer, Condition<?>> expectedBody,
            AuditParameters auditParams) throws Exception {
        logger.info("Running unsupportedMessageTypes test with data set: " + dataName);
        runTest(handler, "/protected/resource", sessionModuleParams, authModuleParametersList,
                expectedResponseStatus, expectResourceToBeCalled, expectedBody, auditParams);
    }
}
