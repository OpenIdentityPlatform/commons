/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2015 ForgeRock AS. All rights reserved.
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

/*global define, Backbone, _, window */
/*jslint regexp: false*/

/**
 * @author mbilski
 */
define("org/forgerock/commons/ui/common/main/Router", [
    "underscore",
    "org/forgerock/commons/ui/common/main/EventManager", 
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/main/AbstractConfigurationAware"
], function(_, eventManager, constants, conf, AbstractConfigurationAware) {
    /**
     * @exports org/forgerock/commons/ui/common/main/Router
     */

    var obj = new AbstractConfigurationAware();
    
    obj.bindedRoutes = {};
    obj.currentRoute = {};


    obj.getUrl = function() {
        return window.location.href;
    };

    obj.getCurrentUrlBasePart = function() {
        return window.location.protocol + "//" + window.location.host;
    };

    /**
     * Returns the query string from the fragment component
     * @returns {String} Unescaped query string or empty string if no query string was found
     */
    obj.getURIFragmentQueryString = function() {
      var fragment = obj.getURIFragment(),
          queryString = '';

      if(fragment.indexOf('&') > -1) {
        queryString = fragment.substring(fragment.indexOf('&') + 1);
      }

      return queryString;
    };

    /**
     * Returns the query string from the URI
     * @returns {String} Unescaped query string or empty string if no query string was found
     */
    obj.getURIQueryString = function() {
      var queryString = window.location.search;

      return queryString.substr(1, queryString.length);
    };

    /**
     * @deprecated
     * @see Use {@link module:org/forgerock/commons/ui/common/main/Router.getURIFragment}
     */
    obj.getCurrentHash = function() {
        if (obj.getUrl().indexOf('#') === -1) {
            return "";
        } else {
            // cannot use window.location.hash due to FF which de-encodes this parameter.
            return obj.getUrl().substring(obj.getUrl().indexOf('#') + 1);
        }
    };

    /**
     * Returns the fragment component of the current URI
     *
     * Use instead of the inconsistent window.location.hash as Firefox unescapes this parameter incorrectly
     * @see {@link https://bugzilla.mozilla.org/show_bug.cgi?id=135309}
     * @returns {String} Unescaped fragment or empty string if no fragment was found
     */
    obj.getURIFragment = function() {
        return obj.getUrl().split('#')[1] || '';
    };

    /**
     * @deprecated
     * @see Use {@link module:org/forgerock/commons/ui/common/main/Router.getCompositeQueryString}
     */
    obj.getCurrentUrlQueryParameters = function() {
        var hash = obj.getCurrentHash(),
            queries = obj.getURIQueryString();
            // location.search will only return a value if there are queries before the hash.
        if (hash && hash.indexOf('&') > -1) {
            queries = hash.substring(hash.indexOf('&') + 1);
        }
        return queries;
    };

    /**
     * Returns an unescaped composite query string constructed from:<br>
     * <ul><li>Fragment query string</li>
     * <li>URL query string</li></ul>
     * <p>
     * If a fragment query string is present it overrides the URL query string entirely
     * @returns {String} Unescaped query string
     */
    obj.getCompositeQueryString = function() {
      var urlQueryString = obj.getURIQueryString(),
          fragmentQueryString = obj.getURIFragmentQueryString();

      return fragmentQueryString.length ? fragmentQueryString : urlQueryString;
    };

    obj.getCurrentPathName = function() {
        return window.location.pathname;
    };

    obj.setUrl = function(url) {
        window.location.href = url;
    };

    obj.normalizeSubPath = function(subPath) {
        if(subPath.endsWith('/')) {
            return subPath.removeLastChars();
        }
        return subPath;
    };

    obj.convertCurrentUrlToJSON = function() {
        var result = {}, parsedQueryParams;

        result.url = obj.getCurrentUrlBasePart();
        result.pathName = obj.normalizeSubPath(obj.getCurrentPathName());

        result.params = obj.convertQueryParametersToJSON(obj.getCurrentUrlQueryParameters());
        return result;
    };

    obj.convertQueryParametersToJSON = function(queryParameters) {
        if(queryParameters) {
            //create a json object from a query string
            //by taking a query string and splitting it up into individual key=value strings
            return _.object(
                //queryParameters.match(/([^&]+)/g) returns an array of key value pair strings
                _.map(queryParameters.match(/([^&]+)/g), function (pair) {
                   //convert each string into a an array 0 index being the key and 1 index being the value
                   var keyAndValue = pair.match(/([^=]+)=?(.*)/).slice(1);
                       //decode the value
                       keyAndValue[1] = decodeURIComponent(keyAndValue[1]);
                       return keyAndValue;
                })
            );
        }
        return {};
    };

    /**
    * Takes a string and checks if there is a matching url parameter.
    * @returns {String} parameter or null
    */
    obj.getParamByName = function(string){
        var urlParams = obj.convertCurrentUrlToJSON().params;
        if (urlParams && urlParams.hasOwnProperty(string)) {
            return urlParams[string];
        } else {
            return null;
        }
    };

    // returns undecoded route parameters for the provided hash
    obj.extractParameters = function (route, hash) {
        if (_.isRegExp(route.url)) {
            return route.url.exec(hash).slice(1);
        } else {
            return null;
        }
    };

    obj.checkRole = function (route) {
        if(route.role) {
            if(!conf.loggedUser || !_.find(route.role.split(','), function(role) {
                return conf.loggedUser.roles.indexOf(role) !== -1;
            })) {
                eventManager.sendEvent(constants.EVENT_UNAUTHORIZED);
                return false;
            }
        }

        if(route.excludedRole) {
            if(conf.loggedUser && conf.loggedUser.roles.indexOf(route.excludedRole) !== -1) {
                eventManager.sendEvent(constants.EVENT_UNAUTHORIZED);
                return false;
            }
        }
        return true;
    };

    obj.init = function() {
        console.debug("Router init");
        
        var Router = Backbone.Router.extend({
            initialize: function(routes) {
                var route, url;
                
                for(route in routes) {
                    url = routes[route].url;
                    this.route(url, route, _.bind(this.processRoute, {key: route}));
                    obj.bindedRoutes[route] = _.bind(this.processRoute, {key: route});
                }
            },
            processRoute : function() {
                var route = obj.configuration.routes[this.key], baseView, i, args;

                // we don't actually use any of the backbone-provided arguments to this function,
                // as they are decoded and that results in the loss of important context.
                // instead we parse the parameters out of the hash ourselves:
                args = obj.extractParameters(route, obj.getURIFragment());

                if (!obj.checkRole(route)) {
                    return;
                }

                if(route.event) {
                    eventManager.sendEvent(route.event, {route: route, args: args});
                } else if(route.dialog) {
                    route.baseView = obj.configuration.routes[route.base];
                    
                    eventManager.sendEvent(constants.EVENT_SHOW_DIALOG, {route: route, args: args, base: route.base});
                } else if(route.view) {
                    eventManager.sendEvent(constants.EVENT_CHANGE_VIEW, {route: route, args: args});
                }
            }
        });
        
        obj.router = new Router(obj.configuration.routes);
        Backbone.history.start();
    };
    
    obj.routeTo = function(route, params) {
        var link;
        
        if(params && params.args) {
            link = obj.getLink(route, params.args);
        } else {
            link = route.url;
        }

        params.replace = false;
        obj.currentRoute = route;
        obj.router.navigate(link, params);
    };
    
    obj.execRouteHandler = function(routeName) {
        obj.bindedRoutes[routeName]();
    };
    
    obj.navigate = function(link, params) {
        obj.router.navigate(link, params);
    };
    
    obj.getLink = function(route, args) {
        var i,maxArgLength, pattern;
        
        if (typeof route.defaults === "object") {
            if (args) {
                maxArgLength = (args.length >= route.defaults.length) ? args.length : route.defaults.length;
                for (i=0;i<maxArgLength;i++) {
                    if (typeof args[i] !== "string" && route.defaults[i] !== undefined) {
                        args[i] = route.defaults[i];
                    } 
                }
            } else {
                args = route.defaults;
            }
        }

        if (!_.isRegExp(route.url)) {
            pattern = route.url.replace(/:[A-Za-z@.]+/, "?");
        } else {
            pattern = route.pattern;
        }
        
        if (args) {
            for(i = 0; i < args.length; i++) {
                if (typeof args[i] === "string") {
                    pattern = pattern.replace("?", args[i]);
                } else {
                    break;
                }
            }
            pattern = pattern.replace(/\?/g, "");
        }

        return pattern;
    };

    return obj;

});
