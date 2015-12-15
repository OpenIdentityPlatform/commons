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
 * Copyright 2014-2015 ForgeRock AS.
 */

/*global require, QUnit, localStorage */

define([
    "jquery",
    "doTimeout",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/EventManager",
    "../test/tests/commons",
    "../test/tests/user",
    "../test/tests/mock",
    "../test/tests/getLoggedUser",
    "../test/tests/uriutils"
], function ($, doTimeout, constants, eventManager, commonsTests, userTests, mockTests, getLoggedUser, uriTests) {

    $.doTimeout = function (name, time, func) {
        func(); // run the function immediately rather than delayed.
    };

    return function (server) {
        eventManager.registerListener(constants.EVENT_APP_INITIALIZED, function () {
            require("ThemeManager").getTheme().then(function () {
                var userParams = {
                        "username": "test",
                        "password": "test"
                    };

                QUnit.testStart(function (testDetails) {
                    console.log("Starting " + testDetails.module + ":" + testDetails.name + "("+ testDetails.testNumber +")");

                    var vm = require("org/forgerock/commons/ui/common/main/ViewManager");

                    vm.currentView = null;
                    vm.currentDialog = null;
                    vm.currentViewArgs = null;
                    vm.currentDialogArgs = null;

                    require("org/forgerock/commons/ui/common/main/Configuration").baseTemplate = null;
                });


                _.delay(function () {
                    QUnit.start();

                    commonsTests.executeAll(server, userParams);
                    userTests.executeAll(server, getLoggedUser());

                    // mockTests disabled pending some major changes expected to be coming soon in this area
                    // not worth the time investment in fixing them, owing to those changes.
                    //mockTests.executeAll(server, userParams);
                    uriTests.executeAll();
                }, 500);

                QUnit.done(function () {
                    localStorage.clear();
                    Backbone.history.stop();
                    window.location.hash = "";
                });
            });
        });
    };

});
