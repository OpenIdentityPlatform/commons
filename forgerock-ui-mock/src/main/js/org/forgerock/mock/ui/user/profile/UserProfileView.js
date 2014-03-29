/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2014 ForgeRock AS. All rights reserved.
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
 * @author Eugenia Sergueeva
 */
define("org/forgerock/mock/ui/user/profile/UserProfileView", [
    "org/forgerock/commons/ui/user/profile/UserProfileView",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/main/ValidatorsManager"
], function (commonProfileView, conf, validatorsManager) {
    var obj = Object.getPrototypeOf(commonProfileView);

    obj.render = function (args, callback) {

        this.parentRender(function () {
            validatorsManager.bindValidators(this.$el);

            _.each(conf.loggedUser, function (val, key) {
                this.$el.find('[name=' + key.toLowerCase() + ']').prop("name", key);
            }, this);

            this.reloadData();

            if (callback) {
                callback();
            }
        });
    };

    return obj;
});
