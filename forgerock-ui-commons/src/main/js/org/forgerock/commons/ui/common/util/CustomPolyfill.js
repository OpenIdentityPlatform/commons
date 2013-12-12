/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 ForgeRock AS. All rights reserved.
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

define("org/forgerock/commons/ui/common/util/CustomPolyfill", [
], function () {

    var fakeConsole = {
            log : function() {
            },
            info : function() {
            },
            debug : function() {
            }
        },
        key,
        proto = "__proto__";

    if (typeof console === "undefined") {
        window.console = fakeConsole;
    }
    else
    {
        if (window.console.log) {
            fakeConsole.log = window.console.log;
        }

        for (key in fakeConsole) {
            if (!window.console[key]) {
                window.console[key] = fakeConsole.log;
            }
        }
    }


    //this is here to catch the issue IE 8 has with getPrototypeOf method
    if(typeof Object.getPrototypeOf !== "function"){
        if (typeof "internet_explorer"[proto] === "object"){
            Object.getPrototypeOf = function(o){
                return o[proto];
            };
        }
        else{
            Object.getPrototypeOf = function(o){
                return o.constructor.prototype;
            };
        }
    }

    if (typeof Object.create !== "function") {
        Object.create = function(o){
            function F() { }
            F.prototype = o;
            return new F();
        };
    }    
});