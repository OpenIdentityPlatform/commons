/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2015 ForgeRock AS.
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

/*global define */

define("org/forgerock/commons/ui/user/anonymousProcess/SelfRegistrationView", [
    "jquery",
    "lodash",
    "form2js",
    "handlebars",
    "org/forgerock/commons/ui/user/anonymousProcess/AnonymousProcessView",
    "org/forgerock/commons/ui/common/main/ValidatorsManager"
], function($, _, form2js, Handlebars, AnonymousProcessView, ValidatorsManager) {

    var SelfRegistrationView = AnonymousProcessView.extend({
        partials: [
            "partials/process/_kbaItem.html"
        ],
        events: _.extend({
            "click #kbaStage #provideAnother": "addKBAQuestion",
            "click .delete-KBA-question": "deleteKBAQuestion",
            "change #kbaStage .kba-questions": "toggleCustomQuestion"
        }, AnonymousProcessView.prototype.events),
        processType: "registration",
        i18nBase: "common.user.selfRegistration",
        renderQuestion: function () {
            var nextIndex = this.$el.find("#kbaItems li").length,
                newQuestion = $("<li>").html(
                Handlebars.compile("{{> process/_kbaItem}}")({
                    index: nextIndex,
                    questions: this.stateData.requirements.properties.kba.questions
                })
            );
            this.$el.find("#kbaItems").append(newQuestion);
        },
        validateForm: function () {
            ValidatorsManager.bindValidators(this.$el, this.baseEntity, _.bind(function () {
                ValidatorsManager.validateAllFields(this.$el);
            }, this));
        },
        toggleMissingQuestionsAlert: function (shown) {
            this.$el.find("#missingKBAQuestions").toggle(shown);
        },
        toggleSaveButtonDisabledProperty: function (disabled) {
            this.$el.find("input[type=submit]").prop("disabled", disabled);
        },
        isNumberOfQuestionsInsufficient: function () {
            return this.stateData.requirements.properties.kba.minItems > this.$el.find("#kbaItems li").length;
        },
        checkQuestionsNumberSufficiency: function () {
            var numberOfQuestionsInsufficient = this.isNumberOfQuestionsInsufficient();

            this.toggleMissingQuestionsAlert(numberOfQuestionsInsufficient);
            this.toggleSaveButtonDisabledProperty(numberOfQuestionsInsufficient);

            if (!numberOfQuestionsInsufficient) {
                this.validateForm();
            }
        },
        addKBAQuestion: function (e) {
            if (e) { e.preventDefault(); }

            this.renderQuestion();
            this.checkQuestionsNumberSufficiency();
        },
        deleteKBAQuestion: function (e) {
            e.preventDefault();

            $(e.target).closest("li").remove();
            this.checkQuestionsNumberSufficiency();
        },
        toggleCustomQuestion: function (e) {
            var questionValue = $(e.target).val(),
                customQuestion = $(e.target).closest(".kbaSet").find(".custom-question");
            if (questionValue === "custom") {
                customQuestion.toggleClass("hidden", false);
            } else {
                customQuestion.toggleClass("hidden", true).find(":input").val("");
            }
            ValidatorsManager.validateAllFields(this.$el);
        },
        getFormContent: function () {
            var $form = $(this.element).find("form");
            if ($form.attr("id") === "kbaStage") {
                return {
                    "kba": _.map(form2js($form[0]).kba, function (kbaSet) {
                        if (kbaSet.questionId === "custom") {
                            delete kbaSet.questionId;
                        }
                        return kbaSet;
                    })
                };
            } else {
                return form2js($form[0]);
            }
        },
        renderProcessState: function (response) {
            AnonymousProcessView.prototype
                .renderProcessState.call(this, response)
                .then(_.bind(function () {
                    if (response.type === "kbaSecurityAnswerDefinitionStage" && response.tag === "initial") {
                        this.$el.find("#kbaStageDescription").text($.t("common.user.kba.description",
                            { numberOfQuestions: response.requirements.properties.kba.minItems }));
                        // initialize the stage with at least 1 (but up to minItems) kba pairs
                        _.times(response.requirements.properties.kba.minItems || 1,
                                function () { this.addKBAQuestion(); }, this);
                    }
                }, this));
        }
    });

    return new SelfRegistrationView();
});
