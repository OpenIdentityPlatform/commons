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
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/util/CookieHelper",
    "./getLoggedUser"
], function (eventManager, constants, localStorage, conf, cookieHelper, getLoggedUser) {
    return {
        executeAll: function (server, parameters) {

            var userRegPromise = $.Deferred(),
                rememberPromise = $.Deferred(),
                securityDataPromise = $.Deferred();

            module('Mock Tests');

            QUnit.asyncTest("Remember Login", function () {

                conf.loggedUser = null;
                var loginView = require("LoginView");
                loginView.element = $("<div>")[0];

                delete loginView.route;

                localStorage.remove('remember-login');

                loginView.render([], function () {

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

                    cookieHelper.deleteCookie("login");

                    QUnit.start();
                });
            });            

            QUnit.asyncTest('User Registration', function () {
                conf.loggedUser = null;

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

                    // initial state
                    QUnit.equal($('input[name="register"]', view.$el).prop('disabled'), true, 'Initial state of submit button is disabled');

                    testStatusAttr(userName, 'error');
                    testStatusAttr(mail, 'error');
                    testStatusAttr(givenName, 'error');
                    testStatusAttr(lastName, 'error');
                    testStatusAttr(phone, 'ok'); // phone is optional
                    testStatusAttr(pwd, 'error');
                    testStatusAttr(pwdConfirm, 'error');
                    testStatusAttr(terms, 'error');

                    QUnit.ok(!terms.is(':checked'), 'Terms of use not checked by default');

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
                    terms.prop('checked', true).trigger('change');
                    QUnit.equal($('.validation-message-checkbox').text(), '', '"Acceptance required for registration" is not shown for checked checkbox');
                    testStatusAttr(terms, 'ok');

                    // register button
                    QUnit.ok(register.prop('disabled') === false, 'Register button is enabled for correct field values');


                    // localStorage.removeItem('mock/repo/internal/user/qqq');
                    register.trigger('click');
                    QUnit.ok(conf.loggedUser && conf.loggedUser.userName === "qqq", 'Logged in with newly created user');

                    QUnit.start();

                });
            });

            QUnit.asyncTest('Change Security Data', function () {
                conf.loggedUser = getLoggedUser();

                var loginView = require("LoginView");
                loginView.render([], function () {
                    $("#login", loginView.$el).val(parameters.username).trigger('keyup');
                    $("#password", loginView.$el).val(parameters.password).trigger('keyup');
                    $("[name=loginButton]", loginView.$el).trigger("click");
                });

                var changeDataView = require('ChangeSecurityDataDialog');
                changeDataView.element = $("<div>")[0];

                delete changeDataView.route;
                changeDataView.render([], function () {
                    var pwd = $('input[name="password"]', changeDataView.$el),
                        pwdConfirm = $('input[name="passwordConfirm"]', changeDataView.$el);

                    // Test if inputs and submit button are available
                    QUnit.ok($('input[name="password"]', changeDataView.$el).length, "Password field is available");
                    QUnit.ok($('input[name="passwordConfirm"]', changeDataView.$el).length, "Password confirm field is available");
                    QUnit.ok($('input[name="Update"]', changeDataView.$el).length, "Submit button is available");

                    // Check submit button initial status
                    QUnit.ok($('input[name="Update"]', changeDataView.$el).prop('disabled'), 'Initial state of submit button is disabled');

                    // Check if inputs pass validation
                    QUnit.equal($('input[name="password"]', changeDataView.$el).data('validation-status'), 'error', "Empty password field doesn't pass validation");
                    QUnit.equal($('input[name="passwordConfirm"]', changeDataView.$el).data('validation-status'), 'error', "Empty password confirm field doesn't pass validation");

                    var passwordConfirmMatchesPassword = $('[data-for-validator="passwordConfirm"]', changeDataView.$el).parent(),
                        passwordRequired = $('[data-for-req="REQUIRED"]', changeDataView.$el).parent(),
                        passwordContainsNumbers = $('[data-for-req="AT_LEAST_X_NUMBERS"]', changeDataView.$el).parent(),
                        passwordMinLength = $('[data-for-req="MIN_LENGTH"]', changeDataView.$el).parent(),
                        passwordContainsCapitalLetters = $('[data-for-req="AT_LEAST_X_CAPITAL_LETTERS"]', changeDataView.$el).parent();

                    pwd.val('abc').trigger('change');
                    QUnit.ok(passwordRequired.find("span.error").length === 0, 'Password field cannot be blank');

                    pwd.val('abc1').trigger('change');
                    QUnit.ok(passwordContainsNumbers.find("span.error").length === 0, 'Number added to password satisfied AT_LEAST_X_NUMBERS');

                    pwd.val('abcdefgh').trigger('change');
                    QUnit.ok(passwordMinLength.find("span.error").length === 0, 'Password length satisfied MIN_LENGTH');

                    pwd.val('abCdefgh').trigger('change');
                    QUnit.ok(passwordContainsCapitalLetters.find("span.error").length === 0, 'Capital letter added to password satisfied AT_LEAST_X_CAPITAL_LETTERS');

                    pwd.val('1122334455').trigger('change');
                    QUnit.equal($('input[name="password"]', changeDataView.$el).data('validation-status'), 'error', "Password doesn't pass validation");

                    pwd.val('Passw0rd').trigger('change');
                    pwdConfirm.val('Passw0rd').trigger('change');
                    QUnit.equal($('input[name="password"]', changeDataView.$el).attr('data-validation-status'), 'ok', 'Password passes validation');
                    QUnit.equal($('input[name="passwordConfirm"]', changeDataView.$el).attr('data-validation-status'), 'ok', 'Password confirm field passes validation');
                    QUnit.ok(passwordConfirmMatchesPassword.find("span.error").length === 0, 'Confirmation matches password');
                    QUnit.ok(!$('input[name="Update"]', changeDataView.$el).prop('disabled'), 'Submit button is enabled');

                    // Check if new password was set for user
                    pwd.val('Passw0rds').trigger('change');
                    pwdConfirm.val('Passw0rds').trigger('change');
                    $('input[name="Update"]', changeDataView.$el).trigger("click");
                    QUnit.ok(conf.loggedUser !== undefined, "User should be logged in");
                    QUnit.ok(conf.loggedUser.password == 'Passw0rds', "New password wasn't set for the user");

                    // log-out
                    localStorage.remove('mock/repo/internal/user/test');
                    conf.setProperty('loggedUser', null);

                    QUnit.start();
                });
            });
        }
    }
});