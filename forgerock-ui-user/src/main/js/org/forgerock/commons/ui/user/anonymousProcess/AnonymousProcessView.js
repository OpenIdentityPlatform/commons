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

/*global define */

define("org/forgerock/commons/ui/user/anonymousProcess/AnonymousProcessView", [
    "jquery",
    "underscore",
    "form2js",
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/user/anonymousProcess/AnonymousProcessDelegate",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "org/forgerock/commons/ui/common/main/ValidatorsManager"
], function($, _, form2js, AbstractView, AnonymousProcessDelegate, Constants, EventManager, Router, UIUtils, ValidatorsManager) {
    /**
     * @exports org/forgerock/commons/ui/user/anonymousProcess/AnonymousProcessView
     *
     * To use this generic object, it is required that two properties be defined prior to initialization:
     * "endpoint" - the Anonymous Process endpoint registered on the backend under Constants.context
     * "i18nBase" - the base of the translation file that contains the entries specific to this instance
     *
     * Example:
     *
     *    new AnonymousProcessView.extend({
     *         endpoint: "reset",
     *         i18nBase: "common.user.passwordReset"
     *     });
     */

    var AnonymousProcessView = AbstractView.extend({
        baseTemplate: "templates/user/AnonymousProcessBaseTemplate.html",
        template: "templates/user/AnonymousProcessWrapper.html",
        events: {
            "click input[type=submit]": "formSubmit",
            "click #restart": "restartProcess",
            "onValidate": "onValidate"
        },
        data: {
            i18n: {}
        },

        getFormContent: function () {
            return form2js($(this.element).find("form")[0]);
        },

        formSubmit: function(event) {
            var formContent = this.getFormContent();

            event.preventDefault();

            this.delegate.submit(formContent).then(
                _.bind(this.renderProcessState, this),
                _.bind(this.renderProcessState, this)
            );

        },

        render: function(args, callback) {
            var params = Router.convertCurrentUrlToJSON().params;
            this.stateData = {};

            if (!this.delegate || args[0] !== "/continue") {
                this.setDelegate(this.endpoint, params.token);
            }

            if (params.token) {
                this.submitDelegate(params, function () {
                    EventManager.sendEvent(Constants.EVENT_CHANGE_VIEW, {
                        route: Router.currentRoute,
                        args: ["/continue"]
                    });
                });
                return;
            }

            this.setTranslationBase();
            this.parentRender();
        },

        parentRender: function () {
            AbstractView.prototype.parentRender.call(this, _.bind(function () {
                this.delegate.start().then(_.bind(this.renderProcessState, this));
            }, this));
        },

        setDelegate: function (endpoint, token) {
            this.delegate = new AnonymousProcessDelegate(endpoint, token);
        },

        submitDelegate: function (params, onSubmit) {
            this.delegate.submit(_.omit(params, "token")).then(onSubmit());
        },

        setTranslationBase: function () {
            _.each(["title", "completed", "failed", "tryAgain", "return"], function (key) {
                this.data.i18n[key] = this.i18nBase + "." + key;
            }, this);
        },

        restartProcess: function (e) {
            e.preventDefault();
            delete this.delegate;
            delete this.stateData;
            EventManager.sendEvent(Constants.EVENT_CHANGE_VIEW, { route: Router.currentRoute });
        },

        renderProcessState: function (response) {
            var processStatePromise = $.Deferred(),
                baseTemplateUrl = "templates/user/process/",
                loadGenericTemplate = function (stateData) {
                    UIUtils.fillTemplateWithData(
                        baseTemplateUrl + (_.has(response, "requirements") ? "GenericInputForm.html" : "GenericEndPage.html"),
                        stateData,
                        processStatePromise.resolve
                    );
                },
                attemptCustomTemplate = function (stateData) {
                    UIUtils.compileTemplate(baseTemplateUrl + response.type + "-" + response.tag + ".html", stateData)
                        .then(function (renderedTemplate) {
                            processStatePromise.resolve(renderedTemplate);
                        }, function () {
                            loadGenericTemplate(stateData);
                        });
                };

            if (_.has(response, "requirements")) {
                this.stateData = _.extend({
                    requirements: response.requirements
                }, this.data);
            } else {
                this.stateData = _.extend({
                    status: response.status
                }, this.data);
            }

            if (_.has(response, "type") && _.has(response, "tag")) {
                attemptCustomTemplate(this.stateData);
            } else {
                loadGenericTemplate(this.stateData);
            }

            processStatePromise.then(_.bind(function (content) {
                this.$el.find("#processContent").html(content);
                ValidatorsManager.bindValidators(this.$el);
                ValidatorsManager.validateAllFields(this.$el);
                this.$el.find(":input:first").focus();
            }, this));

        }

    });

    return AnonymousProcessView;
});
