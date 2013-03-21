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
define("config/routes/UserRoutesConfig", [
    "org/forgerock/commons/ui/common/util/Constants"
], function(constants) {
    
    var obj = {
        "": {
            view: "org/forgerock/commons/ui/user/profile/UserProfileView",
            role: "openidm-authorized,openidm-admin",
            url: ""                                  
        },
        
        //commons
        "profile": {
            view: "org/forgerock/commons/ui/user/profile/UserProfileView",
            role: "openidm-authorized,openidm-admin",
            url: "profile/" 
        },
        "siteIdentification": {
            base: "profile",
            dialog: "org/forgerock/commons/ui/user/profile/ChangeSiteIdentificationDialog",
            url: "profile/site_identification/",
            role: "openidm-authorized,openidm-admin",
            excludedRole: "openidm-admin"
        },
        "register": {
            view: "org/forgerock/commons/ui/user/UserRegistrationView",
            url: "register/"
        },
        "termsOfUse": {
            base: "register",
            dialog: "org/forgerock/commons/ui/user/TermsOfUseDialog",
            url: "register/terms_of_use/"
        },
        "login" : {
            view: "org/forgerock/commons/ui/user/LoginView",
            url: "login/"
        },                           
        "logout" : {
            event: constants.EVENT_LOGOUT,
            url: "logout/"
        },                           
        "loginDialog" : {
            dialog: "org/forgerock/commons/ui/user/LoginDialog",
            url: "loginDialog/"
        },                           
        "forgottenPassword" : {
            base: "login",
            dialog: "org/forgerock/commons/ui/user/ForgottenPasswordDialog",
            url: "profile/forgotten_password/"
        },
        "enterOldPassword": {
            base: "profile",
            dialog: "org/forgerock/commons/ui/user/profile/EnterOldPasswordDialog",
            role: "openidm-authorized,openidm-admin",
            url: "profile/old_password/"
        },
        "changeSecurityData": {
            base: "profile",
            dialog: "org/forgerock/commons/ui/user/profile/ChangeSecurityDataDialog",
            role: "openidm-authorized,openidm-admin",
            url: "profile/change_security_data/"
        }
    };
    
    return obj;
});