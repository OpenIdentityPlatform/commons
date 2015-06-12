/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014-2015 ForgeRock AS.
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
    "sinon",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/components/Dialog",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/util/CookieHelper",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "org/forgerock/commons/ui/common/util/Base64",
    "org/forgerock/commons/ui/common/util/Mime",
    "org/forgerock/commons/ui/common/main/Router",
    "bootstrap-dialog",
    "./getLoggedUser"
], function (sinon, conf, Dialog, eventManager, constants, cookieHelper, UIUtils, Base64, Mime, router, BootstrapDialog, getLoggedUser) {
    return {
        executeAll: function (server, parameters) {

            module('Common Tests');

            QUnit.asyncTest("Login", function () {

                var loginView = require("LoginView");
                // intercept the framework's invocation of the render function to define our own callback function
                sinon.stub(loginView, "render", function (args, callback) {
                    loginView.render.restore();
                    loginView.render(args, function () {

                        QUnit.ok(!_.has(conf.globalData, 'hasOptionalUIFeatures'), "There should be no hasOptionalUIFeatures within conf.globalData (CUI-24)");

                        QUnit.ok($("#login", loginView.$el).length                                  , "Username field available");
                        QUnit.ok($("#password", loginView.$el).length                               , "Password field available");
                        QUnit.ok($("[name=loginButton]", loginView.$el).length                      , "Login button available");

                        $("#login", loginView.$el).val(parameters.username).trigger('keyup');
                        $("#password", loginView.$el).val(parameters.password).trigger('keyup');

                        $("[name=loginButton]", loginView.$el).trigger("click");

                        QUnit.ok(conf.loggedUser !== undefined && conf.loggedUser !== null          , "User should be logged in");

                        delete router.configuration.routes.login.forceUpdate;
                        QUnit.start();

                    });


                });

                router.configuration.routes.login.forceUpdate=true;
                eventManager.sendEvent(constants.EVENT_CHANGE_VIEW, {route: router.configuration.routes.login});

            });

            QUnit.asyncTest("Logout", function () {
                var nav = require("org/forgerock/commons/ui/common/components/Navigation"),
                    sessionManager = require("org/forgerock/commons/ui/common/main/SessionManager");

                $("#qunit-fixture").append("<div id='menu'></div>");

                conf.loggedUser = {
                    "userName": "test"
                };

                nav.navigation.render(null, function () {
                    QUnit.equal($("#user_name").text(), conf.loggedUser.userName                , "Login Bar 'user_name' reflects logged user");
                    QUnit.ok($("#logout_link").length                                           , "Log out link available");

                    sinon.stub(sessionManager, "logout", function (callback) {
                        sessionManager.logout.restore();
                        sessionManager.logout(function () {
                            if (callback) {
                                callback();
                            }
                            QUnit.ok(conf.loggedUser === null                                   , "User should be logged out");
                            QUnit.start();
                        });
                    });

                    window.location.hash = $("#logout_link").attr('href');

                });

            });

            QUnit.asyncTest("Remember Login", function () {

                conf.loggedUser = null;
                var loginView = require("LoginView");

                delete loginView.route;

                loginView.render([], function () {
                    // login with loginRemember checked
                    $("#login", loginView.$el).val(parameters.username).trigger('keyup');
                    $("#password", loginView.$el).val(parameters.password).trigger('keyup');
                    $("[name=loginRemember]", loginView.$el).prop("checked", true);
                    $("[name=loginButton]", loginView.$el).trigger("click"); // login occurs

                    _.delay(function () {
                        sinon.stub(loginView, "render", function (args, callback) {

                            loginView.render.restore();
                            loginView.render(args, function () {
                                QUnit.equal(cookieHelper.getCookie('login'), parameters.username, "Remember-login matches provided username");
                                QUnit.equal($("#login", loginView.$el).val(), parameters.username, "Username is remembered after logout.");
                                QUnit.ok($("input[name=loginRemember]", loginView.$el).prop('checked'), "Login Remember is still checked when the login form is re-rendered");

                                cookieHelper.deleteCookie("login");

                                if (callback) {
                                    callback();
                                }

                                QUnit.start();

                            });

                        });

                        eventManager.sendEvent(constants.EVENT_LOGOUT);
                    }, 10);
                });
            });

            QUnit.asyncTest("Add Actions to Dialog", function () {
                var testDialog = new Dialog();

                QUnit.ok(testDialog.actions.length === 1 && testDialog.actions[0].name === "close", "Cancel Button is Available");

                testDialog.addAction("Test", "TestValue");
                testDialog.addAction("Test", "TestValue");

                QUnit.ok(testDialog.actions.length === 2 && testDialog.actions[0].name === "close" && testDialog.actions[1].name === "Test", "Cancel and Test Buttons are Available");

                QUnit.start();
            });

            QUnit.test("UIUtils loadSelect", function () {
                var select = $("<select>");
                UIUtils.loadSelectOptions([
                        { "data": "option 1", "key": 1},
                        { "data": "option 2", "key": 2}
                    ], select);

                QUnit.ok($("option", select).length, 2, "Options are listed within select box, from data");

                UIUtils.loadSelectOptions([
                        { "data": "option 1", "key": 1},
                        { "data": "option 2", "key": 2}
                    ], select, true);

                QUnit.ok($("option", select).length, 3, "Options are listed within select box, from data (including 'Please choose' prompt)");
            });

            QUnit.test("Dialog globalData pollution (CUI-24)", function () {
                var testDialog = new Dialog();
                sinon.stub(UIUtils, "renderTemplate", function (template, el, data, callback) {
                    if (!callback) {
                        QUnit.ok(data["FOO"] === "BAR", "Custom dialog data correctly passed into renderTemplate with no callback");
                    } else {
                        QUnit.ok(data["FOO"] === "BAZ", "Custom dialog data correctly passed into renderTemplate with callback");
                    }
                });
                testDialog.data = {"FOO": "BAR", "theme": {"path": ""}};
                testDialog.loadContent();
                testDialog.data["FOO"] = "BAZ";
                testDialog.loadContent(function () { return });

                QUnit.ok(conf.globalData.FOO === undefined, "No data pollution in global scope from dialog render");
                UIUtils.renderTemplate.restore();
            });

            QUnit.test("Base64.encodeUTF8", function () {
                var input = "パスワードパスワード";

                QUnit.strictEqual(Base64.encodeUTF8(input), "44OR44K544Ov44O844OJ44OR44K544Ov44O844OJ",
                    "Incorrect base-64 encoding");
            });

            QUnit.test("Base64.encodeUTF8 - 2 pad chars", function() {
                var input = "パスワードパスワードx";

                QUnit.strictEqual(Base64.encodeUTF8(input), "44OR44K544Ov44O844OJ44OR44K544Ov44O844OJeA==",
                    "Incorrect base-64 encoding - 2 pad char case");
            });

            QUnit.test("Base64.encodeUTF8 - 1 pad char", function() {
                var input = "パスワードパスワードxx";

                QUnit.strictEqual(Base64.encodeUTF8(input), "44OR44K544Ov44O844OJ44OR44K544Ov44O844OJeHg=",
                    "Incorrect base-64 encoding - 1 pad char case");
            });

            QUnit.test("Base64.decodeUTF8", function() {
                var input = "44OR44K544Ov44O844OJ44OR44K544Ov44O844OJ";

                QUnit.strictEqual(Base64.decodeUTF8(input), "パスワードパスワード",
                    "Incorrect base-64 decoding");
            });
            QUnit.test("Base64.decodeUTF8 - 1 pad char", function() {
                var input = "44OR44K544Ov44O844OJ44OR44K544Ov44O844OJeHg=";
                QUnit.strictEqual(Base64.decodeUTF8(input), "パスワードパスワードxx",
                    "Incorrect base-64 decoding");
            });

            QUnit.test("Base64.decodeUTF8 - 2 pad chars", function() {
                var input = "44OR44K544Ov44O844OJ44OR44K544Ov44O844OJeA==";
                QUnit.strictEqual(Base64.decodeUTF8(input), "パスワードパスワードx",
                    "Incorrect base-64 decoding");
            });

            QUnit.test("Base64.encodeUTF8/decodeUTF8 - various punctuation characters", function() {
                var input = "43uin 98e2 + 343_ {} 43qafdgfREER\'FDj ionk/.,<>`fj iod Hdfjl";

                QUnit.strictEqual(Base64.decodeUTF8(Base64.encodeUTF8(input)), input,
                    "Unable to round-trip Base64 special characters");
            });

            QUnit.test("Mime.encodeHeader", function() {
                var input = "パスワードパスワード";

                QUnit.strictEqual(Mime.encodeHeader(input), "=?UTF-8?B?44OR44K544Ov44O844OJ44OR44K544Ov44O844OJ?=",
                    "Incorrect Mime encoding in header");
            });

            QUnit.asyncTest("Views using plain route url incorrectly rendered multiple times (CUI-50)", function () {
                var testView = require("org/forgerock/commons/ui/common/EnableCookiesView"), // a view with a plain (non-regexp) route url
                    renderPromise = $.Deferred(),
                    stub;

                stub = sinon.stub(testView, "render", function (args, callback) {
                    renderPromise.resolve();
                });

                window.location.hash = "enableCookies/";

                eventManager.sendEvent(constants.EVENT_CHANGE_VIEW, {route: router.configuration.routes.enableCookies});

                renderPromise.then(_.delay(function () {
                    QUnit.equal(stub.callCount, 1, "Render function only called once");
                    testView.render.restore();
                    QUnit.start();
                }, 10));

            });

            QUnit.asyncTest("Routes with special characters in parameter values (CUI-51)", function () {
                var manySpecialCharacters = "/!@?#$%",
                    additionalSpecialCharacters = "a%?b&c",
                    loginView = require("org/forgerock/commons/ui/common/LoginView"),
                    stub = sinon.stub(loginView, "render", function (args, callback) {
                        var currentParams = router.convertCurrentUrlToJSON().params;
                        QUnit.equal(args[0], manySpecialCharacters, "Characters properly passed to first arg of render function");
                        QUnit.equal(args[1], '&name1=' + additionalSpecialCharacters, "Characters properly passed to last arg of render function");
                        QUnit.equal(currentParams.name1, additionalSpecialCharacters, "Characters accurately parsed out of url params");
                        stub.restore();
                        QUnit.start();
                    });

                window.location.hash = "login" + encodeURIComponent(manySpecialCharacters) + '&name1=' + encodeURIComponent(additionalSpecialCharacters);
            });

            QUnit.asyncTest("Parameters passed to logout event", function () {

                eventManager.registerListener(constants.EVENT_LOGOUT, function (event) {
                    QUnit.equal(event.args[0], "foo/bar", "Logout event called with expected arguments");
                    QUnit.start();
                });

                window.location.hash = "logout/foo/bar";

            });


            QUnit.test("AbstractDelegate - getDifferences (CUI-53)", function () {
                var AbstractDelegate = require("org/forgerock/commons/ui/common/main/AbstractDelegate"),
                    abTest = new AbstractDelegate(""),
                    differences;

                differences = abTest.getDifferences({"a": "b"}, {"a": "b"});
                QUnit.equal(differences.length, 0, "No differences for equal, simple objects");

                differences = abTest.getDifferences({"a": "b"}, {"a": "c"});
                QUnit.ok(differences.length === 1 && differences[0].value === 'c', "One difference found for trivial change");

                differences = abTest.getDifferences({"a": ["b"]}, {"a": ["b"]});
                QUnit.equal(differences.length, 0, "No differences for equal complex objects");
            });

            QUnit.test("AbstractDelegate - patchEntity (CUI-54)", function () {
                var AbstractDelegate = require("org/forgerock/commons/ui/common/main/AbstractDelegate"),
                    abTest = new AbstractDelegate(""),
                    stub = sinon.stub(abTest, "serviceCall", function (params) {
                        return JSON.parse(params.data);
                    }),
                    patchDef = [{"field": "/abc", "operation": "replace", "value": "test"}],
                    response = abTest.patchEntity({"id": 1, "rev": 1}, patchDef);

                QUnit.equal(response[0].field, "/abc", "Field not changed when using proper JSON Pointer");

                patchDef = [{"field": "abc", "operation": "replace", "value": "test"}];
                response = abTest.patchEntity({"id": 1, "rev": 1}, patchDef);
                QUnit.equal(response[0].field, "/abc", "Field changed to proper JSON Pointer");

                patchDef = [{"field": "ab/c", "operation": "replace", "value": "test"}];
                response = abTest.patchEntity({"id": 1, "rev": 1}, patchDef);
                QUnit.equal(response[0].field, "/ab/c", "Field changed to proper JSON Pointer");

                stub.restore();
            });

            QUnit.test("Data scope not shared between views (CUI-57)", function () {
                var notFound = require("org/forgerock/commons/ui/common/NotFoundView"),
                    enableCookies = require("org/forgerock/commons/ui/common/EnableCookiesView");

                notFound.data.testVar = "foo";
                enableCookies.data.testVar = "bar";

                QUnit.ok(notFound.data.testVar === "foo", "View still has original value in data scope after other view sets same name.");

            });

            QUnit.test("url helper for handlebars", function () {
                var template = Handlebars.compile("{{url 'selfRegistration' input}}");

                QUnit.ok(template({input: ["/foo", ""]}) === "#register/foo", "url helper properly handles array value as second argument.");

                template = Handlebars.compile("{{url 'selfRegistration' '/bar'}}");

                QUnit.ok(template() === "#register/bar", "url helper properly handles simple string value as second argument.");
            });
            
            QUnit.asyncTest("Close Dialogs on View Change (OPENIDM-3358)", function () {
                var closeAllSpy = sinon.stub(BootstrapDialog,"closeAll");
                
                conf.loggedUser = getLoggedUser();

                QUnit.ok(!closeAllSpy.called, "BootstrapDialog.closeAll() not yet called before EVENT_CHANGE_VIEW fired");
                
                eventManager.sendEvent(constants.EVENT_CHANGE_VIEW, {
                    route: router.configuration.routes.profile,
                    callback: function () {
                        QUnit.ok(closeAllSpy.called, "BootstrapDialog.closeAll() successfully called after EVENT_CHANGE_VIEW fired");
                        BootstrapDialog.closeAll.restore();
                        BootstrapDialog.closeAll();
                        QUnit.start();
                    }
                });
                
                window.location.hash = "profile/change_security_data/";
            });


        }
    };
});
