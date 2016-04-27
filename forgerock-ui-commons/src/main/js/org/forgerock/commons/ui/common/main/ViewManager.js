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
 * Copyright 2012-2016 ForgeRock AS.
 */

define("org/forgerock/commons/ui/common/main/ViewManager", [
    "jquery",
    "underscore",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "org/forgerock/commons/ui/common/components/Messages",
    "org/forgerock/commons/ui/common/util/ModuleLoader"
], function($, _, uiUtils, msg, ModuleLoader) {
    var obj = {},
        decodeArgs = function (args) {
            return _.map(args, function (a) {
                return (a && decodeURIComponent(a)) || "";
            });
        };

    obj.currentView = null;
    obj.currentDialog = null;
    obj.currentViewArgs = null;
    obj.currentDialogArgs = null;

    /**
     * Initializes view if it is not equal to current view.
     * Changes URL without triggering event.
     */
    obj.changeView = function(viewPath, args, callback, forceUpdate) {
        var view,
            decodedArgs = decodeArgs(args);


        if(obj.currentView !== viewPath || forceUpdate || !_.isEqual(obj.currentViewArgs, args)) {
            if(obj.currentDialog !== null) {
                ModuleLoader.load(obj.currentDialog).then(function (dialog) {
                    dialog.close();
                });
            }

            //close all existing dialogs
            if (typeof $.prototype.modal === "function") {
                $('.modal.in').modal('hide');
            }

            obj.currentDialog = null;

            msg.messages.hideMessages();
            ModuleLoader.load(viewPath).then(function (view) {
                if(view.init) {
                    view.init();
                } else {
                    view.render(decodedArgs, callback);
                }
            });

        } else {
            ModuleLoader.load(obj.currentView).then(function (view) {
                view.rebind();

                if(callback) {
                    callback();
                }
            });
        }

        obj.currentViewArgs = args;
        obj.currentView = viewPath;
    };

    obj.showDialog = function(dialogPath, args, callback) {
        var decodedArgs = decodeArgs(args);

        if(obj.currentDialog !== dialogPath || !_.isEqual(obj.currentDialogArgs, decodedArgs)) {
            msg.messages.hideMessages();
            ModuleLoader.load(dialogPath).then(function (dialog) {
                dialog.render(decodedArgs, callback);
            });
        }

        if(obj.currentDialog !== null) {
            ModuleLoader.load(obj.currentDialog).then(function (dialog) {
                dialog.close();
            });
        }

        obj.currentDialog = dialogPath;
        obj.currentDialogArgs = decodedArgs;
    };

    obj.refresh = function() {
        var cDialog = obj.currentDialog, cDialogArgs = obj.currentDialogArgs;

        obj.changeView(obj.currentView, obj.currentViewArgs, function() {}, true);
        if (cDialog && cDialog !== null) {
            obj.showDialog(cDialog, cDialogArgs);
        }
    };

    return obj;

});
