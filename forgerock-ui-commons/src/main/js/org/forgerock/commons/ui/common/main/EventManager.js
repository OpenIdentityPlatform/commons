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

/*global define, document */

/**
 * @author yaromin
 */
define("org/forgerock/commons/ui/common/main/EventManager", [
    "jquery",
    "underscore",
    "org/forgerock/commons/ui/common/util/Constants"
], function($, _, constants) {

    /**
     * listenerProxyMap - Association of real listeners and proxies which transforms parameter set
     */
    var obj = {}, listenerProxyMap = [],
        subscriptions = {};


    obj.sendEvent = function (eventId, event) {
        return $.when($(document).triggerHandler(eventId, event)).then(
            function (response) {
                var promise;
                if (_.has(subscriptions, eventId)) {
                    promise = subscriptions[eventId];
                    delete subscriptions[eventId];
                    promise.resolve(response);
                }
                return response;
            },
            function (rejection) {
                var promise;
                if (_.has(subscriptions, eventId)) {
                    promise = subscriptions[eventId];
                    delete subscriptions[eventId];
                    promise.reject(rejection);
                }
                return rejection;
            }
        );
    };

    obj.registerListener = function (eventId, callback) {
        var proxyFunction = function(element, event) {
            return callback(event);
        };
        listenerProxyMap[callback] = proxyFunction;
        $(document).on(eventId, proxyFunction);
    };

    obj.unregisterListener = function (eventId, callback) {
        $(document).off(eventId);
    };

    /**
     * Returns a promise that will be resolved the next time the provided eventId is triggered.
     */
    obj.subscribeTo = function (eventId) {
        if (!_.has(subscriptions, eventId)) {
            subscriptions[eventId] = $.Deferred();
        }
        return subscriptions[eventId];
    }

    return obj;
});
