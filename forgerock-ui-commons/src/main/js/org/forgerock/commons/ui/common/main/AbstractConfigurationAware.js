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

/*global define*/

/**
 * @author yaromin
 */
define("org/forgerock/commons/ui/common/main/AbstractConfigurationAware", [
    "jquery",
    "underscore",
    "org/forgerock/commons/ui/common/util/ModuleLoader"
], function($, _, ModuleLoader) {

    var obj = function AbstractConfigurationAware() {
    };

    obj.prototype.updateConfigurationCallback = function(configuration) {
        this.configuration = configuration;

        /*
            Configuration entries may have a "loader" defined, like so:
            loader: [
                {"messages": "config/messages/CommonMessages"},
                {"messages": "config/messages/UserMessages"}
            ]
            Every key found within each map in the array will be used to populate
            an item of the same name within this.configuration. For example, using the above
            you would expect this.configuration.messages to contain the merged values from
            the objects returned from "config/messages/CommonMessages" and
            "config/messages/UserMessages".

            It should be noted that these configuration items are loaded asynchronously,
            and as such this function returns a promise that is only resolved when they are
            all available.
        */
        return $.when.apply($, _.map(configuration.loader, function (mapToLoad) {
            return $.when.apply($, _.map(_.pairs(mapToLoad), function (loadPair) {
                return ModuleLoader.load(loadPair[1]).then(function (loaded) {
                    return _.extend(configuration[loadPair[0]], loaded);
                });
            }));
        }));

    };

    return obj;
});
