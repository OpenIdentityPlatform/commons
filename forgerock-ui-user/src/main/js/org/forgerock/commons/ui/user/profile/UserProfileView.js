/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2014 ForgeRock AS. All rights reserved.
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

/*global window, define, $, form2js, _, js2form, document, console */

/**
 * @author mbilski
 */
define("org/forgerock/commons/ui/user/profile/UserProfileView", [
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/main/ValidatorsManager",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "UserDelegate",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/commons/ui/common/components/Navigation",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/Configuration"
], function(AbstractView, validatorsManager, uiUtils, userDelegate, router, navigation, eventManager, constants, conf) {
    var UserProfileView = AbstractView.extend({
        template: "templates/user/UserProfileTemplate.html",
        baseTemplate: "templates/common/DefaultBaseTemplate.html",
        delegate: userDelegate,
        events: {
            "click input[name=saveButton]": "formSubmit",
            "click input[name=resetButton]": "reloadData",
            "onValidate": "onValidate"
        },

        data:{},

        submit: function(){
            var _this = this;
            this.delegate.updateUser(conf.loggedUser, this.data, _.bind(function(newUserData) {
                if (_.has(newUserData, "_rev")) {
                    _this.data._rev = newUserData._rev;
                }
                $.extend(conf.loggedUser, _this.data);
                eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "profileUpdateSuccessful");
            }, this ),
            function(e){
                console.log('errorCallback', e.responseText);
                eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "profileUpdateFailed");
                _this.reloadData();
            });
        },

        formSubmit: function(event) {

            event.preventDefault();
            event.stopPropagation();

            var _this = this;

            if (validatorsManager.formValidated(this.$el)) {

                this.data = form2js(this.el, '.', false);
                this.data.changedProtected = [];
                _.each(conf.globalData.protectedUserAttributes, function(attr){
                    if(conf.loggedUser[attr] !== _this.data[attr]){
                        _this.data.changedProtected.push(" "+_this.$el.find("label[for="+attr+"]").text()); 
                    }
                });

                if (this.data.changedProtected.length === 0) {
                    this.submit();
                } else {
                    location.hash = router.configuration.routes.confirmPassword.url;
                }

            } else {
                console.log('invalid form');
            }
        },

        render: function(args, callback) {

            this.parentRender(function() {
                validatorsManager.bindValidators( this.$el, this.delegate.getUserResourceName(conf.loggedUser), _.bind(function () {
                    this.reloadData();
                    if(callback) {
                        callback();
                    }
                }, this ));
            });
        },

        reloadData: function() {
            js2form(this.$el.find("#UserProfileForm")[0], conf.loggedUser);
            this.$el.find("input[name=saveButton]").val($.t("common.form.update"));
            this.$el.find("input[name=resetButton]").val($.t("common.form.reset"));
            validatorsManager.validateAllFields(this.$el);
        }
    });

    return new UserProfileView();
});
