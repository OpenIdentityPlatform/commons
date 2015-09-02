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

/*global window, define, $, _, document, console */

/**
 * @author mbilski
 */
define("org/forgerock/commons/ui/user/profile/UserProfileView", [
    "form2js",
    "js2form",
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/main/ValidatorsManager",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "UserDelegate",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/commons/ui/common/components/Navigation",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/components/ChangesPending"

], function(form2js, js2form, AbstractView, validatorsManager, uiUtils, userDelegate, router, navigation, eventManager, constants, conf, ChangesPending) {
    var UserProfileView = AbstractView.extend({
        template: "templates/user/UserProfileTemplate.html",
        baseTemplate: "templates/common/DefaultBaseTemplate.html",
        delegate: userDelegate,
        events: {
            "click #changeSecurity": "changeSecurity",
            "click input[name=saveButton]": "formSubmit",
            "click input[name=resetButton]": "reloadData",
            "onValidate": "onValidate",
            "change input": "checkChanges",
            "change select": "checkChanges"
        },

        data:{},

        changeSecurity: function() {
            eventManager.sendEvent(constants.EVENT_SHOW_CHANGE_SECURITY_DIALOG);
        },

        submit: function(){
            this.delegate.updateUser(conf.loggedUser, this.data, _.bind(function(newUserData) {
                this.changesPendingWidget.saveChanges();
                if (_.has(newUserData, "_rev")) {
                    this.data._rev = newUserData._rev;
                }
                $.extend(conf.loggedUser, this.data);
                eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "profileUpdateSuccessful");
            }, this ),
            _.bind(function(e) {
                console.log('errorCallback', e.responseText);
                eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "profileUpdateFailed");
                this.reloadData();
            }, this));
        },

        checkChanges: function(e) {
            this.changesPendingWidget.makeChanges({ loggedUser: this.getFormData() });
        },

        formSubmit: function(event) {

            event.preventDefault();
            event.stopPropagation();

            var _this = this,
                changedProtected = [];

            if (validatorsManager.formValidated(this.$el)) {

                this.data = this.getFormData();

                _.each(conf.globalData.protectedUserAttributes, function(attr){
                    if(_this.data[attr] && conf.loggedUser[attr] !== _this.data[attr]){
                        changedProtected.push(" "+_this.$el.find("label[for="+attr+"]").text());
                    }
                });

                if (changedProtected.length === 0) {
                    this.submit();
                } else {
                    this.data.changedProtected = changedProtected;
                    eventManager.sendEvent(constants.EVENT_SHOW_CONFIRM_PASSWORD_DIALOG, "ConfirmPasswordDialog");
                }

            }
        },

        getFormData: function() {
            var data = form2js(this.el, '.', false);

            // buttons will be included in this structure, so remove those.
            _.each(data, function (value, key, list) {
                if (this.$el.find("input[name=" + key + "]").hasClass('btn')) {
                    delete data[key];
                }
            }, this);

            return data;
        },

        render: function(args, callback) {
            this.parentRender(function() {
                validatorsManager.bindValidators( this.$el, this.delegate.getUserResourceName(conf.loggedUser), _.bind(function () {
                    this.reloadData();
                    this.changesPendingWidget = ChangesPending.watchChanges({
                        element: this.$el.find(".user-profile-changes-pending"),
                        watchedObj: { loggedUser: this.getFormData() },
                        watchedProperties: ["loggedUser"]
                    });
                    if (callback) {
                        callback();
                    }
                }, this ));
            });
        },

        reloadData: function() {
            js2form(this.$el.find("#userProfileForm")[0], conf.loggedUser);
            this.$el.find("input[name=saveButton]").val($.t("common.form.update"));
            this.$el.find("input[name=resetButton]").val($.t("common.form.reset"));
            validatorsManager.validateAllFields(this.$el);
            if (this.changesPendingWidget) {
                this.changesPendingWidget.makeChanges({ loggedUser: this.getFormData() });
            }
        }
    });

    return new UserProfileView();
});
