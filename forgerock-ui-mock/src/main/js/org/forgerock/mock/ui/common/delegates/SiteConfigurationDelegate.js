/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2015 ForgeRock AS. All rights reserved.
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

/*global define, require */

define("org/forgerock/mock/ui/common/delegates/SiteConfigurationDelegate", [
], function(Constants, AbstractDelegate, configuration, eventManager) {
    var obj = {},
        kbaEnabled = true;

    obj.getConfiguration = function(successCallback, errorCallback) {
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
            "selfRegistration": true,
            "passwordReset": true,
            "lang": "en"
        });
    };
    return obj;
});
