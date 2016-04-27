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
 * Copyright 2011-2016 ForgeRock AS.
 */

define("org/forgerock/commons/ui/common/util/ObjectUtil", [
    "lodash"
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
                    if (_.indexOf(["string","boolean","number"], (typeof p[1])) !== -1 || _.isEmpty(p[1])) {
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
     * Uses a JSONPointer string to find a value within a provided object
     * Examples:
     *   getValueFromPointer({test:["apple", {"foo":"bar", "hello": "world"}]}, "/test/0") returns: "apple"
     *   getValueFromPointer({test:["apple", {"foo":"bar", "hello": "world"}]}, "/test/1/foo") returns: "bar"
     *   getValueFromPointer({test:["apple", {"foo":"bar", "hello": "world"}]}, "/test/1") returns:
     *      {"foo":"bar", "hello": "world"}
     *   getValueFromPointer({test:["apple", {"foo":"bar", "hello": "world"}]}, "/test2") returns: undefined
     *   getValueFromPointer({test:["apple", {"foo":"bar", "hello": "world"}]}, "/") returns:
            {test:["apple", {"foo":"bar", "hello": "world"}]}
     *
     * @param {object} object - the object to search within
     * @param {string} pointer - the JSONPointer to use to find the value within the object
     */
    obj.getValueFromPointer = function (object, pointer) {
        var pathParts = pointer.split("/");
        // remove first item which came from the leading slash
        pathParts.shift(1);
        if (pathParts[0] === "") { // the case when pointer is just "/"
            return object;
        }

        return _.reduce(pathParts, function (result, path) {
            if (_.isObject(result)) {
                return result[path];
            } else {
                return result;
            }
        }, object);
    };

    /**
     * Look through the provided object to see how far it can be traversed using a given JSONPointer string
     * Halts at the first undefined entry, or when it has reached the end of the pointer path.
     * Returns a JSONPointer that represents the point at which it was unable to go further
     * Examples:
     *   walkDefinedPath({test:["apple", {"foo":"bar", "hello": "world"}]}, "/test/0") returns: "/test/0"
     *   walkDefinedPath({test:["apple", {"foo":"bar", "hello": "world"}]}, "/test/3/foo") returns: "/test/3"
     *   walkDefinedPath({test:["apple", {"foo":"bar", "hello": "world"}]}, "/missing") returns: "/missing"
     *   walkDefinedPath({test:["apple", {"foo":"bar", "hello": "world"}]}, "/missing/bar") returns: "/missing"
     *
     * @param {object} object - the object to walk through
     * @param {string} pointer - the JSONPointer to use to walk through the object
     */
    obj.walkDefinedPath = function (object, pointer) {
        var finalPath = "",
            node = object,
            currentPathPart,
            pathParts = pointer.split("/");

        // remove first item which came from the leading slash
        pathParts.shift(1);

        // walk through the path, stopping when hitting undefined
        while (node !== undefined && node !== null && pathParts.length) {
            currentPathPart = pathParts.shift(1);
            finalPath += ("/" + currentPathPart);
            node = node[currentPathPart];
        }

        // if the whole object needs to be added....
        if (finalPath === "") {
            finalPath = "/";
        }
        return finalPath;
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
        var newObjectClosure = newObject, // needed to have access to newObject within _ functions
            oldObjectClosure = oldObject, // needed to have access to oldObject within _ functions
            newPointerMap = obj.toJSONPointerMap(newObject),
            previousPointerMap = obj.toJSONPointerMap(oldObject),
            newValues = _.chain(newPointerMap)
                         .pairs()
                         .filter(function (p) {
                            return previousPointerMap[p[0]] !== p[1];
                         })
                         .map(function (p) {
                            var finalPathToAdd = obj.walkDefinedPath(oldObjectClosure, p[0]),
                                operation = (obj.getValueFromPointer(oldObjectClosure, p[0]) === undefined) ? "add" : "replace";
                            return { "operation": operation, "field": finalPathToAdd, "value": obj.getValueFromPointer(newObjectClosure, finalPathToAdd) };
                         })
                         // Filter out duplicates which might result from adding whole containers
                         // Have to stringify the patch operations to do object comparisons with uniq
                         .uniq(JSON.stringify)
                         .value(),
            removedValues = _.chain(previousPointerMap)
                             .pairs()
                             .filter(function (p) {
                                return obj.getValueFromPointer(newObjectClosure, p[0]) === undefined;
                             })
                             .map(function (p) {
                                var finalPathToRemove = obj.walkDefinedPath(newObjectClosure, p[0]);
                                return { "operation": "remove", "field": finalPathToRemove };
                             })
                             // Filter out duplicates which might result from deleting whole containers
                             // Have to stringify the patch operations to do object comparisons with uniq
                             .uniq(JSON.stringify)
                             .value();

           return newValues.concat(removedValues);
    };

    return obj;
});
