/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2015 ForgeRock AS.
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

/*global define, window, require*/

define("org/forgerock/commons/ui/common/util/ModuleLoader", [
    "jquery",
    "underscore"
], function ($, _) {
    var aliasMap = {};

    return {
        setAlias: function (alias, target) {
            aliasMap[alias] = target;
            return this;
        },
        load: function (libPath) {
            /*
                 For some reason, the first time you try to require([...]) a module like this,
                 require throws an error. But if you do so again immediately afterward (like
                 is being done within the the catch block), it works. Subsequent calls to load
                 the same module should work the first time.
            */

            var promise = $.Deferred();

            if (_.has(aliasMap, libPath)) {
                libPath = aliasMap[libPath];
            }

            try {
                require([libPath], promise.resolve);
            } catch (e) {
                try {
                    require([libPath], promise.resolve);
                } catch (e2) {
                    promise.reject(e2);
                }
            }

            return promise;
        },

        /**
        * Facility to assist migration from callback-based functions to promise-based ones
        *
        * Many legacy functions exist which are of the style func(successCallback, errorCallback).
        * These functions do not return anything; instead they invoke the appropriate callback function
        * and pass it whatever result they eventually have to provide.
        *
        * This style of function is not particularly easy to work with in the context of a promise.
        * To make these functions easier to work with, this promiseWrapper function exists. It returns a
        * promise which is only resolved or rejected when the success or error callback (respectively) is called.
        *
        * If the functionToCall does actually return something (presumably a promise but not necessarily) then
        * whatever was returned will be used to resolve the promise returned from this function.
        *
        * The second argument to this function is "params" - it is an optional object which may contain
        * "success" and "error" callbacks. This is to continue to provide the legact callback support, if it is desired.
        *
        * Example:

            function login(credentials, successCallback, errorCallback) {
                // try logging in
                // if successful: successCallback(userDetails);
                // else : errorCallback(failureDetails);
            }

            var creds = { "userName": "foo", "password": "bar" };

            ModuleLoader.promiseWrapper(_.curry(login)(creds)).then(
                function (user) {
                    console.log("Successfully logged in with user", user);
                },
                function (failureDetails) {
                    console.log("Failed to login", failureDetails);
                }
            );

        * Note the use of _.curry above. This is necessary because the function that you may wish to invoke
        * requires other arguments in addition to the two callbacks (such as the credentials in that sample).
        */
        promiseWrapper: function (functionToCall, params) {
            var promise = $.Deferred(),
                handler = function (response) {
                    return response !== undefined ? response : promise;
                };

            // assumes the functionToCall has two remaining (non-curried) parameters to pass into it, the success and failure handlers
            return $.when(functionToCall(function () {
                if (params && params.success) {
                    params.success.apply(window, arguments);
                }
                promise.resolve.apply(promise, arguments);
            }, function () {
                if (params && params.error) {
                    params.error.apply(window, arguments);
                }
                promise.reject.apply(promise, arguments);
            })).then(handler, handler);
        }

    };
});
