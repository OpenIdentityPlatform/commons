/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2015 ForgeRock AS. All rights reserved.
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

/*global define, $, _*/

define("org/forgerock/commons/ui/user/profile/ConfirmPasswordDialog", [
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/main/ValidatorsManager",
    "org/forgerock/commons/ui/user/profile/UserProfileView",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "bootstrap-dialog"
], function(AbstractView, ValidatorsManager, UserProfileView, UIUtils, BootstrapDialog) {
    var ConfirmPasswordDialog = AbstractView.extend({
        template: "templates/user/ConfirmPasswordDialogTemplate.html",
        events: {
            "onValidate": "onValidate",
            "customValidate": "customValidate"
        },
        errorsHandlers: {
            "Bad Request": { status: "400" }
        },
        customValidate: function () {
            if(ValidatorsManager.formValidated(this.$el.find("#confirmPasswordForm"))) {
                this.$el.find("#btnUpdate").prop('disabled', false);
            }
            else {
                this.$el.find("#btnUpdate").prop('disabled', true);
            }
        },
        show: function() {
            var self = this,
                dialog;

            this.data.changedProtected = UserProfileView.data.changedProtected;
            dialog = new BootstrapDialog({
                type: BootstrapDialog.TYPE_PRIMARY,
                title: $.t("common.user.confirmPassword"),
                buttons: [{
                    id: "btnUpdate",
                    label: $.t("common.form.update"),
                    cssClass: "btn-primary",
                    disabled: true,
                    action: function(dialog) {
                        UserProfileView.data.currentpassword = dialog.$modal.find("#currentPassword").val();
                        UserProfileView.submit();
                        dialog.close();
                    }
                }],
                message: $(UIUtils.fillTemplateWithData(self.template, self.data)),
                onshown: function(dialog){
                    self.element = dialog.$modal;
                    self.rebind();
                    ValidatorsManager.bindValidators(dialog.$modal);
                    self.customValidate();
                }
            });
            dialog.open();
        }
    });

    return new ConfirmPasswordDialog();
});