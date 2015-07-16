/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2015 ForgeRock AS.
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

/*global define*/
define("org/forgerock/commons/ui/common/main/AbstractView", [
    "jquery",
    "underscore",
    "backbone",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "org/forgerock/commons/ui/common/main/ValidatorsManager",
    "org/forgerock/commons/ui/common/util/ValidatorsUtils",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/commons/ui/common/util/Constants",
    "ThemeManager"
], function($, _, Backbone, UIUtils, ValidatorsManager, ValidatorsUtils, Configuration, EventManager, Router, Constants, ThemeManager) {
    /**
     * @exports org/forgerock/commons/ui/common/main/AbstractView
     */
    var View = Backbone.View.extend({

        /**
         * This params should be passed when creating new object, for example:
         * new View({el: "#someId", template: "templates/main.html"});
         */
        element: "#content",

        baseTemplate: "templates/common/DefaultBaseTemplate.html",

        /**
         * View mode: replace or append
         */
        mode: "replace",

        formLock: false,

        initialize: function () {
            this.data = this.data || {};
        },

        /**
         * Change content of 'el' element with 'viewTpl',
         * which is compiled using 'data' attributes.
         */
        parentRender: function(callback) {
            this.callback = callback;

            var _this = this,
                needsNewBaseTemplate = function () {
                    return (Configuration.baseTemplate !== _this.baseTemplate && !_this.noBaseTemplate);
                };
            EventManager.registerListener(Constants.EVENT_REQUEST_RESEND_REQUIRED, function () {
                _this.unlock();
            });

            ThemeManager.getTheme().then(function(theme){
                _this.data.theme = theme;

                if(needsNewBaseTemplate()) {
                    UIUtils.renderTemplate(_this.data.theme.path + _this.baseTemplate, $("#wrapper"), _.extend({}, Configuration.globalData, _this.data), _.bind(_this.loadTemplate, _this), "replace", needsNewBaseTemplate);
                } else {
                    _this.loadTemplate();
                }
            });

            //Added in due to the migration of the username to the main nav bar
            if(Configuration.loggedUser) {
                this.$el.find("#profile_link").show();

                if (Configuration.loggedUser.userName) {
                    this.$el.find("#user_name").text(Configuration.loggedUser.userName); //idm
                } else if (Configuration.loggedUser.cn) {
                    this.$el.find("#user_name").text(Configuration.loggedUser.cn); //am
                }
            }
        },

        loadTemplate: function() {
            var _this = this,
                validateCurrent = function () {
                    if (!_.has(_this, "route")) {
                        return true;
                    } else if (!_this.route.url.length && Router.getCurrentHash().replace(/^#/, '') === "") {
                        return true;
                    } else {
                        return Router.getCurrentHash().replace(/^#/, '').match(_this.route.url);
                    }
                };

            this.setElement($(this.element));
            this.$el.unbind();
            this.delegateEvents();

            if(Configuration.baseTemplate !== this.baseTemplate && !this.noBaseTemplate) {
                Configuration.setProperty("baseTemplate", this.baseTemplate);
                EventManager.sendEvent(Constants.EVENT_CHANGE_BASE_VIEW);
            }

            if(this.callback) {
                UIUtils.renderTemplate(this.data.theme.path + this.template, this.$el, _.extend({}, Configuration.globalData, this.data), _.bind(this.callback, this), this.mode, validateCurrent);
            } else {
                UIUtils.renderTemplate(this.data.theme.path + this.template, this.$el, _.extend({}, Configuration.globalData, this.data), null, this.mode, validateCurrent);
            }

            _.each(this.partials, function(url) {
                UIUtils.preloadPartial(url);
            });
        },

        rebind: function() {
            this.setElement($(this.element));
            this.$el.unbind();
            this.delegateEvents();
        },

        render: function(args, callback) {
            this.parentRender(callback);
        },

        reload: function() {},

        /**
         * Perform only view changes: displays tick, message and
         * change color of submit button.
         */
        onValidate: function(event, input, msg, validatorType) {
            var button = this.$el.find("input[type=submit]"),
                validationMessage = (msg && !_.isArray(msg)) ? msg.split("<br>") : [];

            //clean up existing popover if no message is present
            if (!msg && $(input, this.$el).data()["bs.popover"]) {
                $(input, this.$el).popover('destroy');
            }
            $(input, this.$el).removeClass('field-error');

            if(msg && _.isArray(msg)){
                validationMessage = msg;
            }

            if(msg === "inProgress") {
                //TODO spinner
                //console.log("in progress..");
                return;
            }
            if (!button.length) {
                button = this.$el.find("#submit");
            }
            if (ValidatorsManager.formValidated(this.$el)) {
                button.prop('disabled', false);
                this.$el.find(".input-validation-message").hide();
            } else {
                button.prop('disabled', true);
                this.$el.find(".input-validation-message").show();
            }

            if (msg === "disabled") {
                ValidatorsUtils.hideValidation(input, this.$el);
                return;
            } else {
                ValidatorsUtils.showValidation(input, this.$el);
                if(validationMessage.length){
                    $(input, this.$el).addClass('field-error');
                    //clean up existing popover if validation messsage is different
                    if ($(input, this.$el).data()["bs.popover"] && !_.isEqual(validationMessage,$(input, this.$el).data()["bs.popover"].options.validationMessage)) {
                        $(input, this.$el).popover('destroy');
                    }
                    $(input, this.$el).popover({
                        validationMessage: validationMessage,
                        content: '<i class="fa fa-exclamation-circle"></i> ' + validationMessage.join('<br><i class="fa fa-exclamation-circle"></i> '),
                        trigger:'hover',
                        placement:'top',
                        html: 'true',
                        template: '<div class="popover popover-error" role="tooltip"><div class="arrow"></div><h3 class="popover-title"></h3><div class="popover-content"></div></div>'
                    });
                }
            }

            if (input.nextAll("span")) {
                ValidatorsUtils.setTick(input, msg);
            }

            if (msg) {
                input.parents('.separate-message').addClass('invalid');
                input.addClass('invalid');
            } else {
                input.parents('.separate-message').removeClass('invalid');
                input.removeClass('invalid');
            }

            this.$el.find("div.validation-message[for='" + input.attr('name') + "']").html(msg ? msg : '');

            if (validatorType) {
                ValidatorsUtils.setErrors(this.$el, validatorType, msg);
            }

            this.$el.trigger("customValidate", [input, msg, validatorType]);
        },

        lock: function() {
            this.formLock = true;
        },

        unlock: function() {
            this.formLock = false;
        },

        isFormLocked: function() {
            return this.formLock;
        }
    });

    return View;
});
