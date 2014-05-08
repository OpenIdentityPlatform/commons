/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2014 ForgeRock AS. All Rights Reserved
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

/*global define, $, form2js, _, js2form, window */

/**
 * @author huck.elliott
 * @author Eugenia Sergueeva
 */
define("org/forgerock/mock/ui/user/LoginView", [
    "org/forgerock/commons/ui/common/LoginView",
    "org/forgerock/commons/ui/common/main/ValidatorsManager",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/mock/ui/common/main/LocalStorage"
], function (commonLoginView, validatorsManager, conf, localStorage) {
    var LoginView = function () {
        },
        obj;

    LoginView.prototype = commonLoginView;

    obj = new LoginView();

    obj.render = function (args, callback) {

        if (conf.globalData.selfRegistration) {
            obj.baseTemplate = "templates/common/MediumBaseTemplate.html";
        }

        commonLoginView.render.call(this, args, _.bind(function () {

            var login = localStorage.get('remember-login');
            if (login) {
                this.$el.find("input[name=login]").val(login);
                this.$el.find("[name=loginRemember]").attr("checked", "true");
                validatorsManager.validateAllFields(this.$el);
                this.$el.find("[name=password]").focus();
            }

            if (callback) {
                callback();
            }

        }, this));        
    };

    obj.formSubmit = function (event) {
        commonLoginView.formSubmit.call(this, event);

        if (this.$el.find("[name=loginRemember]:checked").length) {
            localStorage.remove('remember-login');
            localStorage.add('remember-login', this.$el.find("input[name=login]").val());
        } else {
            localStorage.remove('remember-login');
        }
    };

    return obj;
});