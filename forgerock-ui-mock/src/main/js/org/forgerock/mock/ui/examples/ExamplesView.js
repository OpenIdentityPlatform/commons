/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2015 ForgeRock AS. All rights reserved.
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

/*global define, window */

define("org/forgerock/mock/ui/examples/ExamplesView", [
    "jquery",
    "underscore",
    "bootstrap",
    "selectize",
    "org/forgerock/commons/ui/common/main/AbstractView"
], function($, _,
            bootstrap,
            selectize,
            AbstractView) {

    var ExamplesView = AbstractView.extend({
        template: "templates/mock/ExamplesTemplate.html",
        events: {
            "click #sidebar a": "slideNavigation"
        },
        model: {
            leftNavOffset: 360
        },
        render: function(args, callback) {
            this.parentRender(_.bind(function() {
                /*
                 Selectize
                 */
                this.$el.find('#selectizeExample').selectize({
                    create: true
                });

                this.$el.find('#selectizePillExample').selectize({
                    create: true,
                    maxItems: 3
                });

                this.$el.find('#sidebar').affix({
                    offset: {
                        top: this.model.leftNavOffset
                    }
                });

                $(document.body).scrollspy({
                    target: '#rightCol'
                });

                if(callback){
                    callback();
                }
            }, this));
        },
        /*
         Prevents redirection from hash. Also animates a smooth scroll
         */
        slideNavigation : function(event) {
            event.preventDefault();

            var target = $(event.target);

            target =  $(target.attr("href"));

            if (target.length) {
                $('html,body').animate({
                    scrollTop: target.offset().top
                }, 1000);
            }
        }
    });

    return new ExamplesView();
});