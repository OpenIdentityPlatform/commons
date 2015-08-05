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

/*global define, require*/

define("org/forgerock/commons/ui/common/util/ModuleLoader", [
    "jquery"
], function ($) {
    
    return {
        load: function (libPath) {

            /*
                 For some reason, the first time you try to require([...]) a module like this,
                 require throws an error. But if you do so again immediately afterward (like
                 is being done within the the catch block), it works. Subsequent calls to load
                 the same module should work the first time.
            */

            var promise = $.Deferred();

            try {
                require([libPath], promise.resolve);
            } catch (e) {
                try {
                    require([libPath], promise.resolve);
                } catch (e2) {
                    promise.reject(e2);
                }
            }

            return promise;
        }
    };
});
