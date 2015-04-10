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

/*global define, $, _, form2js*/
define("org/forgerock/mock/ui/user/profile/ChangeSecurityDataDialog", [
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/main/ValidatorsManager",
    "org/forgerock/commons/ui/common/main/Configuration",
    "UserDelegate",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/Constants",
    'org/forgerock/commons/ui/common/components/Messages',
    "bootstrap-dialog"
], function(AbstractView, ValidatorsManager, Configuration, UserDelegate, UIUtils, EventManager, Constants, Messages, BootstrapDialog) {
    var ChangeSecurityDataDialog = AbstractView.extend({
        template: "templates/mock/ChangeSecurityDataDialogTemplate.html",
        data: { },
        events: {
            "click input[type=submit]": "formSubmit",
            "onValidate": "onValidate",
            "customValidate": "customValidate"
        },
        errorsHandlers: {
            "Bad Request": { status: "400" }
        },
        show: function(callback) {

            var self = this,
                data = {},
                args = {
                    type: BootstrapDialog.TYPE_PRIMARY,
                    title: $.t("templates.user.ChangeSecurityDataDialogTemplate.securityDataChange"),
                    buttons: [{
                        id: "btnOk",
                        label: $.t("common.form.update"),
                        cssClass: "btn-primary",
                        disabled: true,
                        action: function(dialog) {

                            dialog.getButton("btnOk").text($.t("common.form.working")).prop('disabled', true);

                            var patchDefinitionObject = [];

                            if (ValidatorsManager.formValidated(dialog.$modalBody.find("#passwordChange"))) {

                                patchDefinitionObject.push({operation: "replace", field: "password", value: self.$el.find("input[name=password]").val()});

                                UserDelegate.patchSelectedUserAttributes(UserDelegate.getUserResourceName(Configuration.loggedUser), Configuration.loggedUser._rev, patchDefinitionObject, _.bind(function (r) {
                                    EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "securityDataChanged");
                                    var headers = {};
                                    headers[Constants.HEADER_PARAM_USERNAME] = Configuration.loggedUser.userName;
                                    headers[Constants.HEADER_PARAM_PASSWORD] = self.$el.find("input[name=password]").val();
                                    headers[Constants.HEADER_PARAM_NO_SESSION] = false;
                                    UserDelegate.getProfile(function (user) {
                                        Configuration.loggedUser = user;
                                    }, null, null, headers);
                                    dialog.close();
                                }, self));

                            }
                        }
                    }, {
                        label: $.t("common.form.cancel"),
                        action: function(dialog) {
                           dialog.close();
                        }
                    }],
                    onshown: function(dialog){
                        self.element = dialog.$modal;
                        self.rebind();
                        ValidatorsManager.bindValidators(dialog.$modal);
                        self.customValidate();
                        if (callback && callback.callback) {
                            callback.callback(self);
                        }
                    }
                };

            UIUtils.fillTemplateWithData(this.template, this.data, function(template) {
                args.message = function (dialog) {
                    return $('<div></div>').append(template);
                };

                BootstrapDialog.show(args);
            });

        },

        customValidate: function () {
            if (ValidatorsManager.formValidated(this.$el.find("#passwordChange"))) {
                this.$el.find("#btnOk").prop('disabled', false);
            } else {
                this.$el.find("#btnOk").prop('disabled', true);
            }
        }

    });

    return new ChangeSecurityDataDialog();
});
