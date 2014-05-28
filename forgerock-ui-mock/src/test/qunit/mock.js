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
    "org/forgerock/mock/ui/common/main/LocalStorage",
    "org/forgerock/commons/ui/common/main/Configuration"
], function (eventManager, constants, localStorage, conf) {
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

                var view = require('RegisterView');
                view.element = $("<div>")[0];
                delete view.route;

                view.render(null, function () {
                    // fields
                    var register = $('input[name="register"]', view.$el),
                        userName = $('input[name="userName"]', view.$el),
                        mail = $('input[name="mail"]', view.$el),
                        givenName = $('input[name="givenName"]', view.$el),
                        lastName = $('input[name="sn"]', view.$el),
                        phone = $('input[name="telephoneNumber"]', view.$el),
                        terms = $('input[name="terms"]', view.$el),
                        pwd = $('input[name="password"]', view.$el),
                        pwdConfirm = $('input[name="passwordConfirm"]', view.$el),
                    // password validation messages
                        pwdConfirmMatchesPwd = $('[data-for-validator="passwordConfirm"]', view.$el),
                        pwdRequired = $('[data-for-req="REQUIRED"]', view.$el),
                        pwdContainsNumbers = $('[data-for-req="AT_LEAST_X_NUMBERS"]', view.$el),
                        pwdMinLength = $('[data-for-req="MIN_LENGTH"]', view.$el),
                        pwdContainsCapitalLetters = $('[data-for-req="AT_LEAST_X_CAPITAL_LETTERS"]', view.$el);


                    function testStatusAttr(field, validationStatus) {
                        QUnit.equal(field.attr('data-validation-status'), validationStatus,
                            'Validation status for ' + field.attr('name') + ' is "' + validationStatus + '" for value "' + field.val() + '"');
                    }

                    function testValue(field, value, expectedValidationMsg, validationStatus, view) {
                        field.val(value).trigger('change');
                        var fieldName = field.attr('name'),
                            validationMsg = $('.validation-message[for="' + fieldName + '"]', view.$el).text();
                        QUnit.equal(validationMsg, expectedValidationMsg,
                            'Message "' + expectedValidationMsg + '" is displayed for ' + fieldName + ' for value "' + value + '"');
                        testStatusAttr(field, validationStatus);
                    }

                    function testFieldRule(field, val, rule, ruleName, status) {
                        field.val(val).trigger('change');
                        QUnit.ok(status === 'ok' ? rule.parent().find('.ok').length === 1 : rule.parent().find('.error').length === 1,
                            '"' + ruleName + '" rule has "' + status + '" status for value "' + val + '"');
                    }

                    QUnit.start();

                    // initial state
                    QUnit.equal($('input[name="register"]', view.$el).attr('disabled'), 'disabled', 'Initial state of submit button is not disabled');

                    testStatusAttr(userName, 'error');
                    testStatusAttr(mail, 'error');
                    testStatusAttr(givenName, 'error');
                    testStatusAttr(lastName, 'error');
                    testStatusAttr(phone, 'error');
                    testStatusAttr(pwd, 'error');
                    testStatusAttr(pwdConfirm, 'error');
                    testStatusAttr(terms, 'error');

                    // password
                    testFieldRule(pwd, 'abc', pwdRequired, pwdRequired.attr('data-for-req'), 'ok');
                    testFieldRule(pwd, 'abc1', pwdContainsNumbers, pwdContainsNumbers.attr('data-for-req'), 'ok');
                    testFieldRule(pwd, 'abcdefg1', pwdMinLength, pwdMinLength.attr('data-for-req'), 'ok');
                    testFieldRule(pwd, 'Abcdefg1', pwdContainsCapitalLetters, pwdContainsCapitalLetters.attr('data-for-req'), 'ok');

                    testFieldRule(pwdConfirm, 'abcdefg1', pwdConfirmMatchesPwd, pwdConfirmMatchesPwd.attr('data-for-validator'), 'error');
                    testFieldRule(pwdConfirm, 'Abcdefg1', pwdConfirmMatchesPwd, pwdConfirmMatchesPwd.attr('data-for-validator'), 'ok');

                    testStatusAttr(pwd, 'ok');
                    testStatusAttr(pwdConfirm, 'ok');

                    // username
                    testValue(userName, '', 'Cannot be blank', 'error', view);
                    testValue(userName, 'qqq', '', 'ok', view);

                    // e-mail
                    testValue(mail, '', 'Cannot be blank', 'error', view);
                    testValue(mail, 'abc', 'Must be a valid email address', 'error', view);
                    testValue(mail, 'abc@', 'Must be a valid email address', 'error', view);
                    testValue(mail, 'abc@qqq', 'Must be a valid email address', 'error', view);
                    testValue(mail, 'abc@qqq.', 'Must be a valid email address', 'error', view);
                    testValue(mail, 'abc@qqq.com', '', 'ok', view);

                    // given name
                    testValue(givenName, '', 'Cannot be blank', 'error', view);
                    testValue(givenName, 'abc', '', 'ok', view);

                    // last name
                    testValue(lastName, '', 'Cannot be blank', 'error', view);
                    testValue(lastName, 'qqq', '', 'ok', view);

                    // phone
                    testValue(phone, 'abc', 'Must be a valid phone number', 'error', view);
                    testValue(phone, '12345', '', 'ok', view);

                    // terms of use
                    terms.attr('checked', true).trigger('click');
                    QUnit.equal($('.validation-message-checkbox').text(), '', '"Acceptance required for registration" is not shown for checked checkbox');
                    testStatusAttr(terms, 'ok');

                    // register button
                    QUnit.ok(typeof register.attr('disabled') === 'undefined', 'Register button is enabled for correct field values');


                    // localStorage.removeItem('mock/repo/internal/user/qqq');
                    register.trigger('click');
                    QUnit.ok(conf.loggedUser.userName === "qqq", 'Logged in with newly created user');


                    userRegPromise.resolve();

                });
            });

            return $.when(userRegPromise, rememberPromise);
        }
    }
});