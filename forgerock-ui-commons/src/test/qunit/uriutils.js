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
 * Copyright 2015 ForgeRock AS.
 */

define(["org/forgerock/commons/ui/common/util/URIUtils"], function(subject) {
    return {
        executeAll: function (server, parameters) {
            module("org/forgerock/commons/ui/common/util/URIUtils");
            ['getCurrentFragment'].forEach(function(func) {
                test('#' + func, sinon.test(function() {
                    this.stub(subject, 'getCurrentUrl').returns('http://www.example.com/XUI#login');

                    equal(subject[func](), 'login', 'returns fragment');
                }));

                test('#' + func + ' when there is no fragment', sinon.test(function() {
                    this.stub(subject, 'getCurrentUrl').returns('http://www.example.com/XUI');

                    equal(subject[func](), '', 'returns empty string');
                }));
            });

            ['getCurrentCompositeQueryString'].forEach(function(func) {
                test('#' + func + ' when a URI query string is present', sinon.test(function() {
                    this.stub(subject, 'getCurrentUrl').returns('http://www.example.com/XUI');
                    this.stub(subject, 'getCurrentQueryString').returns('key=value');

                    equal(subject[func](), 'key=value', 'returns URI query string');
                }));

                test('#' + func + ' when a fragment query string is present', sinon.test(function() {
                    this.stub(subject, 'getCurrentUrl').returns('http://www.example.com/XUI#login&key=value');

                    equal(subject[func](), 'key=value', 'returns fragment query string');
                }));

                test('#' + func + ' when a URI and fragment query strings are present', sinon.test(function() {
                    this.stub(subject, 'getCurrentUrl').returns('http://www.example.com/XUI#login&key=fragment');
                    this.stub(subject, 'getCurrentQueryString').returns('?key=url');

                    equal(subject[func](), 'key=fragment', 'returns fragment query string');
                }));
            });
        }
    };
});
