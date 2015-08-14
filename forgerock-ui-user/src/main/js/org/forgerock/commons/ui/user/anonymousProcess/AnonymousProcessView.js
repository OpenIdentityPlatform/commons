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
    "org/forgerock/commons/ui/common/util/UIUtils"
], function($, _, form2js, AbstractView, AnonymousProcessDelegate, Constants, EventManager, Router, UIUtils) {
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
            "click #restart": "restartProcess"
        },
        data: {
            i18n: {}
        },

        formSubmit: function(event) {
            var formContent = form2js($(this.element).find("form")[0]);

            event.preventDefault();

            this.delegate.submit(formContent).then(
                _.bind(this.renderProcessState, this),
                _.bind(this.renderProcessState, this)
            );

        },

        render: function(args, callback) {
            var params = Router.convertCurrentUrlToJSON().params;

            this.delegate = new AnonymousProcessDelegate(this.endpoint, params.token);

            // each instance of this module can define their own i18nBase value to provide specific translation values
            _.each(["title", "completed", "failed", "tryAgain", "return"], function (key) {
                this.data.i18n[key] = this.i18nBase + "." + key;
            }, this);

            this.parentRender(_.bind(function () {
                if (params.token) {
                    this.delegate.submit(_.omit(params, 'token')).then(_.bind(this.renderProcessState, this));
                } else {
                    this.delegate.start().then(_.bind(this.renderProcessState, this));
                }
            }, this));
        },

        restartProcess: function (e) {
            e.preventDefault();
            EventManager.sendEvent(Constants.EVENT_CHANGE_VIEW, { route: Router.currentRoute });
        },

        renderProcessState: function (response) {
            var processStatePromise = $.Deferred(),
                baseTemplateUrl = "templates/user/process/",
                stateData,
                loadGenericTemplate = function () {
                    UIUtils.fillTemplateWithData(
                        baseTemplateUrl + (_.has(response, "requirements") ? "GenericInputForm.html" : "GenericEndPage.html"),
                        stateData,
                        processStatePromise.resolve
                    );
                },
                attemptCustomTemplate = function () {
                    UIUtils.fillTemplateWithData(
                        baseTemplateUrl + response.type + "-" + response.stage + ".html",
                        stateData,
                        function (renderedTemplate) {
                            /*
                                If the template is loaded successfully, then it renderedTemplate a
                                string containing the content of the rendered template. In the case
                                of a failure to load, this function is passed the failed XHR object.
                            */
                            if (typeof renderedTemplate === "string") {
                                processStatePromise.resolve(renderedTemplate);
                            } else {
                                loadGenericTemplate();
                            }
                        }
                    );
                };

            if (_.has(response, "requirements")) {
                stateData = _.extend({
                    requirements: response.requirements
                }, this.data);
            } else {
                stateData = _.extend({
                    status: response.status
                }, this.data);
            }

            if (_.has(response, "type") && _.has(response, "stage")) {
                attemptCustomTemplate();
            } else {
                loadGenericTemplate();
            }

            processStatePromise.then(_.bind(function (content) {
                this.$el.find("#processContent").html(content);
                this.$el.find(":input:first").focus();
            }, this));

        }

    });

    return AnonymousProcessView;
});
