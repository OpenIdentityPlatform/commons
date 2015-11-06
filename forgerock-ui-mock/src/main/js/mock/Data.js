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

/*global define, _ */

 define("mock/Data", [
     "lodash",
     "org/forgerock/commons/ui/common/main/Configuration",
     "org/forgerock/mock/ui/common/main/LocalStorage"
 ], function (_, Configuration, LocalStorage) {
    return function (server) {

    var kbaQuestions = [
        {
            "id": "1",
            "question": {
                "en_GB": "What's your favorite colour?",
                "fr": "Quelle est votre couleur préférée?",
                "en": "What's your favorite color?"
            }
        },
        {
            "id": "2",
            "question": {
                "en": "Who was your first employer?"
            }
        }
    ];

        server.respondWith(
            "GET",
            "/mock/selfservice/kbaQuestions",
            [
                200,
                {
                    "Content-Type": "application/json;charset=UTF-8"
                },
                JSON.stringify({
                    "questions" : kbaQuestions
                })
            ]
        );

        server.respondWith(
            "PATCH",
            "/mock/selfservice/user",
            function (request) {
                var requestContent = JSON.parse(request.requestBody),
                    headers = { "Content-Type": "application/json;charset=UTF-8" };

                request.respond(
                    200,
                    headers,
                    JSON.stringify(LocalStorage.patch("mock/repo/internal/user/" + Configuration.loggedUser.id, requestContent))
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
                        "type" : "userQuery",
                        "tag" : "initial",
                        "requirements" : {
                            "$schema": "http://json-schema.org/draft-04/schema#",
                            "description": "Find your account",
                            "type" : "object",
                            "required" : [
                                "queryFilter"
                            ],
                            "properties" : {
                                "queryFilter" : {
                                    "description" : "filter string to find account",
                                    "type" : "string"
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
                    headers = { "Content-Type": "application/json;charset=UTF-8" };
                switch (requestContent.token) {
                    case undefined:
                        if (_.isObject(requestContent.input) && _.isString(requestContent.input.queryFilter)) {
                            request.respond(
                                200,
                                headers,
                                JSON.stringify({
                                    "type" : "retrieveUsername",
                                    "tag" : "end",
                                    "additions" : {
                                        "userName" : "test"
                                    },
                                    "status" : {
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
                        "type" : "userQuery",
                        "tag" : "initial",
                        "requirements" : {
                            "$schema": "http://json-schema.org/draft-04/schema#",
                            "description": "Find your account",
                            "type" : "object",
                            "required" : [
                                "queryFilter"
                            ],
                            "properties" : {
                                "queryFilter" : {
                                    "description" : "filter string to find account",
                                    "type" : "string"
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
                    headers = { "Content-Type": "application/json;charset=UTF-8" };
                switch (requestContent.token) {
                    case undefined:
                        if (_.isObject(requestContent.input) && _.isString(requestContent.input.queryFilter)) {
                            request.respond(
                                200,
                                headers,
                                JSON.stringify({
                                    "token": "mockToken1",
                                    "type" : "emailValidation",
                                    "tag" : "validateCode",
                                    "requirements" : {
                                        "$schema": "http://json-schema.org/draft-04/schema#",
                                        "description": "Verify email address",
                                        "type" : "object",
                                        "required" : [
                                            "code"
                                        ],
                                        "properties" : {
                                            "code" : {
                                                "description" : "Enter code emailed to your address",
                                                "type" : "string"
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
                                                    "systemQuestion": kbaQuestions[0].question,
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
                                    "token" : "mockToken3",
                                    "type" : "resetStage",
                                    "tag" : "initial",
                                    "requirements" : {
                                        "$schema": "http://json-schema.org/draft-04/schema#",
                                        "description": "Reset password",
                                        "type" : "object",
                                        "required" : [
                                            "password"
                                        ],
                                        "properties" : {
                                            "password" : {
                                                "description" : "Password",
                                                "type" : "string"
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
                                    "type" : "emailValidation",
                                    "tag" : "end",
                                    "status" : {
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
                JSON.stringify(
                    {
                        "type" : "emailValidation",
                        "tag" : "initial",
                        "requirements" : {
                            "$schema": "http://json-schema.org/draft-04/schema#",
                            "description": "Register your account",
                            "type" : "object",
                            "required" : [
                                "mail"
                            ],
                            "properties" : {
                                "mail" : {
                                    "description" : "Email address",
                                    "type" : "string"
                                }
                            }
                        }
                    }
                )
            ]
        );

        server.respondWith(
            "POST",
            "/mock/selfservice/registration?_action=submitRequirements",
            function (request) {
                var requestContent = JSON.parse(request.requestBody),
                    headers = { "Content-Type": "application/json;charset=UTF-8" };
                switch (requestContent.token) {
                    case undefined:
                    if (_.isObject(requestContent.input) && _.isString(requestContent.input.mail)) {
                        request.respond(
                            200,
                            headers,
                            JSON.stringify({
                                "token": "mockToken1",
                                "type" : "emailValidation",
                                "tag" : "validateCode",
                                "requirements" : {
                                    "$schema": "http://json-schema.org/draft-04/schema#",
                                    "description": "Verify email address",
                                    "type" : "object",
                                    "required" : [
                                        "code"
                                    ],
                                    "properties" : {
                                        "code" : {
                                            "description" : "Enter code emailed to your address",
                                            "type" : "string"
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
                case "mockToken1":
                    if (_.isObject(requestContent.input) &&
                        _.isString(requestContent.input.code) &&
                        requestContent.input.code === "12345") {
                            request.respond(
                                200,
                                headers,
                                JSON.stringify({
                                    "token": "mockToken2",
                                    "type": "userDetails",
                                    "tag": "initial",
                                    "requirements": {
                                        "$schema": "http://json-schema.org/draft-04/schema#",
                                        "type": "object",
                                        "description": "New user details",
                                        "required": [
                                            "user"
                                        ],
                                        "properties": {
                                            "user": {
                                                "description": "User details",
                                                "type": "object"
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
                                "message": "Invalid code"
                            })
                        );
                    }
                break;
                case "mockToken2":
                    if (_.isObject(requestContent.input) &&
                        _.isObject(requestContent.input.user)) {
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
                                    "questions": kbaQuestions
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
                                "message": "Missing required input"
                            })
                        );
                    }
                break;
                case "mockToken3":
                    if (_.isObject(requestContent.input) &&
                        _.isObject(requestContent.input.kba)) {
                        request.respond(
                            200,
                            headers,
                            JSON.stringify({
                                "type" : "selfRegistration",
                                "tag" : "end",
                                "status" : {
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
        });

    };
});
