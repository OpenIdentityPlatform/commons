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

define("org/forgerock/commons/ui/user/anonymousProcess/PasswordResetView", [
    "jquery",
    "lodash",
    "org/forgerock/commons/ui/user/anonymousProcess/AnonymousProcessView",
    "org/forgerock/commons/ui/common/util/Constants"
], function($, _, AnonymousProcessView, Constants) {

    /**
     * Given a position in the DOM, look for children elements which comprise a
     * boolean expression. Using all of those found with content, return a filter
     * string which represents them.
     */
    function walkTreeForFilterStrings(basicNode) {
        var groupValues,
            node = $(basicNode);

        if (node.hasClass("filter-value") && node.val().length > 0) {
            return node.attr('name') + ' eq "' + node.val().replace('"', '\\"') + '"';
        } else if (node.hasClass("filter-group")) {
            groupValues = _.chain(node.find(">.form-group>.filter-value, >.filter-group"))
                           .map(walkTreeForFilterStrings)
                           .filter(function (value) {
                               return value.length > 0;
                           })
                           .value();

            if (groupValues.length === 0) {
                return "";
            } else if (groupValues.length === 1) {
                return groupValues[0];
            }

            if (node.hasClass("filter-or")) {
                return "(" + groupValues.join(" OR ") + ")";
            } else {
                return "(" + groupValues.join(" AND ") + ")";
            }
        } else {
            return "";
        }
    }

    var PasswordResetView = AnonymousProcessView.extend({
        endpoint: Constants.SELF_SERVICE_CONTEXT + "reset",
        i18nBase: "common.user.passwordReset",
        events: _.extend({
            "change #userQuery :input": "buildQueryFilter",
            "keyup #userQuery :input": "buildQueryFilter",
            "customValidate #userQuery": "validateForm"
        }, AnonymousProcessView.prototype.events),
        getFormContent: function () {
            if (this.$el.find("form").attr("id") === "userQuery") {
                return {
                    queryFilter: this.$el.find(":input[name=queryFilter]").val()
                };
            } else {
                return AnonymousProcessView.prototype.getFormContent.call(this);
            }
        },
        buildQueryFilter: function () {
            this.$el.find(":input[name=queryFilter]")
                .val(walkTreeForFilterStrings(this.$el.find("#filterContainer")));
            this.validateForm();
        },
        validateForm: function () {
            var button = this.$el.find("input[type=submit]"),
                incompleteAndGroup = false;

            // there has to be some value in the queryFilter or the whole thing is invalid
            if (this.$el.find(":input[name=queryFilter]").val().length === 0) {
                button.prop("disabled", true);
                return;
            }

            // filter-and groups must have each of their children filled-out
            this.$el.find(".filter-and").each(function () {
                // if there are any values at all specified for this "and" group...
                if (walkTreeForFilterStrings(this).length > 0) {
                    // then we need to make sure that they are all populated
                    incompleteAndGroup = !(
                        // check all direct filter-value fields for content
                        _.reduce($(">.form-group>.filter-value", this), function (state, field) {
                            return state && field.value.length > 0;
                        }, true) &&
                        // check all direct sub-groups for content
                        _.reduce($(">.filter-group", this), function (state, subGroup) {
                            return walkTreeForFilterStrings(subGroup).length > 0;
                        }, true)
                    );
                }
            });

            button.prop("disabled", incompleteAndGroup);
        }
    });

    return new PasswordResetView();
});
