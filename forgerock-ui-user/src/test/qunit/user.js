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
        "sinon",
        "underscore",
        "org/forgerock/commons/ui/common/main/Configuration",
        "org/forgerock/commons/ui/common/util/Constants",
        "org/forgerock/commons/ui/common/main/EventManager",
        "org/forgerock/commons/ui/common/util/ModuleLoader",
        "org/forgerock/commons/ui/common/main/Router"
    ], function (sinon, _, conf, Constants, EventManager, ModuleLoader, Router) {
    return {
        executeAll: function (server, loggedUser) {

            module('User Tests');

            //Test 1: Update User Info

            QUnit.asyncTest("Update User Info", function () {

                conf.loggedUser = loggedUser;
                ModuleLoader.load("UserProfileView").then(function (userProfileView) {

                    delete userProfileView.route; // necessary to prevent some error-checking code from causing problems in this context

                    userProfileView.render([],function() {

                        var testVals = {
                                givenName           : 'John',
                                mail                : 'test@test.com',
                                sn                  : 'Doe',
                                telephoneNumber     : '123456789'
                            },
                            modifiedUser = _.extend(conf.loggedUser.toJSON(), testVals);

                        // Testing inputs

                        // docment.activeElement - a hack around testing for .is(":focus") bug in phantomjs: https://github.com/guard/guard-jasmine/issues/48
                        QUnit.equal($("input[name=givenName]", userProfileView.$el)[0], document.activeElement, "First name field has focus on page load");
                        QUnit.equal($('input[type="submit"]', userProfileView.$el).length, 3, "Update button appears to be defined for each tab");
                        QUnit.equal($('input[type="reset"]', userProfileView.$el).length, 3, "Reset button appears to be defined");

                        _.each(_.keys(testVals), function (prop) {
                            QUnit.ok($('input[name="'+prop+'"]', userProfileView.$el).length, prop + " input appears to be defined");
                            QUnit.equal($('input[name="'+prop+'"]', userProfileView.$el).val(), conf.loggedUser.get(prop), prop + " populated");
                            $('input[name="'+prop+'"]', userProfileView.$el).val(modifiedUser[prop]).trigger('change');
                        });

                        QUnit.ok($('input[name="password"]', userProfileView.$el).length, "Password input appears to be defined");

                        $('#userDetailsTab input[type=submit]', userProfileView.$el).trigger('click');

                        _.each(_.keys(testVals), function (prop) {
                            QUnit.equal(conf.loggedUser.get(prop), testVals[prop], prop + " changed");
                        });

                        QUnit.ok(_.isEqual(conf.loggedUser.toJSON(), modifiedUser), "User object doesn't have any unexpected changes");

                        //reset button

                        $('input[name="uid"]', userProfileView.$el).val('AnotherUsername').trigger('change');
                        $('input[name="givenName"]', userProfileView.$el).val('Jane').trigger('change');
                        $('input[name="mail"]', userProfileView.$el).val('test2@test.com').trigger('change');
                        $('input[name="sn"]', userProfileView.$el).val('Doe').trigger('change');
                        $('input[name="telephoneNumber"]', userProfileView.$el).val('987654321').trigger('change');

                        $('#userDetailsTab input[type=reset]', userProfileView.$el).trigger('click');

                        _.each(_.keys(testVals), function (prop) {
                            QUnit.equal($('input[name="'+prop+'"]', userProfileView.$el).val(), testVals[prop], prop + "  was reset");
                        });

                        var routePromise = EventManager.whenComplete(Constants.ROUTE_REQUEST);
                        // change password
                        $(".nav-tabs li a[href='#userPasswordTab']", userProfileView.$el).click();

                        routePromise.then(function () {
                            QUnit.equal(window.location.hash, "#profile/password", "Route updates on tab change");

                            QUnit.equal($("input[name=password]", userProfileView.$el)[0], document.activeElement, "Password field has focus on page load");

                            $("#userPasswordTab #input-password", userProfileView.$el).val("newPassw0rd").trigger('change');
                            $("#userPasswordTab #input-confirmPassword", userProfileView.$el).val("newPassw0rd").trigger('change');

                            $("body").one("shown.bs.modal", function () {
                                QUnit.ok(/Password/.test($("#confirmPasswordFormExplanation code").text()), "Password change triggers confirm password dialog");

                                $("#confirmPasswordForm #currentPassword").val("bad").trigger("change");
                                $(".modal.in #btnUpdate").trigger("click");

                                QUnit.ok($("#userPasswordTab .changes-pending").is(":visible"), "Changes still pending after bad password provided");

                                $("body").one("shown.bs.modal", function () {

                                    $("#confirmPasswordForm #currentPassword").val("test").trigger("change");

                                    $("body").one("hidden.bs.modal", function () {
                                        QUnit.ok($("#userPasswordTab .changes-pending").is(":visible") === false, "Changes pending not visible after good password provided");
                                        QUnit.start();
                                    });

                                    $(".modal.in #btnUpdate").trigger("click");
                                });
                                $('#userPasswordTab input[type=submit]', userProfileView.$el).trigger('click');
                            });
                            _.delay(function () { // push this to the bottom of the event loop, so the validation logic can apply
                                $('#userPasswordTab input[type=submit]', userProfileView.$el).trigger('click');
                            }, 200);
                        });

                    });

                });

            });

/*
            QUnit.asyncTest("Unauthorized Request Behavior", function () {
                conf.loggedUser = loggedUser;
                delete conf.globalData.authorizationFailurePending;
                delete conf.gotoURL;

                ModuleLoader.load("LoginDialog").then(function (loginDialog) {

                    // stub the loginDialog because we don't actually care about its contents, just that it gets called
                    sinon.stub(loginDialog, 'render', function (args, callback) {
                        if (callback) {
                            callback();
                        }
                    });

                    EventManager.sendEvent(Constants.EVENT_CHANGE_VIEW, {
                        route: Router.configuration.routes.profile
                    }).then(function () {

                        EventManager.sendEvent(Constants.EVENT_UNAUTHORIZED, {error: {type:"POST"} }).then(function () {
                            QUnit.ok(conf.loggedUser !== null, "User info should be retained after UNAUTHORIZED POST error");
                            QUnit.ok(loginDialog.render.calledOnce, "LoginDialog render function was called once");
                            loginDialog.render.restore();

                            delete conf.globalData.authorizationFailurePending;

                            EventManager.sendEvent(Constants.EVENT_UNAUTHORIZED, {error: {type:"GET", status: 403} }).then(function () {
                                QUnit.ok(!conf.loggedUser, "User info should be discarded after UNAUTHORIZED GET error");
                                QUnit.equal(conf.gotoURL, "#profile/details", "gotoURL should be preserved after UNAUTHORIZED GET error");
                                QUnit.equal(window.location.hash, "#login/", "Redirected to main login page")
                                delete conf.gotoURL;
                                QUnit.start();
                            });

                        });

                    });

                })
            });
*/
        }
    };
});
