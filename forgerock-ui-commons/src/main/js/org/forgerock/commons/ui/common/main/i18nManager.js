/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014 ForgeRock AS. All rights reserved.
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

/*global require, define, _, $*/

/**
 * @author jkigwana
 */

define( "org/forgerock/commons/ui/common/main/i18nManager", [
        "org/forgerock/commons/ui/common/util/Constants",
        "org/forgerock/commons/ui/common/util/UIUtils"
], function(consts,uiUtils) {

    /*
     * i18nManger with i18next try to detect the user language and load the corresponding translation in the following order:
     * 1) querystring parameter (&locale=fr)
     * 2) serverinfo which is a mirror to the values in the browser request headers. This could be a single value in a string or a string of csv.
     * 3) consts.DEFAULT_LANGUAGE
     */
    
    var obj = {};

    obj.init = function(lang) {

        var fallbacks = [], opts = { },
            urlParams = uiUtils.convertCurrentUrlToJSON().params;         

        if (lang) {
            fallbacks = lang.split(',');    
        }

        if (fallbacks.indexOf(consts.DEFAULT_LANGUAGE) === -1){
            fallbacks.push(consts.DEFAULT_LANGUAGE);
        }

        if (obj.fallbacks !== undefined && obj.fallbacks === fallbacks) {
           return;
        }

        obj.fallbacks = fallbacks;

        opts = { fallbackLng: fallbacks, detectLngQS: 'locale', useCookie:false, getAsync: false, load: 'unspecific' };

        // if urlParams then override lang
        if(urlParams && urlParams.locale){
            opts.lng  = urlParams.locale;
        }

        $.i18n.init(opts);

    };
    
    return obj;

});
