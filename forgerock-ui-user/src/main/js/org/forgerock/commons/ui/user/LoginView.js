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

/*global define, $, form2js, _, js2form, document */

/**
 * @author mbilski
 */
define("org/forgerock/commons/ui/user/LoginView", [
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/main/ValidatorsManager",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/util/CookieHelper"
], function(AbstractView, validatorsManager, eventManager, constants, cookieHelper) {
    var LoginView = AbstractView.extend({
        template: "templates/user/LoginTemplate.html",
        baseTemplate: "templates/user/LoginBaseTemplate.html",
        
        events: {
            "click input[type=submit]": "formSubmit",
            "onValidate": "onValidate"
        },
        
        formSubmit: function(event) {
            event.preventDefault();
            
            if(validatorsManager.formValidated(this.$el)) {
                if(this.$el.find("[name=loginRemember]:checked").length !== 0) {
                    var expire = new Date();
                    expire.setDate(expire.getDate + 365*20);
                    cookieHelper.setCookie("login", this.$el.find("input[name=login]").val(), expire);
                }
                
                eventManager.sendEvent(constants.EVENT_LOGIN_REQUEST, {userName: this.$el.find("input[name=login]").val(), password: this.$el.find("input[name=password]").val()});
            }
        },
        
        render: function(args, callback) {
            this.parentRender(function() {                
                validatorsManager.bindValidators(this.$el);
                
                var login = cookieHelper.getCookie("login");
                this.$el.find("input[name=login]").val(login);
                
                if(callback) {
                    callback();
                }
            });            
        }
    }); 
    
    return new LoginView();
});


