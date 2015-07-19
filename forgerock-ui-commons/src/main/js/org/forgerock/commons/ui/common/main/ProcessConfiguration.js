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

/**
 * @author yaromin
 */
define("org/forgerock/commons/ui/common/main/ProcessConfiguration", [
    "jquery",
    "underscore",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/main/AbstractConfigurationAware",
    "org/forgerock/commons/ui/common/util/ModuleLoader"
], function($, _, constants, eventManager, configuration, AbstractConfigurationAware, ModuleLoader) {

    var obj = new AbstractConfigurationAware();
    obj.em = eventManager;

    eventManager.registerListener(constants.EVENT_CONFIGURATION_CHANGED, function(event) {
        obj.callService(event.moduleClass, "updateConfigurationCallback", [event.configuration]);
    });

    eventManager.registerListener(constants.EVENT_DEPENDENCIES_LOADED, function(event) {
        obj.callService("org/forgerock/commons/ui/common/main/Configuration","sendConfigurationChangeInfo");
    });

    obj.callRegisterListenerFromConfig = function (config) {
        eventManager.registerListener(config.startEvent, function(event) {
            $.when.apply($, _.map(config.dependencies, function (dep) {
                return ModuleLoader.load(dep);
            })).then(function () {
                config.processDescription.apply(this, [event].concat(_.toArray(arguments)));
            });
        });
    };

    obj.updateConfigurationCallback = function(configuration) {
        var oneProcessDefinitionObject,
            processArray = [],
            overrideArray = [];

        AbstractConfigurationAware.prototype.updateConfigurationCallback
            .call(this, configuration)
            .then(function () {

                $.when.apply($, _.map(obj.configuration.processConfigurationFiles, ModuleLoader.load))
                .then(function () {

                    var // all processes
                        processArray = _.flatten(_.toArray(arguments)),
                        // processes which override the default of the same name
                        overrideArray = _.filter(processArray, function (process) {
                            return !!process.override;
                        });

                    // remove those processes which have been overridden
                    processArray = _.reject(processArray, function (process) {
                        return process.override || _.find(overrideArray, function (override) {
                            return override.startEvent === process.startEvent;
                        });
                    });

                    _.map(processArray, obj.callRegisterListenerFromConfig);

                    eventManager.sendEvent(constants.EVENT_READ_CONFIGURATION_REQUEST);
                });

            });
    };

    obj.callService = function(serviceId, methodName, params) {
        ModuleLoader.load(serviceId).then(
            function (service) {
                if(service) {
                    service[methodName].apply(service, params || []);
                }
            }, function (exception) {
                if(params) {
                    params = JSON.stringify(params);
                }
                console.warn("Unable to invoke serviceId=" + serviceId + " method=" + methodName + " params=" + params + " exception=" + exception);
            }
        );
    };

    return obj;
});
