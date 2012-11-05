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
 * @author yaromin
 */
define("org/forgerock/commons/ui/user/delegates/UserDelegate", [
	"org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/AbstractDelegate",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/main/EventManager"
], function(constants, AbstractDelegate, configuration, eventManager) {

    var obj = new AbstractDelegate(constants.host + "/openidm/managed/user");

    obj.usersCallback = null;
    obj.users = null;
    obj.numberOfUsers = 0;

    obj.getAllUsers = function(successCallback, errorCallback) {
        console.info("getting all users");

        obj.usersCallback = successCallback;
        obj.numberOfUsers = 0;

        obj.serviceCall({url: "/?_query-id=query-all&fields=*", success: function(data) {
            if(successCallback) {
                obj.users = data.result;
                successCallback(data.result);
            }
        }, error: errorCallback} );
    };

    /**
     * Starting session. Sending username and password to authenticate and returns user's id.
     */
    obj.login = function(uid, password, successCallback, errorCallback, errorsHandlers) {
        var headers = {};
        headers[constants.OPENIDM_HEADER_PARAM_USERNAME] = uid.toLowerCase();
        headers[constants.OPENIDM_HEADER_PARAM_PASSWORD] = password;
        headers[constants.OPENIDM_HEADER_PARAM_NO_SESION] = false;
        obj.serviceCall({
            serviceUrl: constants.host + "/openidm/info/login",
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
    };
       
    /**
     * Check credentials method
     */
    obj.checkCredentials = function(uid, password, successCallback, errorCallback) {
        obj.serviceCall({
            url: "/?_query-id=for-credentials&" + $.param({password: password, uid: uid.toLowerCase()}),
            success: successCallback,
            error: errorCallback
        });
    };
    
    /**
     * Checks if logged in and returns users id
     */
    obj.getProfile = function(successCallback, errorCallback, errorsHandlers) {
        obj.serviceCall({
            serviceUrl: constants.host + "/openidm/info/login",
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
    };
    
    obj.readInternalEntity = function(id, successCallback, errorCallback) {
        this.serviceCall({serviceUrl: constants.host + "/openidm/repo/internal/user", url: "/" + id, type: "GET", success: successCallback, error: errorCallback});
    };

    /**
     * Check security answer method
     */
    obj.getBySecurityAnswer = function(uid, securityAnswer, successCallback, errorCallback) {
        obj.serviceCall({
            url: "/?_query-id=for-security-answer&" + $.param({uid: uid, securityAnswer: securityAnswer}), 
            success: function (data) {
                if(!data.result) {
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

    obj.getSecurityQuestionForUserName = function(uid, successCallback, errorCallback) {
        obj.serviceCall({
            url: "/?_query-id=get-security-question&" + $.param({uid: uid.toLowerCase()}), 
            success: function (data) {
                if(data.result.length !== 1) {
                    successCallback();
                } else if(successCallback) {
                    successCallback(data.result[0].securityQuestion);
                }
            },
            error: errorCallback
        });
    };

    obj.getForUserName = function(uid, successCallback, errorCallback) {
        obj.serviceCall({
            url: "/?_query-id=for-userName&" + $.param({uid: uid.toLowerCase()}), 
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
    
    /**
     * UserName availability check. 
     * If userName is available successCallback(true) is invoked, otherwise successCallback(false) is invoked
     * If error occurred, errorCallback is invoked. 
     */
    obj.checkUserNameAvailability = function(uid, successCallback, errorCallback) {
        obj.serviceCall({
            url: "/?_query-id=check-userName-availability&" + $.param({uid: uid.toLowerCase()}), 
            success: function (data) {
                if(successCallback) {
                    if(data.result.length === 0) { 
                        successCallback(true);
                    } else {
                        successCallback(false);
                    }
                }
            },
            error: errorCallback
        });
    };

    obj.logout = function() {
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
    
    /**
     * Setting new password for username if security answer is correct
     */
    obj.setNewPassword = function(userName, securityAnswer, newPassword, successCallback, errorCallback) {
        console.info("setting new password for user and security question");
        obj.serviceCall({
            url: "/?_query-id=set-newPassword-for-userName-and-security-answer&" + $.param({newpassword: newPassword, username: userName, securityAnswer: securityAnswer}),
            success: successCallback,
            error: errorCallback
        });
    };

    return obj;
});



