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
 * Portions copyright 2014-2015 ForgeRock AS.
 */

/*global define */

define( "org/forgerock/commons/ui/common/main/i18nManager", [
    "jquery",
    "underscore",
    "require",
    "handlebars",
    "i18next"
], function($, _, require, Handlebars, i18next) {

    var obj = {};

    /**
     * Initialises the i18next module.
     *
     * Takes the following options: serverLang, paramLang, defaultLang, and nameSpace.
     * i18nManger with i18next will try to detect the user language and load the corresponding translation in the following order:
     * 1) paramLang which is a query string parameter (&locale=fr).
     * 2) serverLang, a 2 digit language code passed in from server.
     * 3) defaultLang will be the default language set inside the Constants.DEFAULT_LANGUAGE.
     *
     * @param {object} options
     * @param {string} options.paramLang which is a query string parameter, optionally space separated, (&locale=zh fr).
     * @param {string} options.serverLang, a 2 digit language code passed in from server.
     * @param {string} options.defaultLang will be the default language set inside the Constants.DEFAULT_LANGUAGE.
     * @param {string} [options.nameSpace] The nameSpace is optional and will default to "translation"
     */
    obj.init = function(options) {

        Handlebars.registerHelper("t", function(key) {
            var params = {
                    postProcess: "sprintf",
                    sprintf: _.map(_.toArray(arguments).slice(1, -1),
                    Handlebars.Utils.escapeExpression)
                },
                result = $.i18n.t(key, params);
            return new Handlebars.SafeString(result);
        });

        /**
         * @param {object} map Each key in the map is a locale, each value is a string in that locale
         * @example
            {{mapTranslate map}} where map is an object like so:
            {
                "en_GB": "What's your favorite colour?",
                "fr": "Quelle est votre couleur préférée?",
                "en": "What's your favorite color?"
            }
        */
        Handlebars.registerHelper("mapTranslate", function(map) {
            var fallback;
            if (_.has(map, $.i18n.options.lng)) {
                return new Handlebars.SafeString(map[$.i18n.options.lng]);
            } else {
                fallback = _.find($.i18n.options.fallbackLng, function (lng) {
                    return _.has(map, lng);
                });
                return new Handlebars.SafeString(fallback);
            }
        });

        var locales = [],
            opts = {},
            nameSpace = options.nameSpace ? options.nameSpace : "translation";
        if (options.paramLang && options.paramLang.locale) {
            locales = options.paramLang.locale.split(" ");
            options.serverLang = locales.shift();
        }
        if (options.defaultLang) {
            locales.push(options.defaultLang);
        }

        // return if the stored lang matches the new one.
        if (obj.lang !== undefined && obj.lang === options.serverLang) {
           return;
        }
        obj.lang = options.serverLang;

        opts = {
            fallbackLng: locales,
            detectLngQS: "locale",
            useCookie: false,
            getAsync: false,
            lng: options.serverLang,
            load: "current",
            ns: nameSpace,
            nsseparator: ":::",
            resGetPath: require.toUrl("locales/__lng__/__ns__.json")
        };

        $.i18n.init(opts);

    };

    return obj;
});
