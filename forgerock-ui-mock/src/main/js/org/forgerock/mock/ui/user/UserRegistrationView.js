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

/*global define, form2js */

/**
 * @author mbilski
 * @author Eugenia Sergueeva
 */
define("org/forgerock/mock/ui/user/UserRegistrationView", [
    "jquery",
    "underscore",
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/main/ValidatorsManager",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "UserDelegate",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/Configuration"
], function ($, _, AbstractView, validatorsManager, uiUtils, userDelegate, eventManager, constants, conf) {
    var UserRegistrationView = AbstractView.extend({
        template: "templates/mock/UserRegistrationTemplate.html",
        baseTemplate: "templates/common/MediumBaseTemplate.html",
        events: {
            "click input[type=submit]": "formSubmit",
            "onValidate": "onValidate",
            "click #frgtPasswrdSelfReg": "showForgottenPassword"
        },
        data : {
            showTermsOfUse: true
        },
        showForgottenPassword: function (event) {
            event.preventDefault();
            conf.forgottenPasswordUserName = this.$el.find("input[name=email]").val();
            eventManager.sendEvent(constants.ROUTE_REQUEST, {routeName: "forgottenPassword"});
        },

        formSubmit: function (event) {
            event.preventDefault();

            if (validatorsManager.formValidated(this.$el) && !this.isFormLocked()) {
                this.lock();

                var data = form2js(this.el),
                    self = this;

                delete data.terms;
                delete data.passwordConfirm;

                console.log("ADDING USER: " + JSON.stringify(data));

                $.extend(data, {
                    _id: data.userName,
                    _rev: '1',
                    uid: data.userName,
                    component: 'internal/user',
                    roles: ['ui-user']
                });

                userDelegate.create(userDelegate.getUserResourceName(data), data, function () {
                    eventManager.sendEvent(constants.EVENT_USER_SUCCESSFULLY_REGISTERED, { user: data, autoLogin: true });
                }, function () {
                    eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "userAlreadyExists");
                    self.unlock();
                });
            }
        },

        render: function (args, callback) {
            conf.setProperty("gotoURL", null);

            this.data.baseParams = args;

            this.parentRender(function () {
                this.$el.find("input").placeholder();

                validatorsManager.bindValidators(this.$el, userDelegate.serviceUrl + "/*", _.bind(function () {
                    validatorsManager.validateAllFields(this.$el);
                    this.unlock();
                    if (callback) {
                        callback();
                    }
                }, this));
            });
        }
    });

    return new UserRegistrationView();
});