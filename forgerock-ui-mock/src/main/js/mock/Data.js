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
 ], function () {
    return function (server) {

        server.respondWith(
            "GET",
            "/mock/reset",
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
                            "description": "Reset your password",
                            "type" : "object",
                            "required" : [
                                "mail"
                            ],
                            "properties" : {
                                "mail" : {
                                    "description" : "Enter your email address",
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
            "/mock/reset?_action=submitRequirements",
            function (request) {
                var requestContent = JSON.parse(request.requestBody),
                    headers = { "Content-Type": "application/json;charset=UTF-8" };
                switch (requestContent.token) {
                    case undefined:
                        if (_.isObject(requestContent, "input") && _.isString(requestContent.input.mail)) {
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
                                    "message": "username is missing"
                                })
                            );
                        }
                    break;
                    case "mockToken1":
                        if (_.isObject(requestContent, "input") &&
                            _.isString(requestContent.input.code) &&
                            requestContent.input.code === "12345") {
                                request.respond(
                                    200,
                                    headers,
                                    JSON.stringify({
                                    "token" : "mockToken2",
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
                                    "message": "Invalid code"
                                })
                            );
                        }
                    break;
                    case "mockToken2":
                        if (_.isObject(requestContent, "input") &&
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
            "/mock/registration",
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
            "/mock/registration?_action=submitRequirements",
            function (request) {
                var requestContent = JSON.parse(request.requestBody),
                    headers = { "Content-Type": "application/json;charset=UTF-8" };
                switch (requestContent.token) {
                    case undefined:
                    if (_.isObject(requestContent, "input") && _.isString(requestContent.input.mail)) {
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
                                "message": "username is missing"
                            })
                        );
                    }
                break;
                case "mockToken1":
                    if (_.isObject(requestContent, "input") &&
                        _.isString(requestContent.input.code) &&
                        requestContent.input.code === "12345") {
                            request.respond(
                                200,
                                headers,
                                JSON.stringify({
                                    "token": "mockToken2",
                                    "type": "selfRegistration",
                                    "tag": "initial",
                                    "requirements": {
                                        "$schema": "http://json-schema.org/draft-04/schema#",
                                        "type": "object",
                                        "description": "New user details",
                                        "required": [
                                            "userId",
                                            "user"
                                        ],
                                        "properties": {
                                            "userId": {
                                                "description": "New user Id",
                                                "type": "string"
                                            },
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
                    if (_.isObject(requestContent, "input") &&
                        _.isString(requestContent.input.userId) &&
                        _.isObject(requestContent.input, "user")) {
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
