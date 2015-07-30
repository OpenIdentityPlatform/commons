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
 * Portions copyright 2011-2015 ForgeRock AS.
 */

/*global define, require, window, _*/

define("config/process/CommonConfig", [
    "jquery",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/EventManager"
], function($, Constants, EventManager) {
    var obj = [
        {
            startEvent: Constants.EVENT_APP_INTIALIZED,
            description: "Starting basic components",
            dependencies: [
                "org/forgerock/commons/ui/common/components/Breadcrumbs",
                "org/forgerock/commons/ui/common/main/Router",
                "org/forgerock/commons/ui/common/main/Configuration",
                "org/forgerock/commons/ui/common/util/UIUtils",
                "org/forgerock/commons/ui/common/util/CookieHelper",
                "org/forgerock/commons/ui/common/main/SessionManager"
            ],
            processDescription: function(event, Breadcrumbs, Router, Configuration, UIUtils, CookieHelper,
                                         SessionManager) {
                Breadcrumbs.init();
                UIUtils.preloadInitialTemplates();
                UIUtils.preloadInitialPartials();

                SessionManager.getLoggedUser(function(user) {
                    Configuration.setProperty('loggedUser', user);
                    EventManager.sendEvent(Constants.EVENT_AUTHENTICATION_DATA_CHANGED, { anonymousMode: false});
                    Router.init();
                }, function() {
                    if (!CookieHelper.cookiesEnabled()) {
                        location.href = "#enableCookies/";
                    }
                    EventManager.sendEvent(Constants.EVENT_AUTHENTICATION_DATA_CHANGED, { anonymousMode: true});
                    Router.init();
                });
            }
        },
        {
            startEvent: Constants.EVENT_CHANGE_BASE_VIEW,
            description: "",
            dependencies: [
                "org/forgerock/commons/ui/common/components/Navigation",
                "org/forgerock/commons/ui/common/components/popup/PopupCtrl",
                "org/forgerock/commons/ui/common/components/Breadcrumbs",
                "org/forgerock/commons/ui/common/main/Configuration",
                "org/forgerock/commons/ui/common/components/Footer"
            ],
            processDescription: function(event, navigation, popupCtrl, breadcrumbs, conf,footer) {
                navigation.init();
                popupCtrl.init();

                breadcrumbs.buildByUrl();
                footer.render();
            }
        },
        {
            startEvent: Constants.EVENT_AUTHENTICATION_DATA_CHANGED,
            description: "",
            dependencies: [
                "org/forgerock/commons/ui/common/main/Configuration",
                "org/forgerock/commons/ui/common/components/Navigation"
            ],
            processDescription: function(event, Configuration, Navigation) {
                var serviceInvokerModuleName, serviceInvokerConfig;
                serviceInvokerModuleName = "org/forgerock/commons/ui/common/main/ServiceInvoker";
                serviceInvokerConfig = Configuration.getModuleConfiguration(serviceInvokerModuleName);
                if (!event.anonymousMode) {
                    delete Configuration.globalData.authorizationFailurePending;
                    delete serviceInvokerConfig.defaultHeaders[Constants.HEADER_PARAM_PASSWORD];
                    delete serviceInvokerConfig.defaultHeaders[Constants.HEADER_PARAM_USERNAME];
                    delete serviceInvokerConfig.defaultHeaders[Constants.HEADER_PARAM_NO_SESSION];

                    EventManager.sendEvent(Constants.EVENT_AUTHENTICATED);
                } else {
                    serviceInvokerConfig.defaultHeaders[Constants.HEADER_PARAM_PASSWORD] = Constants.ANONYMOUS_PASSWORD;
                    serviceInvokerConfig.defaultHeaders[Constants.HEADER_PARAM_USERNAME] = Constants.ANONYMOUS_USERNAME;
                    serviceInvokerConfig.defaultHeaders[Constants.HEADER_PARAM_NO_SESSION]= true;

                    Configuration.setProperty("loggedUser", null);
                    Navigation.reload();
                }
                Configuration.sendSingleModuleConfigurationChangeInfo(serviceInvokerModuleName);
            }
        },
        {
            startEvent: Constants.EVENT_UNAUTHORIZED,
            description: "",
            dependencies: [
                "org/forgerock/commons/ui/common/main/Router",
                "org/forgerock/commons/ui/common/main/Configuration",
                "org/forgerock/commons/ui/common/main/SessionManager"
            ],
            processDescription: function(error, Router, Configuration, SessionManager) {
                var setGoToUrlProperty = function () {
                    var hash = Router.getCurrentHash();
                    if (!Configuration.gotoURL && !hash.match(Router.configuration.routes.login.url)) {
                        Configuration.setProperty("gotoURL", "#" + hash);
                    }
                };

                // Multiple rest calls that all return authz failures will cause this event to be called multiple times
                if (Configuration.globalData.authorizationFailurePending !== undefined) {
                    return;
                }

                Configuration.globalData.authorizationFailurePending = true;

                if (!Configuration.loggedUser) {
                    setGoToUrlProperty();

                    EventManager.sendEvent(Constants.EVENT_AUTHENTICATION_DATA_CHANGED, {
                        anonymousMode: true
                    });
                    EventManager.sendEvent(Constants.EVENT_CHANGE_VIEW, {
                        route: Router.configuration.routes.login
                    });
                    return;
                }

                function logout (callback) {
                    setGoToUrlProperty();

                    SessionManager.logout(function() {
                        EventManager.sendEvent(Constants.EVENT_AUTHENTICATION_DATA_CHANGED, {
                            anonymousMode: true
                        });
                        EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "unauthorized");
                        EventManager.sendEvent(Constants.EVENT_CHANGE_VIEW, {
                            route: Router.configuration.routes.login
                        });
                    });
                }

                if (typeof error !== "object" || error === null ||
                    typeof error.error !== "object" || error.error === null) {
                    logout();
                } else {
                    // Special case for GET requests, behavior should be different based on the error code returned
                    if (error.error.type === "GET") {
                        if (error.error.status === 501 || error.error.status === 403) {
                            // 501 Not Implemented or 403 Forbidden. Log out and redirect to the login view
                            logout();
                        } else if (error.error.status === 401) {
                            // 401 Unauthorized. Unauthorized in-app GET requests, just show the login dialog
                            EventManager.sendEvent(Constants.EVENT_SHOW_LOGIN_DIALOG);
                        }
                    } else {
                        // Session expired, just show the login dialog
                        EventManager.sendEvent(Constants.EVENT_SHOW_LOGIN_DIALOG);
                    }
                }
            }
        },
        {
            startEvent: Constants.EVENT_DIALOG_CLOSE,
            description: "",
            dependencies: [
                "org/forgerock/commons/ui/common/main/Router",
                "org/forgerock/commons/ui/common/main/Configuration",
                "org/forgerock/commons/ui/common/main/ViewManager",
                "org/forgerock/commons/ui/common/components/Navigation"
            ],
            processDescription: function(event, Router, Configuration, ViewManager, Navigation) {
                ViewManager.currentDialog = null;
                if (Configuration.baseView) {
                    require(Router.configuration.routes[Configuration.baseView].view).rebind();
                    Router.navigate(Router.getLink(Router.configuration.routes[Configuration.baseView], Configuration.baseViewArgs));
                    Navigation.reload();
                }
            }
        },
        {
            startEvent: Constants.EVENT_REST_CALL_ERROR,
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
            startEvent: Constants.EVENT_START_REST_CALL,
            description: "",
            dependencies: [
                "org/forgerock/commons/ui/common/main/SpinnerManager"
            ],
            processDescription: function(event, spinner) {
                if (!event.suppressSpinner) {
                    spinner.showSpinner();
                }
            }
        },
        {
            startEvent: Constants.EVENT_END_REST_CALL,
            description: "",
            dependencies: [
                "org/forgerock/commons/ui/common/main/SpinnerManager"
            ],
            processDescription: function(event, spinner) {
                spinner.hideSpinner();
            }
        },
        {
            startEvent: Constants.EVENT_CHANGE_VIEW,
            description: "",
            dependencies: [
                "org/forgerock/commons/ui/common/main/ViewManager",
                "org/forgerock/commons/ui/common/main/Router",
                "org/forgerock/commons/ui/common/main/Configuration",
                "org/forgerock/commons/ui/common/components/Navigation",
                "org/forgerock/commons/ui/common/main/SpinnerManager",
                "org/forgerock/commons/ui/common/SiteConfigurator"
            ],
            processDescription: function(args, viewManager, router, conf, navigation, spinner, siteConfigurator) {
                var route = args.route, params = args.args, callback = args.callback,
                    view = require(route.view);

                if (!router.checkRole(route)) {
                    return;
                }

                view.route = route;

                params = params || route.defaults;
                conf.setProperty("baseView", "");
                conf.setProperty("baseViewArgs", "");

                siteConfigurator.configurePage(route, params).then(function () {
                    spinner.hideSpinner(10);
                    router.routeTo(route, {trigger: true, args: params});
                    viewManager.changeView(route.view, params, callback, route.forceUpdate);
                    navigation.reload();
                });
            }
        },
        {
            startEvent: Constants.EVENT_SHOW_DIALOG,
            description: "",
            dependencies: [
                "org/forgerock/commons/ui/common/main/ViewManager",
                "org/forgerock/commons/ui/common/main/Router",
                "org/forgerock/commons/ui/common/main/Configuration",
                "org/forgerock/commons/ui/common/components/Navigation"
            ],
            processDescription: function(args, viewManager, router, conf, navigation) {
                var route = args.route,
                    params = args.args,
                    callback = args.callback,
                    baseViewArgs;

                if (!router.checkRole(route)) {
                    return;
                }

                if (viewManager.currentViewArgs === null) {
                    baseViewArgs = params;
                } else {
                    baseViewArgs = viewManager.currentViewArgs;
                }

                conf.setProperty("baseView", args.base);
                conf.setProperty("baseViewArgs", baseViewArgs);

                navigation.init();

                if (!_.has(route, "baseView") && _.has(route, "base")) {
                    viewManager.changeView(router.configuration.routes[route.base].view, baseViewArgs, function() {
                        viewManager.showDialog(route.dialog, params);
                        router.navigate(router.getLink(route, params));
                        if (callback) {
                            callback();
                        }
                    });
                } else {
                    /*
                     * There is an expectation that the base view uses some subset of the same
                     * params that the dialog uses, and that they are in the same order.
                     * The base might have a url like myView/foo, where '/foo' is the first param.
                     * The dialog should be constructed so that its own arguments follow, like so:
                     * myViewDialog/foo/bar - the params being '/foo' and '/bar'. Because '/foo'
                     * is still in the first position, it is reasonable to pass to the base view
                     * (along with '/bar', which will presumably be ignored)
                     */

                    viewManager.changeView(route.baseView.view, baseViewArgs, function() {
                        viewManager.showDialog(route.dialog, params);
                        router.navigate(router.getLink(route, params));
                        if (callback) {
                            callback();
                        }
                    });
                }
            }
        },
        {
            startEvent: Constants.EVENT_SERVICE_UNAVAILABLE,
            description: "",
            dependencies: [
                "org/forgerock/commons/ui/common/main/Router"
            ],
            processDescription: function(error, router) {
                EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "serviceUnavailable");
            }
        },
        {
            startEvent: Constants.ROUTE_REQUEST,
            description: "",
            dependencies: [
                "org/forgerock/commons/ui/common/main/Router",
                "org/forgerock/commons/ui/common/components/Navigation"
            ],
            processDescription: function(event, router, navigation) {
                if(event.trigger === false) {
                    router.routeTo(router.configuration.routes[event.routeName], {trigger: false, args: event.args});
                } else {
                    router.routeTo(router.configuration.routes[event.routeName], {trigger: true, args: event.args});
                }
                navigation.reload();
            }
        },
        {
            startEvent: Constants.EVENT_DISPLAY_MESSAGE_REQUEST,
            description: "",
            dependencies: [
                "org/forgerock/commons/ui/common/components/Messages"
            ],
            processDescription: function(event, messagesManager) {
                messagesManager.messages.displayMessageFromConfig(event);
            }
        },
        {
            startEvent: Constants.EVENT_LOGIN_REQUEST,
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

                    EventManager.sendEvent(Constants.EVENT_AUTHENTICATION_DATA_CHANGED, { anonymousMode: false});

                    if (! conf.backgroundLogin) {
                        if(conf.globalData.auth.urlParams && conf.globalData.auth.urlParams.goto){
                            window.location.href = conf.globalData.auth.urlParams.goto;
                            return false;
                        }
                        if(conf.gotoURL && _.indexOf(["#","","#/","/#"], conf.gotoURL) === -1) {
                            console.log("Auto redirect to " + conf.gotoURL);
                            router.navigate(conf.gotoURL, {trigger: true});
                            delete conf.gotoURL;
                        } else {
                            if (router.checkRole(router.configuration.routes["default"])) {
                                EventManager.sendEvent(Constants.ROUTE_REQUEST, {routeName: "default", args: []});
                            } else {
                                EventManager.sendEvent(Constants.EVENT_UNAUTHORIZED);
                                return;
                            }
                        }
                    } else if (viewManager.currentDialog !== null) {
                        require(viewManager.currentDialog).close();
                    } else {
                        $(".modal").modal("hide");
                    }

                }, function (reason) {
                    if (conf.globalData.auth.urlParams && conf.globalData.auth.urlParams.gotoOnFail) {
                        window.location.href = conf.globalData.auth.urlParams.gotoOnFail;
                        return false;
                    }
                    reason = reason ? reason : "authenticationFailed";
                    EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, reason);
                });
            }
        },
        {
            startEvent: Constants.EVENT_SHOW_LOGIN_DIALOG,
            description: "",
            dependencies: [
                "LoginDialog"
            ],
            processDescription: function(event, LoginDialog) {
                LoginDialog.render(event);
            }
        },
        {
            startEvent: Constants.EVENT_LOGOUT,
            description: "",
            dependencies: [
                "org/forgerock/commons/ui/common/main/Router",
                "org/forgerock/commons/ui/common/main/Configuration",
                "org/forgerock/commons/ui/common/main/SessionManager"
            ],
            processDescription: function(event, router, conf, sessionManager) {
                sessionManager.logout(function() {
                    EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "loggedOut");
                    EventManager.sendEvent(Constants.EVENT_AUTHENTICATION_DATA_CHANGED, { anonymousMode: true});
                    EventManager.sendEvent(Constants.EVENT_CHANGE_VIEW, {route: router.configuration.routes.login });
                    delete conf.gotoURL;
                });
            }
        }

        ];
    return obj;
});
