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

/*global $, define, _ */

/**
 * This file is a sample, to be copied into a specific application and modified to use the
 * endpoints available in that system.  References to the copy of this module will still 
 * be made with the name "UserDelegate", so keep that the same as it is here. It is up to the 
 * root main.js file to load the proper copy.
 */
define("UserDelegate", [
	"org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/AbstractDelegate",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/main/EventManager"
], function(constants, AbstractDelegate, configuration, eventManager) {

    var obj = new AbstractDelegate(constants.host + "USER_ENDPOINT");

    obj.usersCallback = null;
    obj.users = null;
    obj.numberOfUsers = 0;

    obj.getAllUsers = function(successCallback, errorCallback) {
        console.info("getting all users");

        obj.usersCallback = successCallback;
        obj.numberOfUsers = 0;
        /*
        obj.serviceCall({url: "ALL_USERS_ENDPOINT", success: function(data) {
            if(successCallback) {
                obj.users = data.result;
                successCallback(data.result);
            }
        }, error: errorCallback} );
        */
        successCallback([]); // stub return all users
    };

    /**
     * Starting session. Sending username and password to authenticate and returns user's id.
     */
    obj.login = function(uid, password, successCallback, errorCallback, errorsHandlers) {
        var headers = {};
        /*
        headers[constants.OPENIDM_HEADER_PARAM_USERNAME] = uid;
        headers[constants.OPENIDM_HEADER_PARAM_PASSWORD] = password;
        headers[constants.OPENIDM_HEADER_PARAM_NO_SESION] = false;
        obj.serviceCall({
            serviceUrl: constants.host + "GET_SESSION_DETAILS_ENDPOINT",
            url: "",
            headers: headers,
            success: function (data) {
                if(!data.username) {
                    if(errorCallback) {
                        errorCallback();
                    }
                } else if(successCallback) {
                    successCallback(data);
                }
            },
            error: errorCallback,
            errorsHandlers: errorsHandlers
        });
        delete headers[constants.OPENIDM_HEADER_PARAM_PASSWORD];
        */
        successCallback({username: 'stubUsername', userid: {id : 'stubUserid', component: '' }}); // stub return
    };
       
    /**
     * Check credentials method
     */
    obj.checkCredentials = function(password, successCallback, errorCallback) {

        var headers = {};
        /*
        headers[constants.OPENIDM_HEADER_PARAM_REAUTH] = password;
        obj.serviceCall({
            serviceUrl: constants.host + "REAUTH_PASSWORD_ENDPOINT",
            url: "",
            type: "POST",
            headers: headers,
            success: successCallback,
            error: errorCallback
        });
        */
        successCallback();
    };
    
    /**
     * Checks if logged in and returns users id
     */
    obj.getProfile = function(successCallback, errorCallback, errorsHandlers) {
        /*
        obj.serviceCall({
            serviceUrl: constants.host + "GET_SESSION_DETAILS_ENDPOINT",
            url: "",
            success: function (data) {
                if(!data.username) {
                    if(errorCallback) {
                        errorCallback();
                    }
                } else if(successCallback) {
                    successCallback(data);
                }
            },
            error: errorCallback,
            errorsHandlers: errorsHandlers
        });
        */
        successCallback({username: 'stubUsername', userid: {id : 'stubUserid', component: 'stub' }});
        
    };
    
    obj.getUserById = function(id, component, successCallback, errorCallback) {
        if(component === "stub") {
                successCallback({"_id":"stubUserid","_rev":"0","familyName":"User","givenName":"Stub","userName":"stubUsername","email":"stub@example.com", "roles": "openidm-authorized"});
        }
    };


    
    /**
     * Setting new password for username if security answer is correct
     */
    obj.setNewPassword = function(userName, securityAnswer, newPassword, successCallback, errorCallback) {
        console.info("setting new password for user and security question");
        obj.serviceCall({
            serviceUrl: constants.host + "/openidm/endpoint/securityQA?_action=setNewPasswordForUserName&" + $.param({newPassword: newPassword, uid: userName, securityAnswer: securityAnswer}),
            url: "",
            success: function (data) {
                if(data.result === "correct" && successCallback) {
                    successCallback(data);
                } else if (data.result === "error") {
                    errorCallback(data);
                }
                
            },
            error: errorCallback
        });
    };

    obj.getForUserName = function(uid, successCallback, errorCallback) {
        obj.serviceCall({
            url: "/?_queryId=for-userName&" + $.param({uid: uid}), 
            success: function (data) {
                if(data.result.length !== 1) {
                    if(errorCallback) {
                        errorCallback();
                    }
                } else if(successCallback) {
                    successCallback(data.result[0]);
                }
            },
            error: errorCallback
        });
    };
    
    obj.getForUserID = function(uid, successCallback, errorCallback) {
        obj.serviceCall({
            url: "/" + uid, 
            success: successCallback,
            error: errorCallback
        });
    };

    obj.logout = function() {
        /*
        var callParams = {
                url: "/",
                headers: {  },
                success: function () {
                    console.debug("Successfully logged out");
                },
                error: function () {
                    console.debug("Error during logging out");
                }
        };
        callParams.headers[constants.OPENIDM_HEADER_PARAM_LOGOUT] = true;

        obj.serviceCall(callParams);
        */
    };

    /**
     * See AbstractDelegate.patchEntityDifferences
     */
    obj.patchUserDifferences = function(oldUserData, newUserData, successCallback, errorCallback, noChangesCallback) {
        console.info("updating user");
        obj.patchEntityDifferences({id: oldUserData._id, rev: oldUserData._rev}, oldUserData, newUserData, successCallback, errorCallback, noChangesCallback);
    };

    /**
     * See AbstractDelegate.patchEntity
     */
    obj.patchSelectedUserAttributes = function(id, rev, patchDefinitionObject, successCallback, errorCallback, noChangesCallback) {
        console.info("updating user");
        obj.patchEntity({id: id, rev: rev}, patchDefinitionObject, successCallback, errorCallback, noChangesCallback);
    };

    return obj;
});



