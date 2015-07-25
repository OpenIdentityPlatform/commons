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

/*global define */

define("org/forgerock/commons/ui/common/main/EventManager", [
    "jquery",
    "underscore",
    "org/forgerock/commons/ui/common/util/Constants"
], function($, _, constants) {

    var obj = {},
        eventRegistry = {},
        subscriptions = {};


    obj.sendEvent = function (eventId, event) {
        return $.when.apply($,

            _.map(eventRegistry[eventId], function (eventHandler) {
                var response = eventHandler(event);

                // in the case when the event handler didn't return anything, just pass along the original event
                if (response === undefined) {
                    return event;
                } else {
                    return response;
                }
            })

        ).then(
            function () {
                var promise;
                if (_.has(subscriptions, eventId)) {
                    promise = subscriptions[eventId];
                    delete subscriptions[eventId];
                    promise.resolve(_.toArray(arguments));
                }
                return _.toArray(arguments);
            },
            function () {
                var promise;
                if (_.has(subscriptions, eventId)) {
                    promise = subscriptions[eventId];
                    delete subscriptions[eventId];
                    promise.reject(_.toArray(arguments));
                }
                return _.toArray(arguments);
            }
        );
    };

    obj.registerListener = function (eventId, callback) {
        if (!_.has(eventRegistry, eventId)) {
            eventRegistry[eventId] = [callback];
        } else {
            eventRegistry[eventId].push(callback);
        }
    };

    obj.unregisterListener = function (eventId, callbackToRemove) {
        if (_.has(eventRegistry, eventId)) {
            if (callbackToRemove !== undefined) {
                eventRegistry[eventId] = _.omit(eventRegistry[eventId], function (callback) {
                    return callback === callbackToRemove;
                });
            } else {
                delete eventRegistry[eventId];
            }
        }
    };

    /**
     * Returns a promise that will be resolved the next time the provided eventId has completed processing.
     */
    obj.whenComplete = function (eventId) {
        if (!_.has(subscriptions, eventId)) {
            subscriptions[eventId] = $.Deferred();
        }
        return subscriptions[eventId];
    };

    return obj;
});
