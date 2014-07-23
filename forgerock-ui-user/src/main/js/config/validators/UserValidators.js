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

/*global define, $, _ */

/**
 * @author mbilski
 */
define("config/validators/UserValidators", [
], function(constants, eventManager) {
    var obj = {
            "passPhrase": {
                "name": "Min 4 characters",
                "dependencies": [
                ],
                "validator": function(el, input, callback) {
                    var v = input.val(),
                        oldPassPhrase = el.find("input[name=oldPassPhrase]");
                    if (oldPassPhrase.length !== 0 && oldPassPhrase.val() === v) {
                        callback("disabled");
                        return;
                    }
                    
                    if (v.length < 4) {
                        callback($.t("common.form.validation.minimum4Characters"));
                        return;
                    }

                    callback();  
                }
            },
            "siteImage": {
                "name": "Site image not same as old",
                "dependencies": [
                ],
                "validator": function(el, input, callback) {
                    var v = input.val(),
                        oldSiteImage = el.find("input[name=oldSiteImage]");
                    if (oldSiteImage.length !== 0  && oldSiteImage.val() === v) {
                        callback("disabled");
                        return;
                    }
                    
                    callback();  
                }
            },
            "termsOfUse": {
                "name": "Acceptance required",
                "dependencies": [
                ],
                "validator": function(el, input, callback) {              
                    if(!input.is(':checked')) {
                        callback($.t("common.form.validation.acceptanceRequiredForRegistration"));
                        return;
                    }

                    callback();  
                }
            },
            "oldPassword": {
                "name": "Required field",
                "dependencies": [
                    "org/forgerock/commons/ui/common/main/Configuration",
                    "UserDelegate"
                ],
                "validator": function(el, input, callback, conf, userDelegate) {
                    var v = input.val();
                    
                    if(v === "") {
                        callback($.t("common.form.validation.incorrectPassword"));
                        return;
                    }
                    
                    userDelegate.checkCredentials(v, function() {
                        callback();
                        el.find(input).attr('data-validation-status', 'ok');
                        el.find("input[name='Continue']").click();
                    },
                    function (result) {
                        callback($.t("common.form.validation.incorrectPassword"));
                    });
                }
            },
            "resetPasswordCorrectLogin": {
                "name": "Reset Password Correct Login",
                "dependencies": [
                    "org/forgerock/commons/ui/common/util/ValidatorsUtils",
                    "UserDelegate"
                ],
                "validator": function(el, input, callback, utils, userDelegate) {
                    var v = input.val();
                    
                    if(v === "") {
                        callback($.t("common.form.validation.required"));
                        el.find(input).attr('data-validation-status', 'error');
                        el.find("input[name='Update']").click();
                        return;
                    }
                   
                    userDelegate.getSecurityQuestionForUserName(v, 
                    _.bind(function(securityQuestion) {
                        el.find(input).attr('data-validation-status', 'ok');
                        callback();
                        $(this.el).trigger("userNameFound", securityQuestion);
                    }, this),
                    _.bind(function () {
                        callback("No such user");
                        el.find(input).attr('data-validation-status', 'error');
                        $(this.el).trigger("userNameNotFound");
                    }, this)
                    );  
                }
            },
            "securityAnswer": {
                "name": "Check if security answer is correct",
                "dependencies": [
                    "org/forgerock/commons/ui/common/util/ValidatorsUtils",
                    "UserDelegate"
                ],
                "validator": function(el, input, callback, utils, userDelegate) {
                    var v = input.val(), userName;
                    if(v === "") {
                        callback($.t("common.form.validation.required"));
                        return;
                    }
                    userName = el.find("input[name='resetUsername']").val();
                    userDelegate.getBySecurityAnswer(userName, v, 
                        function(result) {
                            el.find("input[name=_id]").val(result._id);
                            callback();
                        },      
                        function() {
                            el.find("input[name=_id]").val("");
                            callback($.t("common.form.validation.incorrectSecurityAnswer"));
                        }
                    );
                }
            },
            "newSecurityAnswer": {
                "name": "",
                "dependencies": [
                ],
                "validator": function(el, input, callback) {
                    var v = el.find(input).val();
                    
                    if(el.find("input[name=oldSecurityQuestion]").val() !== el.find("select[name=securityQuestion]").val()) {
                        if(v === "") {
                            callback($.t("common.form.validation.required"));
                        } else {
                            callback();
                        }
                        
                        return;
                    }
                    
                    if(v === "") {
                        callback("disabled");
                    } else {
                        callback();
                    }
                }
            }
    };
    
    return obj;
});
