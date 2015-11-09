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

/*global define */

define("config/validators/CommonValidators", [
    "jquery"
], function($) {
    var obj = {
        "required": {
            "name": "Required field",
            "dependencies": [ ],
            "validator": function(el, input, callback, Base64) {
                var v = input.val();
                if (!v || v === "") {
                    callback([$.t("common.form.validation.required")]);
                } else {
                    callback();
                }

            }
        },
        "passwordConfirm": {
            "name": "Password confirmation",
            "dependencies": [ ],
            "validator": function(el, input, callback) {
                var confirmValue = input.val(),
                    mainInput = el.find("input[data-validation-dependents=passwordConfirm]");

                if (mainInput.val() !== confirmValue || mainInput.attr("data-validation-status") === "error") {
                    callback([$.t("common.form.validation.confirmationMatchesPassword")]);
                } else {
                    callback();
                }
            }
        },
        "minLength": {
            "name": "Minimum number of characters",
            "dependencies": [ ],
            "validator": function(el, input, callback) {
                var v = input.val(),
                    len = input.attr('minLength');

                if (v.length < len) {
                    callback([$.t("common.form.validation.MIN_LENGTH", {minLength: len})]);
                } else {
                    callback();
                }
            }
        },
        "atLeastXNumbers": {
            "name": "Minimum occurrence of numeric characters in string",
            "dependencies": [ ],
            "validator": function(el, input, callback) {
                var v = input.val(),
                    minNumbers = input.attr('atLeastXNumbers'),
                    foundNumbers = v.match(/\d/g);

                if (!foundNumbers || foundNumbers.length < minNumbers) {
                    callback([$.t("common.form.validation.AT_LEAST_X_NUMBERS", {numNums: minNumbers})]);
                } else {
                    callback();
                }
            }
        },
        "atLeastXCapitalLetters": {
            "name": "Minimum occurrence of capital letter characters in string",
            "dependencies": [ ],
            "validator": function(el, input, callback) {
                var v = input.val(),
                    minCapitals = input.attr('atLeastXCapitalLetters'),
                    foundCapitals = v.match(/[(A-Z)]/g);

                if (!foundCapitals || foundCapitals.length < minCapitals) {
                    callback([$.t("common.form.validation.AT_LEAST_X_CAPITAL_LETTERS", {numCaps: minCapitals})]);
                } else {
                    callback();
                }
            }
        }
    };
    return obj;
});
