/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2012 ForgeRock AS. All rights reserved.
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

/**
 * Interface
 * @author mbilski
 */
define("org/forgerock/commons/ui/common/main/SessionManager", [
    "jquery",
    "underscore",
    "org/forgerock/commons/ui/common/util/CookieHelper",
    "org/forgerock/commons/ui/common/main/AbstractConfigurationAware",
    "org/forgerock/commons/ui/common/util/ModuleLoader"
], function($, _, cookieHelper, AbstractConfigurationAware, ModuleLoader) {
    var obj = new AbstractConfigurationAware();

    obj.login = function(params, successCallback, errorCallback) {
        cookieHelper.deleteCookie("session-jwt", "/", ""); // resets the session cookie to discard old session that may still exist
        return ModuleLoader.load(obj.configuration.loginHelperClass).then(function (helper) {
            return ModuleLoader.promiseWrapper(_.curry(helper.login)(params), {
                success: successCallback,
                error: errorCallback
            });
        });
    };

    obj.logout = function(successCallback, errorCallback) {
        return ModuleLoader.load(obj.configuration.loginHelperClass).then(function (helper) {
            return ModuleLoader.promiseWrapper(helper.logout, {
                success: successCallback,
                error: errorCallback
            });
        });
    };

    obj.getLoggedUser = function(successCallback, errorCallback) {
        return ModuleLoader.load(obj.configuration.loginHelperClass).then(function (helper) {
            return ModuleLoader.promiseWrapper(helper.getLoggedUser, {
                success: successCallback,
                error: errorCallback
            });
        });
    };

    return obj;
});
