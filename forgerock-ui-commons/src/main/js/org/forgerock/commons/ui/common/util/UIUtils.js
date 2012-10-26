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

/*global $, define, window, Handlebars, i18n */

define("org/forgerock/commons/ui/common/util/UIUtils", [
    "org/forgerock/commons/ui/common/util/typeextentions/String",
    "org/forgerock/commons/ui/common/main/AbstractConfigurationAware",
    "handlebars",
    "i18next"
], function (String, AbstractConfigurationAware, handlebars, i18next) {
    var obj = new AbstractConfigurationAware();

    obj.getUrl = function() {
        return window.location.href;    
    };

    obj.getCurrentUrlBasePart = function() {
        return window.location.protocol + "//" + window.location.host;  
    };

    obj.getCurrentUrlQueryParameters = function() {
        return window.location.search.substr(1,window.location.search.lenght);  
    };

    obj.getCurrentPathName = function() {
        return window.location.pathname;
    };

    obj.setUrl = function(url) {
        window.location.href = url; 
    };

    obj.normalizeSubPath = function(subPath) {
        if(subPath.endsWith('/')) {
            return subPath.removeLastChars();
        }
        return subPath;
    };
    
    obj.convertCurrentUrlToJSON = function() {
        var result = {}, parsedQueryParams;

        result.url = obj.getCurrentUrlBasePart();
        result.pathName = obj.normalizeSubPath(obj.getCurrentPathName());
        
        result.params = obj.convertQueryParametersToJSON(obj.getCurrentUrlQueryParameters());
        return result;
    };
    
    obj.convertQueryParametersToJSON = function(queryParameters) {
        var parsedQueryParams;
        
        if(queryParameters) {
            parsedQueryParams = decodeURI(queryParameters.replace(/&/g, "\",\"").replace(/\=/g,"\":\""));
            return JSON.parse('{"' + parsedQueryParams + '"}');
        }
        return null;
    };
    
    obj.templates = {};

    obj.renderTemplate = function(templateUrl, el, data, clb, mode) {
        obj.fillTemplateWithData(templateUrl, data, function(tpl) {
            if(mode === "append") {
                el.append(tpl);
            } else {
                el.html(tpl);
            }

            if(clb) {
                clb();
            }
        });
    };

    obj.fillTemplateWithData = function(templateUrl, data, callback) {
        if(templateUrl) {
            if (obj.templates[templateUrl]) {
                var code = Handlebars.compile(obj.templates[templateUrl])(data);
                                
                if(callback) {
                    callback(code);
                }
                
                return code;
            } else {               
                $.ajax({
                    type: "GET",
                    url: templateUrl,
                    dataType: "html",
                    success: function(template) {
                        if(data === 'unknown' || data === null) {
                            //don't fill the template
                            callback(template);
                        } else {
                            obj.templates[templateUrl] = template;

                            //fill the template
                            callback(Handlebars.compile(template)(data));
                        }
                    },
                    error: callback
                });
            }
        }
    };
    
    obj.reloadTemplate = function(url) {
        $.ajax({
            type: "GET",
            url: url,
            dataType: "html",
            success: function(template) {
                obj.templates[url] = template;
            }
        });
    };
    
    obj.preloadTemplates = function() {
        var url;
        
        for(url in obj.configuration.templateUrls) {
            obj.reloadTemplate(obj.configuration.templateUrls[url]);
        }
    };

    $.fn.emptySelect = function() {
        return this.each(function() {
            if (this.tagName === 'SELECT') {
                this.options.length = 0;
            }
        });
    };

    $.fn.loadSelect = function(optionsDataArray) {
        return this.emptySelect().each(function() {
            if (this.tagName === 'SELECT') {
                var i, option, selectElement = this;
                for(i=0;i<optionsDataArray.length;i++){
                    option = new Option(optionsDataArray[i].value, optionsDataArray[i].key);
                    if ($.browser.msie) {
                        selectElement.add(option);
                    } else {
                        selectElement.add(option, null);
                    }
                }
            }
        });
    };
    
    $.event.special.delayedkeyup = {            
        setup: function( data, namespaces ) {
            $(this).bind("keyup", $.event.special.delayedkeyup.handler);
        },

        teardown: function( namespaces ) {
            $(this).unbind("keyup", $.event.special.delayedkeyup.handler);
        },

        handler: function( event ) {
            var self = this, args = arguments;
            
            event.type = "delayedkeyup";
                
            $.doTimeout('delayedkeyup', 250, function() {
                $.event.handle.apply(self, args);
            });
        }
    };
    
    Handlebars.registerHelper('t', function(i18n_key) {
        var result = i18n.t(i18n_key);
       
        return new Handlebars.SafeString(result);
    });
    
    Handlebars.registerHelper('e', function(context, options) { 
        var paramsCommaSeparated, paramsAfterSplit, params, result;
        paramsCommaSeparated = options.hash.sprintfParams.substr(1, options.hash.sprintfParams.length - 2);
        paramsAfterSplit = paramsCommaSeparated.replace(/'/g,'').split(',');
        params = { postProcess: 'sprintf', sprintf: paramsAfterSplit};
        result = i18n.t(options.hash.key, params);
        return new Handlebars.SafeString(result);
     });
    
    Handlebars.registerHelper('p', function(countValue, options) { 
        var params, result;
        params = { count: countValue };
        result = i18n.t(options.hash.key, params);
        return new Handlebars.SafeString(result);
     });
    
    obj.loadSelectOptions = function(data, el, empty, callback) {
        if( empty === undefined || empty === true ) {
            data = [ {
                "key" : "",
                "value" : $.t("common.form.pleaseSelect")
            } ].concat(data);
            }
                
        el.loadSelect(data);
                
        if(callback) {
            callback(data);
        }
    };
    
    return obj;
});