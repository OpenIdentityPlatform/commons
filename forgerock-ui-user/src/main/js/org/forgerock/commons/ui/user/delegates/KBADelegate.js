/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2015 ForgeRock AS.
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

/*global define */


define("org/forgerock/commons/ui/user/delegates/KBADelegate", [
    "jquery",
    "lodash",
    "org/forgerock/commons/ui/common/main/AbstractDelegate",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/util/ObjectUtil"
], function ($, _, AbstractDelegate, Configuration, Constants, ObjectUtil) {

    var KBADelegate = new AbstractDelegate("/" + Constants.context + "/" + Constants.SELF_SERVICE_CONTEXT);

    KBADelegate.getQuestions = function () {
        return this.serviceCall({
            "url" : "kba"
        }).then(function (response) {
            return _.map(response.questions, function (value, key) {
                return {
                    "id" : key,
                    "question" : value
                };
            });
        });
    };

    KBADelegate.saveKBAInfo = function (user) {
        return this.serviceCall({
            "type": "PATCH",
            "url": "user/" + Configuration.loggedUser.id,
            "data": JSON.stringify(
                _(user)
                 .map(function (value, key) {
                    return {
                        "operation": "replace",
                        "field": "/" + key,
                        // replace the whole value, rather than just the parts that have changed,
                        // since there is no consistent way to target items in a set across the stack
                        "value": value
                    };
                })
            )
        }).then(function (updatedUser) {
            return Configuration.loggedUser.save(updatedUser, {"silent": true});
        });
    };

    return KBADelegate;
});
