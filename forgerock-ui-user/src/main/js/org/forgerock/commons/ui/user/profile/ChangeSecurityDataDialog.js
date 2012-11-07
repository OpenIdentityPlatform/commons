/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2012 ForgeRock AS. All rights reserved.
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

/*global define, $, _, ContentFlow */

/**
 * @author mbilski
 */
define("org/forgerock/commons/ui/user/profile/ChangeSecurityDataDialog", [
    "org/forgerock/commons/ui/common/components/Dialog",
    "org/forgerock/commons/ui/common/main/ValidatorsManager",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/user/delegates/UserDelegate",
    "org/forgerock/commons/ui/user/delegates/InternalUserDelegate",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/user/delegates/SecurityQuestionDelegate"
], function(Dialog, validatorsManager, conf, userDelegate, internalUserDelegate, uiUtils, eventManager, constants, securityQuestionDelegate) {
    var ChangeSecurityDataDialog = Dialog.extend({    
        contentTemplate: "templates/user/ChangeSecurityDataDialogTemplate.html",
        
        data: {         
            width: 800,
            height: 400
        },
        
        events: {
            "click input[type=submit]": "formSubmit",
            "onValidate": "onValidate",
            "click .dialogCloseCross img": "close",
            "click input[name='close']": "close",
            "click .dialogContainer": "stop"
        },
        
        formSubmit: function(event) {
            event.preventDefault();
            
            if(validatorsManager.formValidated(this.$el)) {            
                var patchDefinitionObject = [], element;
                
                if(this.$el.find("input[name=password]").val()) {
                    patchDefinitionObject.push({replace: "password", value: this.$el.find("input[name=password]").val()});
                }
                
                if(this.$el.find("input[name=securityAnswer]").val()) {
                    patchDefinitionObject.push({replace: "securityQuestion", value: this.$el.find("select[name=securityQuestion]").val()});
                    patchDefinitionObject.push({replace: "securityAnswer", value: this.$el.find("input[name=securityAnswer]").val()});
                }
                
                this.delegate.patchSelectedUserAttributes(conf.loggedUser._id, conf.loggedUser._rev, patchDefinitionObject, _.bind(function(r) {
                    eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "securityDataChanged");
                    this.close();
                    
                    if ($.inArray("openidm-admin", conf.loggedUser.roles.split(",")) === -1) {
                        userDelegate.getForUserID(conf.loggedUser._id, function(user) {
                            conf.loggedUser = user;
                        });
                    } else {
                        userDelegate.getForUserName(conf.loggedUser.userName, function(user) {
                            conf.loggedUser = user;
                        });
                    }
                            
                }, this));
            }
        },
        
        render: function() {
            this.actions = {};
            this.addAction($.t("common.form.update"), "submit");
            
            this.delegate = conf.globalData.userComponent === "internal/user" ? internalUserDelegate : userDelegate;
            
            if(conf.globalData.userComponent === "internal/user") {
                this.data.height = 260;
            } else if(conf.globalData.securityQuestions === true) {
                this.data.height = 400;
            }
                    
            this.show(_.bind(function() {
                    validatorsManager.bindValidators(this.$el, this.delegate.baseEntity, _.bind(function () {
                    
                    if(conf.passwords) {
                        this.$el.find("input[name=oldPassword]").val(conf.passwords.password);                    
                        delete conf.passwords;
                    }
                    
                    this.reloadData();
                }, this));
            }, this));
        },
        
        reloadData: function() {
            var user = conf.loggedUser, self = this;
            this.$el.find("input[name=_id]").val(conf.loggedUser._id);
            securityQuestionDelegate.getAllSecurityQuestions(function(secquestions) {
                uiUtils.loadSelectOptions(secquestions, self.$el.find("select[name='securityQuestion']"), 
                    false, _.bind(function() {
                        this.$el.find("select[name='securityQuestion']").val(user.securityQuestion);                
                        this.$el.find("input[name=oldSecurityQuestion]").val(user.securityQuestion);                
                    validatorsManager.validateAllFields(this.$el);
                }, self));
            });
            this.$el.find("select[name=securityQuestion]").on('change', _.bind(function() {
                this.$el.find("input[name=securityAnswer]").trigger('change');
            }, this));
        }
    }); 
    
    return new ChangeSecurityDataDialog();
});