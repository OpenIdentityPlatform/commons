/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2014 ForgeRock AS. All Rights Reserved
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

/*global define, $, _ */

/**
 * @author mbilski
 * @author Eugenia Sergueeva
 */
define("org/forgerock/mock/ui/user/profile/ChangeSecurityDataDialog", [
    "org/forgerock/commons/ui/common/components/Dialog",
    "org/forgerock/commons/ui/common/main/ValidatorsManager",
    "org/forgerock/commons/ui/common/main/Configuration",
    "UserDelegate",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/Constants"
], function (Dialog, validatorsManager, conf, userDelegate, uiUtils, eventManager, constants) {
    var ChangeSecurityDataDialog = Dialog.extend({
        contentTemplate: "templates/mock/ChangeSecurityDataDialogTemplate.html",

        events: {
            "click input[type=submit]": "formSubmit",
            "onValidate": "onValidate",
            "customValidate": "customValidate",
            "click .dialogCloseCross img": "close",
            "click input[name='close']": "close",
            "click .dialogContainer": "stop",
            "check_reauth": "reauth"
        },
        reauth: function (event, propertyName) {
            // we only need to force re-authentication if the properties needing it are one of the two we are prepared to change
            if (propertyName === "password") {
                if (!conf.hasOwnProperty('passwords')) {
                    this.reauth_required = true;
                    eventManager.sendEvent(constants.ROUTE_REQUEST, {routeName: "enterOldPassword"});
                } else {
                    this.reauth_required = false;
                }
            }
        },

        formSubmit: function (event) {
            event.preventDefault();

            var patchDefinitionObject = [], element;

            if (validatorsManager.formValidated(this.$el.find("#passwordChange"))) {
                patchDefinitionObject.push({operation: "replace", field: "password", value: this.$el.find("input[name=password]").val()});
            }

            userDelegate.patchSelectedUserAttributes(conf.loggedUser._id, conf.loggedUser._rev, patchDefinitionObject, _.bind(function (r) {
                eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "securityDataChanged");
                delete conf.passwords;
                this.close();

                userDelegate.getProfile(function (user) {
                    conf.loggedUser = user;
                });

            }, this));
        },
        customValidate: function () {

            if (validatorsManager.formValidated(this.$el.find("#passwordChange"))) {
                this.$el.find("input[type=submit]").prop('disabled', false);
            }
            else {
                this.$el.find("input[type=submit]").prop('disabled', true);
            }

        },
        render: function () {
            this.actions = [];
            this.addAction($.t("common.form.update"), "submit");

            $("#dialogs").hide();

            this.show(_.bind(function () {
                validatorsManager.bindValidators(this.$el, userDelegate.serviceUrl + "/test", _.bind(function () {
                    $("#dialogs").show();
                    if (!this.reauth_required) {
                        this.reloadData();
                    }
                }, this));

            }, this));

            this.$el.find("input[type=submit]").prop('disabled', true);
        },

        reloadData: function () {
            var user = conf.loggedUser, self = this;
            this.$el.find("input[name=_id]").val(conf.loggedUser._id);
        }
    });

    return new ChangeSecurityDataDialog();
});