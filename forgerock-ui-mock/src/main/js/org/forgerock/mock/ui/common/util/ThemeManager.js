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

/*global define, less */

/**
 * @author Eugenia Sergueeva
 */

define("ThemeManager", [
    "jquery",
    "underscore",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/Configuration"
], function ($, _, constants, conf) {
    var obj = {},
        themePromise;

    obj.loadThemeCSS = function(theme){
        $('head').find('link[href*=less]').remove();
        $('head').find('link[href*=favicon]').remove();

        $("<link/>", {
            rel: "stylesheet/less",
            type: "text/css",
            href: theme.path + "css/styles.less"
        }).appendTo("head");

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

        return $.ajax({
            url: constants.LESS_VERSION,
            dataType: "script",
            cache: true
        });
    };


    obj.loadThemeConfig = function(){
        var prom = $.Deferred();
        //check to see if the config file has been loaded already
        //if so use what is already there if not load it
        if(conf.globalData.themeConfig){
            prom.resolve(conf.globalData.themeConfig);
            return prom;
        }
        else{
            return $.Deferred().resolve({
                "path": "",
                "icon": "favicon.ico",
                "settings": {
                    "logo": {
                        "src": "images/logo.png",
                        "title": "ForgeRock",
                        "alt": "ForgeRock"
                    },
                    "loginLogo": {
                        "src": "images/login-logo.png",
                        "title": "ForgeRock",
                        "alt": "ForgeRock",
                        "height": "104px",
                        "width": "156px"
                    },
                    "lessVars": {
                        "background-image": "url('../images/box-bg.png')",
                        "background-position": "950px -100px",
                        "column-padding": "0px",
                        "login-container-label-align": "left",
                        "highlight-color": "#eeea07",
                        "content-background": "#f9f9f9",
                        "href-color-hover": "#5e887f",
                        "color-error": "#d97986",
                        "color-warning": "yellow",
                        "color-success": "#71bd71",
                        "color-info": "blue",
                        "color-inactive": "gray"
                    },
                    "footer": {
                        "mailto": "info@forgerock.com",
                        "phone": "+47-2108-1746"
                    }
                }
            });
        }
    };

    obj.getTheme = function(){
        if (themePromise === undefined) {
            themePromise = obj.loadThemeConfig().then(function(themeConfig){
                var newLessVars = {};

                conf.globalData.theme = themeConfig;
                return obj.loadThemeCSS(themeConfig).then(function(){
                    _.each(themeConfig.settings.lessVars, function (value, key) {
                        newLessVars['@' + key] = value;
                    });
                    less.modifyVars(newLessVars);

                    return themeConfig;
                });
            });
        }
        return themePromise;
    };

    return obj;
});
