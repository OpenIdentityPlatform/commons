/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2014 ForgeRock AS. All Rights Reserved
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

/*global define, _ */

/**
 * @author Eugenia Sergueeva
 * @author Jake Feasel
 */

define("mock/Data", [
    "text!libs/less-1.5.1-min.js",
    "text!css/styles.less",
    "text!css/common/config.less",
    "text!css/common/forms.less",
    "text!css/common/helpers.less",
    "text!css/common/layout.less",
    "text!locales/en/translation.json",
    "text!templates/common/NavigationTemplate.html",
    "text!templates/common/FooterTemplate.html",
    "text!templates/common/404.html",
    "text!templates/common/LoginTemplate.html",
    "text!templates/common/DefaultBaseTemplate.html",
    "text!templates/user/UserProfileTemplate.html",
    "text!templates/common/LoginBaseTemplate.html",
    "text!templates/mock/UserRegistrationTemplate.html",
    "text!templates/common/MediumBaseTemplate.html",
    "text!templates/mock/ChangeSecurityDataDialogTemplate.html",
    "text!templates/mock/TermsOfUseTemplate.html",
    "text!templates/common/DialogTemplate.html"
], function () {

var deps = arguments,
    /* an unfortunate need to duplicate the file names here, but I haven't
       yet found a way to fool requirejs into doing dynamic dependencies */
    staticFiles = [
    "libs/less-1.5.1-min.js",
    "css/styles.less",
    "css/common/config.less",
    "css/common/forms.less",
    "css/common/helpers.less",
    "css/common/layout.less",
    "locales/en/translation.json",
    "templates/common/NavigationTemplate.html",
    "templates/common/FooterTemplate.html",
    "templates/common/404.html",
    "templates/common/LoginTemplate.html",
    "templates/common/DefaultBaseTemplate.html",
    "templates/user/UserProfileTemplate.html",
    "templates/common/LoginBaseTemplate.html",
    "templates/mock/UserRegistrationTemplate.html",
    "templates/common/MediumBaseTemplate.html",
    "templates/mock/ChangeSecurityDataDialogTemplate.html",
    "templates/mock/TermsOfUseTemplate.html",
    "templates/common/DialogTemplate.html"
    ];

    return function (server) {

        _.each(staticFiles, function (file, i) {
            server.respondWith(
                "GET",
                new RegExp(file.replace(/([\/\.\-])/g, "\\$1") + "$"),
                [
                    200,
                    { },
                    deps[i]
                ]
            );            
        });

        server.respondWith(
            "GET",
            "/mock/config/ui/configuration",
            [
                200,
                {
                    "Content-Type": "application/json;charset=UTF-8"
                },
                JSON.stringify(
                    {
                        "configuration": {
                            "defaultNotificationType": "info",
                            "passwordResetLink": "",
                            "selfRegistration": true,
                            "roles": {
                                "ui-user": "User"
                            },
                            "notificationTypes": {
                                "error": {
                                    "iconPath": "images/notifications/error.png",
                                    "name": "common.notification.types.error"
                                },
                                "warning": {
                                    "iconPath": "images/notifications/warning.png",
                                    "name": "common.notification.types.warning"
                                },
                                "info": {
                                    "iconPath": "images/notifications/info.png",
                                    "name": "common.notification.types.info"
                                }
                            },
                            "lang": "en"
                        }
                    }
                )
            ]
        );

        server.respondWith(
            "GET",
            new RegExp("\/policy\/mock\/repo\/internal\/user\/.*"),
            [
                200,
                {
                    "Content-Type": "application/json;charset=UTF-8"
                },
                JSON.stringify(
                    {
                        "resource": "mock/repo/internal/user/*",
                        "properties": [
                            {
                                "policyRequirements": [
                                    "REQUIRED",
                                    "CANNOT_CONTAIN_CHARACTERS"
                                ],
                                "policies": [
                                    {
                                        "policyRequirements": [
                                            "REQUIRED"
                                        ],
                                        "policyId": "required",
                                        "policyFunction": "\nfunction (fullObject, value, params, propName) {\n    if (value === undefined) {\n        return [{\"policyRequirement\":\"REQUIRED\"}];\n    }\n    return [];\n}\n"
                                    },
                                    {
                                        "policyRequirements": [
                                            "REQUIRED"
                                        ],
                                        "policyId": "not-empty",
                                        "policyFunction": "\nfunction (fullObject, value, params, property) {\n    if (value !== undefined && (value === null || !value.length)) {\n        return [{\"policyRequirement\":\"REQUIRED\"}];\n    } else {\n        return [];\n    }\n}\n"
                                    },
                                    {
                                        "policyRequirements": [
                                            "CANNOT_CONTAIN_CHARACTERS"
                                        ],
                                        "params": {
                                            "forbiddenChars": [
                                                "/"
                                            ]
                                        },
                                        "policyId": "cannot-contain-characters",
                                        "policyFunction": "\nfunction (fullObject, value, params, property) {\n    var i, join = function (arr, d) {\n        var j, list = \"\";\n        for (j in arr) {\n            list += arr[j] + d;\n        }\n        return list.replace(new RegExp(d + \"$\"), \"\");\n    };\n    if (typeof (value) === \"string\" && value.length) {\n        for (i in params.forbiddenChars) {\n            if (value.indexOf(params.forbiddenChars[i]) !== -1) {\n                return [{\"policyRequirement\":\"CANNOT_CONTAIN_CHARACTERS\", \"params\":{\"forbiddenChars\":join(params.forbiddenChars, \", \")}}];\n            }\n        }\n    }\n    return [];\n}\n"
                                    }
                                ],
                                "name": "userName"
                            },
                            {
                                "policyRequirements": [
                                    "REQUIRED",
                                    "AT_LEAST_X_CAPITAL_LETTERS",
                                    "AT_LEAST_X_NUMBERS",
                                    "MIN_LENGTH"
                                ],
                                "policies": [
                                    {
                                        "policyRequirements": [
                                            "REQUIRED"
                                        ],
                                        "policyId": "not-empty",
                                        "policyFunction": "\nfunction (fullObject, value, params, property) {\n    if (value !== undefined && (value === null || !value.length)) {\n        return [{\"policyRequirement\":\"REQUIRED\"}];\n    } else {\n        return [];\n    }\n}\n"
                                    },
                                    {
                                        "policyRequirements": [
                                            "AT_LEAST_X_CAPITAL_LETTERS"
                                        ],
                                        "params": {
                                            "numCaps": 1
                                        },
                                        "policyId": "at-least-X-capitals",
                                        "policyFunction": "\nfunction (fullObject, value, params, property) {\n    var reg = /[(A-Z)]/g;\n        \n        if (typeof(value) === \"string\" && value.length && (value.match(reg) === null || value.match(reg).length < params.numCaps)) {\n            return [ { \"policyRequirement\" : \"AT_LEAST_X_CAPITAL_LETTERS\", \"params\" : {\"numCaps\": params.numCaps} } ];\n        } else {\n            return [];\n        }\n    }"
                                    },
                                    {
                                        "policyRequirements": [
                                            "AT_LEAST_X_NUMBERS"
                                        ],
                                        "params": {
                                            "numNums": 1
                                        },
                                        "policyId": "at-least-X-numbers",
                                        "policyFunction": "\nfunction (fullObject, value, params, property) {\n    var reg = /\\d/g;\n        \n        if (typeof(value) === \"string\" && value.length && (value.match(reg) === null || value.match(reg).length < params.numNums)) {\n            return [ { \"policyRequirement\" : \"AT_LEAST_X_NUMBERS\", \"params\" : {\"numNums\": params.numNums}  } ];\n        } else {\n            return [];\n        }\n    }"
                                    },
                                    {
                                        "policyRequirements": [
                                            "MIN_LENGTH"
                                        ],
                                        "params": {
                                            "minLength": 8
                                        },
                                        "policyId": "minimum-length",
                                        "policyFunction": "\nfunction (fullObject, value, params, property) {\n    if (typeof(value) === \"string\" && value.length && value.length < params.minLength) {\n            return [ { \"policyRequirement\" : \"MIN_LENGTH\", \"params\" : {\"minLength\":params.minLength} } ];\n        } else {\n            return [];\n        }\n    }"
                                    }
                                ],
                                "name": "password"
                            },
                            {
                                "policyRequirements": [
                                    "REQUIRED",
                                    "VALID_EMAIL_ADDRESS_FORMAT"
                                ],
                                "policies": [
                                    {
                                        "policyRequirements": [
                                            "REQUIRED"
                                        ],
                                        "policyId": "required",
                                        "policyFunction": "\nfunction (fullObject, value, params, propName) {\n    if (value === undefined) {\n        return [{\"policyRequirement\":\"REQUIRED\"}];\n    }\n    return [];\n}\n"
                                    },
                                    {
                                        "policyRequirements": [
                                            "REQUIRED"
                                        ],
                                        "policyId": "not-empty",
                                        "policyFunction": "\nfunction (fullObject, value, params, property) {\n    if (value !== undefined && (value === null || !value.length)) {\n        return [{\"policyRequirement\":\"REQUIRED\"}];\n    } else {\n        return [];\n    }\n}\n"
                                    },
                                    {
                                        "policyRequirements": [
                                            "VALID_EMAIL_ADDRESS_FORMAT"
                                        ],
                                        "policyId": "valid-email-address-format",
                                        "policyFunction": "\nfunction (fullObject, value, params, property) {\n    var emailPattern = /^([A-Za-z0-9_\\-\\.])+\\@([A-Za-z0-9_\\-\\.])+\\.([A-Za-z]{2,4})$/; \n        \n        if (typeof(value) === \"string\" && value.length && !emailPattern.test(value)) {\n            return [ {\"policyRequirement\": \"VALID_EMAIL_ADDRESS_FORMAT\"}];\n        } else {\n            return [];\n        }\n    }"
                                    }
                                ],
                                "name": "mail"
                            },
                            {
                                "policyRequirements": [
                                    "REQUIRED",
                                    "VALID_NAME_FORMAT"
                                ],
                                "policies": [
                                    {
                                        "policyRequirements": [
                                            "REQUIRED"
                                        ],
                                        "policyId": "required",
                                        "policyFunction": "\nfunction (fullObject, value, params, propName) {\n    if (value === undefined) {\n        return [{\"policyRequirement\":\"REQUIRED\"}];\n    }\n    return [];\n}\n"
                                    },
                                    {
                                        "policyRequirements": [
                                            "REQUIRED"
                                        ],
                                        "policyId": "not-empty",
                                        "policyFunction": "\nfunction (fullObject, value, params, property) {\n    if (value !== undefined && (value === null || !value.length)) {\n        return [{\"policyRequirement\":\"REQUIRED\"}];\n    } else {\n        return [];\n    }\n}\n"
                                    },
                                    {
                                        "policyRequirements": [
                                            "VALID_NAME_FORMAT"
                                        ],
                                        "policyId": "valid-name-format",
                                        "policyFunction": "\nfunction (fullObject, value, params, property) {\n    var namePattern = /^([A-Za'-\\u0105\\u0107\\u0119\\u0142\\u00F3\\u015B\\u017C\\u017A\\u0104\\u0106\\u0118\\u0141\\u00D3\\u015A\\u017B\\u0179\\u00C0\\u00C8\\u00CC\\u00D2\\u00D9\\u00E0\\u00E8\\u00EC\\u00F2\\u00F9\\u00C1\\u00C9\\u00CD\\u00D3\\u00DA\\u00DD\\u00E1\\u00E9\\u00ED\\u00F3\\u00FA\\u00FD\\u00C2\\u00CA\\u00CE\\u00D4\\u00DB\\u00E2\\u00EA\\u00EE\\u00F4\\u00FB\\u00C3\\u00D1\\u00D5\\u00E3\\u00F1\\u00F5\\u00C4\\u00CB\\u00CF\\u00D6\\u00DC\\u0178\\u00E4\\u00EB\\u00EF\\u00F6\\u00FC\\u0178\\u00A1\\u00BF\\u00E7\\u00C7\\u0152\\u0153\\u00DF\\u00D8\\u00F8\\u00C5\\u00E5\\u00C6\\u00E6\\u00DE\\u00FE\\u00D0\\u00F0\\-\\s])+$/;\n    \n        if (typeof(value) === \"string\" && value.length && !namePattern.test(value)) {\n            return [ {\"policyRequirement\": \"VALID_NAME_FORMAT\"}];\n        } else {\n            return [];\n        }\n    }"
                                    }
                                ],
                                "name": "givenName"
                            },
                            {
                                "policyRequirements": [
                                    "REQUIRED",
                                    "VALID_NAME_FORMAT"
                                ],
                                "policies": [
                                    {
                                        "policyRequirements": [
                                            "REQUIRED"
                                        ],
                                        "policyId": "required",
                                        "policyFunction": "\nfunction (fullObject, value, params, propName) {\n    if (value === undefined) {\n        return [{\"policyRequirement\":\"REQUIRED\"}];\n    }\n    return [];\n}\n"
                                    },
                                    {
                                        "policyRequirements": [
                                            "REQUIRED"
                                        ],
                                        "policyId": "not-empty",
                                        "policyFunction": "\nfunction (fullObject, value, params, property) {\n    if (value !== undefined && (value === null || !value.length)) {\n        return [{\"policyRequirement\":\"REQUIRED\"}];\n    } else {\n        return [];\n    }\n}\n"
                                    },
                                    {
                                        "policyRequirements": [
                                            "VALID_NAME_FORMAT"
                                        ],
                                        "policyId": "valid-name-format",
                                        "policyFunction": "\nfunction (fullObject, value, params, property) {\n    var namePattern = /^([A-Za'-\\u0105\\u0107\\u0119\\u0142\\u00F3\\u015B\\u017C\\u017A\\u0104\\u0106\\u0118\\u0141\\u00D3\\u015A\\u017B\\u0179\\u00C0\\u00C8\\u00CC\\u00D2\\u00D9\\u00E0\\u00E8\\u00EC\\u00F2\\u00F9\\u00C1\\u00C9\\u00CD\\u00D3\\u00DA\\u00DD\\u00E1\\u00E9\\u00ED\\u00F3\\u00FA\\u00FD\\u00C2\\u00CA\\u00CE\\u00D4\\u00DB\\u00E2\\u00EA\\u00EE\\u00F4\\u00FB\\u00C3\\u00D1\\u00D5\\u00E3\\u00F1\\u00F5\\u00C4\\u00CB\\u00CF\\u00D6\\u00DC\\u0178\\u00E4\\u00EB\\u00EF\\u00F6\\u00FC\\u0178\\u00A1\\u00BF\\u00E7\\u00C7\\u0152\\u0153\\u00DF\\u00D8\\u00F8\\u00C5\\u00E5\\u00C6\\u00E6\\u00DE\\u00FE\\u00D0\\u00F0\\-\\s])+$/;\n    \n        if (typeof(value) === \"string\" && value.length && !namePattern.test(value)) {\n            return [ {\"policyRequirement\": \"VALID_NAME_FORMAT\"}];\n        } else {\n            return [];\n        }\n    }"
                                    }
                                ],
                                "name": "sn"
                            },
                            {
                                "policyRequirements": [
                                    "VALID_PHONE_FORMAT"
                                ],
                                "policies": [
                                    {
                                        "policyRequirements": [
                                            "VALID_PHONE_FORMAT"
                                        ],
                                        "policyId": "valid-phone-format",
                                        "policyFunction": "\nfunction (fullObject, value, params, property) {\n    var phonePattern = /^\\+?([0-9\\- \\(\\)])*$/;\n    \n        if (typeof(value) === \"string\" && value.length && !phonePattern.test(value)) {\n            return [ {\"policyRequirement\": \"VALID_PHONE_FORMAT\"}];\n        } else {\n            return [];\n        }\n    }"
                                    }
                                ],
                                "name": "telephoneNumber"
                            }
                        ]
                    }
                )
            ]
        );
    };
});

