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

define("org/forgerock/commons/ui/user/profile/UserProfileKBAView", [
    "jquery",
    "lodash",
    "form2js",
    "js2form",
    "handlebars",
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/user/delegates/KBADelegate",
    "org/forgerock/commons/ui/user/profile/UserProfileView",
    "org/forgerock/commons/ui/common/main/ValidatorsManager"
], function($, _, form2js, js2form, Handlebars,
    AbstractView,
    KBADelegate,
    UserProfileView,
    ValidatorsManager) {

    var UserProfileKBAView = AbstractView.extend({
        events: _.extend({
            "change .kba-pair :input": "checkKBAChanges",
            "click #provideAnother": "addKBAQuestion",
            "click .delete-KBA-question": "deleteKBAQuestion"
        }, UserProfileView.events),
        partials: UserProfileView.partials.concat([
            "partials/profile/_kbaTab.html",
            "partials/profile/_kbaItem.html"
        ]),
        addKBAQuestion: function (e) {
            e.preventDefault();
            var kbaItems = this.$el.find("#kbaItems"),
                newIndex = kbaItems.find(">li").length;
            kbaItems.append(
                $("<li>").html(Handlebars.compile("{{> profile/_kbaItem}}")({
                    questions: this.data.predefinedQuestions,
                    index: newIndex,
                    isNew: true
                }))
            );
            // below event trigger will result in checkKBAChanges to run for this new question
            this.$el.find(".kba-pair[index="+newIndex+"] :input:first").trigger("change");
        },
        deleteKBAQuestion: function (e) {
            var target = $(e.target),
                form = target.closest("form"),
                kbaPair = target.closest(".kba-pair");

            e.preventDefault();
            if (kbaPair.attr("isNew") === "true") {
                kbaPair.parent("li").remove();
            } else {
                kbaPair.hide();
            }
            ValidatorsManager.validateAllFields(form);
            this.changesPendingWidgets[form.attr("id")].makeChanges({ subform: this.getFormContent(form[0]) });
        },
        checkKBAChanges: function (e) {
            var target = $(e.target),
                attributeName = _.keys(form2js(e.target))[0],
                kbaPair = target.closest(".kba-pair"),
                form = target.closest("form"),
                currentKbaInfo = this.changesPendingWidgets[form.attr("id")].data.watchedObj.subform[attributeName],
                predefinedQuestion = kbaPair.find(".kba-questions"),
                customQuestionContainer = kbaPair.find(".custom-question"),
                answer = kbaPair.find(".answer :input"),
                answerRequired = false;

            customQuestionContainer.toggleClass("hidden", predefinedQuestion.val() !== "custom");

            // below conditions check to see if a new KBA answer needs to be provided, or whether it can stay unchanged
            if (currentKbaInfo && currentKbaInfo[kbaPair.attr('index')]) {
                if (predefinedQuestion.val() === "custom") {
                    answerRequired = (currentKbaInfo[kbaPair.attr('index')].customQuestion !== customQuestionContainer.find(":input").val());
                } else {
                    answerRequired = (currentKbaInfo[kbaPair.attr('index')].questionId !== predefinedQuestion.val());
                }
            } else {
                answerRequired = true;
            }

            if (answerRequired) {
                answer.attr("data-validator", "required");
                answer.attr("placeholder", "");
            }

            ValidatorsManager.bindValidators(form);
            ValidatorsManager.validateAllFields(form);
        },
        submit: function (formId, formData) {
            if (formId === "KBA") {
                KBADelegate.saveKBAInfo(formData).then(
                    _.bind(function () {
                        this.submitSuccess(formId);
                    }, this),
                    _.bind(this.submitFailure, this)
                );
            } else {
                UserProfileView.submit.call(this, formId, formData);
            }
        },
        getFormContent: function (form) {
            if (form.id === "KBA") {
                var formContent = form2js(form, ".", false);
                // cannot rely upon a particular named field in the form content,
                // so apply the logic to all fields found in the form
                return _(formContent)
                        .map(function (value, key) {
                            if (_.isArray(value)) {
                                return [
                                    key,
                                    _(value)
                                        .map(function (kbaPair, index) {
                                            var newPair = {};

                                            // deleted pairs will be hidden
                                            if ($(form).is(":visible") && !$(form).find(".kba-pair[index="+index+"]:visible").length) {
                                                // express their removal via an explicit undefined value in that position
                                                return undefined;
                                            }

                                            if (kbaPair.answer && kbaPair.answer.length) {
                                                newPair.answer = kbaPair.answer;
                                            } else if (_.isObject(this.data.user[key][index])) {
                                                newPair.answer = this.data.user[key][index].answer;
                                            }

                                            if (kbaPair.questionId === "custom") {
                                                newPair.customQuestion = kbaPair.customQuestion;
                                            } else {
                                                newPair.questionId = kbaPair.questionId;
                                            }
                                            return newPair;
                                        }, this)
                                        .compact()
                                        .value()
                                ];
                            } else {
                                return [ key, value ];
                            }
                        }, this)
                        .object()
                        .value();
            } else {
                return UserProfileView.getFormContent.call(this, form);
            }
        },
        render: function(args, callback) {
            KBADelegate.getQuestions()
                .then(_.bind(function (questions) {
                    this.data.predefinedQuestions = questions;
                    UserProfileView.render.call(this, args, callback);
                }, this));
        },

        /**
         * Augments the default template used for UserProfileForm with KBA tabs, before returning
         * control to UserProfileView.
         */
        parentRender: function (callback) {
            UserProfileView.parentRender.call(this, function () {
                this.$el.find(".tab-content").append(Handlebars.compile("{{> profile/_kbaTab}}")(this.data));
                this.$el.find(".nav-tabs").append(
                    $('<li>').append(
                        $('<a href="#userKBATab" role="tab" data-toggle="tab">')
                            .text($.t("common.user.kba.securityQuestions"))
                    )
                );
                _.bind(callback, this)();
            });
        },
        reloadFormData: function (form) {
            var newContent;

            if (form.id === "KBA") {
                form = this.$el.find(".tab-content #userKBATab form")[0];
                newContent = Handlebars.compile("{{> profile/_kbaTab}}")(this.data);
                $("#kbaItems", form).empty().append($(newContent).find("#kbaItems"));
                js2form(form,
                    // use the form structure to find out which fields are defined for the kba form...
                    _(form2js(form, ".", false))
                     .map(function (value, key) {
                         // omit the "answer" property from any array found there...
                         if (_.isArray(this.data.user[key])) {
                             return [
                                 key,
                                 _.map(this.data.user[key], function (kbaPair) {
                                     return _.omit(kbaPair, "answer");
                                 })
                             ];
                         } else {
                             return [ key, this.data.user[key] ];
                         }
                     }, this)
                     .object()
                     .value()
                );

                _.each($(".kba-questions", form), function (kbaSelect) {
                    var customQuestionContainer = $(kbaSelect).closest(".kba-pair").find(".custom-question"),
                        customQuestionValue = customQuestionContainer.find(":input").val();
                    if (customQuestionValue !== "") {
                        $(kbaSelect).val("custom");
                        customQuestionContainer.toggleClass("hidden", false);
                    } else {
                        customQuestionContainer.toggleClass("hidden", true);
                    }

                });
            } else {
                UserProfileView.reloadFormData.call(this, form);
            }
        }
    });

     UserProfileKBAView.prototype = _.extend(Object.create(UserProfileView), UserProfileKBAView.prototype);

    return new UserProfileKBAView();
});
