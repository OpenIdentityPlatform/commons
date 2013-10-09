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

/*global require, define, _, $, XDate */

/**
 * @author jdabrowski
 */
define("org/forgerock/commons/ui/common/main/i18nManager", [
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "org/forgerock/commons/ui/common/util/CookieHelper"
], function(consts,uiUtils,cookieHelper) {
    
    var obj = {};
    
    obj.language = consts.DEFAULT_LANGUAGE;
    
    obj.setLanguage = function(language) {
        var mNames, mNamesShort, dNames, dNamesShort,
            urlParams = uiUtils.convertCurrentUrlToJSON().params,
            i18nCookie = cookieHelper.getCookie('i18next');
        
        $.i18n.init({
            fallbackLng: false,
            load: 'current' 
        });
        
        //if there is already a cookie then use that value as the language
        if(i18nCookie){
            language = i18nCookie;
            //if locale is set in the url and is different from the cookie value use the locale param
            if(urlParams && urlParams.locale !== i18nCookie){
                language = urlParams.locale;
            }
        }
        $.i18n.setLng(language);
        obj.language = language;
        
        mNames = $.t("config.dates.monthNames").replace(/ ,/g,',').replace(/, /g,',').split(',');
        mNamesShort = $.t("config.dates.monthNamesShort").replace(/ ,/g,',').replace(/, /g,',').split(',');
        dNames = $.t("config.dates.dayNames").replace(/ ,/g,',').replace(/, /g,',').split(',');
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
            
            XDate.locales[language] = {
                monthNames: mNames,
                monthNamesShort: mNamesShort,
                dayNames: dNames,
                dayNamesShort: dNamesShort
            };
            
            XDate.defaultLocale = language;
        }
    };
    
    return obj;

});
