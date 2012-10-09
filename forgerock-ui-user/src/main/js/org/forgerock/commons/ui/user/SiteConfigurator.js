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

/*global $, define, require */

/**
* Interface
* @author mbilski
*/
define("org/forgerock/commons/ui/user/SiteConfigurator", [
   "org/forgerock/commons/ui/common/main/AbstractConfigurationAware",
   "org/forgerock/commons/ui/common/util/Constants", 
   "org/forgerock/commons/ui/common/main/EventManager",
   "org/forgerock/commons/ui/common/main/Configuration"
], function(AbstractConfigurationAware, constants, eventManager, conf) {
   var obj = new AbstractConfigurationAware();
   
   $(document).on(constants.EVENT_CONFIGURATION_CHANGED, function() {
       if(!conf.globalData) {
           conf.setProperty('globalData', {});
       }
       
       if(obj.configuration) {
           var router, changeSecurityDataDialog;
           
           router = require("org/forgerock/commons/ui/common/main/Router");
           
           if(obj.configuration.selfRegistration === true) {
               conf.globalData.selfRegistration = true;
           } else {               
               if(router.configuration && router.configuration.routes.register) {
                   console.log("Removing register route.");
                   delete router.configuration.routes.register;
               }
           }
           
           if(obj.configuration.enterprise === true) {
               conf.globalData.enterprise = true;             
           } else {
               changeSecurityDataDialog = require("org/forgerock/commons/ui/user/profile/ChangeSecurityDataDialog");
               changeSecurityDataDialog.data.height = 210;
               
               if(router.configuration && router.configuration.routes.siteIdentification) {
                   console.log("Removing siteIdentification route.");
                   delete router.configuration.routes.siteIdentification;
               }
               
               if(router.configuration && router.configuration.routes.forgottenPassword) {
                   console.log("Removing forgottenPassword route.");
                   delete router.configuration.routes.forgottenPassword;
               } 
           }
       }
   });
   
   return obj;
});