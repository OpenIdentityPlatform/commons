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

/*global define, $, _, form2js */

/**
 * @author mbilski
 */
define("org/forgerock/commons/ui/user/profile/ConfirmPasswordDialog", [
    "org/forgerock/commons/ui/common/components/Dialog",
    "org/forgerock/commons/ui/common/main/ValidatorsManager",
    "org/forgerock/commons/ui/common/main/Configuration",
    "UserDelegate",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/user/profile/UserProfileView"
], function(Dialog, validatorsManager, conf, userDelegate, eventManager, constants, userProfileView) {
    var ConfirmPasswordDialog = Dialog.extend({    
        contentTemplate: "templates/common/ConfirmPasswordDialogTemplate.html",
        events: {
            "click .dialogActions input[type=submit]": "formSubmit",
            "onValidate": "onValidate",
            "customValidate": "customValidate",
            "click .dialogCloseCross img": "close",
            "click input[name='close']": "close",
            "click .dialogContainer": "stop"
        },
        data: {},
        formSubmit: function(event) {
            userProfileView.data.currentpassword = this.$el.find("#currentPassword").val();
            userProfileView.submit();
            this.close();
                   
        },
        customValidate: function () {
            if(validatorsManager.formValidated(this.$el.find("#confirmPasswordForm"))) {
                this.$el.find("input[type=submit]").prop('disabled', false); 
            }
            else {
                this.$el.find("input[type=submit]").prop('disabled', true);
            } 
        },
        render: function() {
            var _this = this;
            this.actions = [];
            this.addAction($.t("common.form.update"), "submit");
            this.data.changedProtected = userProfileView.data.changedProtected;
  
            $("#dialogs").hide();
            this.show(_.bind(function() {
                validatorsManager.bindValidators(this.$el);
                $("#dialogs").show();
                this.$el.find("input[type=submit]").prop('disabled', true); 
            }, this)); 
        }
  
    }); 
    
    return new ConfirmPasswordDialog();
});