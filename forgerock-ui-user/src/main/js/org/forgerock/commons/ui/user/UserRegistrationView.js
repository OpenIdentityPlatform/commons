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

/*global define, $, form2js, _, ContentFlow */

/**
 * @author mbilski
 */
define("org/forgerock/commons/ui/user/UserRegistrationView", [
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/main/ValidatorsManager",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "org/forgerock/commons/ui/user/delegates/UserDelegate",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/user/delegates/CountryStateDelegate",
    "org/forgerock/commons/ui/user/delegates/SecurityQuestionDelegate"
], function(AbstractView, validatorsManager, uiUtils, userDelegate, eventManager, constants, conf, countryStateDelegate, securityQuestionDelegate) {
    var UserRegistrationView = AbstractView.extend({
        template: "templates/user/UserRegistrationTemplate.html",
        baseTemplate: "templates/user/LoginBaseTemplate.html",
        
        events: {
            "click input[type=submit]": "formSubmit",
            "onValidate": "onValidate",
            "click #passPhrasePictures img": "selectSiteImage",
            "click #frgtPasswrdSelfReg": "showForgottenPassword"
        },
        
        showForgottenPassword: function(event) {
            event.preventDefault();
            conf.forgottenPasswordUserName = this.$el.find("input[name=email]").val();
            eventManager.sendEvent(constants.ROUTE_REQUEST, {routeName : "forgottenPassword"});
        },
        
        formSubmit: function(event) {
            event.preventDefault();
            
            if(validatorsManager.formValidated(this.$el) && !this.isFormLocked()) {
                this.lock();
                
                var data = form2js(this.$el.attr("id")), element;
                
                delete data.terms;
                delete data.passwordConfirm;
                data.userName = data.email.toLowerCase();
                
                element = this.siteImageFlow.getActiveItem().element;
                data.siteImage = $(element).children().attr("data-site-image");
                
                console.log("ADDING USER: " + JSON.stringify(data));                
                userDelegate.createEntity(data, function(user) {
                    eventManager.sendEvent(constants.EVENT_USER_SUCCESSFULY_REGISTERED, { user: data, selfRegistration: true });                    
                }, function(response) {
                    console.warn(response);
                    if (response.error === 'Conflict') {
                        //TODO
                        eventManager.sendEvent(constants.EVENT_USER_REGISTRATION_ERROR, { causeDescription: 'User already exists' });
                    } else {
                        eventManager.sendEvent(constants.EVENT_USER_REGISTRATION_ERROR, { causeDescription: 'Unknown error' });
                    }
                    this.unlock();
                });
            }
        },
        
        selectSiteImage: function(event) {
            $("#passPhrasePictures img").removeClass('pictureSelected').addClass('pictureNotSelected');
            $(event.target).removeClass('pictureNotSelected').addClass('pictureSelected');
        },
        
        render: function(args, callback) {
            this.parentRender(function() {
                validatorsManager.bindValidators(this.$el);
                this.unlock();
                
                securityQuestionDelegate.getAllSecurityQuestions(function(secquestions) {
                    uiUtils.loadSelectOptions(secquestions, $("select[name='securityQuestion']"));
                });
                
                this.siteImageCounter = 0;
                $("#siteImageFlow img").load(_.bind(this.refreshFlow, this));
                                
                if(callback) {
                    callback();
                }
            });            
        },
        
        refreshFlow: function() {
            this.siteImageCounter++;
            
            if( this.siteImageCounter === $("#siteImageFlow img").length ) {
                console.log("Refreshing flow");
                
                this.siteImageFlow = new ContentFlow('siteImageFlow', { reflectionHeight: 0, circularFlow: false } );
                this.siteImageFlow._init();
                this.siteImageCounter = 0;
                
                this.siteImageFlow.conf.onclickActiveItem = function(){};
                
                this.siteImageFlow.moveTo(parseInt(Math.random() * $("#siteImageFlow img").length, 0));
                $("#siteImageFlow img").show();
            }  
        }
    }); 
    
    return new UserRegistrationView();
});


