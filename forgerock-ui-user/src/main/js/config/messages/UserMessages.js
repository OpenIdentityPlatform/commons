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

/**
 * @author jdabrowski
 */
define("config/messages/UserMessages", [
], function() {
    
    var obj = {
            "invalidCredentials": {
                msg: "Login/password combination is invalid.",
                type: "error"
            },
            "serviceUnavailable": {
                msg: "Service unavailable",
                type: "error"
            },
            "changedPassword": {
                msg: "Password has been changed",
                type: "info"
            },
            "unknown": {
                msg: "Unknown error. Please contact with administrator",
                type: "error"
            },
            "profileUpdateFailed": {
                msg: "Problem during profile update",
                type: "error"
            },
            "profileUpdateSuccessful": {
                msg: "Profile has been updated",
                type: "info"
            },
            "userNameUpdated": {
                msg: "Username has been modified succesfully.",
                type: "info"
            },
            "afterRegistration": {
                msg: "User has been registered successfully",
                type: "info"
            },
            "loggedIn": {
                msg: "You have been successfully logged in.",
                type: "info"
            },
            "errorFetchingData": {
                msg: "Error fetching user data",
                type: "error"
            },
            "loggedOut": {
                msg: "You have been logged out.",
                type: "info"
            },
            "siteIdentificationChanged": {
                msg: "Site identification image has been changed",
                type: "info"
            },
            "securityDataChanged": {
                msg: "Security data has been changed",
                type: "info"
            },
            "unauthorized": {
                msg: "Unauthorized access or session timeout",
                type: "error"
            },
            "userAlreadyExists": {
                msg: "User already exists",
                type: "error"
            },
            "internalError": {
                msg: "Internal server error",
                type: "error"
            },
            "forbiddenError": {
                msg: "Forbidden request error.",
                type: "error"
            },
            "notFoundError": {
                msg: "Not found error.",
                type: "error"
            },
            "badRequestError": {
                msg: "Bad request error.",
                type: "error"
            },
            "conflictError": {
                msg: "Detected conflict in request.",
                type: "error"
            }
    };
    
    return obj;
});