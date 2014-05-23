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
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/mock/ui/common/main/LocalStorage"
], function (eventManager, constants, localStorage) {
    return {
        executeAll: function (server, parameters) {

            var userRegPromise = $.Deferred(),
                rememberPromise = $.Deferred();

            module('Mock Tests');

            QUnit.asyncTest("Remember Login", function () {
                var loginView = require("LoginView");
                loginView.element = $("<div>")[0];
                delete loginView.route;

                localStorage.remove('remember-login');

                loginView.render([], function () {
                    QUnit.start();

                    // login with loginRemember checked
                    $("#login", loginView.$el).val(parameters.username).trigger('keyup');
                    $("#password", loginView.$el).val(parameters.password).trigger('keyup');
                    $("[name=loginRemember]", loginView.$el).trigger("click");
                    $("[name=loginButton]", loginView.$el).trigger("click");

                    QUnit.equal(localStorage.get('remember-login'), parameters.username, "Remember-login matches provided username");

                    eventManager.sendEvent(constants.EVENT_LOGOUT);

                    loginView.render();

                    QUnit.equal($("#login", loginView.$el).val(), parameters.username, "Username is remembered after logout.");                    

                    localStorage.remove('remember-login');
                    rememberPromise.resolve();
                });
            });            

            QUnit.asyncTest('User Registration', function () {

                var registerView = require('RegisterView');
                registerView.element = $("<div>")[0];
                delete registerView.route;

                registerView.render(null, function () {

                    var pwd = $('input[name="password"]', registerView.$el),
                        pwdConfirm = $('input[name="passwordConfirm"]', registerView.$el);

                    QUnit.start();
                    QUnit.equal($('input[name="register"]', registerView.$el).attr('disabled'), 'disabled'              , 'Initial state of submit button is not disabled');

                    QUnit.equal($('input[name="userName"]', registerView.$el).data('validation-status'), 'error'        , "Empty username field passes validation");
                    QUnit.equal($('input[name="mail"]', registerView.$el).data('validation-status'), 'error'            , "Empty e-mail field passes validation");
                    QUnit.equal($('input[name="givenName"]', registerView.$el).data('validation-status'), 'error'       , "Empty first name field passes validation");
                    QUnit.equal($('input[name="sn"]', registerView.$el).data('validation-status'), 'error'              , "Empty last name field passes validation");
                    QUnit.equal($('input[name="telephoneNumber"]', registerView.$el).data('validation-status'), 'error' , "Empty mobile phone field passes validation");
                    QUnit.equal($('input[name="password"]', registerView.$el).data('validation-status'), 'error'        , "Empty password field passes validation");
                    QUnit.equal($('input[name="passwordConfirm"]', registerView.$el).data('validation-status'), 'error' , "Empty password confirm field passes validation");

                    QUnit.equal($('input[name="terms"]', registerView.$el).data('validation-status'), 'error'           , "Initial state of terms checkbox passes validation");

                    pwd.val('abc').trigger('change');
                    QUnit.equal($('input[name="password"]', registerView.$el).attr('data-validation-status'), 'error'   , 'Simplest password rejected');

                    pwd.val('abc1').trigger('change');

                    QUnit.ok($("div.validationRules[data-for-validator='password passwordConfirm'] div.field-rule span[data-for-req='AT_LEAST_X_NUMBERS']", registerView.$el).parent().find("span.error").length === 0
                                                                                                                        , 'Number added to password satisfied AT_LEAST_X_NUMBERS');

                    pwd.val('Passw0rd').trigger('change');
                    pwdConfirm.val('Passw0rd').trigger('change');

                    QUnit.equal($('input[name="password"]', registerView.$el).attr('data-validation-status'), 'ok'      , 'Password passes validation');
                    QUnit.equal($('input[name="passwordConfirm"]', registerView.$el).attr('data-validation-status'), 'ok', 'Password confirm field passes validation');

                    userRegPromise.resolve();

                });
            });

            return $.when(userRegPromise, rememberPromise);
        }
    }
});