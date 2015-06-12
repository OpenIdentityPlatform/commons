/** 
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2014 ForgeRock AS. All rights reserved.
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
/*jslint regexp:false */

/**
 * @author jdabrowski
 */
define("config/routes/UserRoutesConfig", [
], function() {
    //definitions for views here are generic
    //the actual path to each view is defined in config/AppConfiguration.js
    //view files are loaded when the GenericRouteInterfaceMap module is initialized
    var obj = {
        "profile": {
            view: "UserProfileView",
            role: "ui-user",
            url: "profile/" ,
            forceUpdate: true
        },
        "selfRegistration": {
            view: "RegisterView",
            url: /register(\/[^\&]*)(\&.+)?/,
            pattern: "register??",
            forceUpdate: true,
            argumentNames: ["realm", "additionalParameters"],
            defaults: ["/",""]
        },
        "changeSecurityData": {
            base: "profile",
            view: "ChangeSecurityDataDialog",
            role: "ui-user,ui-admin",
            url: "profile/change_security_data/"
        }
    };
    
    return obj;
});