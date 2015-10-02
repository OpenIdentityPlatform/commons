/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2015 ForgeRock AS. All rights reserved.
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
define("org/forgerock/commons/ui/common/components/Footer", [
    "org/forgerock/commons/ui/common/main/AbstractView"
], function(AbstractView) {
    return AbstractView.extend({
        element: "#footer",
        template: "templates/common/FooterTemplate.html",
        noBaseTemplate: true,

        /**
         * Retrieves the version number of the product
         * @return {Promise} Promise representing the return version
         */
        getVersion: function() {
            throw new Error("#getVersion not implemented");
        },
        render: function() {
            var self = this;

            this.data = {};

            if(this.showVersion()) {
                this.getVersion().then(function(version) {
                    self.data.version = version;
                }).always(
                    self.parentRender.bind(self)
                ).always(function() {
                    self.$el.addClass("footer-deep");
                });
            } else {
                self.parentRender();
                self.$el.removeClass("footer-deep");
            }
        },
        /**
         * Determines if to show the version
         * @return {boolean} Whether to show the version
         */
        showVersion: function() {
            return false;
        }
    });
});
