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

/*global define, $, _, less */

/**
 * @author Eugenia Sergueeva
 */

define("ThemeManager", [ ], function () {
    var obj = {},
        themePromise = $.Deferred().resolve({
            "path": "",
            "icon": "favicon.ico",
            "settings": {
                "logo": {
                    "src": "images/logo.png",
                    "title": "ForgeRock",
                    "alt": "ForgeRock",
                    "height": "80",
                    "width": "120"
                },
                "lessVars": {
                    "background-image": "url('../images/box-bg.png')",
                    "background-position": "950px -100px",
                    "column-padding": "0px",
                    "login-container-label-align": "left",
                    "highlight-color": "#eeea07",
                    "login-container-width": "430px",
                    "medium-container-width": "850px",
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
        }).done(function (theme) {
            var newLessVars = {};
            _.each(theme.settings.lessVars, function (value, key) {
                newLessVars['@' + key] = value;
            });
            less.modifyVars(newLessVars);
        });

    obj.getTheme = function () {
        return themePromise;
    };

    return obj;
});