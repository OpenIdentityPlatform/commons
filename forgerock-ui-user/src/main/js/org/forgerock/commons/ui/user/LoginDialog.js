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

/*global $, _, define*/

define("org/forgerock/commons/ui/user/LoginDialog", [
    "org/forgerock/commons/ui/common/main/ValidatorsManager",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/main/EventManager", 
    "org/forgerock/commons/ui/common/util/Constants", 
    "org/forgerock/commons/ui/common/components/Dialog"
], function(validatorsManager, conf, eventManager, constants, Dialog) {
    var LoginDialog = Dialog.extend({
        contentTemplate: "templates/user/LoginDialog.html",
        element: '#dialogs',
        events: {
            "click input[type=submit]": "login",
            "click .dialogCloseCross img": "loginClose",
            "click input[name='close']": "loginClose",
            "click": "loginClose",
            "click .dialogContainer": "stop"
        },
        
        displayed: false,
        
        render: function () {
            if(this.displayed === false) {
                this.displayed = true;
                
                this.show(_.bind(function(){ 
                    validatorsManager.bindValidators(this.$el);
                    this.resize();                 
                                        
                    if (conf.loggedUser && conf.loggedUser.userName)
                    {
                        $("input[name=login]").val(conf.loggedUser.userName).trigger("keyup");
                        $("input[name=password]").focus();
                    }
                    else
                    {
                        $("input[name=login]").focus();
                    }  
                }, this));
            }
        },
        
        loginClose: function(e) {
            e.preventDefault();
            
            this.displayed = false;
            this.close(e);
        },
        
        login: function (e) {
            e.preventDefault();
            
            if(validatorsManager.formValidated(this.$el)) {
                conf.setProperty("backgroundLogin", true);
                var _this = this;
                eventManager.registerListener(constants.EVENT_DISPLAY_MESSAGE_REQUEST, _.once(function (event) {
                    console.log(event);
                    if (event === "loggedIn")
                    {
                        _this.close();
                        _this.displayed = false;
                        conf.setProperty("backgroundLogin", false);
                        eventManager.sendEvent(constants.EVENT_REQUEST_RESEND_REQUIRED);
                    }
                }));
                eventManager.sendEvent(constants.EVENT_LOGIN_REQUEST, {userName: this.$el.find("input[name=login]").val(), password: this.$el.find("input[name=password]").val()});
            }
            
        },
        data : {
            height: 300,
            width: 400
        }
    });

    return new LoginDialog();
    
});

