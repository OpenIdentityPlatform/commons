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

/*globals QUnit */

define([
    "lodash",
    "org/forgerock/commons/ui/common/util/ObjectUtil"
], function (_, ObjectUtil) {
    QUnit.module('ObjectUtil Functions');

    QUnit.test("toJSONPointerMap", function (assert) {
        var jsonMap = ObjectUtil.toJSONPointerMap({"c": 2, "a": {"b": ['x','y','z',true], "d": undefined }});
        assert.equal(jsonMap["/c"], '2', "toJSONPointerMap correctly flattens complex object");
        assert.ok(_.isEqual(jsonMap["/a/b"], ['x','y','z',true]),
            "toJSONPointerMap correctly returns a list when it encounters an array");
        assert.ok(!_.has(jsonMap, '/d'), "undefined value not included in map produced by toJSONPointerMap");
    });

    QUnit.test("getValueFromPointer", function (assert) {
        var testObject = {
            testSet: ["apple", "pear"],
            testMap: {"foo":"bar", "hello": "world"}
        };
        assert.equal(ObjectUtil.getValueFromPointer(testObject, "/testMap/foo"), "bar", "/testMap/foo");
        assert.ok(
            _.isEqual(ObjectUtil.getValueFromPointer(testObject, "/testSet"),
            ["apple", "pear"],
            "/testSet"));
        assert.equal(ObjectUtil.getValueFromPointer(testObject, "/test2"), undefined, "/test2");
        assert.equal(ObjectUtil.getValueFromPointer(testObject, "/"), testObject, "/");
    });

    QUnit.test("isEqualSet", function (assert) {
        assert.ok(ObjectUtil.isEqualSet([1], [1]), "Simple set equality");
        assert.ok(!ObjectUtil.isEqualSet([1], [1,3]), "Simple set inequality");
        assert.ok(ObjectUtil.isEqualSet([3,1], [1,3]), "Set equality regardless of order");
        assert.ok(ObjectUtil.isEqualSet([3,{a:1},1], [1,3,{a:1}]), "Set equality with complex items");
        assert.ok(!ObjectUtil.isEqualSet([3,{a:1},1], [1,3,{a:2}]),
            "Set inequality with differing complex items");
        assert.ok(ObjectUtil.isEqualSet([3,{a:1},['b','a'],1], [1,3,{a:1},['a','b']]),
            "Set equality with complex objects, regardless of order, and with nested sets");
    });

    QUnit.test("findItemsNotInSet", function (assert) {
        assert.ok(_.isEqual(ObjectUtil.findItemsNotInSet([1,2,3],[2,3]), [1]), "Simple difference found");
        assert.ok(_.isEqual(ObjectUtil.findItemsNotInSet([1,2,3],[2,3,1]), []),
            "No differences found despite order differences");
        assert.ok(_.isEqual(ObjectUtil.findItemsNotInSet([1,{a:1},3],[3,1,{a:2}]), [{a:1}]),
            "Complex item difference recognized");
        assert.ok(_.isEqual(ObjectUtil.findItemsNotInSet([1,{b:2,a:1},3],[3,1,{a:1,b:2}]), []),
            "Complex item equality recognized, regardless of order");
    });

    QUnit.test("walkDefinedPath", function (assert) {
        var testObject = {test:["apple", {"foo":"bar", "hello": "world"}]};
        assert.equal(ObjectUtil.walkDefinedPath(testObject, "/test/0"), "/test/0", "/test/0");
        assert.equal(ObjectUtil.walkDefinedPath(testObject, "/test/3/foo"), "/test/3", "/test/3/foo");
        assert.equal(ObjectUtil.walkDefinedPath(testObject, "/missing"), "/missing", "/missing");
        assert.equal(ObjectUtil.walkDefinedPath(testObject, "/missing/bar"), "/missing", "/missing/bar");
        assert.equal(ObjectUtil.walkDefinedPath({ } , "/foo"), "/foo", "/foo with empty object");
        assert.equal(ObjectUtil.walkDefinedPath({ foo: undefined } , "/foo"),
            "/foo",
            "/foo as a property with undefined as the value");
        assert.equal(ObjectUtil.walkDefinedPath({ foo: null }, "/foo/bar"),
            "/foo",
            "/foo as a property with null as the value");
        assert.equal(ObjectUtil.walkDefinedPath({ foo: {bar:null} } , "/foo/bar"),
            "/foo/bar",
            "/foo/bar as a property with null as the value");
    });

    QUnit.test("generatePatchSet", function (assert) {
        var patchDef = ObjectUtil.generatePatchSet({"a": 1, "b": 2}, {"a": 1});
        assert.ok(patchDef.length === 1 && patchDef[0].operation === "add" &&
                patchDef[0].field === "/b" && patchDef[0].value === 2,
            "Simple field addition returned for patchDef");

        patchDef = ObjectUtil.generatePatchSet({"a": 1, "b": 2}, {"c": 1});
        assert.equal(patchDef.length, 3,
            "Expected operation count for removal of one attribute and addition of two others");

        patchDef = ObjectUtil.generatePatchSet({
            "setItems": [{"sub": 2}]
        }, {
            "setItems": [{"sub": 1}, {"sub": 2}]
        });
        assert.ok(patchDef.length === 1 && patchDef[0].operation === "remove" &&
                patchDef[0].field === "/setItems" && _.isEqual(patchDef[0].value, {"sub": 1}),
            "Removal of value from set based on value of item");

        /* note that the order of the items isn't relevant; only the content matters */
        patchDef = ObjectUtil.generatePatchSet({
            "setItems": [{"sub": 4}, {"sub": 2}, {"sub": 3}]
        }, {
            "setItems": [{"sub": 3}, {"sub": 2}]
        });

        assert.ok(patchDef.length === 1 && patchDef[0].operation === "add" &&
                patchDef[0].field === "/setItems/-" && _.isEqual(patchDef[0].value, {"sub": 4}),
            "Addition of value to set");

        patchDef = ObjectUtil.generatePatchSet({manager:{_ref: "a/b/c"}},{});
        assert.ok(patchDef.length === 1 && patchDef[0].operation === "add" &&
                patchDef[0].field === "/manager" && _.isEqual(patchDef[0].value, {_ref: "a/b/c"}),
            "Addition of whole new complex property results in full map added");

        patchDef = ObjectUtil.generatePatchSet({manager:null},{manager:{_ref: "a/b/c"}});
        assert.ok(patchDef.length === 1 && patchDef[0].operation === "remove" &&
                patchDef[0].field === "/manager" && !patchDef[0].value,
            "Setting a complex property to null results in a remove operation on the whole object");

        patchDef = ObjectUtil.generatePatchSet({manager:{_ref: "a/b/c"}},{manager:null});
        assert.ok(patchDef.length === 1 && patchDef[0].operation === "replace" &&
                patchDef[0].field === "/manager" && _.isEqual(patchDef[0].value, {_ref: "a/b/c"}),
            "Replacement of null value with whole new complex property results in full map added");

        patchDef = ObjectUtil.generatePatchSet({manager:{_ref: "a/b/d"}},{manager:{_ref: "a/b/c"}});
        assert.ok(patchDef.length === 1 && patchDef[0].operation === "replace" &&
                patchDef[0].field === "/manager/_ref" && patchDef[0].value === "a/b/d",
            "Replacement of simple value in nested map");
    });

});
