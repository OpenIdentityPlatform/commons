/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2014 ForgeRock AS. All Rights Reserved
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

/*global require, _ */

/**
 * @author yaromin
 * @author Eugenia Sergueeva
 */

require.config({
    paths: {
        // sinon only needed (or available) for Mock project
        sinon: "libs/sinon-1.10.3",

        i18next: "libs/i18next-1.7.3-min",
        i18nGrid: "libs/i18n/grid.locale-en",
        backbone: "libs/backbone-1.1.0-min",
        underscore: "libs/lodash-2.4.1-min",
        js2form: "libs/js2form-2.0",
        form2js: "libs/form2js-2.0",
        spin: "libs/spin-2.0.1-min",
        jquery: "libs/jquery-1.11.1-min",
        jqueryui: "libs/jquery-ui-1.11.1-min",
        jqgrid: "libs/jquery.jqGrid-4.5.4-min",
        xdate: "libs/xdate-0.8-min",
        doTimeout: "libs/jquery.ba-dotimeout-1.0-min",
        handlebars: "libs/handlebars-1.3.0-min",
        moment: "libs/moment-2.8.1-min",
        UserDelegate: "org/forgerock/mock/ui/user/delegates/UserDelegate",
        ThemeManager: "org/forgerock/mock/ui/common/util/ThemeManager"
    },

    shim: {
        sinon: {
            exports: "sinon"
        },
        underscore: {
            exports: "_"
        },
        backbone: {
            deps: ["underscore"],
            exports: "Backbone"
        },
        js2form: {
            exports: "js2form"
        },
        form2js: {
            exports: "form2js"
        },
        spin: {
            exports: "spin"
        },
        jqueryui: {
            exports: "jqueryui"
        },
        jqgrid: {
            deps: ["jqueryui", "i18nGrid"]
        },
        xdate: {
            exports: "xdate"
        },
        doTimeout: {
            exports: "doTimeout"
        },
        handlebars: {
            exports: "handlebars"
        },
        i18next: {
            deps: ["handlebars"],
            exports: "i18next"
        },
        moment: {
            exports: "moment"
        }
    }
});

/**
 * Loads all application on start, so each module will be available to
 * required synchronously
 */
require([
    // sinon only needed (or available) for Mock project
    "sinon",
    "underscore",
    "backbone",
    "form2js",
    "js2form",
    "spin",
    "jqueryui",
    "xdate",
    "moment",
    "doTimeout",
    "handlebars",
    "i18next",
    "org/forgerock/mock/ui/common/main/MockServer",
    "org/forgerock/commons/ui/common/main/i18nManager",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/mock/ui/common/main/LocalStorage",
    "org/forgerock/mock/ui/common/main",
    "org/forgerock/mock/ui/user/main",
    "org/forgerock/commons/ui/user/main",
    "org/forgerock/commons/ui/common/main",
    "UserDelegate",
    "ThemeManager",
    "config/main"
], function ( sinon, _, Backbone, form2js, js2form, spin, $ui, xdate, moment, doTimeout, Handlebars, i18n,
             mockServer, i18nManager, constants, eventManager, localStorage) {

    // Mock project is run without server. Framework requires cookies to be enabled in order to be able to login.
    // Default CookieHelper.cookiesEnabled() implementation will always return false as cookies cannot be set from local
    // file. Hence redefining function to return true
    require('org/forgerock/commons/ui/common/util/CookieHelper').cookiesEnabled = function () {
        return true;
    };

    // Adding stub user
    localStorage.add('mock/repo/internal/user/test', {
        _id: 'test',
        _rev: '1',
        component: 'mock/repo/internal/user',
        roles: ['ui-user'],
        uid: 'test',
        userName: 'test',
        password: 'test',
        telephoneNumber: '12345',
        givenName: 'Jack',
        sn: 'White',
        mail: 'white@test.com'
    });

    eventManager.sendEvent(constants.EVENT_DEPENDECIES_LOADED);
});