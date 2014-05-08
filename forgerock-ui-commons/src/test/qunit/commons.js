/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014 ForgeRock AS. All Rights Reserved
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

/*global require, define, QUnit */

define([
        "org/forgerock/commons/ui/common/main/Configuration"
    ], function (conf) {
    return {
        executeAll: function (server, parameters) {

            var testPromise = $.Deferred();

            module('Common Tests');

            QUnit.test("Test setup", function () {

                QUnit.ok(parameters.username !== undefined && parameters.username.length        , "A username has been passed into the test suite");
                QUnit.ok(parameters.password !== undefined && parameters.password.length        , "A password has been passed into the test suite");

            });

            QUnit.asyncTest("Login Page", function () {
                var loginView = require("LoginView");
                loginView.element = $("<div>")[0];
                loginView.render([], function () {
                    QUnit.start();

                    QUnit.ok($("#login", loginView.$el).length                                  , "Username field available");
                    QUnit.ok($("#password", loginView.$el).length                               , "Password field available");

                    $("#login", loginView.$el).val(parameters.username).trigger('keyup');
                    $("#password", loginView.$el).val(parameters.password).trigger('keyup');

                    $("[name=loginButton]", loginView.$el).trigger("click");

                    QUnit.ok(conf.loggedUser !== undefined                                      , "User should be logged in");

                    testPromise.resolve(); // make sure this is only called after the last async test is finished
                });
            });

            return testPromise;

        }
    };
});