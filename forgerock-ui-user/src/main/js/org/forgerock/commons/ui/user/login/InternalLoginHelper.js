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

/*global define*/

define("org/forgerock/commons/ui/user/login/InternalLoginHelper", [
	"org/forgerock/commons/ui/user/delegates/UserDelegate",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/AbstractConfigurationAware",
    "org/forgerock/commons/ui/common/main/ServiceInvoker"
], function (userDelegate, eventManager, constants, AbstractConfigurationAware, serviceInvoker) {
    var obj = new AbstractConfigurationAware();

    obj.login = function(userName, password, successCallback, errorCallback) {
        userDelegate.logAndGetUserDataByCredentials(userName, password, function(user) {
            successCallback(user);
        }, function() {
            userDelegate.internalLogIn(userName, password, function(user) {
                if(!user.userName && user._id) {
                    user.userName = user._id;
                }
                
                successCallback(user);
            }, function() {
                errorCallback();
            }, {"unauthorized": { status: "401"}});
        }, {"unauthorized": { status: "401"}});
    };

    obj.logout = function() {
        userDelegate.logout();
    };
    
    obj.getLoggedUser = function(successCallback, errorCallback) {
        userDelegate.getProfile(function(user) {
            successCallback(user);
        }, function() {
            userDelegate.forInternalCredentials(function(user) {
                if(!user.userName && user._id) {
                    user.userName = user._id;
                }
                
                successCallback(user);
            }, function() {
                errorCallback();
            }, {"serverError": {status: "503"}, "unauthorized": {status: "401"}});
        }, {"serverError": {status: "503"}, "unauthorized": {status: "401"}});
    };

    return obj;
});
