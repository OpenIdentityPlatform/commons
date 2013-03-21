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

/*global define, require */

/**
 * @author yaromin
 */
define("config/process/CommonConfig", [
    "org/forgerock/commons/ui/common/util/Constants", 
    "org/forgerock/commons/ui/common/main/EventManager"
], function(constants, eventManager) {
    var obj = [        
        {
            startEvent: constants.EVENT_DIALOG_CLOSE,
            description: "",
            dependencies: [
                "org/forgerock/commons/ui/common/main/Router",
                "org/forgerock/commons/ui/common/main/Configuration",
                "org/forgerock/commons/ui/common/main/ViewManager"
            ],
            processDescription: function(event, router, conf, viewManager) {
                if(conf.baseView) {
                    require(router.configuration.routes[conf.baseView].view).rebind();
                    viewManager.currentDialog = "null";
                    router.navigate(router.getLink(router.configuration.routes[conf.baseView], conf.baseViewArgs));
                }
            }
        },
        {
            startEvent: constants.EVENT_REST_CALL_ERROR,
            description: "",
            dependencies: [
                "org/forgerock/commons/ui/common/main/SpinnerManager",
                "org/forgerock/commons/ui/common/main/ErrorsHandler"
            ],
            processDescription: function(event, spinner, errorsHandler) {
                errorsHandler.handleError(event.data, event.errorsHandlers);
                spinner.hideSpinner();
            }
        },
        {
            startEvent: constants.EVENT_START_REST_CALL,
            description: "",
            dependencies: [
                "org/forgerock/commons/ui/common/main/SpinnerManager"
            ],
            processDescription: function(event, spinner) {
                spinner.showSpinner();
            }
        },
        {
            startEvent: constants.EVENT_END_REST_CALL,
            description: "",
            dependencies: [
                "org/forgerock/commons/ui/common/main/SpinnerManager"
            ],
            processDescription: function(event, spinner) {
                spinner.hideSpinner();
            }
        },
        {
            startEvent: constants.EVENT_CHANGE_VIEW,
            description: "",
            dependencies: [
                "org/forgerock/commons/ui/common/main/ViewManager",
                "org/forgerock/commons/ui/common/main/Router",
                "org/forgerock/commons/ui/common/main/Configuration",
                "org/forgerock/commons/ui/common/components/Navigation"
            ],
            processDescription: function(args, viewManager, router, conf, navigation) {
                var route = args.route, params = args.args;
                
                conf.setProperty("baseView", ""); 
                conf.setProperty("baseViewArgs", ""); 
                                        
                router.navigate(router.getLink(route, params));
                viewManager.changeView(route.view, params, undefined, route.forceUpdate);
                navigation.reload();
            }
        },
        {
            startEvent: constants.EVENT_SHOW_DIALOG,
            description: "",
            dependencies: [
                "org/forgerock/commons/ui/common/main/ViewManager",
                "org/forgerock/commons/ui/common/main/Router",
                "org/forgerock/commons/ui/common/main/Configuration",
                "org/forgerock/commons/ui/common/components/Navigation"
            ],
            processDescription: function(args, viewManager, router, conf, navigation) {
                var route = args.route, params = args.args;
                
                conf.setProperty("baseView", args.base); 
                conf.setProperty("baseViewArgs", params); 
                
                navigation.init();
                
                viewManager.changeView(route.baseView.view, params, function() {  
                    viewManager.showDialog(route.dialog, params);
                    router.navigate(router.getLink(route, params));
                });
            }
        },
        {
            startEvent: constants.EVENT_SERVICE_UNAVAILABLE,
            description: "",
            dependencies: [
                "org/forgerock/commons/ui/common/main/Router"
            ],
            processDescription: function(error, router) {
                eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "serviceUnavailable");
            }
        },
        {
            startEvent: constants.ROUTE_REQUEST,
            description: "",
            dependencies: [
                "org/forgerock/commons/ui/common/main/Router",
                "org/forgerock/commons/ui/common/components/Navigation"
            ],
            processDescription: function(event, router, navigation) {
                if(event.trigger === false) {
                    router.routeTo(event.routeName, {trigger: false, args: event.args});
                } else {
                    router.routeTo(event.routeName, {trigger: true, args: event.args});
                }
                navigation.reload();
            }
        },
        {
            startEvent: constants.EVENT_DISPLAY_MESSAGE_REQUEST,
            description: "",
            dependencies: [
                "org/forgerock/commons/ui/common/components/Messages"
            ],
            processDescription: function(event, messagesManager) {
                messagesManager.messages.displayMessageFromConfig(event);
            }
        }
        ];
    return obj;
});
