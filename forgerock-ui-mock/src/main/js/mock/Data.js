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

define([
    "text!libs/less-1.5.1-min.js",
    "text!css/styles.less",
    "text!css/user/config.less",
    "text!css/user/forms.less",
    "text!css/user/helpers.less",
    "text!css/user/layout.less",
    "text!locales/en-US/translation.json",
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
    "css/user/config.less",
    "css/user/forms.less",
    "css/user/helpers.less",
    "css/user/layout.less",
    "locales/en-US/translation.json",
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
                    "Content-Encoding": "gzip",
                    "Server": "Jetty(8.y.z-SNAPSHOT)",
                    "Vary": "Accept-Encoding, User-Agent",
                    "Content-Type": "application/json;charset=UTF-8",
                    "Cache-Control": "no-cache",
                    "Set-Cookie": "session-jwt=eyAiYWxnIjogIlJTQUVTX1BLQ1MxX1YxXzUiLCAidHlwIjogImp3dCIsICJlbmMiOiAiQTEyOENCQ19IUzI1NiIgfQ.TnqAI2Ye3oXsMJzt2S2s1HGKZv9z7_xE5pyQvpnX6RTHCuGEQrvtXP2aeqmXeI4cAZ_7AbvfZfJEeOhDYku_NSlewtMHujfOkE0aiGvsAgqRLPkTLaUD5ea3ubKggjwOVvvaWQlr3dQtSPUWkfeoGChD-MXjhWSgZ_djGheAun2QCgffjXONIKYXeS2ya0ezKzAvq5pE1lmNw9mZ6lP4hROHDMrZAR1VYVG_8vAxQ4vXpaVspinlLiVf-Gu_ry3EC0D3t2jq_cyOJHXCsKLl4ck917tb9K_Eolq6Zg1YmNXXFDaugQSbXGGpDJ31yLRbeU0OHyoafIxyaGxVQQUzfw.AtjdWd0JGWcDznURoRbF5A.7f-rDNjzDmPCiIGVu7ZlfNHWQvUYOBF5imF98fKUgszbrGO429OUwfgvkfdcHOZP5N-OJVhzLQM5yxOlL0a_c8KnmAFcs8JlwoQoQflIDhSxInQd9tXngAC1n_OdgX5lU_Y1U6kgcYJmtOTN99FzlhWqd_UvLyKfL9Lf-1aEzd1eHTrGksJgaWk-ttHfTLXF_6MPYpVk5rXNealJM7yU0H1MeXg7XKcsB4Ove0RTlzfTl6nkhnrCjXie2Bq8v56MoFgKdCnv8VnIxLDFj14eIrMxtcQMdT9Kwb7Sf6IXkIvQqLOHx5Sr8j-dMJikSLalLTKLw-mSeiZ3t-hc4PVk1TQ4ZT5mmpAZ_7LGYeFD3c5g3qmO5zq5xWXneYCmtorkGods8r4-0Bj1CXtJzFP8A6KL9yANZju7Ms-VT5Gxda4.ZhSeegfRKqnhf3NwmYUkJA;Path=/",
                    "Content-Length": "316",
                    "Expires": "Thu, 01 Jan 1970 00:00:00 GMT"
                },
                "{\"configuration\":{\"defaultNotificationType\":\"info\",\"passwordResetLink\":\"\",\"selfRegistration\":true,\"roles\":{\"ui-user\":\"User\"},\"notificationTypes\":{\"error\":{\"iconPath\":\"images/notifications/error.png\",\"name\":\"common.notification.types.error\"},\"warning\":{\"iconPath\":\"images/notifications/warning.png\",\"name\":\"common.notification.types.warning\"},\"info\":{\"iconPath\":\"images/notifications/info.png\",\"name\":\"common.notification.types.info\"}},\"language\":\"en-US\"}}"
            ]
        );

        server.respondWith(
            "GET",
            "/mock/info/login",
            [
                200,
                {
                    "Content-Encoding": "gzip",
                    "Server": "Jetty(8.y.z-SNAPSHOT)",
                    "Vary": "Accept-Encoding, User-Agent",
                    "Content-Type": "application/json;charset=UTF-8",
                    "Cache-Control": "no-cache",
                    "Set-Cookie": "session-jwt=eyAiYWxnIjogIlJTQUVTX1BLQ1MxX1YxXzUiLCAidHlwIjogImp3dCIsICJlbmMiOiAiQTEyOENCQ19IUzI1NiIgfQ.OOCTC9okerzWNt41tIQFZb6qQFYqZEjm0ld0ZR9_Ik_gwQqy29KRfinqLtanYpHNSjTOKUhCNFmhgfixeI5f6DB7m5ZjHYAlV_IKkMDvhb86UZtDATpfgnMuVUn3mI8PAPqGKIxpprzgP3lfXI6PiWF_OwNzR81zYqL1PA7jdGMa9M7wbVLRpxX1kBDYcsIOHbYOVMSgjZbACLXbZ3zQ8mtgrrkf8bcUIoOhNy23zqv2joFJtZXlrRVip781Y2rcGGljmjYCyY_Xmuor2hA7ZO1RLRn4vfxvhflV22k63flbxJAd7iwyfrawdcSUX6qsQr_0HyLW6w1ktEVW51P5fQ.lxOgtwNpAvYknS5BT2E7yQ.DOrhwQg7k-A8EDs7A2Vv9EMFJD4RkU-pXGNuZTqAs674Z8sCJoPHTrhH8pxQu5IFmN0AkSdek7LyXFN013bdQZK01g8jRNFjw2qjJiPVuA3Ml8HrbfMjMTI02X2tKfEa16V3AnebY9JYqMcCw4RIG6IgVwmOGla8C5ALwLn01bvmYrcfUOIayOWNafh-ENyxYjqxgxAssvrIXlKOfC0FGejDxK_86U0p60JMuTTPYCqh92G67oKCflFNCB7puFzDyuIqQ3M22t-LDQJUt2VzXPNre3S4vzLVY2zbohClG-auwvHjrR7d_gT1nPw1nKhfbDGPCMEzHQaQzp2oS8fij6wKrhcoIPou62QHm-lHXID-IEJrXwi8lYKb-0z952hb8i5MksMcPpqXFhOUX2aWHVf5gb2W6ZGItwimbVJUE3o.Nn1CtWsvPbLnh_pNkgNr5Q;Path=/",
                    "Content-Length": "210",
                    "Expires": "Thu, 01 Jan 1970 00:00:00 GMT"
                },
                "{\"authorizationId\":{\"id\":\"test\",\"component\":\"internal/user\",\"roles\":[\"ui-user\"]},\"parent\":{\"id\":\"d69466af-badc-4b06-84e4-20f9b59dd6b6\",\"parent\":null,\"class\":\"org.forgerock.json.resource.RootContext\"},\"class\":\"org.forgerock.json.resource.SecurityContext\",\"authenticationId\":\"test\"}"
            ]
        );

        server.respondWith(
            "GET",
            "/mock/repo/internal/user/test",
            [
                200,
                {
                    "Content-Encoding": "gzip",
                    "Server": "Jetty(8.y.z-SNAPSHOT)",
                    "Cache-Control": "no-cache",
                    "Content-Length": "782",
                    "Vary": "Accept-Encoding, User-Agent",
                    "Content-Type": "application/json;charset=UTF-8"
                },
                "{\"resource\":\"repo/internal/user/*\",\"properties\":[{\"policyRequirements\":[\"CANNOT_CONTAIN_CHARACTERS\"],\"policies\":[{\"policyRequirements\":[\"CANNOT_CONTAIN_CHARACTERS\"],\"params\":{\"forbiddenChars\":[\"/\"]},\"policyId\":\"cannot-contain-characters\",\"policyFunction\":\"\\nfunction (fullObject, value, params, property) {\\n    var i, join = function (arr, d) {\\n        var j, list = \\\"\\\";\\n        for (j in arr) {\\n            list += arr[j] + d;\\n        }\\n        return list.replace(new RegExp(d + \\\"$\\\"), \\\"\\\");\\n    };\\n    if (typeof (value) === \\\"string\\\" && value.length) {\\n        for (i in params.forbiddenChars) {\\n            if (value.indexOf(params.forbiddenChars[i]) !== -1) {\\n                return [{\\\"policyRequirement\\\":\\\"CANNOT_CONTAIN_CHARACTERS\\\", \\\"params\\\":{\\\"forbiddenChars\\\":join(params.forbiddenChars, \\\", \\\")}}];\\n            }\\n        }\\n    }\\n    return [];\\n}\\n\"}],\"name\":\"_id\"},{\"policyRequirements\":[\"REQUIRED\",\"AT_LEAST_X_CAPITAL_LETTERS\",\"AT_LEAST_X_NUMBERS\",\"MIN_LENGTH\"],\"policies\":[{\"policyRequirements\":[\"REQUIRED\"],\"policyId\":\"required\",\"policyFunction\":\"\\nfunction (fullObject, value, params, propName) {\\n    if (value === undefined) {\\n        return [{\\\"policyRequirement\\\":\\\"REQUIRED\\\"}];\\n    }\\n    return [];\\n}\\n\"},{\"policyRequirements\":[\"REQUIRED\"],\"policyId\":\"not-empty\",\"policyFunction\":\"\\nfunction (fullObject, value, params, property) {\\n    if (value !== undefined && (value === null || !value.length)) {\\n        return [{\\\"policyRequirement\\\":\\\"REQUIRED\\\"}];\\n    } else {\\n        return [];\\n    }\\n}\\n\"},{\"policyRequirements\":[\"AT_LEAST_X_CAPITAL_LETTERS\"],\"params\":{\"numCaps\":1},\"policyId\":\"at-least-X-capitals\",\"policyFunction\":\"\\nfunction (fullObject, value, params, property) {\\n    var reg = /[(A-Z)]/g;\\n    if (typeof (value) !== \\\"string\\\" || !value.length || value.match(reg) === null || value.match(reg).length < params.numCaps) {\\n        return [{\\\"policyRequirement\\\":\\\"AT_LEAST_X_CAPITAL_LETTERS\\\", \\\"params\\\":{\\\"numCaps\\\":params.numCaps}}];\\n    } else {\\n        return [];\\n    }\\n}\\n\"},{\"policyRequirements\":[\"AT_LEAST_X_NUMBERS\"],\"params\":{\"numNums\":1},\"policyId\":\"at-least-X-numbers\",\"policyFunction\":\"\\nfunction (fullObject, value, params, property) {\\n    var reg = /\\\\d/g;\\n    if (typeof (value) !== \\\"string\\\" || !value.length || value.match(reg) === null || value.match(reg).length < params.numNums) {\\n        return [{\\\"policyRequirement\\\":\\\"AT_LEAST_X_NUMBERS\\\", \\\"params\\\":{\\\"numNums\\\":params.numNums}}];\\n    } else {\\n        return [];\\n    }\\n}\\n\"},{\"policyRequirements\":[\"MIN_LENGTH\"],\"params\":{\"minLength\":8},\"policyId\":\"minimum-length\",\"policyFunction\":\"\\nfunction (fullObject, value, params, property) {\\n    if (typeof (value) !== \\\"string\\\" || !value.length || value.length < params.minLength) {\\n        return [{\\\"policyRequirement\\\":\\\"MIN_LENGTH\\\", \\\"params\\\":{\\\"minLength\\\":params.minLength}}];\\n    } else {\\n        return [];\\n    }\\n}\\n\"}],\"name\":\"password\"}]}"
            ]
        );

        server.respondWith(
            "GET",
            "/mock/repo/internal/user/*",
            [
                200,
                {
                    "Content-Encoding": "gzip",
                    "Server": "Jetty(8.y.z-SNAPSHOT)",
                    "Cache-Control": "no-cache",
                    "Content-Length": "1933",
                    "Vary": "Accept-Encoding, User-Agent",
                    "Content-Type": "application/json;charset=UTF-8"
                },
                "{\"resource\":\"managed/user/*\",\"properties\":[{\"policyRequirements\":[\"CANNOT_CONTAIN_CHARACTERS\"],\"policies\":[{\"policyRequirements\":[\"CANNOT_CONTAIN_CHARACTERS\"],\"params\":{\"forbiddenChars\":[\"/\"]},\"policyId\":\"cannot-contain-characters\",\"policyFunction\":\"\\nfunction (fullObject, value, params, property) {\\n    var i, join = function (arr, d) {\\n        var j, list = \\\"\\\";\\n        for (j in arr) {\\n            list += arr[j] + d;\\n        }\\n        return list.replace(new RegExp(d + \\\"$\\\"), \\\"\\\");\\n    };\\n    if (typeof (value) === \\\"string\\\" && value.length) {\\n        for (i in params.forbiddenChars) {\\n            if (value.indexOf(params.forbiddenChars[i]) !== -1) {\\n                return [{\\\"policyRequirement\\\":\\\"CANNOT_CONTAIN_CHARACTERS\\\", \\\"params\\\":{\\\"forbiddenChars\\\":join(params.forbiddenChars, \\\", \\\")}}];\\n            }\\n        }\\n    }\\n    return [];\\n}\\n\"}],\"name\":\"_id\"},{\"policyRequirements\":[\"REQUIRED\",\"UNIQUE\",\"CANNOT_CONTAIN_CHARACTERS\"],\"policies\":[{\"policyRequirements\":[\"REQUIRED\"],\"policyId\":\"required\",\"policyFunction\":\"\\nfunction (fullObject, value, params, propName) {\\n    if (value === undefined) {\\n        return [{\\\"policyRequirement\\\":\\\"REQUIRED\\\"}];\\n    }\\n    return [];\\n}\\n\"},{\"policyRequirements\":[\"REQUIRED\"],\"policyId\":\"not-empty\",\"policyFunction\":\"\\nfunction (fullObject, value, params, property) {\\n    if (value !== undefined && (value === null || !value.length)) {\\n        return [{\\\"policyRequirement\\\":\\\"REQUIRED\\\"}];\\n    } else {\\n        return [];\\n    }\\n}\\n\"},{\"policyRequirements\":[\"UNIQUE\"],\"policyId\":\"unique\"},{\"policyRequirements\":[\"UNIQUE\"],\"policyId\":\"no-internal-user-conflict\"},{\"policyRequirements\":[\"CANNOT_CONTAIN_CHARACTERS\"],\"params\":{\"forbiddenChars\":[\"/\"]},\"policyId\":\"cannot-contain-characters\",\"policyFunction\":\"\\nfunction (fullObject, value, params, property) {\\n    var i, join = function (arr, d) {\\n        var j, list = \\\"\\\";\\n        for (j in arr) {\\n            list += arr[j] + d;\\n        }\\n        return list.replace(new RegExp(d + \\\"$\\\"), \\\"\\\");\\n    };\\n    if (typeof (value) === \\\"string\\\" && value.length) {\\n        for (i in params.forbiddenChars) {\\n            if (value.indexOf(params.forbiddenChars[i]) !== -1) {\\n                return [{\\\"policyRequirement\\\":\\\"CANNOT_CONTAIN_CHARACTERS\\\", \\\"params\\\":{\\\"forbiddenChars\\\":join(params.forbiddenChars, \\\", \\\")}}];\\n            }\\n        }\\n    }\\n    return [];\\n}\\n\"}],\"name\":\"userName\"},{\"policyRequirements\":[\"REQUIRED\",\"AT_LEAST_X_CAPITAL_LETTERS\",\"AT_LEAST_X_NUMBERS\",\"MIN_LENGTH\",\"CANNOT_CONTAIN_OTHERS\",\"REAUTH_REQUIRED\"],\"policies\":[{\"policyRequirements\":[\"REQUIRED\"],\"policyId\":\"not-empty\",\"policyFunction\":\"\\nfunction (fullObject, value, params, property) {\\n    if (value !== undefined && (value === null || !value.length)) {\\n        return [{\\\"policyRequirement\\\":\\\"REQUIRED\\\"}];\\n    } else {\\n        return [];\\n    }\\n}\\n\"},{\"policyRequirements\":[\"AT_LEAST_X_CAPITAL_LETTERS\"],\"params\":{\"numCaps\":1},\"policyId\":\"at-least-X-capitals\",\"policyFunction\":\"\\nfunction (fullObject, value, params, property) {\\n    var reg = /[(A-Z)]/g;\\n    if (typeof (value) !== \\\"string\\\" || !value.length || value.match(reg) === null || value.match(reg).length < params.numCaps) {\\n        return [{\\\"policyRequirement\\\":\\\"AT_LEAST_X_CAPITAL_LETTERS\\\", \\\"params\\\":{\\\"numCaps\\\":params.numCaps}}];\\n    } else {\\n        return [];\\n    }\\n}\\n\"},{\"policyRequirements\":[\"AT_LEAST_X_NUMBERS\"],\"params\":{\"numNums\":1},\"policyId\":\"at-least-X-numbers\",\"policyFunction\":\"\\nfunction (fullObject, value, params, property) {\\n    var reg = /\\\\d/g;\\n    if (typeof (value) !== \\\"string\\\" || !value.length || value.match(reg) === null || value.match(reg).length < params.numNums) {\\n        return [{\\\"policyRequirement\\\":\\\"AT_LEAST_X_NUMBERS\\\", \\\"params\\\":{\\\"numNums\\\":params.numNums}}];\\n    } else {\\n        return [];\\n    }\\n}\\n\"},{\"policyRequirements\":[\"MIN_LENGTH\"],\"params\":{\"minLength\":8},\"policyId\":\"minimum-length\",\"policyFunction\":\"\\nfunction (fullObject, value, params, property) {\\n    if (typeof (value) !== \\\"string\\\" || !value.length || value.length < params.minLength) {\\n        return [{\\\"policyRequirement\\\":\\\"MIN_LENGTH\\\", \\\"params\\\":{\\\"minLength\\\":params.minLength}}];\\n    } else {\\n        return [];\\n    }\\n}\\n\"},{\"policyRequirements\":[\"CANNOT_CONTAIN_OTHERS\"],\"params\":{\"disallowedFields\":\"userName,givenName,sn\"},\"policyId\":\"cannot-contain-others\",\"policyFunction\":\"\\nfunction (fullObject, value, params, property) {\\n    var fieldArray = params.disallowedFields.split(\\\",\\\"), fullObject_server = {}, i;\\n    if (typeof (openidm) !== \\\"undefined\\\" && typeof (request) !== \\\"undefined\\\" && request.resourceName && !request.resourceName.match(\\\"/*$\\\")) {\\n        fullObject_server = openidm.read(request.resourceName);\\n        if (fullObject_server === null) {\\n            fullObject_server = {};\\n        }\\n    }\\n    if (value && typeof (value) === \\\"string\\\" && value.length) {\\n        for (i = 0; i < fieldArray.length; i++) {\\n            if (typeof (fullObject[fieldArray[i]]) === \\\"undefined\\\" && typeof (fullObject_server[fieldArray[i]]) !== \\\"undefined\\\") {\\n                fullObject[fieldArray[i]] = fullObject_server[fieldArray[i]];\\n            }\\n            if (typeof (fullObject[fieldArray[i]]) === \\\"string\\\" && value.match(fullObject[fieldArray[i]])) {\\n                return [{\\\"policyRequirement\\\":\\\"CANNOT_CONTAIN_OTHERS\\\", params:{\\\"disallowedFields\\\":fieldArray[i]}}];\\n            }\\n        }\\n    }\\n    return [];\\n}\\n\"},{\"policyRequirements\":[\"REAUTH_REQUIRED\"],\"policyId\":\"re-auth-required\",\"params\":{\"exceptRoles\":[\"openidm-admin\",\"openidm-reg\",\"openidm-cert\"]}}],\"name\":\"password\"},{\"policyRequirements\":[\"REQUIRED\",\"VALID_EMAIL_ADDRESS_FORMAT\"],\"policies\":[{\"policyRequirements\":[\"REQUIRED\"],\"policyId\":\"required\",\"policyFunction\":\"\\nfunction (fullObject, value, params, propName) {\\n    if (value === undefined) {\\n        return [{\\\"policyRequirement\\\":\\\"REQUIRED\\\"}];\\n    }\\n    return [];\\n}\\n\"},{\"policyRequirements\":[\"REQUIRED\"],\"policyId\":\"not-empty\",\"policyFunction\":\"\\nfunction (fullObject, value, params, property) {\\n    if (value !== undefined && (value === null || !value.length)) {\\n        return [{\\\"policyRequirement\\\":\\\"REQUIRED\\\"}];\\n    } else {\\n        return [];\\n    }\\n}\\n\"},{\"policyRequirements\":[\"VALID_EMAIL_ADDRESS_FORMAT\"],\"policyId\":\"valid-email-address-format\",\"policyFunction\":\"\\nfunction (fullObject, value, params, property) {\\n    var emailPattern = /^([A-Za-z0-9_\\\\-\\\\.])+\\\\@([A-Za-z0-9_\\\\-\\\\.])+\\\\.([A-Za-z]{2,4})$/;\\n    if (typeof (value) !== \\\"string\\\" || !value.length || !emailPattern.test(value)) {\\n        return [{\\\"policyRequirement\\\":\\\"VALID_EMAIL_ADDRESS_FORMAT\\\"}];\\n    } else {\\n        return [];\\n    }\\n}\\n\"}],\"name\":\"mail\"},{\"policyRequirements\":[\"REQUIRED\",\"VALID_NAME_FORMAT\"],\"policies\":[{\"policyRequirements\":[\"REQUIRED\"],\"policyId\":\"required\",\"policyFunction\":\"\\nfunction (fullObject, value, params, propName) {\\n    if (value === undefined) {\\n        return [{\\\"policyRequirement\\\":\\\"REQUIRED\\\"}];\\n    }\\n    return [];\\n}\\n\"},{\"policyRequirements\":[\"REQUIRED\"],\"policyId\":\"not-empty\",\"policyFunction\":\"\\nfunction (fullObject, value, params, property) {\\n    if (value !== undefined && (value === null || !value.length)) {\\n        return [{\\\"policyRequirement\\\":\\\"REQUIRED\\\"}];\\n    } else {\\n        return [];\\n    }\\n}\\n\"},{\"policyRequirements\":[\"VALID_NAME_FORMAT\"],\"policyId\":\"valid-name-format\",\"policyFunction\":\"\\nfunction (fullObject, value, params, property) {\\n    var namePattern = /^([A-Za'-\\\\u0105\\\\u0107\\\\u0119\\\\u0142\\\\u00F3\\\\u015B\\\\u017C\\\\u017A\\\\u0104\\\\u0106\\\\u0118\\\\u0141\\\\u00D3\\\\u015A\\\\u017B\\\\u0179\\\\u00C0\\\\u00C8\\\\u00CC\\\\u00D2\\\\u00D9\\\\u00E0\\\\u00E8\\\\u00EC\\\\u00F2\\\\u00F9\\\\u00C1\\\\u00C9\\\\u00CD\\\\u00D3\\\\u00DA\\\\u00DD\\\\u00E1\\\\u00E9\\\\u00ED\\\\u00F3\\\\u00FA\\\\u00FD\\\\u00C2\\\\u00CA\\\\u00CE\\\\u00D4\\\\u00DB\\\\u00E2\\\\u00EA\\\\u00EE\\\\u00F4\\\\u00FB\\\\u00C3\\\\u00D1\\\\u00D5\\\\u00E3\\\\u00F1\\\\u00F5\\\\u00C4\\\\u00CB\\\\u00CF\\\\u00D6\\\\u00DC\\\\u0178\\\\u00E4\\\\u00EB\\\\u00EF\\\\u00F6\\\\u00FC\\\\u0178\\\\u00A1\\\\u00BF\\\\u00E7\\\\u00C7\\\\u0152\\\\u0153\\\\u00DF\\\\u00D8\\\\u00F8\\\\u00C5\\\\u00E5\\\\u00C6\\\\u00E6\\\\u00DE\\\\u00FE\\\\u00D0\\\\u00F0\\\\-\\\\s])+$/;\\n    if (typeof (value) !== \\\"string\\\" || !value.length || !namePattern.test(value)) {\\n        return [{\\\"policyRequirement\\\":\\\"VALID_NAME_FORMAT\\\"}];\\n    } else {\\n        return [];\\n    }\\n}\\n\"}],\"name\":\"givenName\"},{\"policyRequirements\":[\"REQUIRED\",\"VALID_NAME_FORMAT\"],\"policies\":[{\"policyRequirements\":[\"REQUIRED\"],\"policyId\":\"required\",\"policyFunction\":\"\\nfunction (fullObject, value, params, propName) {\\n    if (value === undefined) {\\n        return [{\\\"policyRequirement\\\":\\\"REQUIRED\\\"}];\\n    }\\n    return [];\\n}\\n\"},{\"policyRequirements\":[\"REQUIRED\"],\"policyId\":\"not-empty\",\"policyFunction\":\"\\nfunction (fullObject, value, params, property) {\\n    if (value !== undefined && (value === null || !value.length)) {\\n        return [{\\\"policyRequirement\\\":\\\"REQUIRED\\\"}];\\n    } else {\\n        return [];\\n    }\\n}\\n\"},{\"policyRequirements\":[\"VALID_NAME_FORMAT\"],\"policyId\":\"valid-name-format\",\"policyFunction\":\"\\nfunction (fullObject, value, params, property) {\\n    var namePattern = /^([A-Za'-\\\\u0105\\\\u0107\\\\u0119\\\\u0142\\\\u00F3\\\\u015B\\\\u017C\\\\u017A\\\\u0104\\\\u0106\\\\u0118\\\\u0141\\\\u00D3\\\\u015A\\\\u017B\\\\u0179\\\\u00C0\\\\u00C8\\\\u00CC\\\\u00D2\\\\u00D9\\\\u00E0\\\\u00E8\\\\u00EC\\\\u00F2\\\\u00F9\\\\u00C1\\\\u00C9\\\\u00CD\\\\u00D3\\\\u00DA\\\\u00DD\\\\u00E1\\\\u00E9\\\\u00ED\\\\u00F3\\\\u00FA\\\\u00FD\\\\u00C2\\\\u00CA\\\\u00CE\\\\u00D4\\\\u00DB\\\\u00E2\\\\u00EA\\\\u00EE\\\\u00F4\\\\u00FB\\\\u00C3\\\\u00D1\\\\u00D5\\\\u00E3\\\\u00F1\\\\u00F5\\\\u00C4\\\\u00CB\\\\u00CF\\\\u00D6\\\\u00DC\\\\u0178\\\\u00E4\\\\u00EB\\\\u00EF\\\\u00F6\\\\u00FC\\\\u0178\\\\u00A1\\\\u00BF\\\\u00E7\\\\u00C7\\\\u0152\\\\u0153\\\\u00DF\\\\u00D8\\\\u00F8\\\\u00C5\\\\u00E5\\\\u00C6\\\\u00E6\\\\u00DE\\\\u00FE\\\\u00D0\\\\u00F0\\\\-\\\\s])+$/;\\n    if (typeof (value) !== \\\"string\\\" || !value.length || !namePattern.test(value)) {\\n        return [{\\\"policyRequirement\\\":\\\"VALID_NAME_FORMAT\\\"}];\\n    } else {\\n        return [];\\n    }\\n}\\n\"}],\"name\":\"sn\"},{\"policyRequirements\":[\"REQUIRED\",\"VALID_PHONE_FORMAT\"],\"policies\":[{\"policyRequirements\":[\"REQUIRED\"],\"policyId\":\"required\",\"policyFunction\":\"\\nfunction (fullObject, value, params, propName) {\\n    if (value === undefined) {\\n        return [{\\\"policyRequirement\\\":\\\"REQUIRED\\\"}];\\n    }\\n    return [];\\n}\\n\"},{\"policyRequirements\":[\"REQUIRED\"],\"policyId\":\"not-empty\",\"policyFunction\":\"\\nfunction (fullObject, value, params, property) {\\n    if (value !== undefined && (value === null || !value.length)) {\\n        return [{\\\"policyRequirement\\\":\\\"REQUIRED\\\"}];\\n    } else {\\n        return [];\\n    }\\n}\\n\"},{\"policyRequirements\":[\"VALID_PHONE_FORMAT\"],\"policyId\":\"valid-phone-format\",\"policyFunction\":\"\\nfunction (fullObject, value, params, property) {\\n    var phonePattern = /^\\\\+?([0-9\\\\- \\\\(\\\\)])*$/;\\n    if (typeof (value) !== \\\"string\\\" || !value.length || !phonePattern.test(value)) {\\n        return [{\\\"policyRequirement\\\":\\\"VALID_PHONE_FORMAT\\\"}];\\n    } else {\\n        return [];\\n    }\\n}\\n\"}],\"name\":\"telephoneNumber\"},{\"policyRequirements\":[\"REQUIRED\"],\"policies\":[{\"policyRequirements\":[\"REQUIRED\"],\"policyId\":\"required-if-configured\",\"params\":{\"baseKey\":\"configuration.securityQuestions\",\"exceptRoles\":[\"openidm-admin\"],\"configBase\":\"ui/configuration\"}}],\"name\":\"securityQuestion\"},{\"policyRequirements\":[\"REQUIRED\",\"MIN_LENGTH\",\"REAUTH_REQUIRED\"],\"policies\":[{\"policyRequirements\":[\"REQUIRED\"],\"policyId\":\"required-if-configured\",\"params\":{\"baseKey\":\"configuration.securityQuestions\",\"exceptRoles\":[\"openidm-admin\"],\"configBase\":\"ui/configuration\"}},{\"policyRequirements\":[\"MIN_LENGTH\"],\"params\":{\"minLength\":16},\"policyId\":\"minimum-length\",\"policyFunction\":\"\\nfunction (fullObject, value, params, property) {\\n    if (typeof (value) !== \\\"string\\\" || !value.length || value.length < params.minLength) {\\n        return [{\\\"policyRequirement\\\":\\\"MIN_LENGTH\\\", \\\"params\\\":{\\\"minLength\\\":params.minLength}}];\\n    } else {\\n        return [];\\n    }\\n}\\n\"},{\"policyRequirements\":[\"REAUTH_REQUIRED\"],\"policyId\":\"re-auth-required\",\"params\":{\"exceptRoles\":[\"openidm-admin\",\"openidm-reg\"]}}],\"name\":\"securityAnswer\"},{\"policyRequirements\":[\"NO_MORE_THAN_X_ATTEMPTS_WITHIN_Y_MINUTES\"],\"policies\":[{\"policyRequirements\":[\"NO_MORE_THAN_X_ATTEMPTS_WITHIN_Y_MINUTES\"],\"policyId\":\"max-attempts-triggers-lock-cooldown\",\"params\":{\"dateTimeField\":\"lastSecurityAnswerAttempt\",\"numMinutes\":15,\"max\":3}}],\"name\":\"securityAnswerAttempts\"},{\"policyRequirements\":[\"VALID_DATE\"],\"policies\":[{\"policyRequirements\":[\"VALID_DATE\"],\"policyId\":\"valid-date\",\"policyFunction\":\"\\nfunction (fullObject, value, params, property) {\\n    if (typeof (value) !== \\\"string\\\" || !value.length || isNaN(new Date(value).getTime())) {\\n        return [{\\\"policyRequirement\\\":\\\"VALID_DATE\\\"}];\\n    } else {\\n        return [];\\n    }\\n}\\n\"}],\"name\":\"lastSecurityAnswerAttempt\"},{\"policyRequirements\":[\"MIN_LENGTH\",\"REQUIRED\"],\"policies\":[{\"policyRequirements\":[\"MIN_LENGTH\"],\"params\":{\"minLength\":4},\"policyId\":\"minimum-length\",\"policyFunction\":\"\\nfunction (fullObject, value, params, property) {\\n    if (typeof (value) !== \\\"string\\\" || !value.length || value.length < params.minLength) {\\n        return [{\\\"policyRequirement\\\":\\\"MIN_LENGTH\\\", \\\"params\\\":{\\\"minLength\\\":params.minLength}}];\\n    } else {\\n        return [];\\n    }\\n}\\n\"},{\"policyRequirements\":[\"REQUIRED\"],\"policyId\":\"required-if-configured\",\"params\":{\"baseKey\":\"configuration.siteIdentification\",\"exceptRoles\":[\"openidm-admin\"],\"configBase\":\"ui/configuration\"}}],\"name\":\"passPhrase\"}]}"
            ]
        );
    };
});

