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
                msg: "config.messages.UserMessages.invalidCredentials",
                type: "error"
            },
            "invalidOldPassword": {
                msg: "config.messages.UserMessages.invalidOldPassword",
                type: "error"
            },
            "serviceUnavailable": {
                msg: "config.messages.UserMessages.serviceUnavailable",
                type: "error"
            },
            "changedPassword": {
                msg: "config.messages.UserMessages.changedPassword",
                type: "info"
            },
            "unknown": {
                msg: "config.messages.UserMessages.unknown",
                type: "error"
            },
            "profileUpdateFailed": {
                msg: "config.messages.UserMessages.profileUpdateFailed",
                type: "error"
            },
            "profileUpdateSuccessful": {
                msg: "config.messages.UserMessages.profileUpdateSuccessful",
                type: "info"
            },
            "userNameUpdated": {
                msg: "config.messages.UserMessages.userNameUpdated",
                type: "info"
            },
            "afterRegistration": {
                msg: "config.messages.UserMessages.afterRegistration",
                type: "info"
            },
            "loggedIn": {
                msg: "config.messages.UserMessages.loggedIn",
                type: "info"
            },
            "errorFetchingData": {
                msg: "config.messages.UserMessages.errorFetchingData",
                type: "error"
            },
            "loggedOut": {
                msg: "config.messages.UserMessages.loggedOut",
                type: "info"
            },
            "loginTimeout": {
                msg: "config.messages.UserMessages.loginTimeout",
                type: "info"
            },
            "siteIdentificationChanged": {
                msg: "config.messages.UserMessages.siteIdentificationChanged",
                type: "info"
            },
            "securityDataChanged": {
                msg: "config.messages.UserMessages.securityDataChanged",
                type: "info"
            },
            "unauthorized": {
                msg: "config.messages.UserMessages.unauthorized",
                type: "error"
            },
            "userAlreadyExists": {
                msg: "config.messages.UserMessages.userAlreadyExists",
                type: "error"
            },
            "internalError": {
                msg: "config.messages.UserMessages.internalError",
                type: "error"
            },
            "forbiddenError": {
                msg: "config.messages.UserMessages.forbiddenError",
                type: "error"
            },
            "notFoundError": {
                msg: "config.messages.UserMessages.notFoundError",
                type: "error"
            },
            "badRequestError": {
                msg: "config.messages.UserMessages.badRequestError",
                type: "error"
            },
            "conflictError": {
                msg: "config.messages.UserMessages.conflictError",
                type: "error"
            },
            "errorDeletingNotification": {
                msg: "config.messages.UserMessages.errorDeletingNotification",
                type: "error"
            },
            "errorFetchingNotifications": {
                msg: "config.messages.UserMessages.errorFetchingNotifications",
                type: "error"
            },
            "incorrectRevisionError": {
                msg: "config.messages.UserMessages.incorrectRevisionError",
                type: "error"
            }
    };
    
    return obj;
});