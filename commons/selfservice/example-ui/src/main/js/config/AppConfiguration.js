/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2015 ForgeRock AS. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 */

/*global define*/

define("config/AppConfiguration", [
    "org/forgerock/commons/ui/common/util/Constants"
], function (Constants) {
    return {
        moduleDefinition: [
            {
                moduleClass: "org/forgerock/commons/ui/common/main/SessionManager",
                configuration: {
                    loginHelperClass: "org/forgerock/selfservice/ui/LoginHelper"
                }
            },
            {
                moduleClass: "org/forgerock/commons/ui/common/SiteConfigurator",
                configuration: {
                    lang: "en",
                    selfRegistration: true,
                    passwordReset: true
                }
            },

            {
                moduleClass: "org/forgerock/commons/ui/common/main/ProcessConfiguration",
                configuration: {
                    processConfigurationFiles: [
                        "config/process/CommonConfig"
                    ]
                }
            },
            {
                moduleClass: "org/forgerock/commons/ui/common/main/Router",
                configuration: {
                    routes: {
                    },
                    loader: [
                        {"routes": "config/routes/CommonRoutesConfig"},
                        {"routes": "config/routes/UserRoutesConfig"}
                    ]
                }
            },
            {
                moduleClass: "org/forgerock/commons/ui/common/main/ServiceInvoker",
                configuration: {
                    defaultHeaders: {
                    }
                }
            },
            {
                moduleClass: "org/forgerock/commons/ui/common/main/ErrorsHandler",
                configuration: {
                    defaultHandlers: {
                    },
                    loader: [
                        {"defaultHandlers": "config/errorhandlers/CommonErrorHandlers"}
                    ]
                }
            },
            {
                moduleClass: "org/forgerock/commons/ui/common/components/Navigation",
                configuration: {
                }
            },
            {
                moduleClass: "org/forgerock/commons/ui/common/util/UIUtils",
                configuration: {
                    templateUrls: [
                    ]
                }
            },
            {
                moduleClass: "org/forgerock/commons/ui/common/components/Messages",
                configuration: {
                    messages: {
                    },
                    loader: [
                        {"messages": "config/messages/CommonMessages"},
                        {"messages": "config/messages/UserMessages"}
                    ]
                }
            },
            {
                moduleClass: "org/forgerock/commons/ui/common/main/ValidatorsManager",
                configuration: {
                    policyDelegate: "org/forgerock/mock/ui/common/delegates/PolicyDelegate",
                    validators: { },
                    loader: [
                        {"validators": "config/validators/CommonValidators"}
                    ]
                }
            }
        ],
        loggerLevel: 'debug'
    };
});
