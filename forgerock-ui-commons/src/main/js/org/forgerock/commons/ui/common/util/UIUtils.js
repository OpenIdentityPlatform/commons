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

/*global $, define, window, Handlebars, i18n, _ */
/*jslint regexp: false*/
define("org/forgerock/commons/ui/common/util/UIUtils", [
    "org/forgerock/commons/ui/common/util/typeextentions/String",
    "org/forgerock/commons/ui/common/main/AbstractConfigurationAware",
    "handlebars",
    "i18next",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/commons/ui/common/util/DateUtil"
], function (String, AbstractConfigurationAware, handlebars, i18next, router, dateUtil) {
    var obj = new AbstractConfigurationAware();

    obj.getUrl = function() {
        return window.location.href;    
    };

    obj.getCurrentUrlBasePart = function() {
        return window.location.protocol + "//" + window.location.host;  
    };

    obj.getCurrentUrlQueryParameters = function() {
        if(window.location.hash.length > 1 && window.location.hash.indexOf('&') > -1){
            return window.location.hash.substring(window.location.hash.indexOf('&') + 1);
        }
        else{
            return window.location.search.substr(1,window.location.search.length);
        }  
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
        if(queryParameters) {
            //create a json object from a query string
            //by taking a query string and splitting it up into individual key=value strings
            return _.object(
                        //queryParameters.match(/([^&]+)/g) returns an array of key value pair strings
                        _.map(queryParameters.match(/([^&]+)/g), function (pair) { 
                           //convert each string into a an array 0 index being the key and 1 index being the value
                           var keyAndValue = pair.match(/([^=]+)=?(.*)/).slice(1);
                               //decode the value
                               keyAndValue[1] = decodeURIComponent(keyAndValue[1]);
                               return keyAndValue;
                        })
                    );
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
    
    Handlebars.registerHelper('t', function(i18nKey) {        
        var params = { postProcess: 'sprintf', sprintf: _.toArray(arguments).slice(1, -1)}, result;
        
        result = i18n.t(i18nKey, params);
        
        return new Handlebars.SafeString(result);
     });
    
    Handlebars.registerHelper('url', function(routeKey) {
        var result = "#" + router.getLink(router.configuration.routes[routeKey], _.toArray([arguments[1]]));
        
        //Don't return a safe string to prevent XSS.
        return result;
    });
    
    //format ISO8601; example: 2012-10-29T10:49:49.419+01:00
    Handlebars.registerHelper('date', function(unformattedDate, datePattern) {
        var date = dateUtil.parseDateString(unformattedDate), formattedDate;
        
        if(!dateUtil.isDateValid(date)) {
            return "";
        }
        
        if (datePattern && _.isString(datePattern)) {
            formattedDate = dateUtil.formatDate(date,datePattern);
        } else {
            formattedDate = dateUtil.formatDate(date);
        }
        
        return new Handlebars.SafeString(formattedDate);
    });
    
    //map should have format key : value
    Handlebars.registerHelper('selectm', function(map, elementName, selectedKey, selectedValue, multiple, height) {
        var result, prePart, postPart, content = "", isSelected, entityName;
        
        prePart = '<select';
        
        if (elementName && _.isString(elementName)) {
            prePart += ' name="' + elementName + '"';
        }
        
        if(multiple) {
            prePart += ' multiple="multiple"';
        }
        
        if(height) {
            prePart += ' style="height: '+ height +'px"';
        }
        
        prePart += '>';
        
        postPart = '</select> ';
        
        for (entityName in map) {
            isSelected = false;
            if (selectedValue && _.isString(selectedValue)) {
                if (selectedValue === map[entityName]) {
                    isSelected = true;
                }
            } else {
                if (selectedKey && selectedKey === entityName) {
                    isSelected = true;
                }
            }
            
            if (isSelected) {
                content += '<option value="' + entityName + '" selected="true">' + $.t(map[entityName]) + '</option>';
            } else {
                content += '<option value="' + entityName + '">' + $.t(map[entityName]) + '</option>';
            }
        }
  
        result = prePart + content + postPart;
        return new Handlebars.SafeString(result);
    });
    
    Handlebars.registerHelper('select', function(map, elementName, selectedKey, selectedValue, additionalParams) {
        var result, prePart, postPart, content = "", isSelected, entityName, entityKey;
        
        if (map && _.isString(map)) {
            map = JSON.parse(map);
        }
        
        if (elementName && _.isString(elementName)) {
            prePart = '<select name="' + elementName + '" ' + additionalParams + '>';
        } else{
            prePart = '<select>';
        }
        
        postPart = '</select> ';
        
        for (entityName in map) {
            isSelected = false;
            if (selectedValue && _.isString(selectedValue) && selectedValue !== '') {
                if (selectedValue === map[entityName]) {
                    isSelected = true;
                }
            } else {
                if (selectedKey && selectedKey !== '' && selectedKey === entityName) {
                    isSelected = true;
                }
            }
            
            if (entityName === '__null') {
                entityKey = '';
            } else {
                entityKey = entityName;
            }
            
            if (isSelected) {
                content += '<option value="' + entityKey + '" selected="true">' + $.t(map[entityName]) + '</option>';
            } else {
                content += '<option value="' + entityKey + '">' + $.t(map[entityName]) + '</option>';
            }
        }

        result = prePart + content + postPart;
        return new Handlebars.SafeString(result);
    });
    
    Handlebars.registerHelper('p', function(countValue, options) { 
        var params, result;
        params = { count: countValue };
        result = i18n.t(options.hash.key, params);
        return new Handlebars.SafeString(result);
     });
    
    
    Handlebars.registerHelper('equals', function(val, val2, options) {
        if(val === val2){
            return options.fn(this);
        }
    });
    
    Handlebars.registerHelper('checkbox', function(map, name, options) {
        var ret = "<div class='checkboxList'><ol>", key;
        
        for(key in map) {
            ret += '<li><input type="checkbox" name="'+ name +'" value="'+ key +'" id="'+ name +'_'+ key +'"><label for="'+ name +'_'+ key +'">' + map[key] + '</label></li>';
        }
        
        ret += "</ol></div>";
        
        return new Handlebars.SafeString(ret);
    });
    
    Handlebars.registerHelper('siteImages', function(images, options) {
        var ret = "", i;
        
        for(i = 0; i < images.length; i++) {
            ret += '<img class="item" src="' + encodeURI(images[i]) +'" data-site-image="'+ encodeURI(images[i]) +'" />';
        }
        
        return new Handlebars.SafeString(ret);
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