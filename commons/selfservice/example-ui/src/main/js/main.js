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

/*global require, window */

require.config({
    map: {
        "*" : {
            "Footer": "org/forgerock/selfservice/ui/Footer",
            "ThemeManager": "org/forgerock/selfservice/ui/ThemeManager",
            "LoginView": "org/forgerock/commons/ui/common/LoginView",
            "PasswordResetView": "org/forgerock/commons/ui/user/anonymousProcess/PasswordResetView",
            "UserProfileView": "org/forgerock/commons/ui/user/profile/UserProfileView",
            "LoginDialog": "org/forgerock/commons/ui/common/LoginDialog",
            "RegisterView": "org/forgerock/commons/ui/user/anonymousProcess/SelfRegistrationView",
            // TODO: Remove this when there are no longer any references to the "underscore" dependency
            "underscore": "lodash"
        }
    },
    paths: {
        i18next: "libs/i18next-1.7.3-min",
        backbone: "libs/backbone-1.1.2-min",
        "backbone.paginator": "libs/backbone.paginator.min-2.0.2-min",
        "backbone-relational": "libs/backbone-relational-0.9.0-min",
        "backgrid": "libs/backgrid.min-0.3.5-min",
        "backgrid-filter": "libs/backgrid-filter.min-0.3.7-min",
        "backgrid-paginator": "libs/backgrid-paginator.min-0.3.5-min",
        lodash: "libs/lodash-3.10.1-min",
        js2form: "libs/js2form-2.0",
        form2js: "libs/form2js-2.0",
        spin: "libs/spin-2.0.1-min",
        jquery: "libs/jquery-3.5.1-min",
        xdate: "libs/xdate-0.8-min",
        doTimeout: "libs/jquery.ba-dotimeout-1.0-min",
        handlebars: "libs/handlebars-4.7.6-min",
        moment: "libs/moment-2.28.0-min",
        bootstrap: "libs/bootstrap-3.3.5-custom",
        "bootstrap-dialog": "libs/bootstrap-dialog-1.34.4-min",
        placeholder: "libs/jquery.placeholder-2.0.8"
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
        "backbone.paginator": {
            deps: ["backbone"]
        },
        "backgrid": {
            deps: ["jquery", "underscore", "backbone"],
            exports: "Backgrid"
        },
        "backgrid-filter": {
            deps: ["backgrid"]
        },
        "backgrid-paginator": {
            deps: ["backgrid", "backbone.paginator"]
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
        bootstrap: {
            deps: ["jquery"]
        },
        'bootstrap-dialog': {
            deps: ["jquery", "underscore","backbone", "bootstrap"]
        },
        placeholder: {
            deps: ["jquery"]
        },
        xdate: {
            exports: "xdate"
        },
        doTimeout: {
            deps: ["jquery"],
            exports: "doTimeout"
        },
        i18next: {
            deps: ["jquery", "handlebars"],
            exports: "i18next"
        },
        moment: {
            exports: "moment"
        }
    }
});

require([
    // This list should be all of the things that you either need to use to initialize
    // prior to starting, or should be the modules that you want included in the minified
    // startup bundle. Be sure to only put things in this list that you really need to have
    // loaded on startup (so that you get the benefit of minification without adding more
    // than you really need for the first load)

    // These are used prior to initialization. Note that the callback function names
    // these as arguments, but ignores the others.
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/Constants",

    // core forgerock-ui files
    "org/forgerock/commons/ui/common/main",
    "config/AppConfiguration",
    "org/forgerock/selfservice/ui/Constants",

    // libraries necessary for forgerock-ui (and thus worth bundling)
    "jquery",
    "underscore",
    "backbone",
    "handlebars",
    "i18next",
    "spin"
], function (EventManager, Constants) {
    EventManager.sendEvent(Constants.EVENT_DEPENDENCIES_LOADED);
});
