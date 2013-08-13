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

/*global define, $, _, Backbone */

/**
 * @author mbilski
 */

define("org/forgerock/commons/ui/common/components/Messages", [
    "underscore",
    "backbone",
    "org/forgerock/commons/ui/common/main/AbstractConfigurationAware"
], function(_, backbone, AbstractConfigurationAware) {
    var obj = new AbstractConfigurationAware(), Messages;

    Messages = Backbone.View.extend({

        messages: [],
        numberOfMessages: 0,
        el: "#messages",
        events: {
            "click div": "hideMessages"
        },
        
        displayMessageFromConfig: function(msgKey) {
            this.addMessage({message: $.t(obj.configuration.messages[msgKey].msg), type: obj.configuration.messages[msgKey].type});
        },
        
        /**
         * Add message to array and runs messagesLoop if it is not currently running
         * Usage: addMessage({message: "Some Message", type: "error"})
         */
        addMessage: function(msg) {
            var i;
            
            for(i = 0; i < this.messages.length; i++) {
                if(this.messages[i].message === msg.message) {
                    console.log("duplicated message");
                    return;
                }
            }
            
            this.messages.push(msg);
            
            this.showMessage(msg, this.messagesLoop);
        },

        /**
         * Displays messages singly.
         */
        messagesLoop: function() {       
            var msg = this.messages.shift();
            
            if (this.messages.length > 0) {                
                this.showMessage(this.messages[0], this.messagesLoop);      
            }
        },

        /**
         * Shows message on screen.
         */
        showMessage: function(msg, callback) {
            var obj = this;
            
            if(msg.type === "error") {
                if (this.$el.find(".errorMessage").length) {
                    this.$el.find(".errorMessage").fadeOut(500);
                    this.$el.find(".errorMessage").remove();
                }
                this.$el.append("<div class='errorMessage' style='display: none;'><span><img src='images/span_error.png' width='14' height='14' alt='error' align='top' /></span>" + msg.message + "</div>");
            
                this.$el.find("div:last").fadeIn(500);
                this.messages.shift();
            } else {
                this.$el.find("div").fadeOut(500, function () {
                    $(this).remove();
                });
                this.$el.append("<div class='confirmMessage' style='display: none;'><span><img src='images/span_ok.png' width='14' height='14' alt='error' align='top' /></span>" + msg.message + "</div>");
                                
                this.$el.find("div:last").fadeIn(500).delay(1000).fadeOut(500, function() {
                    $(this).remove();
                    if (callback) {
                        callback.call(obj);
                    }
                });
            }            
        },
        
        hideMessages : function() {
            var obj = this;
            this.$el.find("div").delay(1000).fadeOut(500, function() {
                obj.messagesLoop();
                $(this).remove();
            });
        }
    });
    
    obj.messages = new Messages();

    return obj;
});