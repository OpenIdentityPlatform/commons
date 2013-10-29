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

/*global define, require, window, _*/

/**
 * @author yaromin
 */
define("config/process/UserConfig", [
    "org/forgerock/commons/ui/common/util/Constants", 
    "org/forgerock/commons/ui/common/main/EventManager"
], function(constants, eventManager) {
    var obj = [
        {
            startEvent: constants.EVENT_APP_INTIALIZED,
            description: "Starting basic components",
            dependencies: [
                "org/forgerock/commons/ui/common/components/Navigation",
                "org/forgerock/commons/ui/common/components/popup/PopupCtrl",
                "org/forgerock/commons/ui/common/components/Breadcrumbs",
                "org/forgerock/commons/ui/common/main/Router",
                "UserDelegate",
                "org/forgerock/commons/ui/common/main/Configuration",
                "org/forgerock/commons/ui/common/util/UIUtils",
                "org/forgerock/commons/ui/common/main/SessionManager"
            ],
            processDescription: function(event, 
                    navigation, 
                    popupCtrl, 
                    breadcrumbs, 
                    router,
                    userDelegate,
                    conf,
                    uiUtils,
                    sessionManager) {
                              
                breadcrumbs.init();
                uiUtils.preloadTemplates();
                
                sessionManager.getLoggedUser(function(user) {
                    conf.setProperty('loggedUser', user);
                    eventManager.sendEvent(constants.EVENT_AUTHENTICATION_DATA_CHANGED, { anonymousMode: false});
                    router.init();
                }, function() {
                    eventManager.sendEvent(constants.EVENT_AUTHENTICATION_DATA_CHANGED, { anonymousMode: true});
                    router.init();
                });
            }    
        },
        {
            startEvent: constants.EVENT_CHANGE_BASE_VIEW,
            description: "",
            dependencies: [
                "org/forgerock/commons/ui/common/components/Navigation",
                "org/forgerock/commons/ui/common/components/popup/PopupCtrl",
                "org/forgerock/commons/ui/common/components/Breadcrumbs",
                "org/forgerock/commons/ui/common/main/Configuration",
                "org/forgerock/commons/ui/user/login/LoggedUserBarView",
                "org/forgerock/commons/ui/common/components/Footer"
            ],
            processDescription: function(event, navigation, popupCtrl, breadcrumbs, conf, loggedUserBarView,footer) {
                navigation.init();
                popupCtrl.init();                
                
                breadcrumbs.buildByUrl();
                loggedUserBarView.render();
                footer.render();
            }
        },
        {
            startEvent: constants.EVENT_AUTHENTICATION_DATA_CHANGED,
            description: "",
            dependencies: [
                "org/forgerock/commons/ui/common/main/Configuration",
                "org/forgerock/commons/ui/common/components/Navigation",
                "org/forgerock/commons/ui/user/login/LoggedUserBarView"
            ],
            processDescription: function(event, configuration, navigation, loggedUserBarView) {
                var serviceInvokerModuleName, serviceInvokerConfig; 
                serviceInvokerModuleName = "org/forgerock/commons/ui/common/main/ServiceInvoker";
                serviceInvokerConfig = configuration.getModuleConfiguration(serviceInvokerModuleName);
                if(!event.anonymousMode) {
                    delete serviceInvokerConfig.defaultHeaders[constants.OPENIDM_HEADER_PARAM_PASSWORD];
                    delete serviceInvokerConfig.defaultHeaders[constants.OPENIDM_HEADER_PARAM_USERNAME];
                    delete serviceInvokerConfig.defaultHeaders[constants.OPENIDM_HEADER_PARAM_NO_SESION];
                    
                    loggedUserBarView.render();
                    navigation.reload();
                } else {
                    serviceInvokerConfig.defaultHeaders[constants.OPENIDM_HEADER_PARAM_PASSWORD] = constants.OPENIDM_ANONYMOUS_PASSWORD;
                    serviceInvokerConfig.defaultHeaders[constants.OPENIDM_HEADER_PARAM_USERNAME] = constants.OPENIDM_ANONYMOUS_USERNAME;
                    serviceInvokerConfig.defaultHeaders[constants.OPENIDM_HEADER_PARAM_NO_SESION]= true; 
                    
                    configuration.setProperty('loggedUser', null);
                    loggedUserBarView.render();
                    navigation.reload();
                }
                configuration.sendSingleModuleConfigurationChangeInfo(serviceInvokerModuleName);
            }
        },
        {
            startEvent: constants.FORGOTTEN_PASSWORD_CHANGED_SUCCESSFULLY,
            description: "",
            dependencies: [
            ],
            processDescription: function(event) {
                eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "changedPassword");
                eventManager.sendEvent(constants.EVENT_LOGIN_REQUEST, { userName: event.userName, password: event.password});
            }
        },
        {
            startEvent: constants.EVENT_LOGIN_REQUEST,
            description: "",
            dependencies: [
                "org/forgerock/commons/ui/common/main/SessionManager",
                "org/forgerock/commons/ui/common/main/Configuration",
                "org/forgerock/commons/ui/common/main/Router",
                "org/forgerock/commons/ui/common/main/ViewManager"
            ],
            processDescription: function(event, sessionManager, conf, router, viewManager) {
                sessionManager.login(event, function(user) {
                    conf.setProperty('loggedUser', user);
                    
                    eventManager.sendEvent(constants.EVENT_AUTHENTICATION_DATA_CHANGED, { anonymousMode: false});
                    
                    if (! conf.backgroundLogin)
                    {
                        if(conf.globalData.auth.urlParams && conf.globalData.auth.urlParams.goto){
                            window.location.href = conf.globalData.auth.urlParams.goto;
                            return false;
                        }
                        if(conf.gotoURL && _.indexOf(["#","","#/","/#"], conf.gotoURL) === -1) {
                            console.log("Auto redirect to " + conf.gotoURL);
                            router.navigate(conf.gotoURL, {trigger: true});
                            delete conf.gotoURL;
                        } else {
                            router.navigate("", {trigger: true});
                        }
                    } else if (viewManager.currentDialog !== "null") {
                        require(viewManager.currentDialog).close();
                    }
                    
                    eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "loggedIn");
                }, function() {
                    if(conf.globalData.auth.urlParams && conf.globalData.auth.urlParams.gotoOnFail){
                        window.location.href = conf.globalData.auth.urlParams.gotoOnFail;
                        return false;
                    }
                    eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "invalidCredentials"); 
                });
            }
        },
        {
            startEvent: constants.EVENT_USER_SUCCESSFULLY_REGISTERED,
            description: "User registered",
            dependencies: [
                "org/forgerock/commons/ui/common/main/Router"
            ],
            processDescription: function(event, router) {
                eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "afterRegistration");

                if(event.selfRegistration) {
                    eventManager.sendEvent(constants.EVENT_LOGIN_REQUEST, { userName: event.user.userName, password: event.user.password});
                } else {
                    router.navigate(router.configuration.routes.adminUsers.url, {trigger: true});
                }
            }
        },
        {
            startEvent: constants.EVENT_LOGOUT,
            description: "",
            dependencies: [
                "org/forgerock/commons/ui/common/main/Router",
                "org/forgerock/commons/ui/common/main/Configuration",
                "org/forgerock/commons/ui/common/main/SessionManager"
            ],
            processDescription: function(event, router, conf, sessionManager) {
                sessionManager.logout();
                conf.setProperty('loggedUser', null);
                eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "loggedOut");
                eventManager.sendEvent(constants.EVENT_CHANGE_VIEW, {route: router.configuration.routes.login });
                delete conf.gotoURL;
            }
         },
         {
             startEvent: constants.EVENT_UNAUTHORIZED,
             description: "",
             dependencies: [
                 "org/forgerock/commons/ui/common/main/ViewManager",
                 "org/forgerock/commons/ui/common/main/Router",
                 "org/forgerock/commons/ui/common/main/Configuration",
                 "org/forgerock/commons/ui/common/main/SessionManager",
                 "org/forgerock/commons/ui/user/LoginDialog"
             ],
             processDescription: function(error, viewManager, router, conf, sessionManager, loginDialog) {
                 if(!conf.loggedUser) {
                     if(!conf.gotoURL) {
                         conf.setProperty("gotoURL", window.location.hash);
                     }
                     
                     eventManager.sendEvent(constants.EVENT_CHANGE_VIEW, {route: router.configuration.routes.login });
                     return;
                 }
                 
                 sessionManager.getLoggedUser(function(user) {
                     sessionManager.logout();
                     eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "unauthorized");
                     eventManager.sendEvent(constants.EVENT_CHANGE_VIEW, {route: router.configuration.routes.login });
                 }, function() {
                     if (error.error.type === "GET") {
                         conf.setProperty("gotoURL", window.location.hash); 
                         sessionManager.logout();
                         eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "unauthorized");
                         eventManager.sendEvent(constants.EVENT_CHANGE_VIEW, {route: router.configuration.routes.login });
                     } else {
                         viewManager.showDialog(router.configuration.routes.loginDialog.dialog);
                     }
                 });    
             }
         },
         {
             startEvent: constants.EVENT_NOTIFICATION_DELETE_FAILED,
             description: "Error in deleting notification",
             dependencies: [
             ],
             processDescription: function(event) {
                 eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "errorDeletingNotification");
             }
         },
         {
             startEvent: constants.EVENT_GET_NOTIFICATION_FOR_USER_ERROR,
             description: "Error in getting notifications",
             dependencies: [
             ],
             processDescription: function(event) {
                 eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "errorFetchingNotifications");
             }
         }
         ];
    return obj;
});
