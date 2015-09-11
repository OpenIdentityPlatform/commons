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

/*global define, document */

define("org/forgerock/commons/ui/user/profile/UserProfileView", [
    "jquery",
    "underscore",
    "form2js",
    "js2form",
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/components/ChangesPending",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/user/profile/ConfirmPasswordDialog",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/commons/ui/common/main/ValidatorsManager",
    "bootstrap"
], function($, _, form2js, js2form,
    AbstractView,
    ChangesPending,
    Configuration,
    ConfirmPasswordDialog,
    Constants,
    EventManager,
    Router,
    ValidatorsManager) {

    var UserProfileView = AbstractView.extend({
        template: "templates/user/UserProfileTemplate.html",
        partials: [
            "partials/form/_basicInput.html",
            "partials/form/_basicSaveReset.html"
        ],
        events: {
            "click input[type=submit]": "formSubmit",
            "click input[type=reset]": "resetForm",
            "click a[role=tab]": "updateRoute",
            "shown.bs.tab": "focusInput",
            "onValidate": "onValidate",
            "change :input": "checkChanges"
        },

        focusInput: function (e) {
            $($(e.target).attr("href")).find(":input:not([readonly]):first").focus();
        },

        updateRoute: function (e) {
            var tabPane = $($(e.target).attr("href")),
                form = tabPane.find("form"),
                tabRoute = form.attr("id");

            EventManager.sendEvent(Constants.ROUTE_REQUEST, {routeName: "profile", args: [tabRoute], trigger: false});
        },

        submit: function(formId, formData) {
            Configuration.loggedUser.save(formData, {patch: true}).then(
                _.bind(function() {
                    this.changesPendingWidgets[formId].saveChanges();
                    this.data.user = Configuration.loggedUser.toJSON();
                    this.reloadFormData(document.getElementById(formId));
                    EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "profileUpdateSuccessful");
                }, this),
                _.bind(function(e) {
                    EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "profileUpdateFailed");
                }, this)
            );
        },

        checkChanges: function(e) {
            var form = $(e.target).closest("form");
            this.changesPendingWidgets[form.attr("id")].makeChanges({ subform: form2js(form[0], ".", false) });
        },

        formSubmit: function(event) {

            event.preventDefault();
            event.stopPropagation();

            var changedProtected = [],
                form = $(event.target).closest("form"),
                formData = form2js(form[0], ".", false);

            if (ValidatorsManager.formValidated(form)) {

                changedProtected = _.chain(Configuration.loggedUser.getProtectedAttributes())
                    .filter(function(attr) {
                        return _.has(formData, attr) && Configuration.loggedUser.get(attr) !== formData[attr];
                    }, this)
                    .map(function (attr) {
                        return this.$el.find("label[for=input-"+attr+"]").text();
                    }, this)
                    .value();

                if (changedProtected.length === 0) {
                    this.submit(form.attr("id"), formData);
                } else {
                    ConfirmPasswordDialog.render(changedProtected, _.bind(function (currentPassword) {
                        Configuration.loggedUser.setCurrentPassword(currentPassword);
                        this.submit(form.attr("id"), formData);
                    }, this));
                }

            }
        },

        render: function(args, callback) {
            var tabName = args[0] || "details";

            this.data.user = Configuration.loggedUser.toJSON();

            this.parentRender(function() {
                var selectedTabId = this.$el.find('form#'+tabName).closest(".tab-pane").attr("id"),
                    selectedTab = this.$el.find("ul.nav-tabs a[href='#"+selectedTabId+"']");

                this.loadAllFormData();

                selectedTab.tab('show');

                this.changesPendingWidgets = {};

                _.each(this.$el.find("form"), function (form) {

                    ValidatorsManager.bindValidators($(form));
                    ValidatorsManager.validateAllFields($(form));

                    this.changesPendingWidgets[$(form).attr('id')] = ChangesPending.watchChanges({
                        element: $(".changes-pending", form),
                        watchedObj: { subform: form2js(form, ".", false) },
                        watchedProperties: ["subform"],
                        alertClass: "alert-warning alert-sm"
                    });
                }, this);

                this.$el.find("#" + selectedTabId).find(":input:not([readonly]):first").focus();

                if (callback) {
                    callback();
                }
            });
        },

        reloadFormData: function (form) {
            $("input[type=password]", form).val("");
            js2form(form, this.data.user);
        },
        resetForm: function (e) {
            e.preventDefault();
            var form = this.$el.find(e.target).closest("form");
            this.reloadFormData(form[0]);
            this.checkChanges(e);
        },
        loadAllFormData: function() {
            _.each(this.$el.find("form"), this.reloadFormData, this);
        }
    });

    return new UserProfileView();
});
