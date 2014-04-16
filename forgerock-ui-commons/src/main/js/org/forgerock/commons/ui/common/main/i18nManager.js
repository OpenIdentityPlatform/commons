/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 ForgeRock AS. All rights reserved.
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

/*global require, define, _, $, XDate */

/**
 * @author jdabrowski
 */
define( "org/forgerock/commons/ui/common/main/i18nManager", [
        "org/forgerock/commons/ui/common/util/Constants",
        "org/forgerock/commons/ui/common/util/UIUtils"
], function(consts,uiUtils) {

    /*
     * i18nManger with i18next try to detect the user language and load the corresponding translation in the following order:
     * 1) querystring parameter (&locale=en-US)
     * 2) server info
     * 3) consts.DEFAULT_LANGUAGE
     */
    
    var obj = {};

    obj.init = function(lang) {

        if( obj.locale !== undefined && obj.locale === lang){
           return;
        }

        var urlParams = uiUtils.convertCurrentUrlToJSON().params,
            opts = { fallbackLng: consts.DEFAULT_LANGUAGE,  detectLngQS: 'locale', useCookie:false,  getAsync: false, lng:lang, load: 'unspecific' };

        // if urlParams then override lang
        if(urlParams && urlParams.locale){
            opts.lng  = urlParams.locale;
        }

        $.i18n.init(opts).done(obj.ready);
        obj.locale = $.i18n.lng();
 
    };

    obj.ready = function() {

        var locale = $.i18n.lng(),
            mNames = $.t("config.dates.monthNames").replace(/ ,/g,',').replace(/, /g,',').split(','),
            mNamesShort = $.t("config.dates.monthNamesShort").replace(/ ,/g,',').replace(/, /g,',').split(','),
            dNames = $.t("config.dates.dayNames").replace(/ ,/g,',').replace(/, /g,',').split(','),
            dNamesShort = $.t("config.dates.dayNamesShort").replace(/ ,/g,',').replace(/, /g,',').split(',');

        if (
            ! mNames.length || mNames.length < 12
            || 
            ! mNamesShort.length || mNamesShort.length < 12
            || 
            ! dNames.length || dNames.length < 7
            || 
            ! dNamesShort.length || dNamesShort.length < 7        
        ) 
        {
            console.log("DATE NAMES ARE NOT DEFINED CORRECTLY!");
        } else {
            
            XDate.locales[ locale ] = {
                monthNames: mNames,
                monthNamesShort: mNamesShort,
                dayNames: dNames,
                dayNamesShort: dNamesShort
            };
            
            XDate.defaultLocale = locale;
        }  
       
    };
    
    return obj;

});
