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
            "passwordConfirm": {
                "name": "Password confirmation",
                "dependencies": [
                    "org/forgerock/commons/ui/common/util/ValidatorsUtils"
                ],
                "validator": function(el, input, callback, utils) {
                    var v = $(input).val();
                    
/*                    if(el.find("input[name=oldPassword]").length !== 0) {
                        if(v === "" && $(el).find("input[name=password]").val() === "") {
                            utils.hideValidation($(el).find("input[name=password]"), el);
                            callback("disabled");
                            utils.hideBox(el);
                            return;
                        } else {
                            utils.showBox(el);
                        }
                    }
*/
                    if( v === "" || v !== $(el).find("input[name=password]").val() ) {
                        callback([$.t("common.form.validation.confirmationMatchesPassword")]);
                        return;
                    }

                    callback(); 
                }
            },
            "passPhrase": {
                "name": "Min 4 characters",
                "dependencies": [
                ],
                "validator": function(el, input, callback) {
                    var v = $(input).val();
                    if($(el).find("input[name=oldPassPhrase]").length !== 0) {
                        if($(el).find("input[name=oldPassPhrase]").val() === v) {
                            callback("disabled");
                            return;
                        }
                    }
                    
                    if(v.length < 4) {
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
                    var v = $(input).val();
                    if(el.find("input[name=oldSiteImage]").length !== 0) {
                        if(el.find("input[name=oldSiteImage]").val() === v) {
                            callback("disabled");
                            return;
                        }
                    }
                    
                    callback();  
                }
            },
            "termsOfUse": {
                "name": "Acceptance required",
                "dependencies": [
                ],
                "validator": function(el, input, callback) {              
                    if(!$(input).is(':checked')) {
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
                    var v = $(input).val();
                    
                    if(v === "") {
                        callback($.t("common.form.validation.incorrectPassword"));
                        return;
                    }
                    
                    userDelegate.checkCredentials(v, function(result) {
                        if(result.reauthenticated) {
                            callback();
                            $(input).attr('data-validation-status', 'ok');
                            $("input[name='Continue']").click();
                        } else {
                            callback($.t("common.form.validation.incorrectPassword"));
                        }
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
                    var v = $(input).val();
                    
                    if(v === "") {
                        callback($.t("common.form.validation.required"));
                        $(input).attr('data-validation-status', 'error');
                        $("input[name='Update']").click();
                        return;
                    }
                   
                    userDelegate.getSecurityQuestionForUserName(v, 
                    _.bind(function(securityQuestion) {
                        $(input).attr('data-validation-status', 'ok');
                        callback();
                        $(this.el).trigger("userNameFound", securityQuestion);
                    }, this),
                    _.bind(function () {
                        callback("No such user");
                        $(input).attr('data-validation-status', 'error');
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
                    var v = $(input).val(), userName;
                    if(v === "") {
                        callback($.t("common.form.validation.required"));
                        return;
                    }
                    userName = $(el).find("input[name='resetUsername']").val();
                    userDelegate.getBySecurityAnswer(userName, v, 
                        function(result) {
                            $(el).find("input[name=_id]").val(result._id);
                            callback();
                        },      
                        function() {
                            $(el).find("input[name=_id]").val("");
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
                    var v = $(input).val();
                    
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
