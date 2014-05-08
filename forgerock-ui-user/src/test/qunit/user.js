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
        executeAll: function (server) {

            var testPromise = $.Deferred();

            
            module('User Tests');

            QUnit.test("Logged in user", function () {

                QUnit.ok(conf.loggedUser !== undefined, "There must be a user logged in before running this test suite");

            });

            //Test 1: Update User Info
 
            QUnit.asyncTest("Update User Info", function () {

                var userProfileView = require("UserProfileView");
                userProfileView.element = $("<div>")[0];

                userProfileView.render(null,function() {

                    QUnit.start();

                    // Testing inputs
                    QUnit.ok($('input[name="saveButton"]', userProfileView.$el).length          , "Update button appears to be defined");
                    QUnit.ok($('input[name="resetButton"]', userProfileView.$el).length         , "Reset button appears to be defined");
                    QUnit.ok($('input[name="uid"]', userProfileView.$el).length                 , "Username input appears to be defined");
                    QUnit.ok($('input[name="givenName"]', userProfileView.$el).length           , "First name input appears to be defined");
                    QUnit.ok($('input[name="mail"]', userProfileView.$el).length                , "Email address input appears to be defined");
                    QUnit.ok($('input[name="sn"]', userProfileView.$el).length                  , "Last Name input appears to be defined");
                    QUnit.ok($('input[name="telephoneNumber"]', userProfileView.$el).length     , "Mobile Phone input appears to be defined");


                   // Testing user data 
                    QUnit.equal($('input[name="uid"]', userProfileView.$el).val(), conf.loggedUser.uid                          , "Username populated");
                    QUnit.equal($('input[name="givenName"]', userProfileView.$el).val(), conf.loggedUser.givenName              , "First name populated");
                    QUnit.equal($('input[name="mail"]', userProfileView.$el).val(), conf.loggedUser.mail                        , "Email address populated");
                    QUnit.equal($('input[name="sn"]', userProfileView.$el).val(), conf.loggedUser.sn                            , "Last Name populated");
                    QUnit.equal($('input[name="telephoneNumber"]', userProfileView.$el).val(), conf.loggedUser.telephoneNumber  , "Mobile Phone populated");

                  
                    // Testing validation
                    
                    QUnit.equal($('input[name="givenName"]', userProfileView.$el).attr('data-validation-status'),          'ok', 'First name input passes validation');
                    QUnit.equal($('input[name="mail"]', userProfileView.$el).attr('data-validation-status'),               'ok', 'Email address input passes validation');
                    QUnit.equal($('input[name="sn"]', userProfileView.$el).attr('data-validation-status'),                 'ok', 'Last Name input passes validation');
                    QUnit.equal($('input[name="telephoneNumber"]', userProfileView.$el).attr('data-validation-status'),    'ok', 'Mobile Phone input passes validation');
                   
                    testPromise.resolve(); // make sure this is only called after the last async test is finished
                });
            });

            return testPromise;
        }
    };
});