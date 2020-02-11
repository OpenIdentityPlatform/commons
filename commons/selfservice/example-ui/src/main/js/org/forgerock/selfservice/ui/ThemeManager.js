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

/*global define */

define("org/forgerock/selfservice/ui/ThemeManager", [
    "jquery",
    "underscore",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/Configuration"
], function ($, _, constants, conf) {
    var obj = {},
        themePromise;

    obj.loadThemeCSS = function (theme) {
        $('head').find('link[href*=favicon]').remove();

        $("<link/>", {
            rel: "icon",
            type: "image/x-icon",
            href: theme.path + theme.icon
        }).appendTo("head");

        $("<link/>", {
            rel: "shortcut icon",
            type: "image/x-icon",
            href: theme.path + theme.icon
        }).appendTo("head");

        _.forEach(theme.stylesheets, function(stylesheet) {
            $("<link/>", {
                rel: "stylesheet",
                type: "text/css",
                href: stylesheet
            }).appendTo("head");
        });
    };


    obj.loadThemeConfig = function () {
        var prom = $.Deferred();
        //check to see if the config file has been loaded already
        //if so use what is already there if not load it
        if (conf.globalData.themeConfig) {
            prom.resolve(conf.globalData.themeConfig);
            return prom;
        } else {
            return $.Deferred().resolve({
                "path": "",
                "icon": "favicon.ico",
                "stylesheets": ["css/theme.css", "css/structure.css"],
                "settings": {
                    "logo": {
                        "src": "images/logo-horizontal.png",
                        "title": "ForgeRock",
                        "alt": "ForgeRock"
                    },
                    "loginLogo": {
                        "src": "images/login-logo.png",
                        "title": "ForgeRock",
                        "alt": "ForgeRock",
                        "height": "104px",
                        "width": "210px"
                    },
                    "footer": {
                        "mailto": "info@forgerock.com"
                    }
                }
            });
        }
    };

    obj.getTheme = function () {
        if (themePromise === undefined) {
            themePromise = obj.loadThemeConfig().then(function (themeConfig) {
                conf.globalData.theme = themeConfig;
                obj.loadThemeCSS(themeConfig);
                return themeConfig;
            });
        }
        return themePromise;
    };

    return obj;
});
