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
 * Copyright 2011-2016 ForgeRock AS.
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

        focusInput: function (event) {
            $($(event.target).attr("href")).find(":input:not([readonly]):first").focus();
        },

        updateRoute: function (event) {
            var tabPane = $($(event.target).attr("href")),
                form = tabPane.find("form"),
                tabRoute = form.attr("id");

            EventManager.sendEvent(Constants.ROUTE_REQUEST, {routeName: "profile", args: [tabRoute], trigger: false});
        },

        submit: function (formId, formData) {
            Configuration.loggedUser.save(formData, {patch: true}).then(
                _.bind(function () {
                    this.submitSuccess(formId);
                }, this)
            );
        },

        submitSuccess: function (formId) {
            var form = document.getElementById(formId);

            this.changesPendingWidgets[formId].saveChanges();
            this.data.user = Configuration.loggedUser.toJSON();
            this.reloadFormData(form);
            this.changesPendingWidgets[formId] = ChangesPending.watchChanges({
                element: $(".changes-pending", form),
                watchedObj: { subform: this.getFormContent(form) },
                watchedProperties: ["subform"],
                alertClass: "alert-warning alert-sm"
            });
            EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "profileUpdateSuccessful");
        },

        checkChanges: function (event) {
            var form = $(event.target).closest("form");

            ValidatorsManager.bindValidators($(form), Configuration.loggedUser.baseEntity, function () {
                ValidatorsManager.validateAllFields($(form));
            });

            this.changesPendingWidgets[form.attr("id")].makeChanges({ subform: this.getFormContent(form[0]) });
            $(form).find("input[type='reset']").prop("disabled", false);
        },

        getFormContent: function (form) {
            return form2js(form, ".", false);
        },

        formSubmit: function (event) {

            event.preventDefault();
            event.stopPropagation();

            var changedProtected = [],
                form = $(event.target).closest("form"),
                formData = this.getFormContent(form[0]);

            if (ValidatorsManager.formValidated(form)) {

                changedProtected = _.chain(Configuration.loggedUser.getProtectedAttributes())
                    .filter(function(attr) {
                        if (_.has(formData, attr)) {
                            if (_.isEmpty(Configuration.loggedUser.get(attr)) && _.isEmpty(formData[attr])) {
                                return false;
                            } else {
                                return !_.isEqual(Configuration.loggedUser.get(attr),formData[attr]);
                            }
                        } else {
                            return false;
                        }
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

        render: function (args, callback) {
            var tabName = args[0] || "details";

            this.data.user = Configuration.loggedUser.toJSON();

            this.parentRender(function () {
                var selectedTabId = this.$el.find('form#'+tabName).closest(".tab-pane").attr("id"),
                    selectedTab = this.$el.find("ul.nav-tabs a[href='#"+selectedTabId+"']");

                _.each(this.$el.find("form"), this.reloadFormData, this);

                selectedTab.tab('show');

                this.changesPendingWidgets = {};

                _.each(this.$el.find("form"), function (form) {

                    if ($(form).attr("data-form-validate")) {
                        ValidatorsManager.bindValidators($(form), Configuration.loggedUser.baseEntity);
                    }

                    this.changesPendingWidgets[$(form).attr('id')] = ChangesPending.watchChanges({
                        element: $(".changes-pending", form),
                        watchedObj: { subform: this.getFormContent(form) },
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
            js2form(form, this.data.user);
            $("input[type=password]", form).val("").attr("placeholder", $.t("common.form.passwordPlaceholder"));

            _.each($(form).find("div[data-form-group]"), function (group) {
                $(group).removeClass("has-error");
                $(group).find("input").off().popover('destroy');
            }, this);

            $(form).find("input[type='reset']").prop("disabled", true);
            $(form).find("input[type='submit']").prop("disabled", true);
        },

        resetForm: function (event) {
            event.preventDefault();
            var form = this.$el.find(event.target).closest("form");

            this.reloadFormData(form[0]);
        }
    });

    return new UserProfileView();
});

