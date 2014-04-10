/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2014 ForgeRock AS. All Rights Reserved
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

/*global define, localStorage */

/**
 * Local storage helper.
 *
 * @author Eugenia Sergueeva
 */

define("org/forgerock/mock/ui/common/main/LocalStorage", function () {
    var mockPrefix = 'forgerock-mock-';

    function isLocalStorageSupported() {
        return typeof localStorage !== 'undefined';
    }

    return isLocalStorageSupported() ? {
        /**
         * Adds data.
         *
         * @param key
         * @param data
         * @returns {Object} newly added data
         */
        add: function (key, data) {
            if (!this.get(key)) {
                console.log('Adding item to localStorage: ' + data);
                localStorage.setItem(mockPrefix + key, JSON.stringify(data));
                return key;
            }

            return null;
        },

        /**
         * Patches data.
         *
         * @param key
         * @param data
         * @returns {Object} patched data
         */
        patch: function (key, data) {
            var item = this.get(key),
                dataLength = data.length,
                i;

            if (item) {
                for (i = 0; i < dataLength; i++) {
                    item[data[i].field] = data[i].value;
                }
                localStorage.setItem(mockPrefix + key, JSON.stringify(item));
            }
            return item;
        },

        /**
         * Gets data by key.
         *
         * @param key
         * @returns {Object}
         */
        get: function (key) {
            return JSON.parse(localStorage.getItem(mockPrefix + key));
        },

        /**
         * Removes data by key.
         *
         * @param key
         * @returns {boolean} whether data was removed
         */
        remove: function (key) {
            return delete localStorage[mockPrefix + key];
        }
    } : {
        add: function () {
            console.log('LocalStorage is not supported');
        },

        patch: function () {
            console.log('LocalStorage is not supported');
        },

        get: function () {
            console.log('LocalStorage is not supported');
        },

        remove: function () {
            console.log('LocalStorage is not supported');
        }
    };
});