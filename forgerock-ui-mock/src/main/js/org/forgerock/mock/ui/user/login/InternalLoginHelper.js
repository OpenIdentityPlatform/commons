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

/*global define, window */

define("org/forgerock/mock/ui/user/login/InternalLoginHelper", [
    "UserDelegate",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/AbstractConfigurationAware",
    "org/forgerock/commons/ui/common/main/ServiceInvoker",
    "org/forgerock/commons/ui/common/main/Configuration"
], function (userDelegate, eventManager, constants, AbstractConfigurationAware, serviceInvoker, conf) {
    var obj = new AbstractConfigurationAware();

    obj.login = function (params, successCallback, errorCallback) {

        userDelegate.login(params.userName, params.password,
            function (user) {
                conf.globalData.userComponent = user.component;
                successCallback(user);
            },
            function () {
                if (errorCallback) {
                    errorCallback();
                }
            },
            {
                "forbidden": {
                    status: "403"
                },
                "unauthorized": {
                    status: "401",
                    message: "authenticationFailed"
                }
            }
        );
    };

    obj.logout = function (successCallback, errorCallback) {
        delete conf.loggedUser;
        successCallback();
    };

    obj.getLoggedUser = function (successCallback, errorCallback) {
        userDelegate.getProfile(
            function (user) {
                conf.globalData.userComponent = user.component;
                successCallback(user);
            },
            function () {
                if (errorCallback) {
                    errorCallback();
                }
            },
            {
                "forbidden": {
                    status: "403"
                },
                "unauthorized": {
                    status: "401"
                }
            }
        );
    };
    return obj;
});