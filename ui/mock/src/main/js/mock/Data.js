/**
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
 * Copyright 2011-2016 ForgeRock AS.
 */

define([
    "lodash",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/mock/ui/common/main/LocalStorage"
], function (_, Configuration, LocalStorage) {
    return function (server) {

        var kbaQuestions = {
            "1": {
                "en": "What's your favorite color?",
                "en_GB": "What's your favorite colour?",
                "fr": "Quelle est votre couleur préférée?"
            },
            "2": {
                "en": "Who was your first employer?"
            }
        };

        server.respondWith(
            "GET",
            "/mock/selfservice/kba",
            [
                200,
                {
                    "Content-Type": "application/json;charset=UTF-8"
                },
                JSON.stringify({
                    "questions": kbaQuestions
                })
            ]
        );

        server.respondWith(
            "POST",
            /\/mock\/loginCheck/,
            function (request) {
                if (!Configuration.loggedUser) {
                    request.respond(
                        401,
                        {},
                        JSON.stringify({"code": 401, "reason": "Unauthorized", "message": "Access Denied"})
                    );
                } else {
                    request.respond(
                        200,
                        {},
                        JSON.stringify({"code": 200, details: Configuration.loggedUser})
                    );
                }
            }
        );

        server.respondWith(
            "PATCH",
            /\/mock\/selfservice\/user\/.*/,
            function (request) {
                var requestContent = JSON.parse(request.requestBody),
                    headers = {"Content-Type": "application/json;charset=UTF-8"};

                request.respond(
                    200,
                    headers,
                    JSON.stringify(LocalStorage.patch("mock/repo/internal/user/" + Configuration.loggedUser.id,
                        requestContent))
                );
            }
        );

        server.respondWith(
            "GET",
            "/mock/selfservice/username",
            [
                200,
                {
                    "Content-Type": "application/json;charset=UTF-8"
                },
                JSON.stringify(
                    {
                        "type": "userQuery",
                        "tag": "initial",
                        "requirements": {
                            "$schema": "http://json-schema.org/draft-04/schema#",
                            "description": "Find your account",
                            "type": "object",
                            "required": [
                                "queryFilter"
                            ],
                            "properties": {
                                "queryFilter": {
                                    "description": "filter string to find account",
                                    "type": "string"
                                }
                            }
                        }
                    }
                )
            ]
        );


        server.respondWith(
            "POST",
            "/mock/selfservice/username?_action=submitRequirements",
            function (request) {
                var requestContent = JSON.parse(request.requestBody),
                    headers = {"Content-Type": "application/json;charset=UTF-8"};
                switch (requestContent.token) {
                    case undefined:
                        if (_.isObject(requestContent.input) && _.isString(requestContent.input.queryFilter)) {
                            request.respond(
                                200,
                                headers,
                                JSON.stringify({
                                    "type": "retrieveUsername",
                                    "tag": "end",
                                    "additions": {
                                        "userName": "test"
                                    },
                                    "status": {
                                        "success": true
                                    }
                                })
                            );
                        } else {
                            request.respond(
                                400,
                                headers,
                                JSON.stringify({
                                    "code": 400,
                                    "reason": "Bad Request",
                                    "message": "queryFilter is missing"
                                })
                            );
                        }
                        break;
                    default:
                        request.respond(
                            400,
                            headers,
                            JSON.stringify({
                                "code": 400,
                                "reason": "Bad Request",
                                "message": "Token provided not recognized"
                            })
                        );
                }
            }
        );


        server.respondWith(
            "GET",
            "/mock/selfservice/reset",
            [
                200,
                {
                    "Content-Type": "application/json;charset=UTF-8"
                },
                JSON.stringify(
                    {
                        "type": "userQuery",
                        "tag": "initial",
                        "requirements": {
                            "$schema": "http://json-schema.org/draft-04/schema#",
                            "description": "Find your account",
                            "type": "object",
                            "required": [
                                "queryFilter"
                            ],
                            "properties": {
                                "queryFilter": {
                                    "description": "filter string to find account",
                                    "type": "string"
                                }
                            }
                        }
                    }
                )
            ]
        );

        server.respondWith(
            "POST",
            "/mock/selfservice/reset?_action=submitRequirements",
            function (request) {
                var requestContent = JSON.parse(request.requestBody),
                    headers = {"Content-Type": "application/json;charset=UTF-8"};
                switch (requestContent.token) {
                    case undefined:
                        if (_.isObject(requestContent.input) && _.isString(requestContent.input.queryFilter)) {
                            request.respond(
                                200,
                                headers,
                                JSON.stringify({
                                    "token": "mockToken1",
                                    "type": "emailValidation",
                                    "tag": "validateCode",
                                    "requirements": {
                                        "$schema": "http://json-schema.org/draft-04/schema#",
                                        "description": "Verify email address",
                                        "type": "object",
                                        "required": [
                                            "code"
                                        ],
                                        "properties": {
                                            "code": {
                                                "description": "Enter code emailed to your address",
                                                "type": "string"
                                            }
                                        }
                                    }
                                })
                            );
                        } else {
                            request.respond(
                                400,
                                headers,
                                JSON.stringify({
                                    "code": 400,
                                    "reason": "Bad Request",
                                    "message": "queryFilter is missing"
                                })
                            );
                        }
                        break;
                    case "mockToken1":
                        if (_.isObject(requestContent.input) &&
                            _.isString(requestContent.input.code) &&
                            requestContent.input.code === "12345") {
                            request.respond(
                                200,
                                headers,
                                JSON.stringify({
                                    "type": "kbaSecurityAnswerVerificationStage",
                                    "tag": "initial",
                                    "requirements": {
                                        "$schema": "http://json-schema.org/draft-04/schema#",
                                        "description": "Answer security questions",
                                        "type": "object",
                                        "required": [
                                            "answer1",
                                            "answer2"
                                        ],
                                        "properties": {
                                            "answer1": {
                                                "systemQuestion": kbaQuestions["1"],
                                                "type": "string"
                                            },
                                            "answer2": {
                                                "userQuestion": "Who is your favorite author?",
                                                "type": "string"
                                            }
                                        }
                                    },
                                    "token": "mockToken2"
                                })
                            );
                        } else {
                            request.respond(
                                400,
                                headers,
                                JSON.stringify({
                                    "code": 400,
                                    "reason": "Bad Request",
                                    "message": "Invalid code"
                                })
                            );
                        }
                        break;
                    case "mockToken2":
                        if (_.isObject(requestContent.input) &&
                            _.isString(requestContent.input.answer1) &&
                            _.isString(requestContent.input.answer2)) {
                            request.respond(
                                200,
                                headers,
                                JSON.stringify({
                                    "token": "mockToken3",
                                    "type": "resetStage",
                                    "tag": "initial",
                                    "requirements": {
                                        "$schema": "http://json-schema.org/draft-04/schema#",
                                        "description": "Reset password",
                                        "type": "object",
                                        "required": [
                                            "password"
                                        ],
                                        "properties": {
                                            "password": {
                                                "description": "Password",
                                                "type": "string"
                                            }
                                        }
                                    }
                                })
                            );
                        } else {
                            request.respond(
                                400,
                                headers,
                                JSON.stringify({
                                    "code": 400,
                                    "reason": "Bad Request",
                                    "message": "answer1 is missing from input"
                                })
                            );
                        }
                        break;
                    case "mockToken3":
                        if (_.isObject(requestContent.input) &&
                            _.isString(requestContent.input.password)) {
                            request.respond(
                                200,
                                headers,
                                JSON.stringify({
                                    "type": "emailValidation",
                                    "tag": "end",
                                    "status": {
                                        "success": true
                                    }
                                })
                            );
                        } else {
                            request.respond(
                                400,
                                headers,
                                JSON.stringify({
                                    "code": 400,
                                    "reason": "Bad Request",
                                    "message": "password is missing from input"
                                })
                            );
                        }
                        break;
                    default:
                        request.respond(
                            400,
                            headers,
                            JSON.stringify({
                                "code": 400,
                                "reason": "Bad Request",
                                "message": "Token provided not recognized"
                            })
                        );
                }
            }
        );


        server.respondWith(
            "GET",
            "/mock/selfservice/registration",
            [
                200,
                {
                    "Content-Type": "application/json;charset=UTF-8"
                },
                JSON.stringify({
                    "type": "userDetails",
                    "tag": "initial",
                    "requirements": {
                        "$schema": "http://json-schema.org/draft-04/schema#",
                        "type": "object",
                        "description": "New user details",
                        "required": [],
                        "properties": {
                            "user": {
                                "description": "User details",
                                "type": "object",
                                "required": [
                                    "userName",
                                    "name",
                                    "emails"
                                ],
                                "properties": {
                                    "userName": {
                                        "description": "User Name",
                                        "type": "string"
                                    },
                                    "emails": {
                                        "type": "array",
                                        "items": {
                                            "description": "Email",
                                            "type": "object",
                                            "required": [
                                                "value"
                                            ],
                                            "properties": {
                                                "type": {
                                                    "description": "Type",
                                                    "type": "string"
                                                },
                                                "value": {
                                                    "description": "Value",
                                                    "type": "string"
                                                },
                                                "primary": {
                                                    "description": "Primary",
                                                    "type": "boolean"
                                                }
                                            }
                                        }
                                    },
                                    "name": {
                                        "description": "Name",
                                        "type": "object",
                                        "required": [
                                            "familyName",
                                            "givenName"
                                        ],
                                        "properties": {
                                            "honorificSuffix": {
                                                "description": "Suffix",
                                                "type": "string"
                                            },
                                            "familyName": {
                                                "description": "Family Name",
                                                "type": "string"
                                            },
                                            "givenName": {
                                                "description": "Given Name",
                                                "type": "string"
                                            },
                                            "honorificPrefix": {
                                                "description": "Prefix",
                                                "type": "string"
                                            },
                                            "middleName": {
                                                "description": "Middle Name",
                                                "type": "string"
                                            }
                                        }
                                    }
                                }
                            },
                            "provider": {
                                "description": "OAuth IDP Name",
                                "type": "string"
                            },
                            "code": {
                                "description": "OAuth Authorization Code",
                                "type": "string"
                            },
                            "redirect_uri": {
                                "description": "URI where IDP returns the Authorization Code",
                                "type": "string"
                            }
                        },
                        "definitions": {
                            "providers": {
                                "type": "array",
                                "items": {
                                    "type": "object",
                                    "oneOf": [
                                        {
                                            "name": "Google",
                                            "type": "openid_connect",
                                            "icon": "https://developers.google.com/accounts/images/sign-in-with-google.png", //eslint-disable-line max-len
                                            "client_id": "#yourClientIDHere",
                                            "authorization_endpoint": "https://accounts.google.com/o/oauth2/v2/auth", //eslint-disable-line max-len,
                                            "scopes": "openid profile email"
                                        },
                                        {
                                            "name": "Mock",
                                            "type": "openid_connect",
                                            "icon": "images/mockSignIn.png",
                                            "client_id": "mockClientIDXYZ1234",
                                            "authorization_endpoint": "mockOAuthAuthorization.html",
                                            "scopes": "openid profile email mock"
                                        }
                                    ]
                                }
                            }
                        }
                    }
                })
            ]
        );

        server.respondWith(
            "POST",
            "/mock/selfservice/registration?_action=submitRequirements",
            function (request) {
                var requestContent = JSON.parse(request.requestBody),
                    headers = {"Content-Type": "application/json;charset=UTF-8"};
                switch (requestContent.token) {
                    case undefined:
                    case "mockToken1":
                        if (_.isObject(requestContent.input) &&
                            _.isObject(requestContent.input.user)) {
                            request.respond(
                                200,
                                headers,
                                JSON.stringify({
                                    "token": "mockToken2",
                                    "type": "emailValidation",
                                    "tag": "validateCode",
                                    "requirements": {
                                        "$schema": "http://json-schema.org/draft-04/schema#",
                                        "description": "Verify email address",
                                        "type": "object",
                                        "required": [
                                            "code"
                                        ],
                                        "properties": {
                                            "code": {
                                                "description": "Enter code emailed to your address",
                                                "type": "string"
                                            }
                                        }
                                    }
                                })
                            );
                        } else if (_.isObject(requestContent.input) &&
                            _.isString(requestContent.input.code) &&
                            _.isString(requestContent.input.provider) &&
                            _.isString(requestContent.input.redirect_uri)) {
                            request.respond(
                                200,
                                headers,
                                JSON.stringify({
                                    "token": "mockToken1",
                                    "type": "userDetails",
                                    "tag": "validateUserProfile",
                                    "requirements": {
                                        "$schema": "http://json-schema.org/draft-04/schema#",
                                        "description": "Verify user profile",
                                        "type": "object",
                                        "required": [
                                            "user"
                                        ],
                                        "properties": {
                                            "user": {
                                                "description": "User details",
                                                "type": "object",
                                                "required": [
                                                    "name",
                                                    "userName",
                                                    "emails"
                                                ],
                                                "properties": {
                                                    "userName": {
                                                        "default": "brmiller",
                                                        "description": "User Name",
                                                        "type": "string"
                                                    },
                                                    "emails": {
                                                        "type": "array",
                                                        "items": {
                                                            "description": "Email",
                                                            "type": "object",
                                                            "required": [
                                                                "value"
                                                            ],
                                                            "properties": {
                                                                "type": {
                                                                    "description": "Type",
                                                                    "type": "string"
                                                                },
                                                                "value": {
                                                                    "description": "Value",
                                                                    "type": "string"
                                                                },
                                                                "primary": {
                                                                    "description": "Primary",
                                                                    "type": "boolean"
                                                                }
                                                            }
                                                        },
                                                        "default": [
                                                            {
                                                                "value": "brendan.miller@example.com",
                                                                "type": "other",
                                                                "primary": true
                                                            }
                                                        ]
                                                    },
                                                    "name": {
                                                        "description": "Name",
                                                        "type": "object",
                                                        "required": [
                                                            "familyName",
                                                            "givenName"
                                                        ],
                                                        "properties": {
                                                            "honorificSuffix": {
                                                                "description": "Suffix",
                                                                "type": "string"
                                                            },
                                                            "familyName": {
                                                                "default": "Miller",
                                                                "description": "Family Name",
                                                                "type": "string"
                                                            },
                                                            "givenName": {
                                                                "default": "Brendan",
                                                                "description": "Given Name",
                                                                "type": "string"
                                                            },
                                                            "honorificPrefix": {
                                                                "description": "Prefix",
                                                                "type": "string"
                                                            },
                                                            "middleName": {
                                                                "description": "Middle Name",
                                                                "type": "string"
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                })
                            );
                        } else {
                            request.respond(
                                400,
                                headers,
                                JSON.stringify({
                                    "code": 400,
                                    "reason": "Bad Request",
                                    "message": "mail is missing"
                                })
                            );
                        }
                        break;
                    case "mockToken2":
                        if (_.isObject(requestContent.input) &&
                            _.isString(requestContent.input.code) &&
                            requestContent.input.code === "12345") {
                            request.respond(
                                200,
                                headers,
                                JSON.stringify({
                                    "type": "kbaSecurityAnswerDefinitionStage",
                                    "tag": "initial",
                                    "requirements": {
                                        "$schema": "http://json-schema.org/draft-04/schema#",
                                        "description": "Knowledge based questions",
                                        "type": "object",
                                        "required": [
                                            "kba"
                                        ],
                                        "properties": {
                                            "kba": {
                                                "type": "array",
                                                "minItems": 2,
                                                "items": {
                                                    "type": "object",
                                                    "oneOf": [
                                                        {
                                                            "$ref": "#/definitions/systemQuestion"
                                                        },
                                                        {
                                                            "$ref": "#/definitions/userQuestion"
                                                        }
                                                    ]
                                                },
                                                "questions": _.map(kbaQuestions, function (value, key) {
                                                    return {
                                                        "id": key,
                                                        "question": value
                                                    };
                                                })
                                            }
                                        },
                                        "definitions": {
                                            "userQuestion": {
                                                "description": "User Question",
                                                "type": "object",
                                                "required": [
                                                    "customQuestion",
                                                    "answer"
                                                ],
                                                "properties": {
                                                    "customQuestion": {
                                                        "description": "Question defined by the user",
                                                        "type": "string"
                                                    },
                                                    "answer": {
                                                        "description": "Answer to the question",
                                                        "type": "string"
                                                    }
                                                },
                                                "additionalProperties": false
                                            },
                                            "systemQuestion": {
                                                "description": "System Question",
                                                "type": "object",
                                                "required": [
                                                    "questionId",
                                                    "answer"
                                                ],
                                                "properties": {
                                                    "questionId": {
                                                        "description": "Id of predefined question",
                                                        "type": "string"
                                                    },
                                                    "answer": {
                                                        "description": "Answer to the referenced question",
                                                        "type": "string"
                                                    }
                                                },
                                                "additionalProperties": false
                                            }
                                        }
                                    },
                                    "token": "mockToken3"
                                })
                            );
                        } else {
                            request.respond(
                                400,
                                headers,
                                JSON.stringify({
                                    "code": 400,
                                    "reason": "Bad Request",
                                    "message": "Invalid code"
                                })
                            );
                        }
                        break;
                    case "mockToken3":
                        if (_.isObject(requestContent.input) &&
                            _.isArray(requestContent.input.kba) &&
                            requestContent.input.kba.length >= 2) {
                            request.respond(
                                200,
                                headers,
                                JSON.stringify({
                                    "type": "selfRegistration",
                                    "tag": "end",
                                    "status": {
                                        "success": true
                                    }
                                })
                            );
                        } else {
                            request.respond(
                                400,
                                headers,
                                JSON.stringify({
                                    "code": 400,
                                    "reason": "Bad Request",
                                    "message": "Missing required input"
                                })
                            );
                        }
                }
            });

    };
});
