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
    "org/forgerock/commons/ui/common/util/ObjectUtil"
], function (ObjectUtil) {
    QUnit.module('ObjectUtil Functions');

    QUnit.test("toJSONPointerMap", function () {
        var jsonMap = ObjectUtil.toJSONPointerMap({"c": 2, "a": {"b": ['x','y','z',true], "d": undefined }});
        QUnit.equal(jsonMap["/a/b/2"], 'z', "toJSONPointerMap correctly flattens complex object");
        QUnit.ok(!_.has(jsonMap, '/d'), "undefined value not included in map produced by toJSONPointerMap");
    });

    QUnit.test("generatePatchSet", function () {
        var patchDef = ObjectUtil.generatePatchSet({"a": 1, "b": 2}, {"a": 1});
        QUnit.ok(patchDef.length === 1 && patchDef[0].operation === "add" && patchDef[0].field === "/b" && patchDef[0].value === 2, "Simple field addition returned for patchDef");

        patchDef = ObjectUtil.generatePatchSet({"a": 1, "b": 2}, {"c": 1});
        QUnit.equal(patchDef.length, 3, "Expected operation count for removal of one attribute and addition of two others");
    });
});
