/**
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2015-2016 ForgeRock AS.
 */

define([
    "jquery",
    "underscore",
    "libs/codemirror-4.10/lib/codemirror",
    "libs/codemirror-4.10/mode/xml/xml",
    "libs/codemirror-4.10/mode/javascript/javascript",
    "bootstrap",
    "selectize",
    "org/forgerock/commons/ui/common/main/AbstractView"
], function($, _,
            CodeMirror,
            xmlmode,
            jsmode,
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
                Code Snippets
                */
                this.$el.find('.example').each(function(i, elem) {

                    var snippet = $('.snippet')[i].innerHTML;

                    CodeMirror(elem, {
                        value: snippet,
                        mode: 'xml',
                        lineNumbers: false,
                        readOnly: true,
                        lineWrapping: false
                    });
                });

                this.$el.find('.code').each(function() {

                    var $this = $(this),
                        $code = $this.html();

                    $this.empty();

                    CodeMirror(this, {
                        value: $code,
                        mode: 'javascript',
                        lineNumbers: true,
                        readOnly: true,
                        lineWrapping: true
                    });
                });

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
