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
* @author mbilski
*/
define("org/forgerock/commons/ui/common/SiteConfigurator", [
    "org/forgerock/commons/ui/common/main/AbstractConfigurationAware",
    "org/forgerock/commons/ui/common/util/Constants", 
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/main/i18nManager"
], function(AbstractConfigurationAware, constants, eventManager, conf, i18nManager) {
    var obj = new AbstractConfigurationAware();
    
    obj.initialized = false;
    
    $(document).on(constants.EVENT_READ_CONFIGURATION_REQUEST, function() {
        var configurationDelegate;

        if (!conf.globalData) {
            conf.setProperty('globalData', {});
            conf.globalData.auth = {};
        }

        if (!conf.delegateCache) {
            conf.setProperty('delegateCache', {});
        }

        console.info("READING CONFIGURATION");

        if (obj.configuration && obj.initialized === false) {
            obj.initialized = true;
            
            if(obj.configuration.remoteConfig === true) {
                configurationDelegate = require(obj.configuration.delegate);
                configurationDelegate.getConfiguration(function(config) {
                    obj.processConfiguration(config); 
                    eventManager.sendEvent(constants.EVENT_APP_INTIALIZED);
                }, function() {
                    obj.processConfiguration({}); 
                    eventManager.sendEvent(constants.EVENT_APP_INTIALIZED);
                });
            } else {
                obj.processConfiguration(obj.configuration); 
                eventManager.sendEvent(constants.EVENT_APP_INTIALIZED);
            }          
        }
    });

    obj.configurePage = function (route, params) {
        var promise = $.Deferred(),
            configurationDelegate;

            if (obj.configuration.remoteConfig === true) {
                configurationDelegate = require(obj.configuration.delegate);
                if (typeof configurationDelegate.checkForDifferences === "function") {
                    configurationDelegate.checkForDifferences(route, params).then(function (config) {
                        if (config) {
                            obj.processConfiguration(config);
                        }
                        promise.resolve();
                    });
            } else {
               promise.resolve();
            }
        } else {
            promise.resolve();
        }

        return promise;
    };
    
    obj.processConfiguration = function(config) {
        var router, changeSecurityDataDialog;
        console.log("SiteConfigurator processConfiguration");
        router = require("org/forgerock/commons/ui/common/main/Router");
       
        if (config.selfRegistration === "true" || config.selfRegistration === true) {
            conf.globalData.selfRegistration = config.selfRegistration;
        } 
        
        if (config.securityQuestions === true) {
            conf.globalData.securityQuestions = true;
        } 
 
        if (config.siteImages) {
            conf.globalData.siteImages = config.siteImages;
        }
        
        if (config.passwordResetLink) {
            conf.globalData.passwordResetLink = config.passwordResetLink;
        }

        i18nManager.init(config.lang);

        conf.globalData.requirePasswordForEmailChange = config.requirePasswordForEmailChange;
        conf.globalData.forgotPassword = config.forgotPassword;
        conf.globalData.successfulUserRegistrationDestination = config.successfulUserRegistrationDestination ;
        conf.globalData.auth.cookieName =  config.cookieName;
        conf.globalData.auth.cookieDomains = config.domains;

        if (config.roles) {
            conf.globalData.userRoles = config.roles;
        }
        
        if (config.notificationTypes) {
            conf.notificationTypes = config.notificationTypes;
        }
        
        if (config.defaultNotificationType) {
            conf.defaultType = config.defaultNotificationType;
        }
        
        
    };
    
    return obj;
});
