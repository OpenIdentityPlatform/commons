/**
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2015-2016 ForgeRock AS.
 */

define([
], function() {
    var obj = {},
        kbaEnabled = true;

    obj.getConfiguration = function(successCallback) {
        // based on whatever environmental condition able to be read, set the appropriate
        // version of the UserProfileView. In this case, hard-code it to the KBA version

        if (kbaEnabled === true) {
            require.config({"map": { "*": {
                "UserProfileView" : "org/forgerock/commons/ui/user/profile/UserProfileKBAView"
            } } } );
        } else {
            require.config({"map": { "*": {
                "UserProfileView": "org/forgerock/commons/ui/user/profile/UserProfileView"
            } } } );
        }

        successCallback({
            "passwordResetLink": "",
            "forgotUsername": true,
            "selfRegistration": true,
            "passwordReset": true,
            "lang": "en"
        });
    };
    return obj;
});
