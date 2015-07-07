/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2015 ForgeRock AS. All rights reserved.
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

/*global define*/

define("org/forgerock/commons/ui/common/util/ObjectUtil", [
    "underscore"
], function (_) {
    /**
     * @exports org/forgerock/commons/ui/common/util/ObjectUtil
     */

    var obj = {};

    /**
     * Translates an arbitrarily-complex object into a flat one composed of JSONPointer key-value pairs.
     * Example:
     *   toJSONPointerMap({"c": 2, "a": {"b": ['x','y','z',true]}}) returns:
     *   {/c: 2, /a/b/0: "x", /a/b/1: "y", /a/b/2: "z", /a/b/3: true}
     * @param {object} originalObject - the object to convert to a flat map of JSONPointer values
     */
    obj.toJSONPointerMap = function (originalObject) {
        var pointerList;
        pointerList = function (obj) {
            return _.chain(obj)
                .pairs()
                .filter(function (p) {
                    return p[1] !== undefined;
                })
                .map(function (p) {
                    if (_.indexOf(["string","boolean","number"], (typeof p[1])) !== -1 || p[1] === null) {
                      return { "pointer": "/" + p[0], "value": p[1]};
                    } else {
                        return _.map(pointerList(p[1]), function (child) {
                            return {"pointer": "/" + p[0] + child.pointer, "value": child.value };
                        });
                    }
                })
                .flatten(true)
                .value();
            };

         return _.reduce(pointerList(originalObject), function (map, entry) {
                  map[entry.pointer] = entry.value;
                  return map;
              }, {});
    };

    /**
     * Compares two objects and generates a patchset necessary to convert the second object to match the first
     * Examples:
     *   generatePatchSet({"a": 1, "b": 2}, {"a": 1}) returns:
     *   [{"operation":"add","field":"/b","value":2}]
     *
     *   generatePatchSet({"a": 1, "b": 2}, {"c": 1}) returns:
     *   [
     *     {"operation":"add","field":"/a","value":1},
     *     {"operation":"add","field":"/b","value":2},
     *     {"operation":"remove","field":"/c"}
     *   ]
     *
     * @param {object} newObject - the object to build up to
     * @param {object} oldObject - the object to start from
     */
    obj.generatePatchSet = function (newObject, oldObject) {
        var newPointerMap = obj.toJSONPointerMap(newObject),
            previousPointerMap = obj.toJSONPointerMap(oldObject),
            newValues = _.chain(newPointerMap)
                         .pairs()
                         .filter(function (p) {
                            return previousPointerMap[p[0]] !== p[1];
                         })
                         .map(function (p) {
                            if (previousPointerMap[p[0]] === undefined) {
                                return { "operation": "add", "field": p[0], "value": p[1] };
                            } else {
                                return { "operation": "replace", "field": p[0], "value": p[1] };
                            }
                         })
                         .value(),
            removedValues = _.chain(previousPointerMap)
                             .pairs()
                             .filter(function (p) {
                                return newPointerMap[p[0]] === undefined;
                             })
                             .map(function (p) {
                                    return { "operation": "remove", "field": p[0] };
                             })
                             .value();

           return newValues.concat(removedValues);
    };

    return obj;
});
