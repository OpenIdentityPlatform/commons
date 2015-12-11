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
 * Copyright 2011-2015 ForgeRock AS.
 */

/*global define, window, _*/

define("config/process/CommonConfig", [
    "jquery",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/EventManager"
], function($, Constants, EventManager) {
    var obj = [
        {
            startEvent: Constants.EVENT_APP_INITIALIZED,
            description: "Starting basic components",
            dependencies: [
                "org/forgerock/commons/ui/common/main/Router",
                "org/forgerock/commons/ui/common/main/Configuration",
                "org/forgerock/commons/ui/common/util/UIUtils",
                "org/forgerock/commons/ui/common/util/CookieHelper",
                "org/forgerock/commons/ui/common/main/SessionManager",
                "org/forgerock/commons/ui/common/main/i18nManager"
            ],
            processDescription: function(event, Router, Configuration, UIUtils, CookieHelper, SessionManager, i18nManager) {
                var postSessionCheck = function () {
                        UIUtils.preloadInitialTemplates();
                        UIUtils.preloadInitialPartials();
                        Router.init();
                    },
                    initLocalization = function () {
                        i18nManager.init({
                            serverLang: Configuration.globalData.lang,
                            paramLang: Router.convertCurrentUrlToJSON().params,
                            defaultLang: Constants.DEFAULT_LANGUAGE
                        });
                    };

                SessionManager.getLoggedUser(function(user) {
                    Configuration.setProperty('loggedUser', user);
                    initLocalization();
                    // WARNING - do not use the promise returned from sendEvent as an example for using this system
                    // TODO - replace with simplified event system as per CUI-110
                    EventManager.sendEvent(Constants.EVENT_AUTHENTICATION_DATA_CHANGED, { anonymousMode: false}).then(postSessionCheck);
                }, function() {
                    initLocalization();
                    if (!CookieHelper.cookiesEnabled()) {
                        location.href = "#enableCookies/";
                    }
                    EventManager.sendEvent(Constants.EVENT_AUTHENTICATION_DATA_CHANGED, { anonymousMode: true}).then(postSessionCheck);
                });
            }
        },
        {
            startEvent: Constants.EVENT_CHANGE_BASE_VIEW,
            description: "",
            dependencies: [
                "org/forgerock/commons/ui/common/components/Navigation",
                "org/forgerock/commons/ui/common/components/LoginHeader",
                "Footer"

            ],
            processDescription: function(event, Navigation, LoginHeader, Footer) {
                LoginHeader.render();
                Navigation.init();
                Footer.render();
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
                    return EventManager.sendEvent(Constants.EVENT_CHANGE_VIEW, {
                        route: Router.configuration.routes.login
                    });
                }

                function logout (callback) {
                    setGoToUrlProperty();

                    return SessionManager.logout().then(function() {
                        EventManager.sendEvent(Constants.EVENT_AUTHENTICATION_DATA_CHANGED, {
                            anonymousMode: true
                        });
                        EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "unauthorized");
                        return EventManager.sendEvent(Constants.EVENT_CHANGE_VIEW, {
                            route: Router.configuration.routes.login
                        });
                    });

                }

                if (typeof error !== "object" || error === null ||
                    typeof error.error !== "object" || error.error === null) {
                    return logout();
                } else {
                    // Special case for GET requests, behavior should be different based on the error code returned
                    if (error.error.type === "GET") {
                        if (error.error.status === 403) {
                            // 403 Forbidden. Log out and redirect to the login view
                            return logout();
                        } else if (error.error.status === 401) {
                            // 401 Unauthorized. Unauthorized in-app GET requests, just show the login dialog
                            return EventManager.sendEvent(Constants.EVENT_SHOW_LOGIN_DIALOG);
                        }
                    } else {
                        // Session expired, just show the login dialog
                        return EventManager.sendEvent(Constants.EVENT_SHOW_LOGIN_DIALOG);
                    }
                }
            }
        },
        {
            startEvent: Constants.EVENT_DIALOG_CLOSE,
            description: "",
            dependencies: [
                "org/forgerock/commons/ui/common/main/Configuration",
                "org/forgerock/commons/ui/common/util/ModuleLoader",
                "org/forgerock/commons/ui/common/components/Navigation",
                "org/forgerock/commons/ui/common/main/Router",
                "org/forgerock/commons/ui/common/main/ViewManager"
            ],
            processDescription: function(event, Configuration, ModuleLoader, Navigation, Router, ViewManager) {
                ViewManager.currentDialog = null;
                if(Configuration.baseView) {
                    ModuleLoader.load(Router.configuration.routes[Configuration.baseView].view).then(function (view) {
                        view.rebind();
                        Router.navigate(Router.getLink(Router.configuration.routes[Configuration.baseView], Configuration.baseViewArgs));
                        Navigation.reload();
                    });
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
                "org/forgerock/commons/ui/common/main/Configuration",
                "org/forgerock/commons/ui/common/util/ModuleLoader",
                "org/forgerock/commons/ui/common/components/Navigation",
                "org/forgerock/commons/ui/common/main/Router",
                "org/forgerock/commons/ui/common/SiteConfigurator",
                "org/forgerock/commons/ui/common/main/SpinnerManager",
                "org/forgerock/commons/ui/common/main/ViewManager"
            ],
            processDescription: function(event, Configuration, ModuleLoader, Navigation, Router, SiteConfigurator, SpinnerManager, ViewManager) {
                var route = event.route,
                    params = event.args,
                    callback = event.callback,
                    fromRouter = event.fromRouter;

                if (!Router.checkRole(route)) {
                    return;
                }

                if (Configuration.backgroundLogin) {
                    return;
                }

                return ModuleLoader.load(route.view).then(function (view) {
                    view.route = route;

                    params = params || route.defaults;
                    Configuration.setProperty("baseView", "");
                    Configuration.setProperty("baseViewArgs", "");

                    return SiteConfigurator.configurePage(route, params).then(function () {
                        var promise = $.Deferred();
                        SpinnerManager.hideSpinner(10);
                        if (!fromRouter) {
                            Router.routeTo(route, {trigger: true, args: params});
                        }

                        ViewManager.changeView(route.view, params, function () {
                            if (callback) {
                                callback();
                            }
                            promise.resolve(view);
                        }, route.forceUpdate);

                        Navigation.reload();
                        return promise;
                    });
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
                "org/forgerock/commons/ui/common/components/Navigation",
                "org/forgerock/commons/ui/common/main/Router",
                "org/forgerock/commons/ui/common/main/ViewManager"
            ],
            processDescription: function(event, Navigation, Router, ViewManager) {
                var route = Router.configuration.routes[event.routeName];

                // trigger defaults to true
                if (event.trigger === undefined) {
                    event.trigger = true;
                } else if (event.trigger === false) {
                    ViewManager.currentView = route.view;
                    ViewManager.currentViewArgs = event.args;
                }

                Router.routeTo(route, event);
                Navigation.reload();
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
                "org/forgerock/commons/ui/common/main/Configuration",
                "org/forgerock/commons/ui/common/util/ModuleLoader",
                "org/forgerock/commons/ui/common/main/Router",
                "org/forgerock/commons/ui/common/main/SessionManager",
                "org/forgerock/commons/ui/common/main/ViewManager"
            ],
            processDescription: function(event, Configuration, ModuleLoader, Router, SessionManager, ViewManager) {
                var promise = $.Deferred(),
                    submitContent = event.submitContent || event;

                SessionManager.login(submitContent, function(user) {
                    Configuration.setProperty('loggedUser', user);

                    EventManager.sendEvent(Constants.EVENT_AUTHENTICATION_DATA_CHANGED, { anonymousMode: false});

                    if (!Configuration.backgroundLogin) {
                        if (Configuration.globalData.auth.urlParams && Configuration.globalData.auth.urlParams.goto) {
                            window.location.href = Configuration.globalData.auth.urlParams.goto;
                            return false;
                        }

                        if (Configuration.gotoURL && _.indexOf(["#","","#/","/#"], Configuration.gotoURL) === -1) {
                            Router.navigate(Configuration.gotoURL, {trigger: true});
                            delete Configuration.gotoURL;
                        } else {
                            if (Router.checkRole(Router.configuration.routes["default"])) {
                                EventManager.sendEvent(Constants.ROUTE_REQUEST, {routeName: "default", args: []});
                            } else {
                                EventManager.sendEvent(Constants.EVENT_UNAUTHORIZED);
                                return;
                            }
                        }
                    } else if (ViewManager.currentDialog !== null) {
                        ModuleLoader.load(ViewManager.currentDialog).then(function (dialog) {
                            dialog.close();
                        });
                    } else if (typeof $.prototype.modal === "function") {
                        $(".modal.in").modal("hide");
                        // There are some cases, when user is presented with login modal panel,
                        // rather than a normal login view. backgroundLogin property is used to
                        // indicate such cases. It should be deleted afterwards for correct
                        // display of the login view later
                        delete Configuration.backgroundLogin;
                    }

                    promise.resolve(user);

                }, function (reason) {

                    if (Configuration.globalData.auth.urlParams && Configuration.globalData.auth.urlParams.gotoOnFail) {
                        window.location.href = Configuration.globalData.auth.urlParams.gotoOnFail;
                        return false;
                    }

                    reason = reason ? reason : "authenticationFailed";
                    EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, reason);

                    if (event.failureCallback) {
                        event.failureCallback(reason);
                    }

                    promise.reject(reason);

                });

                return promise;
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
                return sessionManager.logout(function() {
                    delete conf.gotoURL;
                    EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "loggedOut");
                    EventManager.sendEvent(Constants.EVENT_AUTHENTICATION_DATA_CHANGED, { anonymousMode: true});
                    return EventManager.sendEvent(Constants.EVENT_CHANGE_VIEW, {route: router.configuration.routes.login });
                });
            }
        }

        ];
    return obj;
});
