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
 * Portions copyright 2011-2015 ForgeRock AS.
 */

/*global define*/

define("org/forgerock/commons/ui/common/main/AbstractView", [
    "jquery",
    "underscore",
    "backbone",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/ModuleLoader",
    "org/forgerock/commons/ui/common/main/Router",
    "ThemeManager",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "org/forgerock/commons/ui/common/main/ValidatorsManager",
    "org/forgerock/commons/ui/common/util/ValidatorsUtils"
], function($, _, Backbone, Configuration, Constants, EventManager, ModuleLoader, Router, ThemeManager, UIUtils,
            ValidatorsManager, ValidatorsUtils) {
    /**
     * @exports org/forgerock/commons/ui/common/main/AbstractView
     */
    return Backbone.View.extend({

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

                if (needsNewBaseTemplate()) {
                    UIUtils.renderTemplate(
                        _this.baseTemplate,
                        $("#wrapper"),
                        _.extend({}, Configuration.globalData, _this.data),
                        _.bind(_this.loadTemplate, _this),
                        "replace",
                        needsNewBaseTemplate);
                } else {
                    _this.loadTemplate();
                }
            });
        },

        loadTemplate: function() {
            var self = this,
                validateCurrent = function () {
                    if (!_.has(self, "route")) {
                        return true;
                    } else if (!self.route.url.length && Router.getCurrentHash().replace(/^#/, '') === "") {
                        return true;
                    } else {
                        return Router.getCurrentHash().replace(/^#/, '').match(self.route.url);
                    }
                };

            this.setElement($(this.element));
            this.$el.unbind();
            this.delegateEvents();

            if (Configuration.baseTemplate !== this.baseTemplate && !this.noBaseTemplate) {
                Configuration.setProperty("baseTemplate", this.baseTemplate);
                EventManager.sendEvent(Constants.EVENT_CHANGE_BASE_VIEW);
            }

            // Ensure all partials are (pre)loaded before rendering the template
            $.when.apply($, _.map(this.partials, UIUtils.preloadPartial)).then(function () {
                UIUtils.renderTemplate(
                    self.template,
                    self.$el,
                    _.extend({}, Configuration.globalData, self.data),
                    self.callback ? _.bind(self.callback, self) : _.noop(),
                    self.mode,
                    validateCurrent);
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
            var $input = $(input, this.$el),
                $form = $input.closest("form"),
                $button = $form.find("input[type=submit]"),
                validationMessage = (msg && !_.isArray(msg)) ? msg.split("<br>") : [];

            // necessary to load bootstrap for popover support (which isn't always necessary for AbstractView)
            ModuleLoader.load("bootstrap").then(_.bind(function () {

                //clean up existing popover if no message is present
                if (!msg && $input.data()["bs.popover"]) {
                    $input.popover('destroy');
                }
                $input.parents(".form-group").removeClass('has-feedback has-error');

                if(msg && _.isArray(msg)){
                    validationMessage = msg;
                }

                if(msg === "inProgress") {
                    return;
                }
                if (!$button.length) {
                    $button = $form.find("#submit");
                }
                if (ValidatorsManager.formValidated($form)) {
                    $button.prop('disabled', false);
                    $form.find(".input-validation-message").hide();
                } else {
                    $button.prop('disabled', true);
                    $form.find(".input-validation-message").show();
                }

                if (msg === "disabled") {
                    ValidatorsUtils.hideValidation($input, $form);
                    return;
                } else {
                    ValidatorsUtils.showValidation($input, $form);
                    if(validationMessage.length){
                        $input.parents(".form-group").addClass('has-feedback has-error');
                        //clean up existing popover if validation messsage is different
                        if ($input.data()["bs.popover"] && !_.isEqual(validationMessage,$input.data()["bs.popover"].options.validationMessage)) {
                            $input.popover('destroy');
                        }
                        $input.popover({
                            validationMessage: validationMessage,
                            content: '<i class="fa fa-exclamation-circle"></i> ' + validationMessage.join('<br><i class="fa fa-exclamation-circle"></i> '),
                            trigger:'hover',
                            placement:'top',
                            html: 'true',
                            template: '<div class="popover popover-error help-block" role="tooltip"><div class="arrow"></div><h3 class="popover-title"></h3><div class="popover-content"></div></div>'
                        });
                    }
                }

                $form.find("div.validation-message[for='" + $input.attr('name') + "']").html(msg ? msg : '');

                if (validatorType) {
                    ValidatorsUtils.setErrors($form, validatorType, msg);
                }

                $form.trigger("customValidate", [$input, msg, validatorType]);

            }, this));

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
});
