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

/*global $, define, _ */

/**
 * @author yaromin
 * @author Eugenia Sergueeva
 */

define("UserDelegate", [
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/AbstractDelegate",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/mock/ui/common/main/LocalStorage"
], function (constants, AbstractDelegate, configuration, eventManager, localStorage) {

    var obj = new AbstractDelegate("mock/repo/internal/user");

    obj.usersCallback = null;
    obj.users = null;
    obj.numberOfUsers = 0;

    /**
     * Starting session. Sending username and password to authenticate and returns user's id.
     */
    obj.login = function (uid, password, successCallback, errorCallback, errorsHandlers) {
        var headers = {};
        headers[constants.HEADER_PARAM_USERNAME] = uid;
        headers[constants.HEADER_PARAM_PASSWORD] = password;
        headers[constants.HEADER_PARAM_NO_SESSION] = false;

        obj.getProfile(successCallback, errorCallback, errorsHandlers, headers);

        delete headers[constants.HEADER_PARAM_PASSWORD];
    };

    obj.getUserById = function (id, component, successCallback, errorCallback, errorsHandlers) {
        this.serviceCall({
            url: "/" + id,
            type: "GET",
            success: successCallback,
            error: errorCallback,
            errorsHandlers: errorsHandlers});
    };

    obj.checkCredentials = function (password, successCallback, errorCallback) {
        // not used
    };

    /**
     * Checks if logged in and returns users id
     */
    obj.getProfile = function (successCallback, errorCallback, errorsHandlers, headers) {
        if (headers) {
            var storedUser = localStorage.get(headers[constants.HEADER_PARAM_USERNAME]);
            if (storedUser && storedUser.password === headers[constants.HEADER_PARAM_PASSWORD]) {
                successCallback(storedUser);
            } else {
                if (errorCallback) {
                    errorCallback();
                }
            }
        } else {
            if (errorCallback) {
                errorCallback();
            }
        }
    };

    /**
     * Creates new user.
     */
    obj.create = function (id, data, successCallback, errorCallback) {
        if (localStorage.add(data.userName, data)) {
            successCallback();
        } else {
            errorCallback();
        }
        return localStorage.add(data.userName, data);
    };

    obj.getForUserName = function (uid, successCallback, errorCallback) {
        // not used
    };

    obj.getForUserID = function (uid, successCallback, errorCallback) {
        // not used
    };

    obj.patchUserDifferences = function (oldUserData, newUserData, successCallback, errorCallback, noChangesCallback, errorsHandlers) {
        console.info("updating user");

        var updatedData = obj.getDifferences(oldUserData, newUserData);
        if (localStorage.patch(configuration.loggedUser._id, updatedData)) {
            successCallback(updatedData);
        }
    };

    obj.updateUser = function (oldUserData, stub, newUserData, successCallback, errorCallback, noChangesCallback) {
        obj.patchUserDifferences(oldUserData, newUserData, successCallback, errorCallback, noChangesCallback, {
            "forbidden": {
                status: "403",
                event: constants.EVENT_USER_UPDATE_POLICY_FAILURE
            }
        });
    };

    obj.patchSelectedUserAttributes = function (id, rev, patchDefinitionObject, successCallback, errorCallback, noChangesCallback) {
        console.log('changing password');
        if (localStorage.patch(id, patchDefinitionObject)) {
            successCallback();
        }
    };

    return obj;
});