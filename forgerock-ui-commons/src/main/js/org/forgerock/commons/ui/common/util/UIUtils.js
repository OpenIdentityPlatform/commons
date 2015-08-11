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
 * Portions copyright 2011-2015 ForgeRock AS.
 */

/*global define, i18n, sessionStorage */

define("org/forgerock/commons/ui/common/util/UIUtils", [
    "jquery",
    "underscore",
    "require",
    "handlebars",
    "i18next",
    "org/forgerock/commons/ui/common/main/AbstractConfigurationAware",
    "org/forgerock/commons/ui/common/util/ModuleLoader",
    "org/forgerock/commons/ui/common/main/Router"
], function ($, _, require, Handlebars, i18next, AbstractConfigurationAware, ModuleLoader, Router) {
    /**
     * @exports org/forgerock/commons/ui/common/util/UIUtils
     */
    var obj = new AbstractConfigurationAware();

    // these functions used to exist in this module, but they were moved to the
    // Router module (they are all route-related, rather than UI)
    // this remains so that old code doesn't just break;
    _.each([
        "getUrl",
        "getCurrentUrlBasePart",
        "getURIFragmentQueryString",
        "getURIQueryString",
        "getCurrentHash",
        "getURIFragment",
        "getCurrentUrlQueryParameters",
        "getCompositeQueryString",
        "getCurrentPathName",
        "setUrl",
        "normalizeSubPath",
        "convertCurrentUrlToJSON",
        "convertQueryParametersToJSON",
        "getParamByName"
    ], function (f) {
        obj[f] = function () {
            console.warn("Deprecated use of UIUtils." + f + "; Update code to use Router." + f);
            return Router[f].apply(this, arguments);
        };
    });

    obj.templates = {};

    obj.renderTemplate = function(templateUrl, el, data, clb, mode, validation) {
        obj.fillTemplateWithData(templateUrl, data, function(tpl) {
            // if we were passed a validation function and it returns false, abort
            if (validation && !validation()) {
                return false;
            }

            if (mode === "append") {
                el.append(tpl);
            } else {
                el.html(tpl);
            }

            if (clb) {
                clb();
            }
        });
    };

    obj.fillTemplateWithData = function(templateUrl, data, callback) {
        if (templateUrl) {
            if (obj.templates[templateUrl]) {
                var code = Handlebars.compile(obj.templates[templateUrl])(data);

                if (callback) {
                    callback(code);
                }

                return code;
            } else {
                $.ajax({
                    type: "GET",
                    url: require.toUrl(templateUrl),
                    dataType: "html",
                    success: function(template) {
                        if (data === "unknown" || data === null) {
                            //don't fill the template
                            if (callback) {
                                callback(template);
                            }
                        } else {
                            obj.templates[templateUrl] = template;

                            //fill the template
                            if (callback) {
                                callback(Handlebars.compile(template)(data));
                            }
                        }
                    },
                    error: callback
                });
            }
        }
    };

    /**
     * Preloads templates for their later usage.
     * @param {(string|string[])} urls - Urls to be preloaded, can be either a string or an array.
     */
    obj.preloadTemplates = function(urls) {
        if (typeof urls === "string") {
            urls = [urls];
        }

        _.each(urls, function(url) {
            obj.reloadTemplate(url).done(function (data) {
                obj.templates[url] = data;
            }).fail(function() {
                console.error("Template \"" + url + "\" failed to loaded");
            });
        });
    };

    obj.reloadTemplate = function(url) {
        return $.ajax({
            type: "GET",
            url: require.toUrl(url),
            dataType: "html"
        });
    };

    /**
     * Loads all the templates defined in the "templateUrls" attribute of this module's configuration.
     */
    obj.preloadInitialTemplates = function() {
        obj.preloadTemplates(obj.configuration.templateUrls);
    };

    /**
     * Loads a Handlebars partial.
     * <p>
     * The registered name for the partial is inferred from the URL specified. e.g.
     * "partials/headers/_Title.html" => "headers/_Title"
     * <p>
     * Will not reload and register partials that are already loaded and registered
     * @param  {String} url URL of partial to load in the format "partials/<path_to_partial>.html"
     * @return {Promise.<Object>|false} Load promise or false if partial is already loaded
     */
    obj.preloadPartial = function(url) {
        var name = url.replace(/(^partials\/)|(\.html$)/g, "");

        if (Handlebars.partials[name]) { return false; }

        return obj.reloadTemplate(url).done(function (data) {
            Handlebars.registerPartial(name, Handlebars.compile(data));
        }).fail(function() {
            console.error("Partial \"" + url + "\" failed to loaded");
        });
    };

    /**
     * Loads all the Handlebars partials defined in the "partialUrls" attribute of this module's configuration
     */
    obj.preloadInitialPartials = function() {
        _.each(obj.configuration.partialUrls, function(url) {
            obj.preloadPartial(url);
        });
    };

    $.fn.emptySelect = function() {
        return this.each(function() {
            if (this.tagName === "SELECT") {
                this.options.length = 0;
            }
        });
    };

    $.fn.loadSelect = function(optionsDataArray) {
        return this.emptySelect().each(function() {
            if (this.tagName === "SELECT") {
                var i, option, selectElement = this;
                for(i=0;i<optionsDataArray.length;i++){
                    option = new Option(optionsDataArray[i].value, optionsDataArray[i].key);
                    selectElement.options[selectElement.options.length] = option;
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

            $.doTimeout("delayedkeyup", 250, function() {
                $.event.handle.apply(self, args);
            });
        }
    };

    Handlebars.registerHelper("url", function(routeKey) {
        var result;
        if (_.isArray(arguments[1])) {
            result = "#" + Router.getLink(Router.configuration.routes[routeKey], arguments[1]);
        } else {
            result = "#" + Router.getLink(Router.configuration.routes[routeKey], _.toArray([arguments[1]]));
        }

        //Don't return a safe string to prevent XSS.
        return result;
    });

    //map should have format key : value
    Handlebars.registerHelper("selectm", function(map, elementName, selectedKey, selectedValue, multiple, height) {
        var result, prePart, postPart, content = "", isSelected, entityName;

        prePart = "<select";

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

    Handlebars.registerHelper("staticSelect", function(value, options){
        var selected = $("<select />").html(options.fn(this));
        selected.find("[value=" + value + "]").attr({"selected":"selected"});

        return selected.html();
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
        var ret = "<div class='checkboxList' id='"+name+"'><ol>", idx,
            sortedMap = _.chain(map)
                            .pairs()
                            .sortBy(function (arr) { return arr[1]; })
                            .value();

        for(idx=0;idx<sortedMap.length;idx++) {
            ret += '<li><input type="checkbox" name="'+ name +'" value="'+ sortedMap[idx][0] +'" id="'+ name +'_'+ encodeURIComponent(sortedMap[idx][0]) +'"><label for="'+ name +'_'+ encodeURIComponent(sortedMap[idx][0]) +'">' + sortedMap[idx][1] + '</label></li>';
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

    Handlebars.registerHelper("each_with_index", function(array, fn) {
        var buffer = "",
            item,
            k=0,
            i=0,
            j=0;

        for (i = 0, j = array.length; i < j; i++) {
            if (array[i]) {
                item = {};
                item.value = array[i];

                // stick an index property onto the item, starting with 0
                item.index = k;

                item.first = (k === 0);
                item.last = (k === array.length);

                // show the inside of the block
                buffer += fn.fn(item);

                k++;
            }
        }

        // return the finished buffer
        return buffer;

    });

    Handlebars.registerHelper('camelCaseToTitle', function(string) {
        var newString = string.replace(/([a-z])([A-Z])/g, '$1 $2');
        return new Handlebars.SafeString(newString[0].toUpperCase() + newString.slice(1));
    });

    Handlebars.registerHelper('stringify', function(string, spaces) {
        spaces = spaces ? spaces : 0 ;
        var newString = JSON.stringify(string, null, spaces);
        return newString;
    });

    Handlebars.registerHelper('ifObject', function(item, options) {
        if(typeof item === 'object') {
            return options.fn(this);
        } else {
            return options.inverse(this);
        }
    });

    /**
     * Handlebars 'routeTo' helper
     * Creates a routing hash will all arguments passed through #encodeURIComponent
     */
    Handlebars.registerHelper('routeTo', function (routeKey) {
        var result = '#',
            args = _.toArray(arguments).slice(1, -1);
        args = _.map(args, function (arg) {
            return encodeURIComponent(arg);
        });

        result += Router.getLink(Router.configuration.routes[routeKey], args);

        return new Handlebars.SafeString(result);
    });

    /**
     * Handlebars "partial" helper
     * @example
     * {{partial this.partialName this}}
     */
    Handlebars.registerHelper("partial", function(name, context) {
        var partial = Handlebars.partials[name];

        if(!partial) {
            console.error("Handlebars \"partial\" helper unable to find partial \"" + name + "\"");
        } else {
            return new Handlebars.SafeString(partial(context));
        }
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


    obj.jqConfirm = function(message, confirmCallback, width){
        ModuleLoader.load("bootstrap-dialog").then(function (BootstrapDialog) {
            BootstrapDialog.show({
                title: $.t('common.form.confirm'),
                type: BootstrapDialog.TYPE_DEFAULT,
                message: message,
                buttons: [
                    {
                        label: $.t('common.form.cancel'),
                        action: function(dialog){
                            dialog.close();
                        }
                    },
                    {
                        label: $.t('common.form.ok'),
                        cssClass: "btn-primary",
                        action: function(dialog) {
                            if(confirmCallback) {
                                confirmCallback();
                            }
                            dialog.close();
                        }
                    }
                ]
            });
        });
     };

    obj.responseMessageMatch = function(error, string){
        var responseMessage = JSON.parse(error).message;
        return responseMessage.indexOf(string) > -1;
    };

    // Registering global mixins

    _.mixin({

        /**
        * findByValues takes a collection and returns a subset made up of objects where the given property name matches a value in the list.
        * @returns {Array} subset of made up of {Object} where there is no match between the given property name and the values in the list.
        * @example
        *
        *    var collections = [
        *        {id: 1, stack: 'am'},
        *        {id: 2, stack: 'dj'},
        *        {id: 3, stack: 'idm'},
        *        {id: 4, stack: 'api'},
        *        {id: 5, stack: 'rest'}
        *    ];
        *
        *    var filtered = _.findByValues(collections, "id", [1,3,4]);
        *
        *    filtered = [
        *        {id: 1, stack: 'am'},
        *        {id: 3, stack: 'idm'},
        *        {id: 4, stack: 'api'}
        *    ]
        *
        */
        "findByValues": function(collection, property, values) {
            return _.filter(collection, function(item) {
                return _.contains(values, item[property]);
            });
        },

        /**
        * Returns subset array from a collection
        * @returns {Array} subset of made up of {Object} where there is no match between the given property name and the values in the list.
        * @example
        *
        *    var filtered = _.removeByValues(collections, "id", [1,3,4]);
        *
        *    filtered = [
        *        {id: 2, stack: 'dj'},
        *        {id: 5, stack: 'rest'}
        *    ]
        *
        */
        "removeByValues": function(collection, property, values) {
            return _.reject(collection, function(item) {
                return _.contains(values, item[property]);
            });
        },

        /**
        * isUrl checks to see if string is a valid URL
        * @returns {Boolean}
        */
        "isUrl": function(string){
            var regexp = /(http|https):\/\/(\w+:{0,1}\w*@)?(\S+)(:[0-9]+)?(\/|\/([\w#!:.?+=&%@!\-\/]))?/;
            return regexp.test(string);
        }

    });

    return obj;
});
