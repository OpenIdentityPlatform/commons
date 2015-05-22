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

/*global define*/

define("org/forgerock/commons/ui/common/LoginDialog", [
    "jquery",
    "underscore",
    "org/forgerock/commons/ui/common/components/BootstrapDialogView",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/main/SessionManager",
    "org/forgerock/commons/ui/common/main/ValidatorsManager",
    "org/forgerock/commons/ui/common/main/ViewManager"
], function( $, _, BootstrapDialogView, Configuration, Constants, EventManager, SessionManager, ValidatorsManager, ViewManager) {
    var LoginDialog = BootstrapDialogView.extend({
        contentTemplate: "templates/common/LoginDialog.html",
        displayed: false,
        title: function(){ return $.t("common.form.sessionExpired");},
        closable: false,
        submitForm: function(event) {
            if(event.which === 13) {
                this.login(event);
            }
        },
        actions: [{
            label: function(){ return $.t("common.user.login");},
            cssClass: "btn-primary",
            action: function(dialog) {
                var userName,
                    password,
                    refreshOnLogin,
                    self = this;

                userName = dialog.$modalBody.find("input[name=login]").val();
                password = dialog.$modalBody.find("input[name=password]").val();
                refreshOnLogin = dialog.$modalBody.find("input[name=refreshOnLogin]:checked").val();

                SessionManager.login({"userName":userName, "password":password}, function(user) {
                    Configuration.setProperty('loggedUser', user);
                    EventManager.sendEvent(Constants.EVENT_AUTHENTICATION_DATA_CHANGED, { anonymousMode: false});
                    EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "loggedIn");
                    dialog.close();
                    if (refreshOnLogin) {
                        ViewManager.refresh();
                    }
                }, function() {
                    EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "authenticationFailed");
                });
            }
        }],

        render: function () {
            var self = this;
            if(this.displayed === false) {
                this.displayed = true;
                this.show(function(){
                    ValidatorsManager.bindValidators(self.$el);
                    if (Configuration.loggedUser && Configuration.loggedUser.userName) {
                        self.$el.find("input[name=login]").val(Configuration.loggedUser.userName).trigger("keyup");
                        self.$el.find("input[name=password]").focus();
                    } else {
                        self.$el.find("input[name=login]").focus();
                    }
                });
            }
        }
    });

    return new LoginDialog();
});