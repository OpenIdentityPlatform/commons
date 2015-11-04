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
 * Copyright 2016 ForgeRock AS.
 */

define([
    "lodash",
    "org/forgerock/commons/ui/common/util/ObjectUtil"
], function (_, ObjectUtil) {
    QUnit.module('ObjectUtil Functions');

    QUnit.test("toJSONPointerMap", function () {
        var jsonMap = ObjectUtil.toJSONPointerMap({"c": 2, "a": {"b": ['x','y','z',true], "d": undefined }});
        QUnit.equal(jsonMap["/a/b/2"], 'z', "toJSONPointerMap correctly flattens complex object");
        QUnit.ok(!_.has(jsonMap, '/d'), "undefined value not included in map produced by toJSONPointerMap");
    });

    QUnit.test("getValueFromPointer", function () {
        var testObject = {test:["apple", {"foo":"bar", "hello": "world"}]};
        QUnit.equal(ObjectUtil.getValueFromPointer(testObject, "/test/0"), "apple", "/test/0");
        QUnit.equal(ObjectUtil.getValueFromPointer(testObject, "/test/1/foo"), "bar", "/test/1/foo");
        QUnit.ok(_.isEqual(ObjectUtil.getValueFromPointer(testObject, "/test/1"), {"foo":"bar", "hello": "world"}), "/test/1");
        QUnit.equal(ObjectUtil.getValueFromPointer(testObject, "/test2"), undefined, "/test2");
        QUnit.equal(ObjectUtil.getValueFromPointer(testObject, "/"), testObject, "/");
    });

    QUnit.test("walkDefinedPath", function () {
        var testObject = {test:["apple", {"foo":"bar", "hello": "world"}]};
        QUnit.equal(ObjectUtil.walkDefinedPath(testObject, "/test/0"), "/test/0", "/test/0");
        QUnit.equal(ObjectUtil.walkDefinedPath(testObject, "/test/3/foo"), "/test/3", "/test/3/foo");
        QUnit.equal(ObjectUtil.walkDefinedPath(testObject, "/missing"), "/missing", "/missing");
        QUnit.equal(ObjectUtil.walkDefinedPath(testObject, "/missing/bar"), "/missing", "/missing/bar");
        QUnit.equal(ObjectUtil.walkDefinedPath({ } , "/foo"), "/foo", "/foo with empty object");
        QUnit.equal(ObjectUtil.walkDefinedPath({ foo: undefined } , "/foo"), "/foo", "/foo as a property with undefined as the value");
        QUnit.equal(ObjectUtil.walkDefinedPath({ foo: null } , "/foo/bar"), "/foo", "/foo as a property with null as the value");
        QUnit.equal(ObjectUtil.walkDefinedPath({ foo: {bar:null} } , "/foo/bar"), "/foo/bar", "/foo/bar as a property with null as the value");
    });

    QUnit.test("generatePatchSet", function () {
        var patchDef = ObjectUtil.generatePatchSet({"a": 1, "b": 2}, {"a": 1});
        QUnit.ok(patchDef.length === 1 && patchDef[0].operation === "add" && patchDef[0].field === "/b" && patchDef[0].value === 2,
            "Simple field addition returned for patchDef");

        patchDef = ObjectUtil.generatePatchSet({"a": 1, "b": 2}, {"c": 1});
        QUnit.equal(patchDef.length, 3, "Expected operation count for removal of one attribute and addition of two others");

        patchDef = ObjectUtil.generatePatchSet({"arrayItem": [undefined, {"sub": 2}]}, {"arrayItem": [{"sub": 1}, {"sub": 2}]});
        QUnit.ok(patchDef.length === 1 && patchDef[0].operation === "remove" && patchDef[0].field === "/arrayItem/0",
            "Removal of nested array value via explicit undefined removes whole array element");

        patchDef = ObjectUtil.generatePatchSet({manager:{_ref: "a/b/c"}},{});
        QUnit.ok(patchDef.length === 1 && patchDef[0].operation === "add" && patchDef[0].field === "/manager" && _.isEqual(patchDef[0].value, {_ref: "a/b/c"}),
            "Addition of whole new complex property results in full map added");

        patchDef = ObjectUtil.generatePatchSet({manager:{_ref: "a/b/c"}},{manager:null});
        QUnit.ok(patchDef.length === 1 && patchDef[0].operation === "replace" && patchDef[0].field === "/manager" && _.isEqual(patchDef[0].value, {_ref: "a/b/c"}),
            "Replacement of null value with whole new complex property results in full map added");

        patchDef = ObjectUtil.generatePatchSet({manager:{_ref: "a/b/d"}},{manager:{_ref: "a/b/c"}});
        QUnit.ok(patchDef.length === 1 && patchDef[0].operation === "replace" && patchDef[0].field === "/manager/_ref" && patchDef[0].value === "a/b/d",
            "Replacement of simple value in nested map");

    });

});
